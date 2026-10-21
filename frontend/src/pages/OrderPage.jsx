import { useEffect, useState } from 'react';
import StoreHeader from '../components/StoreHeader.jsx';
import { money } from '../utils/format.js';
import { apiRequest } from '../api/client.js';

function OrderPage({ orderCode, pending, catalog }) {
    const [order, setOrder] = useState(null);
    const [message, setMessage] = useState('');
    const [pollKey, setPollKey] = useState(0);
    const [checking, setChecking] = useState(false);
    useEffect(() => {
        let active = true;
        let timer = 0;
        const loadOrder = async () => {
            try {
                const data = await apiRequest(`/api/checkout/orders/${encodeURIComponent(orderCode)}`);
                if (!active)
                    return;
                setOrder(data);
                setChecking(false);
                if (pending && data.paid) {
                    window.location.hash = `/checkout/success/${data.orderCode}`;
                    return;
                }
                if (pending)
                    timer = window.setTimeout(loadOrder, 3000);
            }
            catch (reason) {
                setChecking(false);
                if (active)
                    setMessage(reason instanceof Error ? reason.message : 'Không tải được đơn hàng.');
            }
        };
        void loadOrder();
        return () => { active = false; window.clearTimeout(timer); };
    }, [orderCode, pending, pollKey]);
    return (<div className="order-page">
      <StoreHeader catalog={catalog}/>
      <main className={`order-container ${pending ? 'pending-container' : 'success-container'}`}>
        {message && <p className="notice error" role="alert">{message}</p>}
        {!order && !message && <p className="loading">Đang tải thông tin đơn hàng...</p>}
        {order && (pending ? <section className="order-card pending-card">
          <div className={`status-pill ${order.paid ? 'paid' : ''}`}>{order.paid ? <i className="ti ti-check"/> : <span className="spinner"/>} {order.paid ? 'Đã nhận được thanh toán' : 'Đang chờ thanh toán'}</div>
          <h1>{order.paid ? 'Đã nhận được thanh toán' : 'Quét mã để hoàn tất đơn hàng'}</h1>
          <p className="order-sub">Mã đơn hàng: <strong>{order.orderCode}</strong> — trang sẽ tự động chuyển tiếp ngay khi nhận được tiền</p>
          {!order.paid && order.qrUrl && <><div className="qr-wrap"><img src={order.qrUrl} alt="Mã QR chuyển khoản VietQR"/></div><div className="bank-info"><div><span>Ngân hàng</span><strong>{order.bankId}</strong></div><div><span>Số tài khoản</span><strong>{order.bankAccountNo}</strong></div><div><span>Chủ tài khoản</span><strong>{order.bankAccountName}</strong></div><div><span>Số tiền</span><strong className="amount-highlight">{money.format(order.totalAmount)}</strong></div><div><span>Nội dung CK</span><strong>Thanh toan don hang {order.orderCode}</strong></div></div><p className="payment-note"><i className="ti ti-info-circle"/> Vui lòng giữ nguyên nội dung chuyển khoản và chuyển đúng số tiền để hệ thống tự động đối soát. Đơn hàng sẽ được xác nhận trong vòng vài giây sau khi ngân hàng báo có.</p><div className="order-actions"><button type="button" className="order-secondary-button" disabled={checking} onClick={() => { setChecking(true); setPollKey((key) => key + 1); }}>{checking ? 'Đang kiểm tra...' : 'Tôi đã chuyển khoản, kiểm tra ngay'}</button><a className="order-secondary-button" href="/#/cart">Huỷ, quay lại giỏ hàng</a></div></>}
        </section> : <section className="order-card success-card">
          <div className="success-icon"><i className="ti ti-check"/></div>
          <h1>Đặt hàng thành công!</h1>
          <p>Mã đơn hàng của bạn: <strong className="order-code">{order.orderCode}</strong></p>
          <div className="order-box"><div className="order-row"><span>Người nhận</span><strong>{order.fullName}</strong></div><div className="order-row"><span>Số điện thoại</span><strong>{order.phone}</strong></div><div className="order-row"><span>Địa chỉ giao hàng</span><strong>{order.address}</strong></div><div className="order-row"><span>Phương thức thanh toán</span><strong>{order.paymentMethod === 'TRANSFER' ? 'Chuyển khoản ngân hàng' : 'Thanh toán khi nhận hàng (COD)'}</strong></div><div className="order-row"><span>Tổng tiền</span><strong>{money.format(order.totalAmount)}</strong></div>{order.paymentMethod === 'TRANSFER' && <div className="order-row"><span>Thanh toán chuyển khoản</span><span className={`status-pill ${order.paid ? 'paid' : ''}`}>{order.paid ? <><i className="ti ti-check"/> Đã nhận được tiền</> : 'Đang chờ thanh toán'}</span></div>}{order.paymentMethod === 'TRANSFER' && order.paidAt && <div className="order-row"><span>Thời gian thanh toán</span><strong>{new Date(order.paidAt).toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' })}</strong></div>}<div className="order-row"><span>Trạng thái đơn hàng</span><span className="status-pill">{order.status === 'DA_THANH_TOAN' ? 'Đã thanh toán - chờ xử lý' : 'Chờ xác nhận'}</span></div></div>
          {order.paymentMethod === 'TRANSFER' && order.qrUrl && !order.paid && <div className="success-qr"><h3>Thanh toán chuyển khoản</h3><img src={order.qrUrl} alt="Mã QR thanh toán"/></div>}
          <div className="order-actions"><a className="order-secondary-button" href="/">Tiếp tục mua sắm</a><a className="order-primary-button" href="/#/profile">Xem đơn hàng của tôi</a></div>
        </section>)}
      </main>
    </div>);
}

export default OrderPage;