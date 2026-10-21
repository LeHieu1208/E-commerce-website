import { useState } from 'react';
import { money } from '../utils/format.js';
import { apiRequest } from '../api/client.js';

function ProductTile({ product }) {
    const [message, setMessage] = useState('');
    async function addToCart() {
        try {
            await apiRequest('/api/cart/items', { method: 'POST', body: JSON.stringify({ productId: product.id, quantity: 1 }) });
            setMessage('Đã thêm vào giỏ hàng.');
        }
        catch (reason) {
            setMessage(reason instanceof Error ? reason.message : 'Không thể thêm sản phẩm.');
        }
    }
    async function addToWishlist() {
        try {
            await apiRequest(`/api/wishlist/items/${product.id}`, { method: 'POST' });
            setMessage('Đã thêm vào yêu thích.');
        }
        catch (reason) {
            setMessage(reason instanceof Error ? reason.message : 'Không thể thêm sản phẩm.');
        }
    }
    return (<article className="product-card" key={product.id}>
      <a className="product-visual" href={`/#/products/${product.id}`}>
        {product.discountPercent != null && product.discountPercent > 0 && <span className="discount">-{product.discountPercent}%</span>}
        {product.imageUrl ? <img src={product.imageUrl} alt={product.name}/> : <i className={`ti ${product.icon || 'ti-package'} product-placeholder`} aria-hidden="true"/>}
      </a>
      <div className="product-details">
        <a className="product-name" href={`/#/products/${product.id}`}>{product.name}</a>
        <div className="price-line">
          <strong>{money.format(product.discountedPrice ?? product.price)}</strong>
          {(product.oldPrice != null || (product.discountPercent != null && product.discountPercent > 0)) && (<del>{money.format(product.oldPrice ?? product.price)}</del>)}
        </div>
        <div className="rating"><i className="ti ti-star-filled" aria-hidden="true"/> {product.rating ?? 0} <span>({product.reviewCount ?? 0})</span></div>
        <div className="product-actions">
          <button className="add-button" type="button" onClick={addToCart} title="Thêm vào giỏ" aria-label={`Thêm ${product.name} vào giỏ`}><i className="ti ti-shopping-cart-plus" aria-hidden="true"/><span>Thêm vào giỏ</span></button>
          <button className="wish-button" type="button" onClick={addToWishlist} title="Thêm vào yêu thích" aria-label={`Thêm ${product.name} vào yêu thích`}><i className="ti ti-heart" aria-hidden="true"/></button>
          <a className="detail-button" href={`/#/products/${product.id}`}>Xem chi tiết</a>
        </div>
        {message && <p className="product-feedback" role="status">{message}</p>}
      </div>
    </article>);
}

export default ProductTile;