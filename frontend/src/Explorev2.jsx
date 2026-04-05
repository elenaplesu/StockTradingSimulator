import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { API_BASE_URL } from './config';

export default function Explore({ userId }) {
    const [symbol, setSymbol] = useState('');
    const [stockData, setStockData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [quantity, setQuantity] = useState('');
    const [notifications, setNotifications] = useState([]);
    const [chartData, setChartData] = useState([]);
    const [isPolling, setIsPolling] = useState(false);
    const [timeRange, setTimeRange] = useState('1D');

    const addNotification = (message, isError) => {
        const id = Date.now();
        setNotifications(prev => [...prev, { id, message, isError }]);

        // Remove notification after 5 seconds
        setTimeout(() => {
            setNotifications(prev => prev.filter(notification => notification.id !== id));
        }, 5000);
    };

    const formatAndPadData = (apiHistory, livePrice, range) => {
        if (!apiHistory || apiHistory.length === 0) return [];

        let finalData = [...apiHistory];
        const lastHistoryPoint = finalData[finalData.length - 1];
        const now = Date.now();

        // Add Live Price Dot (only if the market is actively open)
        if (isMarketCurrentlyOpen) {
            finalData.push({
                timestamp: now,
                price: livePrice
            });
        }

        // PADDING LOGIC (For both 1D and 5D)
        if (isMarketCurrentlyOpen) {
            // Use 5-min steps for 1D, and 15-min steps for 5D
            const stepMs = range === '1D' ? 5 * 60 * 1000 : 15 * 60 * 1000;
            let currentPadTime = now + stepMs;

            const lastDateString = new Date(lastHistoryPoint.timestamp).toDateString();
            const firstPointOfCurrentDay = finalData.find(p =>
                new Date(p.timestamp).toDateString() === lastDateString
            );

            if (firstPointOfCurrentDay) {
                // A standard trading day is exactly 6.5 hours
                const marketCloseTime = firstPointOfCurrentDay.timestamp + (6.5 * 60 * 60 * 1000);

                // Fill the remainder of TODAY with blank space
                while (currentPadTime <= marketCloseTime) {
                    finalData.push({
                        timestamp: currentPadTime,
                        price: null
                    });
                    currentPadTime += stepMs;
                }
            }
        }

        finalData.sort((a, b) => a.timestamp - b.timestamp);
        return finalData;
    };

    // CORE FETCHER
    const fetchChartData = (targetSymbol, targetRange, isBackgroundUpdate = false) => {
        if (!isBackgroundUpdate) {
            setStockData(null);
            setChartData([]);
            setLoading(true);
            setError(null);
            setIsPolling(false);
        }

        fetch(`${API_BASE_URL}/api/stocks/${targetSymbol.toUpperCase()}/history?range=${targetRange}`)
            .then(res => res.ok ? res.json() : [])
            .then(historyData => {
                fetch(`${API_BASE_URL}/api/stocks/${targetSymbol.toUpperCase()}`)
                    .then(response => {
                        if (!response.ok) throw new Error("Stock not found");
                        return response.json();
                    })
                    .then(liveData => {
                        setStockData(liveData);
                        const livePrice = liveData.currentPrice || liveData.price;

                        const processedData = formatAndPadData(historyData, livePrice, targetRange);
                        setChartData(processedData);

                        if (!isBackgroundUpdate) {
                            setLoading(false);
                            setIsPolling(true);
                        }
                    })
                    .catch(err => {
                        if (!isBackgroundUpdate) {
                            setError("Could not find that stock.");
                            setLoading(false);
                        }
                    });
            }).catch(err => {
            if (!isBackgroundUpdate) {
                setError("Failed to fetch historical data.");
                setLoading(false);
            }
        });
    };

    const handleSearch = (e) => {
        e.preventDefault();
        if (!symbol) return;
        fetchChartData(symbol, timeRange, false);
    };

    const handleRangeChange = (newRange) => {
        setTimeRange(newRange);
        if (symbol && stockData) fetchChartData(symbol, newRange, false);
    };

    // FAST LOOP: Every 5 seconds
    useEffect(() => {
        let fastInterval;
        let isMounted = true;
        if (isPolling && symbol) {
            fastInterval = setInterval(() => {
                fetch(`${API_BASE_URL}/api/stocks/${symbol.toUpperCase()}`)
                    .then(res => res.ok ? res.json() : null)
                    .then(liveData => {
                        if (isMounted && liveData) {
                            // 1. Update the Banner Data
                            setStockData(liveData);
                            const realPrice = liveData.currentPrice || liveData.price;

                            // 2. Advance the Chart Dot
                            setChartData(prev => {
                                const newData = [...prev];
                                const now = Date.now();
                                let lastValidIdx = -1;

                                //  index of the dot we are currently wiggling
                                for(let i = 0; i < newData.length; i++) {
                                    if (newData[i].price !== null) lastValidIdx = i;
                                }

                                if (lastValidIdx !== -1) {
                                    const nextIdx = lastValidIdx + 1;

                                    // Check if the NEXT blank slot exists AND time has passed its required timestamp
                                    if (nextIdx < newData.length && now >= newData[nextIdx].timestamp) {
                                        newData[nextIdx] = { ...newData[nextIdx], price: realPrice };
                                    }
                                }
                                return newData;
                            });
                        }
                    }).catch(console.error);
            }, 5 * 1000);
        }
        return () => {
            isMounted = false;
            clearInterval(fastInterval);
        }
    }, [isPolling, symbol]);

    const handleBuy = () => {
        if (!quantity || quantity <= 0) {
            addNotification("Please enter a valid quantity.", true);
            return;
        }
        const payload = { userId, symbol: stockData.symbol, quantity: parseInt(quantity) };
        fetch(`${API_BASE_URL}/api/trade/buy`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
            .then(async res => {
                const text = await res.text();
                if (!res.ok) throw new Error(text);
                return text;
            })
            .then(msg => {
                addNotification(msg, false);
                setQuantity('');
            })
            .catch(err => {
                addNotification(err.message, true);
            });
    };

    // MATH FOR CHART COLORS & STATUS
    const validPoints = chartData.filter(p => p.price !== null);
    const firstPrice = validPoints.length > 0 ? validPoints[0].price : 0;
    const currentPrice = validPoints.length > 0 ? validPoints[validPoints.length - 1].price : 0;
    const isTrendingUp = currentPrice >= firstPrice;

    const chartColor = isTrendingUp ? '#198754' : '#dc3545';

    // Assume closed if the last point we plotted is older than 30 minutes
    const isMarketCurrentlyOpen = validPoints.length > 0
        ? validPoints[validPoints.length - 1].timestamp >= (Date.now() - 30 * 60 * 1000)
        : false;

    return (
        <div className="container mt-5 d-flex flex-column align-items-center text-center">
            <h2 className="fw-bold mb-3">Explore the Market</h2>
            <div className="w-100 mb-4" style={{ maxWidth: '500px' }}>
                <form onSubmit={handleSearch} className="d-flex gap-2">
                    <input type="text" className="form-control form-control-lg shadow-sm" placeholder="Enter ticker (e.g. AAPL)" value={symbol} onChange={(e) => setSymbol(e.target.value)} />
                    <button type="submit" className="btn btn-primary btn-lg shadow-sm px-4">Search</button>
                </form>
            </div>

            <div style={{
                position: 'fixed',
                bottom: '20px',
                right: '20px',
                zIndex: 9999,
                display: 'flex',
                flexDirection: 'column',
                gap: '10px',
                alignItems: 'flex-end',
                width: '350px'
            }}>
                {notifications.map((notification) => (
                    <div
                        key={notification.id}
                        className={`alert ${notification.isError ? 'alert-danger' : 'alert-success'} shadow-lg alert-dismissible fade show m-0`}
                        style={{
                            width: '350px',
                            minHeight: '80px',
                            maxHeight: '80px',
                            borderLeft: `5px solid ${notification.isError ? '#dc3545' : '#198754'}`,
                            animation: 'slideIn 0.3s ease-out',
                            position: 'relative',
                            opacity: 1,
                            padding: '12px 35px 12px 15px',
                            display: 'flex',
                            flexDirection: 'column',
                            justifyContent: 'center',
                            overflow: 'hidden'
                        }}
                        role="alert"
                    >
                        <div className="fw-bold mb-1" style={{ fontSize: '14px' }}>
                            {notification.isError ? 'Purchase Failed' : 'Purchase Successful'}
                        </div>
                        <div style={{
                            fontSize: '12px',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap'
                        }}>
                            {notification.message}
                        </div>

                        <button
                            type="button"
                            className="btn-close"
                            style={{ position: 'absolute', right: '10px', top: '10px' }}
                            onClick={() => setNotifications(prev => prev.filter(n => n.id !== notification.id))}
                        ></button>
                    </div>
                ))}
            </div>

            <style jsx>{`
                @keyframes slideIn {
                    from {
                        transform: translateX(100%);
                        opacity: 0;
                    }
                    to {
                        transform: translateX(0);
                        opacity: 1;
                    }
                }
            `}</style>

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
                                    <h1 className="fw-bold mb-0" style={{ color: chartColor }}>${(stockData.currentPrice || stockData.price)?.toFixed(2)}</h1>
                                    {!isMarketCurrentlyOpen && (
                                        <span className="badge bg-secondary opacity-75" style={{fontSize: '0.7rem'}}>MARKET CLOSED</span>
                                    )}
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
        </div>
    );
}