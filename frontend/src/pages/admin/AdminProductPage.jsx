import { useEffect, useState } from 'react';
import AdminFrame from '../../components/admin/AdminFrame.jsx';
import { money } from '../../utils/format.js';
import { apiRequest } from '../../api/client.js';
import { useAdminData } from '../../hooks/useAdminData.js';

function AdminProductPage({ path }) {
    const [query, setQuery] = useState('');
    const [categoryFilter, setCategoryFilter] = useState('');
    const [productsUrl, setProductsUrl] = useState('/api/admin/products');
    const products = useAdminData(productsUrl);
    const categories = useAdminData('/api/admin/categories');
    const [editing, setEditing] = useState(null);
    const [message, setMessage] = useState('');
    const editId = Number(path.match(/^\/admin\/products\/(\d+)\/edit$/)?.[1]) || null;
    const isFormPage = path.endsWith('/new') || editId !== null;
    useEffect(() => {
        if (editId === null)
            setEditing(null);
        else if (products.data)
            setEditing(products.data.find((product) => product.id === editId) || null);
    }, [editId, products.data]);
    async function save(event) {
        event.preventDefault();
        const form = Object.fromEntries(new FormData(event.currentTarget).entries());
        const payload = { ...form, price: Number(form.price), oldPrice: form.oldPrice ? Number(form.oldPrice) : null, discountPercent: form.discountPercent ? Number(form.discountPercent) : null, stockQuantity: Number(form.stockQuantity), categoryId: form.categoryId ? Number(form.categoryId) : null };
        try {
            await apiRequest(`/api/admin/products${editId ? `/${editId}` : ''}`, { method: editId ? 'PUT' : 'POST', body: JSON.stringify(payload) });
            window.location.hash = '/admin/products';
            products.reload();
        }
        catch (reason) {
            setMessage(reason instanceof Error ? reason.message : 'Không thể lưu sản phẩm.');
        }
    }
    async function remove(id) {
        if (!window.confirm('Xóa sản phẩm này?'))
            return;
        try {
            await apiRequest(`/api/admin/products/${id}`, { method: 'DELETE' });
            products.reload();
            setMessage('Đã xóa sản phẩm.');
        }
        catch (reason) {
            setMessage(reason instanceof Error ? reason.message : 'Không thể xóa sản phẩm.');
        }
    }
    return <AdminFrame title="Sản phẩm" error={products.error || categories.error}>
    {isFormPage && <><a className="text-link admin-back-link" href="/#/admin/products"><i className="ti ti-arrow-left" aria-hidden="true"/> Quay lại danh sách sản phẩm</a><div className="admin-panel"><div className="panel-heading"><h2>{editId ? 'Sửa sản phẩm' : 'Thêm sản phẩm mới'}</h2></div>{message && <p className="admin-feedback" role="status">{message}</p>}{editId && products.loading && <p>Đang tải sản phẩm...</p>}{(!editId || editing) && <form key={editId ?? 'new'} className="admin-form-grid" onSubmit={save}><label>Tên sản phẩm<input name="name" defaultValue={editing?.name || ''} required/></label><label>Danh mục<select name="categoryId" defaultValue={editing?.categoryId ?? ''}><option value="">Chọn danh mục</option>{categories.data?.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label><label>Giá bán<input name="price" type="number" min="0" step="1" defaultValue={editing?.price ?? ''} required/></label><label>Giá cũ<input name="oldPrice" type="number" min="0" step="1" defaultValue={editing?.oldPrice ?? ''}/></label><label>Giảm giá (%)<input name="discountPercent" type="number" min="0" max="100" defaultValue={editing?.discountPercent ?? ''}/></label><label>Tồn kho<input name="stockQuantity" type="number" min="0" defaultValue={editing?.stockQuantity ?? 0}/></label><label className="span-2">URL hình ảnh<input name="imageUrl" type="url" defaultValue={editing?.imageUrl || ''}/></label><label className="span-2">Mô tả<textarea name="description" rows={2} defaultValue={editing?.description || ''}/></label><button className="primary-link" type="submit">{editId ? 'Lưu thay đổi' : 'Thêm sản phẩm'}</button></form>}</div></>}
    {!isFormPage && <><form className="admin-toolbar" onSubmit={(event) => { event.preventDefault(); const params = new URLSearchParams(); if (query)
        params.set('keyword', query); if (categoryFilter)
        params.set('categoryId', categoryFilter); setProductsUrl(`/api/admin/products?${params}`); }}><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm theo tên sản phẩm..."/><select value={categoryFilter} onChange={(event) => setCategoryFilter(event.target.value)}><option value="">Tất cả danh mục</option>{categories.data?.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select><button className="primary-link" type="submit"><i className="ti ti-search" aria-hidden="true"/> Lọc</button><a className="primary-link" href="/#/admin/products/new"><i className="ti ti-plus" aria-hidden="true"/> Thêm sản phẩm</a></form><section className="admin-panel table-panel"><div className="panel-heading"><h2>Danh sách sản phẩm</h2><span>{products.data?.length ?? 0} sản phẩm</span></div><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Sản phẩm</th><th>Danh mục</th><th>Giá</th><th>Tồn kho</th><th></th></tr></thead><tbody>{products.data?.map((item) => <tr key={item.id}><td><div className="prod-cell">{item.imageUrl ? <img src={item.imageUrl} alt=""/> : <span className="noimg"><i className="ti ti-photo" aria-hidden="true"/></span>}<strong>{item.name}</strong></div></td><td>{item.categoryName || '—'}</td><td>{money.format(item.price)}</td><td className={item.stockQuantity < 5 ? 'stock-low' : 'stock-ok'}>{item.stockQuantity}</td><td className="row-actions"><a href={`/#/admin/products/${item.id}/edit`} aria-label={`Sửa ${item.name}`}><i className="ti ti-edit" aria-hidden="true"/></a><button onClick={() => remove(item.id)} title="Xóa"><i className="ti ti-trash" aria-hidden="true"/></button></td></tr>)}</tbody></table></div>{products.data?.length === 0 && <p className="empty-note">Không có sản phẩm nào phù hợp.</p>}</section></>}
  </AdminFrame>;
}

export default AdminProductPage;