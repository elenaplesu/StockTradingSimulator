import { useState } from 'react';
import { Link } from 'react-router-dom';

export default function Explore({ userId }) {
    const [symbol, setSymbol] = useState('');
    const [stockData, setStockData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const [quantity, setQuantity] = useState('');
    const [tradeMessage, setTradeMessage] = useState(null);
    const [isTradeError, setIsTradeError] = useState(false);

    const handleSearch = (e) => {
        e.preventDefault();
        if (!symbol) return;

        setLoading(true);
        setError(null);
        setStockData(null);
        setTradeMessage(null);

        fetch(`http://localhost:8080/api/stocks/${symbol.toUpperCase()}`)
            .then(response => {
                if (!response.ok) throw new Error("Stock not found");
                return response.json();
            })
            .then(data => {
                setStockData(data);
                setLoading(false);
            })
            .catch(err => {
                setError("Could not find that stock. Try AAPL, TSLA, or MSFT.");
                setLoading(false);
            });
    };

    const handleBuy = () => {
        if (!quantity || quantity <= 0) {
            setIsTradeError(true);
            setTradeMessage("Please enter a valid quantity.");
            return;
        }

        const payload = {
            userId: userId,
            symbol: stockData.symbol,
            quantity: parseInt(quantity)
        };

        fetch(`http://localhost:8080/api/trade/buy`, {
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
                setQuantity('');
            })
            .catch(err => {
                setIsTradeError(true);
                setTradeMessage(err.message);
            });
    };

    return (
        <div className="container mt-5 d-flex flex-column align-items-center text-center">

            <h2 className="fw-bold mb-3">Explore the Market</h2>

            <div className="w-100 mb-4" style={{ maxWidth: '500px' }}>
                <form onSubmit={handleSearch} className="d-flex gap-2">
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

            {loading && <div className="text-muted mb-4"><span className="spinner-border spinner-border-sm me-2"></span>Fetching live price...</div>}
            {error && <div className="alert alert-danger w-100 shadow-sm" style={{ maxWidth: '500px' }}>{error}</div>}

            {stockData && (
                <div className="card shadow border-0 w-100" style={{ maxWidth: '400px' }}>
                    <div className="card-body bg-light rounded p-4">
                        <h3 className="card-title fw-bold mb-1">{stockData.symbol}</h3>
                        <h1 className="text-success mb-4 fw-bold">
                            ${(stockData.currentPrice || stockData.price)?.toFixed(2)}
                        </h1>

                        {userId ? (
                            <div className="mt-2 p-3 bg-white border rounded text-start shadow-sm">
                                <label className="form-label fw-bold text-muted mb-2">Shares to Buy</label>
                                <input
                                    type="number"
                                    className="form-control form-control-lg mb-3 bg-light"
                                    placeholder="0"
                                    min="1"
                                    value={quantity}
                                    onChange={(e) => setQuantity(e.target.value)}
                                />
                                <button
                                    className="btn btn-success btn-lg w-100 fw-bold shadow-sm"
                                    onClick={handleBuy}
                                >
                                    Buy {stockData.symbol}
                                </button>
                            </div>
                        ) : (
                            <div className="mt-2 p-3 bg-white border rounded shadow-sm">
                                <p className="text-muted mb-0" style={{ fontSize: '1.1rem' }}>
                                    Want to trade? <br/>
                                    <Link to="/login" className="text-decoration-none fw-bold text-primary">Log in here</Link> to start building your portfolio.
                                </p>
                            </div>
                        )}

                        {tradeMessage && (
                            <div className={`alert mt-3 mb-0 shadow-sm ${isTradeError ? 'alert-danger' : 'alert-success'}`}>
                                {tradeMessage}
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}