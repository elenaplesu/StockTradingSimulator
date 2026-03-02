import { useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Link, NavLink, Navigate } from 'react-router-dom';
import Portfolio from './Portfolio';
import Explore from './Explore';
import Login from './Login';
import Register from './Register';

const Home = () => (
    <div className="container mt-5">
        <h2>Welcome to the Stock Simulator</h2>
        <p>Please log in to start trading.</p>
    </div>
);

const Learn = () => (
    <div className="container mt-5">
        <h2>Learn to Trade</h2>
        <p className="text-muted">Educational content and trading guides coming soon!</p>
    </div>
);

function App() {
    const [userId, setUserId] = useState(null);

    const handleLogout = () => {
        setUserId(null);
    };

    const navLinkClass = ({ isActive }) =>
        isActive ? "nav-link active fw-bold text-white" : "nav-link";

    return (
        <Router>
            <nav className="navbar navbar-expand navbar-dark bg-dark">
                <div className="container">
                    <Link className="navbar-brand" to="/">StockSimulator</Link>

                    <div className="navbar-nav flex-row gap-3 me-auto ms-4">
                        <NavLink className={navLinkClass} to="/" end>Home</NavLink>
                        <NavLink className={navLinkClass} to="/explore">Explore</NavLink>
                        <NavLink className={navLinkClass} to="/portfolio">Portfolio</NavLink>
                        <NavLink className={navLinkClass} to="/learn">Learn</NavLink>
                    </div>

                    <div className="navbar-nav flex-row ms-auto gap-2">
                        {!userId ? (
                            <>
                                <NavLink className={navLinkClass} to="/login">Login</NavLink>
                                <NavLink className={navLinkClass} to="/register">Sign Up</NavLink>
                            </>
                        ) : (
                            <button className="nav-link btn btn-outline-danger btn-sm px-3" onClick={handleLogout}>Logout</button>
                        )}
                    </div>
                </div>
            </nav>

            <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/login" element={<Login setUserId={setUserId} />} />
                <Route path="/register" element={<Register setUserId={setUserId} />} />
                <Route path="/learn" element={<Learn />} />
                <Route path="/explore" element={<Explore userId={userId} />} />
                <Route path="/portfolio" element={userId ? <Portfolio userId={userId} /> : <Navigate to="/login" />} />
            </Routes>
        </Router>
    );
}

export default App;