import { useEffect, useState } from 'react';
import StoreHeader from '../components/StoreHeader.jsx';
import { apiRequest } from '../api/client.js';

function ProfilePage({ catalog }) {
    const [profile, setProfile] = useState(null);
    const [message, setMessage] = useState('');
    const [saved, setSaved] = useState(false);
    useEffect(() => {
        apiRequest('/api/profile').then(setProfile).catch((reason) => setMessage(reason instanceof Error ? reason.message : 'Không tải được hồ sơ.'));
    }, []);
    async function submit(event) {
        event.preventDefault();
        const values = Object.fromEntries(new FormData(event.currentTarget).entries());
        try {
            setProfile(await apiRequest('/api/profile', { method: 'PUT', body: JSON.stringify(values) }));
            setSaved(true);
            setMessage('');
        }
        catch (reason) {
            setMessage(reason instanceof Error ? reason.message : 'Không thể lưu hồ sơ.');
        }
    }
    return (<div className="storefront"><StoreHeader catalog={catalog}/><main className="legacy-profile">
      <h1>Thông tin cá nhân</h1>
      {message && <div className="legacy-message" role="alert">{message}</div>}
      {!profile && !message && <p>Đang tải...</p>}
      {profile && <form onSubmit={submit}><div className="legacy-field"><label htmlFor="profile-name">Họ tên</label><input id="profile-name" name="fullName" defaultValue={profile.user.fullName || ''} required/></div><div className="legacy-field"><label htmlFor="profile-email">Email</label><input id="profile-email" type="email" value={profile.user.email} readOnly/></div><div className="legacy-field"><label htmlFor="profile-phone">Số điện thoại</label><input id="profile-phone" name="phone" defaultValue={profile.user.phone || ''}/></div><div className="legacy-field"><label htmlFor="profile-address">Địa chỉ</label><input id="profile-address" name="address" defaultValue={profile.user.address || ''}/></div><button type="submit">Lưu thông tin</button>{saved && <div className="legacy-message" role="status">Đã lưu thông tin.</div>}</form>}
      <p className="profile-home-link"><a href="/">Quay về trang chủ</a></p>
    </main></div>);
}

export default ProfilePage;