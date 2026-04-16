export const API_BASE_URL = "https://stocktradingsimulatorthesis.onrender.com";

export const getCsrfToken = async () => {
    try {
        const response = await fetch(`${API_BASE_URL}/api/csrf`, {
            credentials: 'include'
        });
        if (!response.ok) return null;
        const data = await response.json();
        return data.token;
    } catch (err) {
        console.error("CSRF Fetch Failed", err);
        return null;
    }
};