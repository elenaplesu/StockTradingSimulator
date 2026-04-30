import { useState, useEffect } from 'react';
import { API_BASE_URL, getCsrfToken } from './config';
import { useNotifications } from './useNotifications';
import NotificationContainer from './NotificationContainer';

export default function Portfolio({userId}) {
    const [data, setData] = useState(null);
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [transactionsLoading, setTransactionsLoading] = useState(true);
    const [transactionError, setTransactionError] = useState(false);
    const [sellQuantities, setSellQuantities] = useState({});

    const { notifications, addNotification, removeNotification } = useNotifications();

    const fetchPortfolio = () => {

        fetch(`${API_BASE_URL}/api/portfolio/${userId}/analytics`, {credentials: 'include'})
            .then(response => {
                if (!response.ok) throw new Error("Failed to load analytics");
                return response.json();
            })
            .then(analyticsData => {
                setData(analyticsData);
                setLoading(false);
            })
            .catch(error => {
                console.error("Error fetching data:", error);
                setLoading(false);
            });

        fetch(`${API_BASE_URL}/api/portfolio/${userId}/transactions`,{credentials: 'include'})
            .then(response => {
                if (!response.ok) throw new Error("Failed to load transactions");
                return response.json();
            })
            .then(historyData => {
                setTransactions(historyData);
                setTransactionsLoading(false);
                setTransactionError(false);
            })
            .catch(error => {
                console.error("Error fetching transactions:", error);
                setTransactionsLoading(false);
                setTransactionError(true);
            });
    };

    useEffect(() => {
        if (userId) {
            fetchPortfolio();

            const intervalId = setInterval(() => {
                fetchPortfolio();
            }, 20 * 1000);

            return () => clearInterval(intervalId);
        }
    }, [userId]);

    const handleSell = (symbol) => {
        const qty = parseInt(sellQuantities[symbol]);
        if (!qty || qty <= 0) {
            addNotification(`Please enter a valid quantity to sell for ${symbol}.`, true);
            return;
        }

        const payload = { userId, symbol, quantity: qty };

        fetch(`${API_BASE_URL}/api/trade/sell`, {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': getCsrfToken()},
            body: JSON.stringify(payload)
        })
            .then(async response => {
                const data = await response.json();
                if (!response.ok) throw new Error(data.message || "Sale failed");
                return data.message;
            })
            .then(message => {
                addNotification(message, false);
                setSellQuantities(prev => ({...prev, [symbol]: ''}));
                fetchPortfolio();
            })
            .catch(err => {
                addNotification(err.message, true);
            });
    };

    if (loading) return <div className="container mt-5"><h4><span className="spinner-border spinner-border-sm me-2"></span>Loading Quantitative Analytics...</h4></div>;
    if (!data) return <div className="container mt-5"><h4>Error: Could not load data. Ensure your Java server is running!</h4></div>;

    return (
        <div className="container mt-5 mb-5">
            <h2 className="fw-bold">Portfolio Analytics Terminal</h2>
            <hr />

            <div className="row mt-4 mb-4">
                <div className="col-md-3 mb-3">
                    <div className="card text-white bg-success h-100 shadow-sm">
                        <div className="card-header border-0 pb-0 fw-bold">Available Cash</div>
                        <div className="card-body d-flex align-items-center">
                            <h2 className="card-title mb-0">${Number(data.cashBalance).toLocaleString('en-US', {minimumFractionDigits: 2, maximumFractionDigits: 2})}</h2>
                        </div>
                    </div>
                </div>
                <div className="col-md-3 mb-3">
                    <div className="card text-white bg-primary h-100 shadow-sm">
                        <div className="card-header border-0 pb-0 fw-bold">Total Net Worth</div>
                        <div className="card-body d-flex align-items-center">
                            <h2 className="card-title mb-0">${Number(data.netWorth).toLocaleString('en-US', {minimumFractionDigits: 2, maximumFractionDigits: 2})}</h2>
                        </div>
                    </div>
                </div>
                <div className="col-md-3 mb-3">
                    <div className={`card text-white h-100 shadow-sm ${Number(data.aggregateROI) >= 0 ? 'bg-success' : 'bg-danger'}`}>
                        <div className="card-header border-0 pb-0 fw-bold">Aggregate ROI</div>
                        <div className="card-body d-flex align-items-center">
                            <h2 className="card-title mb-0">
                                {Number(data.aggregateROI) >= 0 ? '+' : ''}{Number(data.aggregateROI).toFixed(2)}%
                            </h2>
                        </div>
                    </div>
                </div>
                <div className="col-md-3 mb-3">
                    <div className="card text-white bg-dark h-100 shadow-sm">
                        <div className="card-header border-0 pb-0 fw-bold">HHI / Variance</div>
                        <div className="card-body d-flex flex-column justify-content-center">
                            <h3 className="card-title mb-0">
                                {Number(data.hhi).toFixed(0)} <span className="fs-6 text-muted">HHI</span>
                            </h3>
                            <small className="text-info mt-1">Var: {Number(data.crossSectionalVariance).toFixed(4)}</small>
                        </div>
                    </div>
                </div>
            </div>

            <NotificationContainer notifications={notifications} removeNotification={removeNotification} />

            <h4 className="mt-4 fw-bold text-muted">Quantitative Asset Breakdown</h4>
            <div className="table-responsive shadow-sm rounded border bg-white mb-5">
                <table className="table table-hover align-middle mt-2 mb-0">
                    <thead className="table-light text-muted">
                    <tr>
                        <th>Asset</th>
                        <th>Weight</th>
                        <th>Shares</th>
                        <th>Avg. Cost</th>
                        <th>Live Price</th>
                        <th>Total Return</th>
                        <th>Action</th>
                    </tr>
                    </thead>
                    <tbody>
                    {data.holdings.length === 0 ? (
                        <tr>
                            <td colSpan="7" className="text-center py-5 text-muted">
                                You don't own any stocks yet. Go to the <strong>Explore</strong> tab to deploy capital!
                            </td>
                        </tr>
                    ) : (
                        data.holdings.map((stock) => {

                            const totalCost = Number(stock.quantity) * Number(stock.averageBuyPrice);
                            const rawDollarReturn = Number(stock.totalValue) - totalCost;

                            const cleanDollarReturn = Number(rawDollarReturn.toFixed(2));
                            const cleanROI = Number(Number(stock.returnOnInvestment).toFixed(2));

                            const isPositive = cleanDollarReturn > 0;
                            const isNegative = cleanDollarReturn < 0;

                            const returnColorClass = isPositive ? 'text-success' : isNegative ? 'text-danger' : 'text-muted';
                            const returnSign = isPositive ? '+' : isNegative ? '-' : '';

                            const displayDollar = Math.abs(cleanDollarReturn).toFixed(2);
                            const displayROI = Math.abs(cleanROI).toFixed(2);

                            return (
                                <tr key={stock.symbol}>

                                    <td><strong className="fs-5">{stock.symbol}</strong></td>

                                    <td>
                                        <div className="d-flex align-items-center gap-2">
                                            <span className="fw-bold">{Number(stock.weightPercentage).toFixed(1)}%</span>
                                            <div className="progress" style={{width: '50px', height: '6px'}}>
                                                <div className="progress-bar bg-primary" style={{width: `${Number(stock.weightPercentage)}%`}}></div>
                                            </div>
                                        </div>
                                    </td>

                                    <td>{stock.quantity}</td>

                                    <td>${Number(stock.averageBuyPrice).toFixed(2)}</td>

                                    <td><strong>${Number(stock.currentPrice).toFixed(2)}</strong></td>

                                    <td>
                                        <div className={`fw-bold ${returnColorClass}`}>
                                            {returnSign}${displayDollar} <br/>
                                            <small className={returnColorClass === 'text-muted' ? '' : 'opacity-75'}>
                                                {returnSign}{displayROI}%
                                            </small>
                                        </div>
                                    </td>

                                    <td>
                                        <div className="d-flex gap-2" style={{maxWidth: '180px'}}>
                                            <input
                                                type="number"
                                                className="form-control form-control-sm"
                                                placeholder="Qty"
                                                min="1"
                                                max={stock.quantity}
                                                value={sellQuantities[stock.symbol] || ''}
                                                onChange={(e) => setSellQuantities({...sellQuantities, [stock.symbol]: e.target.value})}
                                            />
                                            <button
                                                className="btn btn-outline-danger btn-sm fw-bold w-100"
                                                onClick={() => handleSell(stock.symbol)}
                                            >
                                                Sell
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            );
                        })
                    )}
                    </tbody>
                </table>
            </div>

            <h4 className="mt-5 fw-bold text-muted">Transaction History</h4>
            <div className="table-responsive shadow-sm rounded border bg-white mb-5">
                <table className="table table-hover align-middle mb-0">
                    <thead className="table-light text-muted">
                    <tr>
                        <th>Date & Time</th>
                        <th>Type</th>
                        <th>Asset</th>
                        <th>Shares</th>
                        <th>Execution Price</th>
                        <th>Total Value</th>
                    </tr>
                    </thead>
                    <tbody>
                    {transactionError || !Array.isArray(transactions) ? (
                        <tr>
                            <td colSpan="6" className="text-center py-4 text-danger fw-bold">
                                Failed to load transactions. Check the backend logs.
                            </td>
                        </tr>
                    ) : transactionsLoading ? (
                        <tr>
                            <td colSpan="6" className="text-center py-4 text-muted">
                                <span className="spinner-border spinner-border-sm me-2"></span>
                                Loading transactions...
                            </td>
                        </tr>
                    ) : transactions.length === 0 ? (
                        <tr>
                            <td colSpan="6" className="text-center py-4 text-muted">
                                No transactions recorded yet.
                            </td>
                        </tr>
                    ) : (
                        transactions.map((tx) => {
                            const txType = tx?.type || 'UNKNOWN';
                            const isBuy = txType.toUpperCase() === 'BUY';
                            const execPrice = Number(tx?.executionPrice || tx?.price || 0);
                            const qty = Number(tx?.quantity || 0);

                            let formattedDate = "Unknown Date";

                            if (tx && tx.timestamp) {
                                if (typeof tx.timestamp === 'string') {
                                    formattedDate = new Date(tx.timestamp.endsWith('Z') ? tx.timestamp : tx.timestamp + 'Z').toLocaleString([], {
                                        year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
                                    });
                                }
                                else if (Array.isArray(tx.timestamp)) {
                                    const [year, month, day, hour, minute, second] = tx.timestamp;
                                    formattedDate = new Date(year, month - 1, day, hour || 0, minute || 0, second || 0).toLocaleString([], {
                                        year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
                                    });
                                }
                            }

                            return (
                                <tr key={tx?.id || Math.random()}>
                                    <td className="text-muted">{formattedDate}</td>
                                    <td>
                                        <span className={`badge ${isBuy ? 'bg-success' : 'bg-danger'} bg-opacity-75`}>
                                            {txType.toUpperCase()}
                                        </span>
                                    </td>
                                    <td><strong>{tx?.symbol || 'N/A'}</strong></td>
                                    <td>{qty}</td>
                                    <td>${execPrice.toFixed(2)}</td>
                                    <td className="fw-bold text-muted">
                                        ${(qty * execPrice).toFixed(2)}
                                    </td>
                                </tr>
                            );
                        })
                    )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}