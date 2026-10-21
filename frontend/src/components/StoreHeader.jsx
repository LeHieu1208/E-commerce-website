import SearchBox from './SearchBox.jsx';

function StoreHeader({ catalog }) {
    return (<header className="site-header">
      <div className="shell header-content">
        <a className="wordmark" href="/">Shopmart</a>
        <SearchBox />
        <nav className="quick-links" aria-label="Tài khoản và giỏ hàng">
          {catalog?.currentUser ? <div className="header-user"><i className="ti ti-user" aria-hidden="true"/><span>Xin chào, <strong>{catalog.currentUser.fullName || catalog.currentUser.email}</strong></span><a href="/logout">Đăng xuất</a></div> : <a className="account-link" href="/#/auth?mode=login"><i className="ti ti-user-circle" aria-hidden="true"/><span>Đăng nhập</span></a>}
          <a href="/#/wishlist" className="icon-link" aria-label="Yêu thích" title="Yêu thích"><i className="ti ti-heart" aria-hidden="true"/></a>
          <a href="/#/cart" className="icon-link cart-link" aria-label="Giỏ hàng" title="Giỏ hàng"><i className="ti ti-shopping-cart" aria-hidden="true"/><span className="count">{catalog?.cartCount ?? 0}</span></a>
          <a href="/#/profile" className="icon-link" aria-label="Thông tin cá nhân" title="Thông tin cá nhân"><i className="ti ti-id" aria-hidden="true"/></a>
          {catalog?.currentUser?.role === 'ROLE_ADMIN' && <a href="/#/admin" className="icon-link" aria-label="Quản trị" title="Quản trị"><i className="ti ti-layout-dashboard" aria-hidden="true"/></a>}
        </nav>
      </div>
    </header>);
}

export default StoreHeader;