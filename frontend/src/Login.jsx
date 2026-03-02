import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom'; // NEW: Imported Link

export default function Login({ setUserId }) {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState(null);
    const navigate = useNavigate();

    const handleLogin = (e) => {
        e.preventDefault();
        setError(null);

        fetch('http://localhost:8080/api/users/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        })
            .then(async response => {
                if (!response.ok) throw new Error("Invalid username or password");
                return response.json();
            })
            .then(id => {
                setUserId(id);
                navigate('/portfolio');
            })
            .catch(err => setError(err.message));
    };

    return (
        <div className="container mt-5">
            <div className="row justify-content-center">
                <div className="col-md-5">
                    <div className="card shadow-sm border-0">
                        <div className="card-body bg-light rounded p-4">
                            <h3 className="card-title text-center mb-4 fw-bold">Login</h3>

                            {error && <div className="alert alert-danger">{error}</div>}

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