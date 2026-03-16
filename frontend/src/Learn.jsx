import React from 'react';
import { Link } from 'react-router-dom';

export default function Learn() {
    return (
        <div className="container mt-5 mb-5">
            <div className="text-center mb-5">
                <h2 className="fw-bold">Trading 101</h2>
                <p className="text-muted fs-5">Master the basics of the stock market before risking your virtual cash.</p>
            </div>

            <div className="row justify-content-center">
                <div className="col-lg-8">

                    <div className="card shadow-sm border-0 mb-4">
                        <div className="card-body bg-light rounded p-4">
                            <h4 className="fw-bold text-primary mb-3">1. What is a Stock?</h4>
                            <p>
                                A stock represents a fractional ownership interest in a company. When you buy a stock, you are buying a small piece of that corporation. Companies issue shares to raise capital to grow their business.
                            </p>
                            <div className="p-3 bg-white border rounded shadow-sm mt-3">
                                <strong>Ticker Symbol:</strong> A unique series of letters representing a specific stock on the exchange.
                                <br /><em>Examples: <strong>AAPL</strong> (Apple), <strong>TSLA</strong> (Tesla), <strong>AMZN</strong> (Amazon).</em>
                            </div>
                        </div>
                    </div>

                    <div className="card shadow-sm border-0 mb-4">
                        <div className="card-body bg-light rounded p-4">
                            <h4 className="fw-bold text-success mb-3">2. Executing Trades</h4>
                            <p>
                                This simulator processes <strong>Market Orders</strong>. This means your trade is executed immediately at the current real-time market price.
                            </p>
                            <ul className="mb-0">
                                <li className="mb-2"><strong>Buying:</strong> You exchange your available Cash Balance for shares. You cannot buy if you do not have sufficient funds!</li>
                                <li><strong>Selling:</strong> You liquidate your owned shares back into cash at the current market price, realizing any profit or loss.</li>
                            </ul>
                        </div>
                    </div>

                    <div className="card shadow-sm border-0 mb-4">
                        <div className="card-body bg-light rounded p-4">
                            <h4 className="fw-bold text-info mb-3">3. Profit and Loss (P/L)</h4>
                            <p>
                                Your goal as a trader is simple: Buy low, sell high. Your Portfolio dashboard tracks your performance automatically.
                            </p>
                            <ul>
                                <li className="mb-2"><strong>Average Buy Price:</strong> If you buy 10 shares at $100 and later buy 10 more at $150, your average buy price becomes $125.</li>
                                <li><strong>Calculating P/L:</strong> The system calculates your returns using this formula: <br/>
                                    <code>(Current Price - Average Buy Price) × Number of Shares</code></li>
                            </ul>
                        </div>
                    </div>

                    <div className="text-center mt-5">
                        <h5 className="fw-bold mb-3">Ready to put your knowledge to the test?</h5>
                        <Link to="/explore" className="btn btn-primary btn-lg shadow-sm px-5 fw-bold">
                            Explore the Market
                        </Link>
                    </div>

                </div>
            </div>
        </div>
    );
}