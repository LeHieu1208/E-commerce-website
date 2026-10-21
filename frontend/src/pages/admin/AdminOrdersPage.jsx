import { useState } from 'react';
import AdminFrame from '../../components/admin/AdminFrame.jsx';
import AdminOrderTable from '../../components/admin/AdminOrderTable.jsx';
import { apiRequest } from '../../api/client.js';
import { useAdminData } from '../../hooks/useAdminData.js';
import { statusLabels, statusList } from '../../utils/orderStatus.js';

function AdminOrdersPage() {
    const [query, setQuery] = useState('');
    const [status, setStatus] = useState('');
    const [paymentMethod, setPaymentMethod] = useState('');
    const [searchUrl, setSearchUrl] = useState('/api/admin/orders');
    const orders = useAdminData(searchUrl);
    async function update(order, newStatus) {
        try {
            await apiRequest(`/api/admin/orders/${order.id}`, { method: 'PATCH', body: JSON.stringify({ newStatus, trackingCode: order.trackingCode, adminNote: order.adminNote, cancelReason: order.cancelReason }) });
            orders.reload();
        }
        catch (reason) {
            orders.setError(reason instanceof Error ? reason.message : 'Không thể cập nhật đơn hàng.');
        }
    }
    function search(event) { event.preventDefault(); const params = new URLSearchParams(); if (query)
        params.set('keyword', query); if (status)
        params.set('status', status); if (paymentMethod)
        params.set('paymentMethod', paymentMethod); setSearchUrl(`/api/admin/orders?${params}`); }
    function reset() { setQuery(''); setStatus(''); setPaymentMethod(''); setSearchUrl('/api/admin/orders'); }
    return <AdminFrame title="Đơn hàng" error={orders.error}><form className="admin-toolbar" onSubmit={search}><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Mã đơn, tên khách, số điện thoại"/><select value={status} onChange={(event) => setStatus(event.target.value)}><option value="">Tất cả trạng thái</option>{statusList.map((item) => <option key={item} value={item}>{statusLabels[item]}</option>)}</select><select value={paymentMethod} onChange={(event) => setPaymentMethod(event.target.value)}><option value="">Tất cả thanh toán</option><option value="COD">COD</option><option value="TRANSFER">Chuyển khoản</option></select><button className="primary-link" type="submit"><i className="ti ti-filter" aria-hidden="true"/> Lọc</button><button className="text-link" type="button" onClick={reset}>Xóa lọc</button></form><section className="admin-panel table-panel"><div className="panel-heading"><h2>Danh sách đơn hàng</h2><span>Tìm thấy {orders.data?.length ?? 0} đơn hàng</span></div>{orders.loading ? <p className="loading">Đang tải đơn hàng...</p> : orders.data && <AdminOrderTable orders={orders.data} onStatusChange={update}/>}{!orders.loading && orders.data?.length === 0 && <p className="empty-note">Không tìm thấy đơn hàng nào phù hợp.</p>}</section></AdminFrame>;
}

export default AdminOrdersPage;