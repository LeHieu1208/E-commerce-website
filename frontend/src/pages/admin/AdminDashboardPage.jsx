import AdminFrame from '../../components/admin/AdminFrame.jsx';
import AdminOrderTable from '../../components/admin/AdminOrderTable.jsx';
import { money } from '../../utils/format.js';
import { useAdminData } from '../../hooks/useAdminData.js';

function AdminDashboardPage() {
    const { data, error, loading } = useAdminData('/api/admin/dashboard');
    const max = Math.max(1, ...(data?.dailyRevenue.map((item) => item.amount) || [1]));
    return <AdminFrame title="Dashboard" error={error}>
    {loading && <p className="loading">Đang tải dữ liệu...</p>}
    {data && <>
      <div className="metric-grid dashboard-metrics">
        <div className="metric coral"><span><i className="ti ti-cash"/> Doanh thu hôm nay</span><strong>{money.format(data.revenueToday)}</strong></div>
        <div className="metric amber"><span><i className="ti ti-calendar"/> Doanh thu tháng này</span><strong>{money.format(data.revenueMonth)}</strong></div>
        <div className="metric green"><span><i className="ti ti-chart-bar"/> Tổng doanh thu</span><strong>{money.format(data.revenueTotal)}</strong></div>
        <div className="metric"><span><i className="ti ti-receipt"/> Tổng đơn hàng</span><strong>{data.totalOrders}</strong></div>
        <div className="metric amber"><span><i className="ti ti-clock"/> Đang xử lý</span><strong>{data.pendingOrders}</strong></div>
        <div className="metric red"><span><i className="ti ti-x"/> Đã hủy</span><strong>{data.cancelledOrders}</strong></div>
      </div>
      <div className="admin-dashboard-grid">
        <div>
          <section className="admin-panel"><div className="panel-heading"><h2>Doanh thu 14 ngày gần nhất</h2><a href="/#/admin/revenue">Báo cáo <i className="ti ti-arrow-right"/></a></div><div className="bar-chart">{data.dailyRevenue.map((item) => <div className="bar-column" key={item.label} title={`${item.label}: ${money.format(item.amount)}`}><div className="bar-track"><span style={{ height: `${Math.max(3, item.amount / max * 100)}%` }}/></div><small>{item.label}</small></div>)}</div></section>
          <section className="admin-panel table-panel"><div className="panel-heading"><h2>Đơn hàng gần đây</h2><a href="/#/admin/orders">Xem tất cả <i className="ti ti-arrow-right"/></a></div><AdminOrderTable orders={data.recentOrders} compact/></section>
        </div>
        <div>
          <section className="admin-panel"><h2>Sản phẩm bán chạy</h2>{data.topProducts.length ? <table className="admin-table"><thead><tr><th>Sản phẩm</th><th>SL</th></tr></thead><tbody>{data.topProducts.map((product) => <tr key={product.name}><td>{product.name}</td><td>{product.quantity}</td></tr>)}</tbody></table> : <p className="empty-note">Chưa có dữ liệu bán hàng.</p>}</section>
          <section className="admin-panel"><h2>Sắp hết hàng <a href="/#/admin/products">Quản lý →</a></h2>{data.lowStock.length ? <table className="admin-table"><thead><tr><th>Sản phẩm</th><th>Tồn kho</th></tr></thead><tbody>{data.lowStock.map((product) => <tr key={product.id}><td>{product.name}</td><td className="stock-low">{product.stockQuantity}</td></tr>)}</tbody></table> : <p className="empty-note">Không có sản phẩm sắp hết hàng.</p>}</section>
        </div>
      </div>
    </>}
  </AdminFrame>;
}

export default AdminDashboardPage;