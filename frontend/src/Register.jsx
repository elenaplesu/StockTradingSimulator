import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {API_BASE_URL, getCsrfToken} from './config';

export default function Register({ setUserId }) {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState(null);
    const navigate = useNavigate();

    // Auto-dismiss the error after 5 seconds
    useEffect(() => {
        if (error) {
            const timer = setTimeout(() => setError(null), 5000);
            return () => clearTimeout(timer);
        }
    }, [error]);

    const handleRegister = (e) => {
        e.preventDefault();
        setError(null);

        // Basic frontend validation
        if (password !== confirmPassword) {
            setError("Passwords do not match!");
            return;
        }

        fetch(`${API_BASE_URL}/api/auth/register`, {
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
                    let errorMessage = "Registration failed";
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
                setUserId(userData.id);
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
                    <div className="fw-bold mb-1">Registration Failed</div>
                    <div style={{ fontSize: '0.9rem' }}>{error}</div>
                </div>
            )}

            <div className="row justify-content-center">
                <div className="col-md-5">
                    <div className="card shadow-sm border-0">
                        <div className="card-body bg-light rounded p-4">
                            <h3 className="card-title text-center mb-4 fw-bold">Create Account</h3>

                            <form onSubmit={handleRegister}>
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
                                <div className="mb-3">
                                    <label className="form-label text-muted fw-bold">Password</label>
                                    <input
                                        type="password"
                                        className="form-control"
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        required
                                        minLength="6"
                                    />
                                </div>
                                <div className="mb-4">
                                    <label className="form-label text-muted fw-bold">Confirm Password</label>
                                    <input
                                        type="password"
                                        className="form-control"
                                        value={confirmPassword}
                                        onChange={(e) => setConfirmPassword(e.target.value)}
                                        required
                                    />
                                </div>
                                <button type="submit" className="btn btn-success w-100 fw-bold mb-3">
                                    Sign Up
                                </button>

                                <div className="text-center">
                                    <span className="text-muted">Already have an account? </span>
                                    <Link to="/login" className="text-decoration-none fw-bold">Log in</Link>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}