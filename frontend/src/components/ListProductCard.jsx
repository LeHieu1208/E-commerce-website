import { money } from '../utils/format.js';

function ListProductCard({ product }) {
    const hasDiscount = product.discountPercent != null && product.discountPercent > 0;
    const hasReviews = product.rating != null && product.reviewCount != null && product.reviewCount > 0;
    return (<a className="list-card" href={`/#/products/${product.id}`}>
      <div className="list-card-image">
        {hasDiscount && <span className="product-tag">-{product.discountPercent}%</span>}
        {product.imageUrl ? <img src={product.imageUrl} alt={product.name}/> : <i className={`ti ${product.icon || 'ti-package'}`} aria-hidden="true"/>}
      </div>
      <div className="list-card-info">
        <p className="list-card-name">{product.name}</p>
        <div className="list-card-price"><span className="price">{money.format(hasDiscount ? product.discountedPrice ?? product.price : product.price)}</span>{(product.oldPrice != null || hasDiscount) && <span className="price-old">{money.format(product.oldPrice ?? product.price)}</span>}</div>
        <div className="list-card-rating"><i className="ti ti-star" aria-hidden="true"/>{hasReviews ? <span>{product.rating} ({product.reviewCount})</span> : <span>Chưa có đánh giá</span>}</div>
      </div>
    </a>);
}

export default ListProductCard;