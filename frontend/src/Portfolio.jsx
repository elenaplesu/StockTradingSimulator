import { useState, useEffect } from 'react';

export default function Portfolio({userId}) {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);

    const [sellQuantities, setSellQuantities] = useState({});
    const [tradeMessage, setTradeMessage] = useState(null);
    const [isTradeError, setIsTradeError] = useState(false);

    const fetchPortfolio = () => {
        fetch('http://localhost:8080/api/portfolio/1')
            .then(response => response.json())
            .then(data => {
                setData(data);
                setLoading(false);
            })
            .catch(error => {
                console.error("Error fetching data:", error);
                setLoading(false);
            });
    };

    useEffect(() => {
        fetchPortfolio();
    }, []);

    const handleSell = (symbol) => {
        const qty = parseInt(sellQuantities[symbol]);

        if (!qty || qty <= 0) {
            setIsTradeError(true);
            setTradeMessage(`Please enter a valid quantity to sell for ${symbol}.`);
            return;
        }

        const payload = {
            userId: userId,
            symbol: symbol,
            quantity: qty
        };

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

    if (loading) return <div className="container mt-5"><h4>Loading portfolio...</h4></div>;
    if (!data) return <div className="container mt-5"><h4>Error: Could not load data. Ensure your Java server is running!</h4></div>;

    return (
        <div className="container mt-5">
            <h2>My Portfolio</h2>
            <hr />

            <div className="row mt-4">
                <div className="col-md-6">
                    <div className="card text-white bg-success mb-3 shadow-sm">
                        <div className="card-header border-0 pb-0">Available Cash</div>
                        <div className="card-body">
                            <h1 className="card-title">${data.cashBalance.toFixed(2)}</h1>
                        </div>
                    </div>
                </div>
            </div>

            {/* NEW: Sell Feedback Message */}
            {tradeMessage && (
                <div className={`alert ${isTradeError ? 'alert-danger' : 'alert-success'} alert-dismissible fade show`}>
                    {tradeMessage}
                    <button type="button" className="btn-close" onClick={() => setTradeMessage(null)}></button>
                </div>
            )}

            <h4 className="mt-4">My Stocks</h4>
            <div className="table-responsive shadow-sm rounded">
                <table className="table table-hover mt-2 mb-0">
                    <thead className="table-dark">
                    <tr>
                        <th>Ticker Symbol</th>
                        <th>Quantity</th>
                        <th>Avg. Buy Price</th>
                        <th>Total Cost</th>
                        <th>Action</th>
                    </tr>
                    </thead>
                    <tbody>
                    {data.holdings.length === 0 ? (
                        <tr>
                            <td colSpan="5" className="text-center py-4 text-muted">
                                You don't own any stocks yet. Go to Explore to buy some!
                            </td>
                        </tr>
                    ) : (
                        data.holdings.map((stock) => (
                            <tr key={stock.id}>
                                <td className="align-middle"><strong>{stock.symbol}</strong></td>
                                <td className="align-middle">{stock.quantity} shares</td>
                                <td className="align-middle">${stock.averageBuyPrice.toFixed(2)}</td>
                                <td className="align-middle">${(stock.quantity * stock.averageBuyPrice).toFixed(2)}</td>

                                <td className="align-middle">
                                    <div className="d-flex gap-2">
                                        <input
                                            type="number"
                                            className="form-control form-control-sm"
                                            style={{width: '80px'}}
                                            placeholder="Qty"
                                            min="1"
                                            max={stock.quantity}
                                            value={sellQuantities[stock.symbol] || ''}
                                            onChange={(e) => setSellQuantities({...sellQuantities, [stock.symbol]: e.target.value})}
                                        />
                                        <button
                                            className="btn btn-danger btn-sm fw-bold"
                                            onClick={() => handleSell(stock.symbol)}
                                        >
                                            Sell
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))
                    )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}