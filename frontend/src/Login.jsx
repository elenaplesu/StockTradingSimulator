import {useEffect, useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {API_BASE_URL, getCsrfToken} from './config';

export default function Login({ setUserId }) {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState(null);
    const navigate = useNavigate();

    useEffect(() => {
        if (error) {
            const timer = setTimeout(() => setError(null), 5000);
            return () => clearTimeout(timer);
        }
    }, [error]);

    const handleLogin = (e) => {
        e.preventDefault();
        setError(null);

        fetch(`${API_BASE_URL}/api/auth/login`, {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': getCsrfToken()
            },
            body: JSON.stringify({ username, password })
        })
            .then(async response => {
                const rawText = await response.text();

                if (!response.ok) {
                    let errorMessage = "Invalid username or password";
                    try {
                        const errorData = JSON.parse(rawText);
                        errorMessage = errorData.message || errorData.error || response.statusText;
                    } catch (e) {
                        errorMessage = rawText || response.statusText;
                    }
                    throw new Error(errorMessage);
                }

                return JSON.parse(rawText);
            })
            .then(userData => {
                setUserId(userData);
                navigate('/portfolio');
            })
            .catch(err => setError(err.message));
    };

    return (
        <div className="container mt-5">
            {error && (
                <div
                    className="alert alert-danger shadow-lg m-0"
                    style={{
                        position: 'fixed',
                        bottom: '20px',
                        right: '20px',
                        zIndex: 9999,
                        minWidth: '350px',
                        borderLeft: '5px solid #dc3545',
                        boxShadow: '0 10px 30px rgba(0,0,0,0.15)'
                    }}
                    role="alert"
                >
                    <div className="fw-bold mb-1">Login Failed</div>
                    <div style={{ fontSize: '0.9rem' }}>{error}</div>
                </div>
            )}

            <div className="row justify-content-center">
                <div className="col-md-5">
                    <div className="card shadow-sm border-0">
                        <div className="card-body bg-light rounded p-4">
                            <h3 className="card-title text-center mb-4 fw-bold">Login</h3>
                            <form onSubmit={handleLogin}>
                                <div className="mb-3">
                                    <label className="form-label text-muted fw-bold">Username</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        value={username}
                                        onChange={(e) => setUsername(e.target.value)}
                                        required
                                    />
                                </div>
                                <div className="mb-4">
                                    <label className="form-label text-muted fw-bold">Password</label>
                                    <input
                                        type="password"
                                        className="form-control"
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        required
                                    />
                                </div>
                                <button type="submit" className="btn btn-primary w-100 fw-bold mb-3">
                                    Sign In
                                </button>
                                <div className="text-center">
                                    <span className="text-muted">Don't have an account? </span>
                                    <Link to="/register" className="text-decoration-none fw-bold">Sign up</Link>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}