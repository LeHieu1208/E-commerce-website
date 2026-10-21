import { lazy, Suspense } from 'react';
import { useCatalog } from './hooks/useCatalog.js';
import { useRoute } from './hooks/useRoute.js';
import AuthPage from './pages/AuthPage.jsx';
import CartPage from './pages/CartPage.jsx';
import HomePage from './pages/HomePage.jsx';
import OrderPage from './pages/OrderPage.jsx';
import ProductDetailPage from './pages/ProductDetailPage.jsx';
import ProductListPage from './pages/ProductListPage.jsx';
import ProfilePage from './pages/ProfilePage.jsx';
import WishlistPage from './pages/WishlistPage.jsx';

// Các trang nặng (bản đồ Leaflet, khu quản trị) chỉ tải khi người dùng mở tới.
const CheckoutPage = lazy(() => import('./pages/CheckoutPage.jsx'));
const AdminPage = lazy(() => import('./pages/admin/AdminPage.jsx'));

function App() {
    const { catalog, error } = useCatalog();
    const url = useRoute();
    const path = url.pathname;
    const productMatch = path.match(/^\/products\/(\d+)$/);
    const orderMatch = path.match(/^\/checkout\/(payment|success)\/([^/]+)$/);
    let page;
    if (productMatch)
        page = <ProductDetailPage key={productMatch[1]} catalog={catalog} productId={productMatch[1]}/>;
    else if (path.startsWith('/products'))
        page = <ProductListPage catalog={catalog} search={url.search}/>;
    else if (path === '/auth')
        page = <AuthPage mode={url.searchParams.get('mode') || 'login'} catalog={catalog}/>;
    else if (path === '/cart')
        page = <CartPage catalog={catalog}/>;
    else if (path === '/wishlist')
        page = <WishlistPage catalog={catalog}/>;
    else if (path === '/profile')
        page = <ProfilePage catalog={catalog}/>;
    else if (path === '/checkout')
        page = <CheckoutPage catalog={catalog}/>;
    else if (orderMatch)
        page = <OrderPage orderCode={orderMatch[2]} pending={orderMatch[1] === 'payment'} catalog={catalog}/>;
    else if (path.startsWith('/admin'))
        page = <AdminPage path={path}/>;
    else
        page = <HomePage catalog={catalog} error={error}/>;
    return <Suspense fallback={<p className="loading">Đang tải...</p>}>{page}</Suspense>;
}
export default App;