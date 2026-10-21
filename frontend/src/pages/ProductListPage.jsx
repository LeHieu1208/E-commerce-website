import { useEffect, useState } from 'react';
import StoreHeader from '../components/StoreHeader.jsx';
import ListProductCard from '../components/ListProductCard.jsx';
import { useProductList } from '../hooks/useProducts.js';

function ProductListPage({ catalog, search }) {
    const { products, loading, error } = useProductList(search);
    const params = new URLSearchParams(search);
    const keyword = params.get('keyword') || '';
    const categoryId = params.get('categoryId') ? Number(params.get('categoryId')) : null;
    const selectedCategory = categoryId == null ? null : catalog?.categories.find((category) => category.id === categoryId) || null;
    const [text, setText] = useState(keyword);
    useEffect(() => setText(keyword), [keyword]);
    function link(id) {
        const next = new URLSearchParams();
        if (id != null)
            next.set('categoryId', String(id));
        if (keyword)
            next.set('keyword', keyword);
        const query = next.toString();
        return `/#/products${query ? `?${query}` : ''}`;
    }
    function submit(event) {
        event.preventDefault();
        const next = new URLSearchParams();
        if (categoryId != null)
            next.set('categoryId', String(categoryId));
        if (text.trim())
            next.set('keyword', text.trim());
        const query = next.toString();
        window.location.hash = `/products${query ? `?${query}` : ''}`;
    }
    return (<div className="storefront">
      <StoreHeader catalog={catalog}/>
      <main className="shell list-page">
        <form className="list-search" onSubmit={submit}><input type="text" value={text} onChange={(event) => setText(event.target.value)} placeholder="Tìm sản phẩm..."/><button type="submit">Tìm kiếm</button></form>
        <h1 className="list-title">Danh sách sản phẩm</h1>
        <div className="category-filter">
          <a href={link(null)} className={`category-chip ${categoryId == null ? 'active' : ''}`}>Tất cả</a>
          {catalog?.categories.map((category) => <a key={category.id} href={link(category.id)} className={`category-chip ${categoryId === category.id ? 'active' : ''}`}>{category.name}</a>)}
        </div>
        {selectedCategory && <div className="list-category-header"><h2><i className={`ti ${selectedCategory.icon || 'ti-package'}`} aria-hidden="true"/><span>{selectedCategory.name}</span></h2></div>}
        {loading && <p className="loading">Đang tải sản phẩm...</p>}
        {error && <p className="notice error" role="alert">{error}</p>}
        {!loading && !error && products.length > 0 && <div className="list-grid">{products.map((product) => <ListProductCard product={product} key={product.id}/>)}</div>}
        {!loading && !error && products.length === 0 && <p className="empty-state">Không tìm thấy sản phẩm nào.</p>}
      </main>
    </div>);
}

export default ProductListPage;