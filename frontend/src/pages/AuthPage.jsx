import { useState } from 'react';
import StoreHeader from '../components/StoreHeader.jsx';
import { apiRequest } from '../api/client.js';

function AuthPage({ mode, catalog }) {
    const register = mode === 'register';
    const [message, setMessage] = useState('');
    const [busy, setBusy] = useState(false);
    async function submit(event) {
        event.preventDefault();
        setBusy(true);
        setMessage('');
        const values = Object.fromEntries(new FormData(event.currentTarget).entries());
        try {
            await apiRequest(`/api/auth/${register ? 'register' : 'login'}`, {
                method: 'POST',
                body: JSON.stringify(values),
            });
            window.location.assign('/');
        }
        catch (reason) {
            setMessage(reason instanceof Error ? reason.message : 'Không thể đăng nhập lúc này.');
        }
        finally {
            setBusy(false);
        }
    }
    return (<div className="storefront"><StoreHeader catalog={catalog}/><main className="auth-screen">
      <section className="auth-card">
        <h3>{register ? 'Đăng ký' : 'Đăng nhập'}</h3>
        {message && <div className="auth-message" role="alert">{message}</div>}
        <form onSubmit={submit}>
          {register && <input type="text" name="fullName" placeholder="Họ và tên" required/>}
          <input type="email" name="email" placeholder="Email" required/>
          <input type="password" name="password" placeholder="Mật khẩu" required/>
          <button type="submit" disabled={busy}>{busy ? 'Đang xử lý...' : register ? 'Đăng ký' : 'Đăng nhập'}</button>
        </form>
        <div className="auth-switch"><a href={`/#/auth?mode=${register ? 'login' : 'register'}`}>{register ? 'Đã có tài khoản? Đăng nhập' : 'Chưa có tài khoản? Đăng ký'}</a></div>
      </section>
    </main></div>);
}

export default AuthPage;