import { useEffect, useState } from 'react';

async function getJson(url, signal, notFoundMessage) {
    const response = await fetch(url, { signal });
    if (!response.ok)
        throw new Error(response.status === 404 ? notFoundMessage : 'Không thể tải sản phẩm.');
    return response.json();
}

/** Danh sách sản phẩm theo query (`?keyword=...&categoryId=...`). */
export function useProductList(search) {
    const [products, setProducts] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError('');
        getJson(`/api/catalog/products${search}`, controller.signal, 'Không tìm thấy sản phẩm.')
            .then((data) => setProducts(data))
            .catch((reason) => {
            if (reason instanceof Error && reason.name !== 'AbortError')
                setError(reason.message);
        })
            .finally(() => { if (!controller.signal.aborted)
            setLoading(false); });
        return () => controller.abort();
    }, [search]);
    return { products: products || [], loading: loading || products === null && !error, error };
}

/** Chi tiết một sản phẩm (kèm sản phẩm liên quan, trạng thái đánh giá). */
export function useProductDetail(productId) {
    const [detail, setDetail] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError('');
        setDetail(null);
        getJson(`/api/catalog/products/${productId}`, controller.signal, 'Không tìm thấy sản phẩm.')
            .then(setDetail)
            .catch((reason) => {
            if (reason instanceof Error && reason.name !== 'AbortError')
                setError(reason.message);
        })
            .finally(() => { if (!controller.signal.aborted)
            setLoading(false); });
        return () => controller.abort();
    }, [productId]);
    return { detail, setDetail, loading, error };
}