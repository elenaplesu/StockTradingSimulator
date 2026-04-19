export const API_BASE_URL = "https://stocktradingsimulatorthesis.onrender.com";

export function getCsrfToken() {
    const match = document.cookie
        .split('; ')
        .find(row => row.startsWith('XSRF-TOKEN='));
    return match ? decodeURIComponent(match.split('=')[1]) : '';
}