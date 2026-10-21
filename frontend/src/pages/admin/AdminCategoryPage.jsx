import { useEffect, useState } from 'react';
import AdminFrame from '../../components/admin/AdminFrame.jsx';
import { apiRequest } from '../../api/client.js';
import { useAdminData } from '../../hooks/useAdminData.js';

function AdminCategoryPage({ path }) {
    const categories = useAdminData('/api/admin/categories');
    const [editing, setEditing] = useState(null);
    const [message, setMessage] = useState('');
    const editId = Number(path.match(/^\/admin\/categories\/(\d+)\/edit$/)?.[1]) || null;
    useEffect(() => {
        if (editId === null)
            setEditing(null);
        else if (categories.data)
            setEditing(categories.data.find((category) => category.id === editId) || null);
    }, [editId, categories.data]);
    async function save(event) {
        event.preventDefault();
        const payload = Object.fromEntries(new FormData(event.currentTarget).entries());
        try {
            await apiRequest(`/api/admin/categories${editId ? `/${editId}` : ''}`, { method: editId ? 'PUT' : 'POST', body: JSON.stringify(payload) });
            window.location.hash = '/admin/categories';
            categories.reload();
            setMessage('Đã lưu danh mục.');
        }
        catch (reason) {
            setMessage(reason instanceof Error ? reason.message : 'Không thể lưu danh mục.');
        }
    }
    async function remove(id) {
        if (!window.confirm('Xóa danh mục này?'))
            return;
        try {
            await apiRequest(`/api/admin/categories/${id}`, { method: 'DELETE' });
            categories.reload();
            setMessage('Đã xóa danh mục.');
        }
        catch (reason) {
            setMessage(reason instanceof Error ? reason.message : 'Không thể xóa danh mục.');
        }
    }
    return <AdminFrame title="Danh mục sản phẩm" error={categories.error}><div className="admin-two-column"><section className="admin-panel"><h2>Danh sách danh mục</h2>{message && <p className="admin-feedback" role="status">{message}</p>}{categories.data?.length === 0 && <p className="empty-note">Chưa có danh mục nào.</p>}<div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Danh mục</th><th>Số sản phẩm</th><th></th></tr></thead><tbody>{categories.data?.map((item) => <tr key={item.id}><td><strong><i className={`ti ${item.icon || 'ti-category'}`} aria-hidden="true"/> {item.name}</strong></td><td>{item.productCount} sản phẩm</td><td className="row-actions"><a href={`/#/admin/categories/${item.id}/edit`}>Sửa</a><button onClick={() => remove(item.id)} title="Xóa"><i className="ti ti-trash" aria-hidden="true"/></button></td></tr>)}</tbody></table></div></section><section className="admin-panel"><h2>{editId ? 'Sửa danh mục' : 'Thêm danh mục mới'}</h2>{editId && categories.loading && <p>Đang tải danh mục...</p>}{(!editId || editing) && <form key={editId ?? 'new'} className="admin-form-grid single-column" onSubmit={save}><label>Tên danh mục<input name="name" defaultValue={editing?.name || ''} required/></label><label>Icon (Tabler icon)<input name="icon" placeholder="ti-device-mobile" defaultValue={editing?.icon || ''}/></label><label>Mô tả<textarea name="description" rows={3} defaultValue={editing?.description || ''}/></label><button className="primary-link" type="submit">{editId ? 'Lưu thay đổi' : 'Thêm danh mục'}</button></form>}{editId && <a className="text-link cancel-link" href="/#/admin/categories">Hủy, thêm mới thay vào đó</a>}</section></div></AdminFrame>;
}

export default AdminCategoryPage;