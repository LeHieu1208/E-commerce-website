import { useEffect, useState } from 'react';

/** Tải dữ liệu dùng chung của cửa hàng (banner, danh mục, sản phẩm, người dùng, giỏ hàng). */
export function useCatalog() {
    const [catalog, setCatalog] = useState(null);
    const [error, setError] = useState('');
    useEffect(() => {
        const controller = new AbortController();
        fetch('/api/catalog/home', { signal: controller.signal })
            .then((response) => {
            if (!response.ok)
                throw new Error('Không thể tải dữ liệu cửa hàng.');
            return response.json();
        })
            .then(setCatalog)
            .catch((reason) => {
            if (reason instanceof Error && reason.name !== 'AbortError') {
                setError(reason.message);
            }
        });
        return () => controller.abort();
    }, []);
    return { catalog, error };
}