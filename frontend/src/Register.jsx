import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';

export default function Register({ setUserId }) {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState(null);
    const navigate = useNavigate();

    const handleRegister = (e) => {
        e.preventDefault();
        setError(null);

        // Basic frontend validation
        if (password !== confirmPassword) {
            setError("Passwords do not match!");
            return;
        }

        fetch('http://localhost:8080/api/users/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        })
            .then(async response => {
                if (!response.ok) {
                    const text = await response.text();
                    throw new Error(text || "Registration failed");
                }
                return response.json(); // Grabs the newly created User ID
            })
            .then(id => {
                setUserId(id);          // Instantly log them in
                navigate('/portfolio'); // Teleport to the Portfolio page
            })
            .catch(err => setError(err.message));
    };

    return (
        <div className="container mt-5">
            <div className="row justify-content-center">
                <div className="col-md-5">
                    <div className="card shadow-sm border-0">
                        <div className="card-body bg-light rounded p-4">
                            <h3 className="card-title text-center mb-4 fw-bold">Create Account</h3>

                            {error && <div className="alert alert-danger">{error}</div>}

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