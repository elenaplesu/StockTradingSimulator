import {useEffect, useState} from 'react';
import {Link} from 'react-router-dom';
import {Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis} from 'recharts';
import {API_BASE_URL, getCsrfToken} from './config';
import {useNotifications} from './useNotifications';
import NotificationContainer from './NotificationContainer';
import Holidays from 'date-holidays';

const isMarketHoliday = (date) => {
    const hd = new Holidays('US', 'ny');
    const holidays = hd.getHolidays(date.getFullYear());

    const holidayCheck = hd.isHoliday(date);

    const isGoodFriday = holidays.find(h =>
        h.name === 'Good Friday' &&
        new Date(h.date).toDateString() === date.toDateString()
    );

    return (holidayCheck && holidayCheck.type === 'public') || !!isGoodFriday;
};

const checkNYMarketOpen = () => {
    const nyString = new Date().toLocaleString("en-US", {
        timeZone: "America/New_York",
        hour12: false
    });
    const nyDate = new Date(nyString);

    const day = nyDate.getDay();
    if (day === 0 || day === 6) return false;

    const hd = new Holidays('US', 'ny');
    const holidayCheck = hd.isHoliday(nyDate);

    if (holidayCheck && holidayCheck.type === 'public') return false;

    const holidays = hd.getHolidays(nyDate.getFullYear());
    const isGoodFriday = holidays.find(h =>
        h.name === 'Good Friday' &&
        new Date(h.date).toDateString() === nyDate.toDateString()
    );
    if (isGoodFriday) return false;

    const hours = nyDate.getHours();
    const minutes = nyDate.getMinutes();
    const currentTimeAsMinutes = hours * 60 + minutes;

    return currentTimeAsMinutes >= (9 * 60 + 30) && currentTimeAsMinutes < (16 * 60);
};

export default function Explore({ userId }) {
    const [symbol, setSymbol] = useState('');
    const [stockData, setStockData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [quantity, setQuantity] = useState('');
    const [chartData, setChartData] = useState([]);
    const [isPolling, setIsPolling] = useState(false);
    const [timeRange, setTimeRange] = useState('1D');

    const { notifications, addNotification, removeNotification } = useNotifications();

    const formatAndPadData = (apiHistory, livePrice, range, isOpen) => {
        if (!apiHistory || apiHistory.length === 0) return [];

        let finalData = [...apiHistory];
        const lastHistoryPoint = finalData[finalData.length - 1];
        const now = Date.now();

        if (isOpen) {
            finalData.push({ timestamp: now, price: livePrice });

            const stepMs = range === '1D' ? 5 * 60 * 1000 : 15 * 60 * 1000;
            let currentPadTime = lastHistoryPoint.timestamp + stepMs;

            const lastDate = new Date(lastHistoryPoint.timestamp);
            const midnightUTC = Date.UTC(lastDate.getUTCFullYear(), lastDate.getUTCMonth(), lastDate.getUTCDate());
            const marketCloseUTC = midnightUTC + (20 * 60 * 60 * 1000);

            if (now <= marketCloseUTC) {
                while (currentPadTime <= marketCloseUTC) {
                    if (currentPadTime > now) {
                        finalData.push({ timestamp: currentPadTime, price: null });
                    }
                    currentPadTime += stepMs;
                }
            }
        }
        return finalData.sort((a, b) => a.timestamp - b.timestamp);
    };

    const fetchChartData = (targetSymbol, targetRange, isBackgroundUpdate = false) => {
        if (!isBackgroundUpdate) {
            setStockData(null);
            setChartData([]);
            setLoading(true);
            setError(null);
            setIsPolling(false);
        }

        const symbolUpper = targetSymbol.toUpperCase();

        fetch(`${API_BASE_URL}/api/stocks/${symbolUpper}/history?range=${targetRange}`, { credentials: 'include' })
            .then(res => res.ok ? res.json() : [])
            .then(historyData => {
                fetch(`${API_BASE_URL}/api/stocks/${symbolUpper}`, { credentials: 'include' })
                    .then(response => {
                        if (!response.ok) throw new Error("Stock not found");
                        return response.json();
                    })
                    .then(liveData => {
                        const livePrice = liveData.currentPrice || liveData.price;
                        const isOpen = checkNYMarketOpen();

                        setStockData(liveData);
                        setChartData(formatAndPadData(historyData, livePrice, targetRange, isOpen));

                        if (!isBackgroundUpdate) {
                            setLoading(false);
                            setIsPolling(isOpen);
                        }
                    })
                    .catch(() => {
                        if (!isBackgroundUpdate) {
                            setError("Could not find that stock.");
                            setLoading(false);
                        }
                    });
            })
            .catch(() => {
                if (!isBackgroundUpdate) {
                    setError("Failed to fetch historical data.");
                    setLoading(false);
                }
            });
    };

    useEffect(() => {
        if (!isPolling || !symbol) return;

        let isMounted = true;
        const interval = setInterval(() => {
            if (!checkNYMarketOpen()) {
                setIsPolling(false);
                return;
            }

            fetch(`${API_BASE_URL}/api/stocks/${symbol.toUpperCase()}`, { credentials: 'include' })
                .then(res => res.ok ? res.json() : null)
                .then(liveData => {
                    if (!isMounted || !liveData) return;

                    setStockData(liveData);
                    const realPrice = liveData.currentPrice || liveData.price;

                    setChartData(prev => {
                        const now = Date.now();
                        const newData = [...prev];
                        let lastValidIdx = -1;

                        for (let i = 0; i < newData.length; i++) {
                            if (newData[i].price !== null) lastValidIdx = i;
                        }

                        if (lastValidIdx === -1) return newData;

                        const nextIdx = lastValidIdx + 1;
                        if (nextIdx < newData.length && now >= newData[nextIdx].timestamp) {
                            newData[nextIdx] = { ...newData[nextIdx], price: realPrice };
                        }

                        if (!newData.some(p => p.price === null)) setIsPolling(false);

                        return newData;
                    });
                })
                .catch(console.error);
        }, 5000);

        return () => {
            isMounted = false;
            clearInterval(interval);
        };
    }, [isPolling, symbol, timeRange]);

    const validPoints = chartData.filter(p => p.price !== null);
    const firstPrice = validPoints.length > 0 ? validPoints[0].price : 0;
    const currentPrice = validPoints.length > 0 ? validPoints[validPoints.length - 1].price : 0;
    const isTrendingUp = currentPrice >= firstPrice;
    const chartColor = isTrendingUp ? '#198754' : '#dc3545';
    const marketCurrentlyOpen = checkNYMarketOpen();

    const handleSearch = (e) => {
        e.preventDefault();
        if (symbol) fetchChartData(symbol, timeRange, false);
    };

    const handleRangeChange = (newRange) => {
        setTimeRange(newRange);
        if (symbol && stockData) fetchChartData(symbol, newRange, false);
    };

    const handleBuy = () => {
        if (!quantity || quantity <= 0) {
            addNotification("Please enter a valid quantity.", true);
            return;
        }
        const payload = { userId, symbol: stockData.symbol, quantity: parseInt(quantity) };
        fetch(`${API_BASE_URL}/api/trade/buy`, {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': getCsrfToken()
            },
            body: JSON.stringify(payload)
        })
            .then(async res => {
                const data = await res.json();
                if (!res.ok) throw new Error(data.message || "Purchase failed");
                addNotification(data.message, false);
                setQuantity('');
            })
            .catch(err => addNotification(err.message, true));
    };

    return (
        <div className="container mt-5 d-flex flex-column align-items-center text-center">
            <h2 className="fw-bold mb-3">Explore the Market</h2>
            <div className="w-100 mb-4" style={{ maxWidth: '500px' }}>
                <form onSubmit={handleSearch} aria-label="stock search" className="d-flex gap-2">
                    <input
                        type="text"
                        className="form-control form-control-lg shadow-sm"
                        placeholder="Enter ticker (e.g. AAPL)"
                        value={symbol}
                        onChange={(e) => setSymbol(e.target.value)}
                    />
                    <button type="submit" className="btn btn-primary btn-lg shadow-sm px-4">Search</button>
                </form>
            </div>

            <NotificationContainer notifications={notifications} removeNotification={removeNotification} />

            {loading && (
                <div className="text-muted mb-4">
                    <span className="spinner-border spinner-border-sm me-2"></span>
                    Fetching live data...
                </div>
            )}

            {error && (
                <div className="alert alert-danger w-100 shadow-sm mb-4" style={{ maxWidth: '500px' }}>
                    {error}
                </div>
            )}

            {stockData && chartData.length > 0 && (
                <div className="card shadow border-0 w-100 mb-5" style={{ maxWidth: '900px' }}>
                    <div className="card-body bg-light rounded p-4">
                        <div className="row">
                            <div className="col-md-7 mb-4 mb-md-0" style={{ minHeight: '350px', minWidth: '0' }}>
                                <div className="d-flex justify-content-between align-items-center mb-3">
                                    <h5 className="text-muted text-start mb-0">Price Chart</h5>
                                    <div className="btn-group btn-group-sm shadow-sm">
                                        <button
                                            className={`btn ${timeRange === '1D' ? 'btn-primary' : 'btn-outline-primary'}`}
                                            onClick={() => handleRangeChange('1D')}
                                        >1D</button>
                                        <button
                                            className={`btn ${timeRange === '1W' ? 'btn-primary' : 'btn-outline-primary'}`}
                                            onClick={() => handleRangeChange('1W')}
                                        >5D</button>
                                    </div>
                                </div>
                                <div style={{ width: '100%', height: 300 }}>
                                    <ResponsiveContainer width="100%" height="100%">
                                        <AreaChart data={chartData}>
                                            <defs>
                                                <linearGradient id="fade" x1="0" y1="0" x2="0" y2="1">
                                                    <stop offset="5%" stopColor={chartColor} stopOpacity={0.3}/>
                                                    <stop offset="95%" stopColor={chartColor} stopOpacity={0}/>
                                                </linearGradient>
                                            </defs>
                                            <CartesianGrid strokeDasharray="3 3" vertical={false} opacity={0.3} />
                                            <XAxis
                                                dataKey="timestamp"
                                                type="category"
                                                tickFormatter={(t) => {
                                                    const date = new Date(t);
                                                    if (timeRange === '1W') return date.toLocaleDateString([], { weekday: 'short' });
                                                    return date.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
                                                }}
                                                tick={{fontSize: 11}}
                                                minTickGap={40}
                                                axisLine={false}
                                                tickLine={false}
                                            />
                                            <YAxis domain={['auto', 'auto']} orientation="right" tick={{fontSize: 11}} axisLine={false} tickLine={false} />
                                            <Tooltip
                                                labelFormatter={(t) => new Date(t).toLocaleString()}
                                                formatter={(value) => value ? [`$${value.toFixed(2)}`, "Price"] : []}
                                                contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px rgba(0,0,0,0.1)' }}
                                            />
                                            <Area
                                                type="monotone"
                                                dataKey="price"
                                                stroke={chartColor}
                                                strokeWidth={3}
                                                fill="url(#fade)"
                                                connectNulls={false}
                                                isAnimationActive={false}
                                            />
                                        </AreaChart>
                                    </ResponsiveContainer>
                                </div>
                            </div>
                            <div className="col-md-5 d-flex flex-column justify-content-center border-start px-4">
                                <h3 className="fw-bold mb-1">{stockData.symbol}</h3>
                                <div className="d-flex align-items-center justify-content-center gap-2">
                                    <h1 className="fw-bold mb-0" style={{ color: chartColor }}>
                                        ${(stockData.currentPrice || stockData.price)?.toFixed(2)}
                                    </h1>
                                    {!marketCurrentlyOpen && <span className="badge bg-secondary opacity-75" style={{fontSize: '0.7rem'}}>MARKET CLOSED</span>}
                                </div>
                                <p className={`fw-bold mb-4 ${isTrendingUp ? 'text-success' : 'text-danger'}`}>
                                    {isTrendingUp ? '▲' : '▼'} {Math.abs(((currentPrice - firstPrice) / firstPrice) * 100).toFixed(2)}% {timeRange === '1D' ? 'Today' : 'Past 5 Days'}
                                </p>
                                {userId ? (
                                    <div className="text-start">
                                        <label className="form-label fw-bold text-muted">Shares to Buy</label>
                                        <input type="number" className="form-control form-control-lg mb-3" placeholder="0" value={quantity} onChange={(e) => setQuantity(e.target.value)} />
                                        <button className="btn btn-lg w-100 fw-bold text-white shadow-sm" style={{ backgroundColor: chartColor, borderColor: chartColor }} onClick={handleBuy}>Buy {stockData.symbol}</button>
                                    </div>
                                ) : (
                                    <div className="p-3 bg-white border rounded text-center"><p className="text-muted mb-0">Want to trade? <Link to="/login" className="fw-bold text-primary text-decoration-none">Log in</Link></p></div>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            )}
            <style>{`
                @keyframes slideIn {
                    from { transform: translateX(100%); opacity: 0; }
                    to { transform: translateX(0); opacity: 1; }
                }
            `}</style>
        </div>
    );
}