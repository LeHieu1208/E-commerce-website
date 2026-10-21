import { useEffect, useRef, useState } from 'react';

function SearchBox() {
    const [text, setText] = useState(() => new URLSearchParams(window.location.hash.split('?')[1] || '').get('keyword') || '');
    const [results, setResults] = useState(null);
    const [open, setOpen] = useState(false);
    const box = useRef(null);
    useEffect(() => {
        const keyword = text.trim();
        if (!keyword) {
            setResults(null);
            return;
        }
        const controller = new AbortController();
        const timer = window.setTimeout(() => {
            fetch(`/api/catalog/search?keyword=${encodeURIComponent(keyword)}`, { signal: controller.signal })
                .then((response) => { if (!response.ok)
                throw new Error('search'); return response.json(); })
                .then((data) => { setResults(data); setOpen(true); })
                .catch((reason) => { if (reason?.name !== 'AbortError') {
                setResults('error');
                setOpen(true);
            } });
        }, 200);
        return () => { window.clearTimeout(timer); controller.abort(); };
    }, [text]);
    useEffect(() => {
        const close = (event) => { if (box.current && !box.current.contains(event.target))
            setOpen(false); };
        document.addEventListener('click', close);
        return () => document.removeEventListener('click', close);
    }, []);
    function submit(event) {
        event.preventDefault();
        setOpen(false);
        const keyword = text.trim();
        window.location.hash = keyword ? `/products?keyword=${encodeURIComponent(keyword)}` : '/products';
    }
    return (<form className="search" ref={box} onSubmit={submit}>
      <i className="ti ti-search" aria-hidden="true"/>
      <input value={text} onChange={(event) => setText(event.target.value)} onFocus={() => { if (results)
        setOpen(true); }} placeholder="Tìm sản phẩm..." aria-label="Tìm sản phẩm" autoComplete="off"/>
      {open && results && <div className="search-results show">
        {results === 'error' && <div className="search-result-item"><strong>Lỗi tìm kiếm</strong><span>Vui lòng thử lại</span></div>}
        {Array.isArray(results) && results.length === 0 && <div className="search-result-item"><strong>Không tìm thấy sản phẩm</strong><span>Thử từ khóa khác</span></div>}
        {Array.isArray(results) && results.map((product) => <a key={product.id} className="search-result-item" href={`/#/products/${product.id}`} onClick={() => setOpen(false)}><strong>{product.name}</strong><span>{product.categoryName || 'Danh mục'}</span></a>)}
      </div>}
    </form>);
}

export default SearchBox;