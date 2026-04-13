export const API_BASE_URL = "http://localhost:8080";

export const getCsrfToken = () => {
    const match = document.cookie.match(new RegExp('(^| )XSRF-TOKEN=([^;]+)'));
    if (match) return match[2];
    return '';
};