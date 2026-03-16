import { useState, useEffect } from 'react';

export default function Portfolio({userId}) {
    const [data, setData] = useState(null);
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(true);

    const [sellQuantities, setSellQuantities] = useState({});
    const [tradeMessage, setTradeMessage] = useState(null);
    const [isTradeError, setIsTradeError] = useState(false);

    const fetchPortfolio = () => {
        // 1. Fetch Analytics
        fetch(`http://localhost:8080/api/portfolio/${userId}/analytics`)
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

        // 2. Fetch Transaction History
        fetch(`http://localhost:8080/api/portfolio/${userId}/transactions`)
            .then(response => {
                if (!response.ok) throw new Error("Failed to load transactions");
                return response.json();
            })
            .then(historyData => {
                setTransactions(historyData);
            })
            .catch(error => console.error("Error fetching transactions:", error));
    };

    useEffect(() => {
        if (userId) fetchPortfolio();
    }, [userId]);

    const handleSell = (symbol) => {
        const qty = parseInt(sellQuantities[symbol]);
        if (!qty || qty <= 0) {
            setIsTradeError(true);
            setTradeMessage(`Please enter a valid quantity to sell for ${symbol}.`);
            return;
        }

        const payload = { userId, symbol, quantity: qty };

        fetch(`http://localhost:8080/api/trade/sell`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
            .then(async response => {
                const text = await response.text();
                if (!response.ok) throw new Error(text);
                return text;
            })
            .then(message => {
                setIsTradeError(false);
                setTradeMessage(message);
                setSellQuantities(prev => ({...prev, [symbol]: ''}));
                fetchPortfolio();
            })
            .catch(err => {
                setIsTradeError(true);
                setTradeMessage(err.message);
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
                            <h2 className="card-title mb-0">${data.cashBalance.toLocaleString('en-US', {minimumFractionDigits: 2, maximumFractionDigits: 2})}</h2>
                        </div>
                    </div>
                </div>
                <div className="col-md-3 mb-3">
                    <div className="card text-white bg-primary h-100 shadow-sm">
                        <div className="card-header border-0 pb-0 fw-bold">Total Net Worth</div>
                        <div className="card-body d-flex align-items-center">
                            <h2 className="card-title mb-0">${data.netWorth.toLocaleString('en-US', {minimumFractionDigits: 2, maximumFractionDigits: 2})}</h2>
                        </div>
                    </div>
                </div>
                <div className="col-md-3 mb-3">
                    <div className={`card text-white h-100 shadow-sm ${data.aggregateROI >= 0 ? 'bg-success' : 'bg-danger'}`}>
                        <div className="card-header border-0 pb-0 fw-bold">Aggregate ROI</div>
                        <div className="card-body d-flex align-items-center">
                            <h2 className="card-title mb-0">
                                {data.aggregateROI >= 0 ? '+' : ''}{data.aggregateROI.toFixed(2)}%
                            </h2>
                        </div>
                    </div>
                </div>
                <div className="col-md-3 mb-3">
                    <div className="card text-white bg-dark h-100 shadow-sm">
                        <div className="card-header border-0 pb-0 fw-bold">HHI / Variance</div>
                        <div className="card-body d-flex flex-column justify-content-center">
                            <h3 className="card-title mb-0">
                                {data.hhi.toFixed(0)} <span className="fs-6 text-muted">HHI</span>
                            </h3>
                            <small className="text-info mt-1">Var: {data.crossSectionalVariance.toFixed(4)}</small>
                        </div>
                    </div>
                </div>
            </div>

            {tradeMessage && (
                <div className={`alert ${isTradeError ? 'alert-danger' : 'alert-success'} alert-dismissible shadow-sm fade show`}>
                    {tradeMessage}
                    <button type="button" className="btn-close" onClick={() => setTradeMessage(null)}></button>
                </div>
            )}

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
                            const totalCost = stock.quantity * stock.averageBuyPrice;
                            const dollarReturn = stock.totalValue - totalCost;
                            const isPositive = stock.returnOnInvestment >= 0;
                            const returnColorClass = isPositive ? 'text-success' : 'text-danger';
                            const returnSign = isPositive ? '+' : '';

                            return (
                                <tr key={stock.symbol}>
                                    <td><strong className="fs-5">{stock.symbol}</strong></td>
                                    <td>
                                        <div className="d-flex align-items-center gap-2">
                                            <span className="fw-bold">{stock.weightPercentage.toFixed(1)}%</span>
                                            <div className="progress" style={{width: '50px', height: '6px'}}>
                                                <div className="progress-bar bg-primary" style={{width: `${stock.weightPercentage}%`}}></div>
                                            </div>
                                        </div>
                                    </td>
                                    <td>{stock.quantity}</td>
                                    <td>${stock.averageBuyPrice.toFixed(2)}</td>
                                    <td><strong>${stock.currentPrice.toFixed(2)}</strong></td>
                                    <td>
                                        <div className={`fw-bold ${returnColorClass}`}>
                                            {returnSign}${dollarReturn.toFixed(2)} <br/>
                                            <small className="opacity-75">{returnSign}{stock.returnOnInvestment.toFixed(2)}%</small>
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
                    {transactions.length === 0 ? (
                        <tr>
                            <td colSpan="6" className="text-center py-4 text-muted">
                                No transactions recorded yet.
                            </td>
                        </tr>
                    ) : (
                        transactions.map((tx) => {
                            const txType = tx.type || 'UNKNOWN';
                            const isBuy = txType.toUpperCase() === 'BUY';
                            const execPrice = tx.executionPrice || 0;

                            return (
                                <tr key={tx.id}>
                                    <td className="text-muted">
                                        {new Date(tx.timestamp).toLocaleString([], {
                                            year: 'numeric', month: 'short', day: 'numeric',
                                            hour: '2-digit', minute: '2-digit'
                                        })}
                                    </td>
                                    <td>
                                        <span className={`badge ${isBuy ? 'bg-success' : 'bg-danger'} bg-opacity-75`}>
                                            {txType.toUpperCase()}
                                        </span>
                                    </td>
                                    <td><strong>{tx.symbol}</strong></td>
                                    <td>{tx.quantity}</td>
                                    <td>${execPrice.toFixed(2)}</td>
                                    <td className="fw-bold text-muted">
                                        ${(tx.quantity * execPrice).toFixed(2)}
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