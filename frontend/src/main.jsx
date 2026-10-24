import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './index.css';
import './styles/layout.css';
import './styles/home.css';
import './styles/product-card.css';
import './styles/product-detail.css';
import './styles/auth.css';
import './styles/wishlist.css';
import './styles/cart-profile.css';
import './styles/checkout.css';
import './styles/admin.css';
import './styles/footer.css';
import './styles/responsive.css';
import './styles/order.css';
import './styles/responsive-mobile.css';
import './styles/search-dropdown.css';
import './styles/product-list.css';
import './styles/product-detail-extras.css';
import './styles/admin-extras.css';
import './styles/theme.css';
import App from './App.jsx';

createRoot(document.getElementById('root')).render(<StrictMode>
    <App />
  </StrictMode>);