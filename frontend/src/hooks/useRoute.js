import { useEffect, useMemo, useState } from 'react';

function readRoute() {
    return window.location.hash.slice(1) || `${window.location.pathname}${window.location.search}`;
}

/** Theo dõi route dạng hash (`/#/products?keyword=...`) và trả về một đối tượng URL. */
export function useRoute() {
    const [route, setRoute] = useState(readRoute);
    useEffect(() => {
        const onHashChange = () => setRoute(readRoute());
        window.addEventListener('hashchange', onHashChange);
        return () => window.removeEventListener('hashchange', onHashChange);
    }, []);
    return useMemo(() => new URL(route, window.location.origin), [route]);
}