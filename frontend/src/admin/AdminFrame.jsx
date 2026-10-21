const adminNav = [
    { path: '/admin', label: 'Tổng quan', icon: 'ti-layout-dashboard' },
    { path: '/admin/orders', label: 'Đơn hàng', icon: 'ti-receipt' },
    { path: '/admin/products', label: 'Sản phẩm', icon: 'ti-package' },
    { path: '/admin/categories', label: 'Danh mục', icon: 'ti-category' },
    { path: '/admin/banners', label: 'Quảng cáo', icon: 'ti-ad-2' },
    { path: '/admin/revenue', label: 'Doanh thu', icon: 'ti-chart-bar' },
];
function AdminFrame({ children, title, error }) {
    const currentPath = window.location.hash.slice(1).split('?')[0] || window.location.pathname;
    return (<div className="admin-app">
      <aside className="admin-sidebar"><a href="/" className="admin-brand"><i className="ti ti-shopping-bag" aria-hidden="true"/><span>Shopmart</span></a><nav>{adminNav.map((item) => <a key={item.path} className={currentPath === item.path || (item.path !== '/admin' && currentPath.startsWith(`${item.path}/`)) ? 'active' : ''} href={`/#${item.path}`}><i className={`ti ${item.icon}`} aria-hidden="true"/><span>{item.label}</span></a>)}</nav><div className="admin-sidebar-divider"/><a href="/" className="back-store">← Về trang bán hàng</a><a href="/logout" className="back-store"><i className="ti ti-logout" aria-hidden="true"/> Đăng xuất</a></aside>
      <main className="admin-main"><header className="admin-topbar"><div><span className="eyebrow">SHOPMART · QUẢN TRỊ</span><h1>{title}</h1></div><a href="/" className="admin-user"><i className="ti ti-user-circle" aria-hidden="true"/> Cửa hàng</a></header><div className="admin-content">{error && <p className="notice error" role="alert">{error}</p>}{children}</div></main>
    </div>);
}

export default AdminFrame;