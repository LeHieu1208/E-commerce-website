import { useState } from 'react';
import StoreHeader from '../components/StoreHeader.jsx';
import ProductTile from '../components/ProductTile.jsx';
import { money } from '../utils/format.js';
import { apiRequest } from '../api/client.js';
import { useProductDetail } from '../hooks/useProducts.js';

function ProductDetailPage({ catalog, productId }) {
    const { detail: productDetail, setDetail: setProductDetail, loading: pageLoading, error: pageError } = useProductDetail(productId);
    const [productActionMessage, setProductActionMessage] = useState('');
    const [ratingMessage, setRatingMessage] = useState('');
    async function submitProductCart(event) {
        event.preventDefault();
        if (!productDetail)
            return;
        const quantity = Number(new FormData(event.currentTarget).get('quantity') || 1);
        try {
            await apiRequest('/api/cart/items', { method: 'POST', body: JSON.stringify({ productId: productDetail.product.id, quantity }) });
            setProductActionMessage('Đã thêm vào giỏ hàng.');
        }
        catch (reason) {
            setProductActionMessage(reason instanceof Error ? reason.message : 'Không thể thêm sản phẩm.');
        }
    }
    async function saveProductWishlist() {
        if (!productDetail)
            return;
        try {
            await apiRequest(`/api/wishlist/items/${productDetail.product.id}`, { method: 'POST' });
            setProductActionMessage('Đã thêm vào yêu thích.');
        }
        catch (reason) {
            setProductActionMessage(reason instanceof Error ? reason.message : 'Không thể thêm sản phẩm.');
        }
    }
    async function submitRating(event) {
        event.preventDefault();
        if (!productDetail)
            return;
        const ratingValue = Number(new FormData(event.currentTarget).get('ratingValue'));
        setRatingMessage('');
        try {
            const result = await apiRequest(`/api/catalog/products/${productDetail.product.id}/rating`, {
                method: 'POST',
                body: JSON.stringify({ ratingValue }),
            });
            setProductDetail((current) => current ? {
                ...current,
                reviewed: result.reviewed,
                product: { ...current.product, rating: result.rating, reviewCount: result.reviewCount },
            } : current);
            setRatingMessage(result.message);
        }
        catch (reason) {
            setRatingMessage(reason instanceof Error ? reason.message : 'Không thể gửi đánh giá.');
        }
    }

    const title = productDetail?.product.name || 'Chi tiết sản phẩm';
        const inStock = (productDetail?.product.stockQuantity ?? 0) > 0;
        return (<div className="storefront">
        <StoreHeader catalog={catalog}/>
        <main className="shell main-content">
          <nav className="breadcrumb" aria-label="Đường dẫn"><a href="/">Trang chủ</a><span>/</span>{productDetail?.product.category && <><a href={`/#/products?categoryId=${productDetail.product.category.id}`}>{productDetail.product.category.name}</a><span>/</span></>}<span className="current">{title}</span></nav>
          {pageLoading && <p className="loading">Đang tải sản phẩm...</p>}
          {pageError && <p className="notice error" role="alert">{pageError}</p>}
          {productId && productDetail && (<>
              <section className="detail-layout">
                <div className="detail-visual">{productDetail.product.discountPercent != null && productDetail.product.discountPercent > 0 && <span className="discount detail-discount">-{productDetail.product.discountPercent}%</span>}{productDetail.product.imageUrl ? <img src={productDetail.product.imageUrl} alt={productDetail.product.name}/> : <i className={`ti ${productDetail.product.icon || 'ti-package'} product-placeholder`} aria-hidden="true"/>}</div>
                <div className="detail-copy">
                  <span className="eyebrow">{productDetail.product.category?.name || 'SẢN PHẨM SHOPMART'}</span>
                  <h1>{productDetail.product.name}</h1>
                  <div className="detail-rating"><i className="ti ti-star-filled" aria-hidden="true"/> {productDetail.product.rating != null && productDetail.product.reviewCount > 0 ? `${productDetail.product.rating} · ${productDetail.product.reviewCount} đánh giá` : 'Chưa có đánh giá'}</div>
                  <div className="detail-price">{money.format(productDetail.product.discountedPrice ?? productDetail.product.price)}{(productDetail.product.oldPrice != null || productDetail.product.discountPercent > 0) && <del>{money.format(productDetail.product.oldPrice ?? productDetail.product.price)}</del>}{productDetail.product.discountPercent > 0 && <span className="saving-badge">Tiết kiệm {productDetail.product.discountPercent}%</span>}</div>
                  <p className="detail-description">{productDetail.product.description || 'Thông tin sản phẩm đang được cập nhật.'}</p>
                  {inStock ? <p className="stock-info in-stock"><i className="ti ti-circle-check" aria-hidden="true"/> Còn hàng ({productDetail.product.stockQuantity} sản phẩm)</p> : <p className="stock-info out-stock"><i className="ti ti-circle-x" aria-hidden="true"/> Hết hàng</p>}
                  <form className="detail-buy" onSubmit={submitProductCart}><label>Số lượng <input type="number" name="quantity" min="1" max={productDetail.product.stockQuantity ?? undefined} defaultValue="1" disabled={!inStock}/></label><button className="primary-link" type="submit" disabled={!inStock}><i className="ti ti-shopping-cart-plus" aria-hidden="true"/> Thêm vào giỏ</button></form>
                  <div className="detail-secondary"><button className="text-link wishlist-action" type="button" onClick={saveProductWishlist}><i className="ti ti-heart" aria-hidden="true"/> Yêu thích</button><a className="text-link" href="/">Tiếp tục mua sắm</a></div>
                  {productActionMessage && <p className="product-feedback" role="status">{productActionMessage}</p>}
                  <section className="rating-box"><h2>Đánh giá sản phẩm</h2>{!productDetail.loggedIn ? <div><p>Bạn cần đăng nhập để gửi đánh giá.</p><a className="primary-link" href="/#/auth?mode=login">Đăng nhập để đánh giá</a></div> : productDetail.reviewed ? <p>Bạn đã đánh giá sản phẩm này. Mỗi tài khoản chỉ được đánh giá một lần.</p> : <form className="rating-form" onSubmit={submitRating}><label>Chọn số sao<select name="ratingValue" defaultValue="5"><option value="5">5 sao</option><option value="4">4 sao</option><option value="3">3 sao</option><option value="2">2 sao</option><option value="1">1 sao</option></select></label><button className="text-link" type="submit">Gửi đánh giá</button></form>}{ratingMessage && <p className="rating-feedback" role="status">{ratingMessage}</p>}</section>
                </div>
              </section>
              {productDetail.relatedProducts.length > 0 && <section className="catalog-section"><div className="section-heading"><div><span className="eyebrow">CÙNG DANH MỤC</span><h2>Sản phẩm liên quan</h2></div></div><div className="product-grid">{productDetail.relatedProducts.map((product) => <ProductTile product={product} key={product.id}/>)}</div></section>}
            </>)}
        </main>
      </div>);
}

export default ProductDetailPage;