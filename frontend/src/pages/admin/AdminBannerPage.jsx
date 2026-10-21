import { useEffect, useState } from 'react';
import AdminFrame from '../../components/admin/AdminFrame.jsx';
import { apiRequest } from '../../api/client.js';
import { useAdminData } from '../../hooks/useAdminData.js';

function AdminBannerPage({ path }) {
    const banners = useAdminData('/api/admin/banners');
    const [editing, setEditing] = useState(null);
    const [message, setMessage] = useState('');
    const editId = Number(path.match(/^\/admin\/banners\/(\d+)\/edit$/)?.[1]) || null;
    const isFormPage = path.endsWith('/new') || editId !== null;
    const [previewUrl, setPreviewUrl] = useState('');
    useEffect(() => {
        if (editId === null) {
            setEditing(null);
            setPreviewUrl('');
        }
        else if (banners.data) {
            const banner = banners.data.find((item) => item.id === editId) || null;
            setEditing(banner);
            setPreviewUrl(banner?.imageUrl || '');
        }
    }, [editId, banners.data]);
    async function save(event) {
        event.preventDefault();
        const payload = Object.fromEntries(new FormData(event.currentTarget).entries());
        try {
            await apiRequest(`/api/admin/banners${editId ? `/${editId}` : ''}`, { method: editId ? 'PUT' : 'POST', body: JSON.stringify(payload) });
            window.location.hash = '/admin/banners';
            banners.reload();
        }
        catch (reason) {
            setMessage(reason instanceof Error ? reason.message : 'Không thể lưu banner.');
        }
    }
    async function remove(id) {
        if (!window.confirm('Xóa banner này?'))
            return;
        try {
            await apiRequest(`/api/admin/banners/${id}`, { method: 'DELETE' });
            banners.reload();
            setMessage('Đã xóa banner.');
        }
        catch (reason) {
            setMessage(reason instanceof Error ? reason.message : 'Không thể xóa banner.');
        }
    }
    return <AdminFrame title="Banner quảng cáo" error={banners.error}>
    {message && <p className="admin-feedback" role="status">{message}</p>}
    {isFormPage ? <><a className="text-link admin-back-link" href="/#/admin/banners"><i className="ti ti-arrow-left" aria-hidden="true"/> Quay lại danh sách banner</a><section className="admin-panel admin-banner-form-panel"><h2>{editId ? 'Sửa banner' : 'Thêm banner mới'}</h2>{editId && banners.loading && <p>Đang tải banner...</p>}{(!editId || editing) && <form key={editId ?? 'new'} className="admin-form-grid single-column" onSubmit={save}><label>Tiêu đề banner<input name="title" defaultValue={editing?.title || ''} required/></label><label>Link khi nhấn vào<input name="link" defaultValue={editing?.link || '/products'} required/></label><label>URL hình ảnh<input name="imageUrl" type="url" defaultValue={editing?.imageUrl || ''} onChange={(event) => setPreviewUrl(event.target.value)} placeholder="https://..." required/></label>{previewUrl && <div className="admin-banner-preview"><img src={previewUrl} alt="Preview banner" onError={(event) => { event.currentTarget.hidden = true; }}/></div>}<button className="primary-link" type="submit">{editId ? 'Lưu thay đổi' : 'Thêm banner'}</button></form>}</section></> : <><section className="admin-panel banner-list-toolbar"><span>Các banner sẽ tự động chuyển đổi mỗi 4 giây ở trang chủ, theo đúng thứ tự bên dưới.</span><a className="primary-link" href="/#/admin/banners/new"><i className="ti ti-plus" aria-hidden="true"/> Thêm banner</a></section>{banners.data?.length === 0 && <section className="admin-panel"><p className="empty-note">Chưa có banner nào. Trang chủ sẽ hiển thị banner mặc định.</p></section>}<section className="banner-admin-grid">{banners.data?.map((item) => <article className="banner-admin-item" key={item.id}><img src={item.imageUrl} alt={item.title}/><div><strong>{item.title}</strong><span>{item.link}</span><div className="row-actions"><a href={`/#/admin/banners/${item.id}/edit`}>Sửa</a><button onClick={() => remove(item.id)} title="Xóa">Xóa</button></div></div></article>)}</section></>}
  </AdminFrame>;
}

export default AdminBannerPage;