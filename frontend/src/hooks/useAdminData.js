import { useEffect, useState } from 'react';
import { apiRequest } from '../api/client.js';

export function useAdminData(url) {
    const [data, setData] = useState(null);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(true);
    const [reloadKey, setReloadKey] = useState(0);
    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        apiRequest(url, { signal: controller.signal }).then(setData)
            .catch((reason) => {
            if (reason instanceof Error && reason.name !== 'AbortError')
                setError(reason.message);
        })
            .finally(() => { if (!controller.signal.aborted)
            setLoading(false); });
        return () => controller.abort();
    }, [url, reloadKey]);
    return { data, setData, error, loading, reload: () => setReloadKey((key) => key + 1), setError };
}