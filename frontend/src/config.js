export const API_BASE_URL = "http://localhost:8080";

export function getCsrfToken() {
    const match = document.cookie
        .split('; ')
        .find(row => row.startsWith('XSRF-TOKEN='));
    return match ? decodeURIComponent(match.split('=')[1]) : '';
}