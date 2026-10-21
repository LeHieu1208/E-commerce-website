import { useState } from 'react';
import AdminFrame from '../../components/admin/AdminFrame.jsx';
import { money } from '../../utils/format.js';
import { useAdminData } from '../../hooks/useAdminData.js';

function AdminRevenuePage() {
    const today = new Date().toISOString().slice(0, 10);
    const monthAgo = new Date(Date.now() - 29 * 86400000).toISOString().slice(0, 10);
    const [range, setRange] = useState({ from: monthAgo, to: today });
    const [url, setUrl] = useState(`/api/admin/revenue?from=${monthAgo}&to=${today}`);
    const report = useAdminData(url);
    const max = Math.max(1, ...(report.data?.dailyRevenue.map((item) => item.amount) || [1]));
    return <AdminFrame title="Báo cáo doanh thu" error={report.error}>
    <form className="admin-toolbar" onSubmit={(event) => { event.preventDefault(); setUrl(`/api/admin/revenue?from=${range.from}&to=${range.to}`); }}><label>Từ<input type="date" value={range.from} onChange={(event) => setRange({ ...range, from: event.target.value })}/></label><label>Đến<input type="date" value={range.to} onChange={(event) => setRange({ ...range, to: event.target.value })}/></label><button className="primary-link" type="submit">Xem báo cáo</button><span className="quick-ranges">{[7, 30, 90].map((days) => <button type="button" className="text-link" key={days} onClick={() => { const from = new Date(Date.now() - (days - 1) * 86400000).toISOString().slice(0, 10); setRange({ from, to: today }); setUrl(`/api/admin/revenue?from=${from}&to=${today}`); }}>{days} ngày</button>)}</span></form>
    {report.data && <><div className="metric-grid revenue-metrics"><div className="metric"><span>Doanh thu trong khoảng chọn</span><strong>{money.format(report.data.totalRevenue)}</strong></div><div className="metric"><span>Số đơn hàng tính doanh thu</span><strong>{report.data.orderCount}</strong></div><div className="metric"><span>Giá trị đơn hàng trung bình</span><strong>{money.format(report.data.averageOrderValue)}</strong></div></div><section className="admin-panel chart-panel"><h2>Doanh thu theo ngày</h2><div className="bar-chart revenue-chart">{report.data.dailyRevenue.map((item) => <div className="bar-column" key={item.label} title={`${item.label}: ${money.format(item.amount)}`}><div className="bar-track"><span style={{ height: `${Math.max(3, item.amount / max * 100)}%` }}/></div><small>{item.label}</small></div>)}</div></section><div className="admin-two-column"><section className="admin-panel"><h2>Sản phẩm bán chạy nhất</h2>{report.data.topProducts.length ? <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Sản phẩm</th><th>SL bán</th><th>Doanh thu</th></tr></thead><tbody>{report.data.topProducts.map((item) => <tr key={item.name}><td>{item.name}</td><td>{item.quantity}</td><td>{money.format(item.revenue)}</td></tr>)}</tbody></table></div> : <p className="empty-note">Không có dữ liệu trong khoảng thời gian này.</p>}</section><section className="admin-panel"><h2>Doanh thu theo danh mục</h2>{report.data.categoryBreakdown.length ? <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Danh mục</th><th>Doanh thu</th></tr></thead><tbody>{report.data.categoryBreakdown.map((item) => <tr key={item.name}><td>{item.name}</td><td>{money.format(item.amount)}</td></tr>)}</tbody></table></div> : <p className="empty-note">Không có dữ liệu trong khoảng thời gian này.</p>}</section></div></>}
  </AdminFrame>;
}

export default AdminRevenuePage;