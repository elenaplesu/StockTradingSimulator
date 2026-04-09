import { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Link, NavLink, Navigate } from 'react-router-dom';
import Portfolio from './Portfolio';
import Explore from './Explorev2';
import Login from './Login';
import Register from './Register';
import Learn from './Learn';
import { API_BASE_URL } from './config';

const Home = () => (
    <div className="container mt-5 text-center">
        <h2 className="fw-bold">Welcome to the Stock Simulator</h2>
        <p className="text-muted text-center">This is a learning platform.</p>
    </div>
);

function App() {
    const [userId, setUserId] = useState(null);
    const [appReady, setAppReady] = useState(false);

    useEffect(() => {
        fetch(`${API_BASE_URL}/api/auth/me`, { credentials: 'include' })
            .then(res => res.ok ? res.json() : null)
            .then(id => {
                if (id) setUserId(id);
                setAppReady(true);
            })
            .catch(() => setAppReady(true));
    }, []);

    const handleLogout = () => {
        fetch(`${API_BASE_URL}/api/auth/logout`, { method: 'POST', credentials: 'include' })
            .then(() => {
                setUserId(null);
                window.location.href = '/login';
            });
    };

    const navLinkClass = ({ isActive }) =>
        isActive ? "nav-link active fw-bold text-white" : "nav-link";

    if (!appReady) {
        return (
            <div className="vh-100 d-flex justify-content-center align-items-center">
                <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Loading Session...</span>
                </div>
            </div>
        );
    }

    return (
        <Router>
            <nav className="navbar navbar-expand navbar-dark bg-dark">
                <div className="container">
                    <Link className="navbar-brand fw-bold" to="/">StockSimulator</Link>
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
                <Route path="/learn" element={<Learn userId={userId} />} />
                <Route path="/explore" element={<Explore userId={userId} />} />
                <Route path="/portfolio" element={userId ? <Portfolio userId={userId} /> : <Navigate to="/login" />} />
            </Routes>
        </Router>
    );
}

export default App;