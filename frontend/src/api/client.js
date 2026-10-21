export async function apiRequest(url, init = {}) {
    const response = await fetch(url, {
        credentials: 'same-origin',
        ...init,
        headers: { 'Content-Type': 'application/json', ...init.headers },
    });
    if (response.status === 401 && !url.startsWith('/api/auth/')) {
        window.location.hash = '/auth?mode=login';
    }
    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) {
        throw new Error(data && 'message' in data ? data.message || 'Yêu cầu không thành công.' : 'Yêu cầu không thành công.');
    }
    return data;
}