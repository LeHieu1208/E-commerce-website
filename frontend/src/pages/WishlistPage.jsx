import { useEffect, useState } from 'react';
import StoreHeader from '../components/StoreHeader.jsx';
import { apiRequest } from '../api/client.js';

function WishlistPage({ catalog }) {
    const [items, setItems] = useState(null);
    const [message, setMessage] = useState('');
    useEffect(() => {
        apiRequest('/api/wishlist').then(setItems).catch((reason) => setMessage(reason instanceof Error ? reason.message : 'Không tải được danh sách yêu thích.'));
    }, []);
    async function remove(productId) {
        try {
            setItems(await apiRequest(`/api/wishlist/items/${productId}`, { method: 'DELETE' }));
        }
        catch (reason) {
            setMessage(reason instanceof Error ? reason.message : 'Không thể xóa sản phẩm.');
        }
    }
    return (<div className="wishlist-page">
      <StoreHeader catalog={catalog}/>
      <main className="wishlist-container">
        <h1 className="wishlist-title">Danh sách yêu thích</h1>
        {message && <p className="notice error" role="alert">{message}</p>}
        {!items && !message && <p className="empty-state">Đang tải...</p>}
        {items?.length === 0 && <section className="wishlist-card"><p className="empty-state">Bạn chưa có sản phẩm nào trong danh sách yêu thích.</p></section>}
        {items && items.length > 0 && <section className="wishlist-card">{items.map((item) => <article className="wishlist-row" key={item.id}><div className="wishlist-info"><a className="wishlist-name" href={`/#/products/${item.id}`}>{item.name}</a><span className="wishlist-description">{item.description ? (item.description.length > 80 ? `${item.description.slice(0, 77)}...` : item.description) : 'Sản phẩm yêu thích'}</span></div><div className="wishlist-actions"><a className="wishlist-view" href={`/#/products/${item.id}`}>Xem chi tiết</a><button type="button" className="wishlist-remove" onClick={() => remove(item.id)}>Xóa</button></div></article>)}</section>}
      </main>
    </div>);
}

export default WishlistPage;