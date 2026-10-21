import AdminDashboardPage from './AdminDashboardPage.jsx';
import AdminProductPage from './AdminProductPage.jsx';
import AdminCategoryPage from './AdminCategoryPage.jsx';
import AdminBannerPage from './AdminBannerPage.jsx';
import AdminOrdersPage from './AdminOrdersPage.jsx';
import AdminOrderDetailPage from './AdminOrderDetailPage.jsx';
import AdminRevenuePage from './AdminRevenuePage.jsx';

function AdminPage({ path }) {
    const orderDetail = path.match(/^\/admin\/orders\/(\d+)$/);
    if (orderDetail)
        return <AdminOrderDetailPage orderId={Number(orderDetail[1])}/>;
    if (path.startsWith('/admin/products'))
        return <AdminProductPage path={path}/>;
    if (path.startsWith('/admin/categories'))
        return <AdminCategoryPage path={path}/>;
    if (path.startsWith('/admin/banners'))
        return <AdminBannerPage path={path}/>;
    if (path.startsWith('/admin/orders'))
        return <AdminOrdersPage />;
    if (path.startsWith('/admin/revenue'))
        return <AdminRevenuePage />;
    return <AdminDashboardPage />;
}

export default AdminPage;