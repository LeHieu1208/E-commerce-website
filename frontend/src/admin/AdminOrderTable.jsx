import { money } from '../../utils/format.js';
import { statusLabels, statusList } from '../../utils/orderStatus.js';

function AdminOrderTable({ orders, compact = false, onStatusChange }) {
    return (<div className="admin-table-wrap">
      <table className="admin-table">
        <thead><tr><th>Mã đơn</th><th>Khách hàng</th><th>Thanh toán</th><th>Tổng tiền</th><th>Trạng thái</th>{!compact && <th>Cập nhật</th>}</tr></thead>
        <tbody>{orders.map((order) => (<tr key={order.id}>
            <td><a className="order-code-link" href={`/#/admin/orders/${order.id}`}>{order.orderCode}</a><small>{new Date(order.createdAt).toLocaleDateString('vi-VN')}</small></td>
            <td>{order.fullName}<small>{order.phone}</small></td>
            <td>{order.paymentMethod === 'TRANSFER' ? 'Chuyển khoản' : 'COD'}{order.paid && <small>Đã thu tiền</small>}</td>
            <td>{money.format(order.totalAmount)}</td>
            <td><span className={`status-pill status-${order.status.toLowerCase()}`}>{statusLabels[order.status] || order.status}</span></td>
            {!compact && <td><select aria-label={`Trạng thái đơn ${order.orderCode}`} value={order.status} onChange={(event) => onStatusChange?.(order, event.target.value)}>{statusList.map((status) => <option value={status} key={status}>{statusLabels[status]}</option>)}</select></td>}
          </tr>))}</tbody>
      </table>
    </div>);
}

export default AdminOrderTable;