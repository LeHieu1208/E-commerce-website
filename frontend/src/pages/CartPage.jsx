import { useEffect, useState } from 'react';
import PageFrame from '../components/PageFrame.jsx';
import { money } from '../utils/format.js';
import { apiRequest } from '../api/client.js';

function CartPage({ catalog }) {
    const [cart, setCart] = useState(null);
    const [message, setMessage] = useState('');
    useEffect(() => {
        apiRequest('/api/cart').then(setCart).catch((reason) => setMessage(reason instanceof Error ? reason.message : 'Không tải được giỏ hàng.'));
    }, []);
    async function update(productId, quantity) {
        try {
            setCart(await apiRequest(`/api/cart/items/${productId}`, { method: 'PUT', body: JSON.stringify({ quantity }) }));
            setMessage('');
        }
        catch (reason) {
            setMessage(reason instanceof Error ? reason.message : 'Không thể cập nhật giỏ hàng.');
        }
    }
    async function remove(productId) {
        try {
            setCart(await apiRequest(`/api/cart/items/${productId}`, { method: 'DELETE' }));
        }
        catch (reason) {
            setMessage(reason instanceof Error ? reason.message : 'Không thể xóa sản phẩm.');
        }
    }
    return (<PageFrame title="Giỏ hàng" catalog={catalog}>
      <h1 className="page-title">Giỏ hàng của bạn</h1>
      {message && <p className="notice error" role="alert">{message}</p>}
      {!cart && !message && <p className="loading">Đang tải giỏ hàng...</p>}
      {cart && cart.items.length === 0 && <section className="empty-cart"><i className="ti ti-shopping-cart-off" aria-hidden="true"/><p>Giỏ hàng của bạn đang trống.</p><a className="btn-primary" href="/">Tiếp tục mua sắm</a></section>}
      {cart && cart.items.length > 0 && <div className="cart-layout"><section className="cart-items">{cart.items.map((item) => <article className="cart-item" key={item.productId}><a className="cart-item-image" href={`/#/products/${item.productId}`}>{item.imageUrl ? <img src={item.imageUrl} alt={item.name}/> : <i className="ti ti-shopping-bag" aria-hidden="true"/>}</a><div className="cart-item-details"><a className="cart-item-name" href={`/#/products/${item.productId}`}>{item.name}</a><span className="cart-item-price">{money.format(item.price)} / sản phẩm</span></div><div className="cart-item-actions"><label className="sr-only" htmlFor={`cart-quantity-${item.productId}`}>Số lượng {item.name}</label><input id={`cart-quantity-${item.productId}`} className="qty-input" type="number" min="1" max={item.stockQuantity ?? undefined} defaultValue={item.quantity} onBlur={(event) => { const quantity = Math.max(1, Number(event.currentTarget.value) || 1); event.currentTarget.value = String(quantity); if (quantity !== item.quantity)
        void update(item.productId, quantity); }}/><button className="btn-remove" type="button" aria-label={`Xóa ${item.name}`} onClick={() => remove(item.productId)}><i className="ti ti-trash" aria-hidden="true"/></button></div><div className="cart-item-subtotal">{money.format(item.price * item.quantity)}</div></article>)}</section><aside className="cart-summary"><h2>Tóm tắt đơn hàng</h2><div className="summary-row"><span>Tạm tính</span><span>{money.format(cart.subtotal)}</span></div><div className="summary-row"><span>Phí vận chuyển</span><span>Tính ở bước sau</span></div><div className="summary-row total"><span>Tổng cộng</span><span>{money.format(cart.subtotal)}</span></div><a className="btn-checkout" href="/#/checkout">Tiến hành thanh toán</a></aside></div>}
    </PageFrame>);
}

export default CartPage;