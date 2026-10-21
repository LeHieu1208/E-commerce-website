import { useState } from 'react';
import AdminFrame from '../../components/admin/AdminFrame.jsx';
import { money } from '../../utils/format.js';
import { apiRequest } from '../../api/client.js';
import { useAdminData } from '../../hooks/useAdminData.js';
import { statusLabels, statusList } from '../../utils/orderStatus.js';

function AdminOrderDetailPage({ orderId }) {
    const orders = useAdminData('/api/admin/orders');
    const order = orders.data?.find((item) => item.id === orderId);
    const [message, setMessage] = useState('');
    async function save(event) {
        event.preventDefault();
        const values = Object.fromEntries(new FormData(event.currentTarget).entries());
        try {
            await apiRequest(`/api/admin/orders/${orderId}`, { method: 'PATCH', body: JSON.stringify(values) });
            setMessage('Đã cập nhật đơn hàng.');
            orders.reload();
        }
        catch (reason) {
            setMessage(reason instanceof Error ? reason.message : 'Không thể cập nhật đơn hàng.');
        }
    }
    return <AdminFrame title="Chi tiết đơn hàng" error={orders.error}><a className="text-link" href="/#/admin/orders"><i className="ti ti-arrow-left" aria-hidden="true"/> Quay lại danh sách</a>{orders.loading && <p className="loading">Đang tải đơn hàng...</p>}{!orders.loading && !order && <p className="notice error">Không tìm thấy đơn hàng.</p>}{order && <><div className="admin-order-detail-grid"><section className="admin-panel"><div className="panel-heading"><h2>Thông tin người nhận</h2><strong>{order.orderCode}</strong></div><dl className="admin-order-facts"><dt>Khách hàng</dt><dd>{order.fullName}</dd><dt>Số điện thoại</dt><dd>{order.phone}</dd><dt>Địa chỉ giao hàng</dt><dd>{order.address}</dd><dt>Ngày đặt</dt><dd>{new Date(order.createdAt).toLocaleString('vi-VN')}</dd><dt>Thanh toán</dt><dd>{order.paymentMethod === 'TRANSFER' ? 'Chuyển khoản' : 'Thanh toán khi nhận hàng'}{order.paid ? ' · Đã thanh toán' : ''}</dd><dt>Tổng tiền</dt><dd>{money.format(order.totalAmount)}</dd></dl></section><section className="admin-panel"><div className="panel-heading"><h2>Cập nhật xử lý</h2></div>{message && <p className="admin-feedback" role="status">{message}</p>}<form className="order-edit-form" onSubmit={save}><label>Trạng thái<select name="newStatus" defaultValue={order.status}>{statusList.map((status) => <option value={status} key={status}>{statusLabels[status]}</option>)}</select></label><label>Mã vận đơn<input name="trackingCode" defaultValue={order.trackingCode || ''} placeholder="VD: GH123456789"/></label><label>Ghi chú của admin<textarea name="adminNote" rows={3} defaultValue={order.adminNote || ''}/></label><label>Lý do hủy<textarea name="cancelReason" rows={2} defaultValue={order.cancelReason || ''}/></label><button className="primary-link" type="submit">Cập nhật đơn hàng</button></form></section></div><section className="admin-panel table-panel"><div className="panel-heading"><h2>Sản phẩm trong đơn</h2></div><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Sản phẩm</th><th>Số lượng</th><th>Đơn giá</th><th>Thành tiền</th></tr></thead><tbody>{order.items.map((item, index) => <tr key={`${item.productName}-${index}`}><td>{item.productName}</td><td>{item.quantity}</td><td>{money.format(item.price)}</td><td>{money.format(item.subtotal)}</td></tr>)}</tbody></table></div></section></>}</AdminFrame>;
}

export default AdminOrderDetailPage;