import { useEffect, useState } from 'react';
import StoreHeader from '../components/StoreHeader.jsx';
import ProductTile from '../components/ProductTile.jsx';

function HomePage({ catalog, error }) {
    const [activeBanner, setActiveBanner] = useState(0);
    useEffect(() => {
        if (!catalog || catalog.banners.length < 2)
            return;
        const timer = window.setInterval(() => {
            setActiveBanner((current) => (current + 1) % catalog.banners.length);
        }, 4000);
        return () => window.clearInterval(timer);
    }, [catalog]);
    const productGroups = catalog?.categories
        .map((category) => ({
        category,
        products: catalog.products.filter((product) => product.category?.id === category.id),
    }))
        .filter((group) => group.products.length > 0);
    return (<div className="storefront">
      <StoreHeader catalog={catalog}/>

      <main className="shell main-content">
        {error && <div className="notice error" role="alert">{error}</div>}
        {!catalog && !error && <div className="loading">Đang tải cửa hàng...</div>}

        {catalog && (<>
            {catalog.banners.length > 0 ? (<section className="campaign banner-slider" aria-label="Banner">
                <a className="campaign-image" href={`/#${catalog.banners[activeBanner]?.link || '/products'}`}>
                  <img src={catalog.banners[activeBanner]?.imageUrl} alt={catalog.banners[activeBanner]?.title || 'Ưu đãi Shopmart'}/>
                </a>
                {catalog.banners.length > 1 && (<div className="campaign-controls" aria-label="Chọn banner">
                    {catalog.banners.map((banner, index) => (<button key={banner.id} type="button" className={index === activeBanner ? 'active' : ''} onClick={() => setActiveBanner(index)} aria-label={`Banner ${index + 1}`}/>))}
                  </div>)}
              </section>) : (<section className="campaign campaign-fallback">
                <div className="campaign-copy">
                  <h1>Sale hè giảm đến 50%</h1>
                  <p>Áp dụng cho toàn bộ danh mục thời trang, số lượng có hạn</p>
                  <a className="primary-link" href="/#/products">Mua ngay</a>
                </div>
                <i className="ti ti-shopping-bag campaign-art" aria-hidden="true"/>
              </section>)}

            <section className="category-section">
              <h2 className="section-title">Danh mục</h2>
              <div className="category-list">
                {catalog.categories.map((category) => (<a className="category-item" href={`/#/products?categoryId=${category.id}`} key={category.id}>
                    <i className={`ti ${category.icon || 'ti-package'}`} aria-hidden="true"/>
                    <span>{category.name}</span>
                  </a>))}
              </div>
            </section>

            <section className="catalog-section">
              {productGroups?.map(({ category, products }) => (<section className="product-group" key={category.id}>
                  <div className="group-heading"><h2 className="section-title">{category.name}</h2><a href={`/#/products?categoryId=${category.id}`}>Xem tất cả</a></div>
                  <div className="product-grid">
                    {products.map((product) => <ProductTile product={product} key={product.id}/>)}
                  </div>
                </section>))}
              {catalog.products.length === 0 && <p className="empty-state">Chưa có sản phẩm nào để hiển thị.</p>}
            </section>
          </>)}
      </main>
    </div>);
}

export default HomePage;