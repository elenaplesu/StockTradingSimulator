export const API_BASE_URL = "https://stocktradingsimulatorthesis.onrender.com";

export const getCsrfToken = () => {
    const match = document.cookie.match(new RegExp('(^| )XSRF-TOKEN=([^;]+)'));
    if (match) return match[2];
    return '';
};