import { useEffect, useState } from 'react';
import PageFrame from '../components/PageFrame.jsx';
import MapPickerModal from '../components/MapPickerModal.jsx';
import { money } from '../utils/format.js';
import { apiRequest } from '../api/client.js';

function CheckoutPage({ catalog }) {
    const [checkout, setCheckout] = useState(null);
    const [message, setMessage] = useState('');
    const [busy, setBusy] = useState(false);
    const [mapOpen, setMapOpen] = useState(false);
    const [address, setAddress] = useState('');
    const [coordinates, setCoordinates] = useState(null);
    const [paymentMethod, setPaymentMethod] = useState('COD');
    useEffect(() => {
        apiRequest('/api/checkout').then((data) => {
            setCheckout(data);
            setAddress(data.recipient.address || '');
        }).catch((reason) => setMessage(reason instanceof Error ? reason.message : 'Không tải được trang thanh toán.'));
    }, []);
    async function submit(event) {
        event.preventDefault();
        setBusy(true);
        setMessage('');
        const form = new FormData(event.currentTarget);
        const values = {
            fullName: String(form.get('fullName') || ''),
            phone: String(form.get('phone') || ''),
            address,
            note: String(form.get('note') || ''),
            lat: coordinates?.lat ?? null,
            lng: coordinates?.lng ?? null,
            paymentMethod,
        };
        try {
            const order = await apiRequest('/api/checkout', { method: 'POST', body: JSON.stringify(values) });
            window.location.hash = `/checkout/${order.paymentMethod === 'TRANSFER' ? 'payment' : 'success'}/${order.orderCode}`;
        }
        catch (reason) {
            setMessage(reason instanceof Error ? reason.message : 'Không thể tạo đơn hàng.');
        }
        finally {
            setBusy(false);
        }
    }
    const previewQr = checkout
        ? `https://img.vietqr.io/image/VCB-0123456789-compact2.png?amount=${Math.round(checkout.subtotal)}&addInfo=${encodeURIComponent('Thanh toan don hang')}&accountName=${encodeURIComponent('SHOPMART JSC')}`
        : '';
    return (<PageFrame title="Thanh toán" catalog={catalog}>
      <div className="checkout-page">
      <nav className="checkout-breadcrumb" aria-label="Đường dẫn"><a href="/#/cart">Giỏ hàng</a><span>/</span><span>Thanh toán</span></nav>
      <div className="checkout-page-header"><h1>Thanh toán</h1><div className="checkout-stepper"><span className="checkout-step"><i>1</i> Giỏ hàng</span><span className="checkout-step active"><i>2</i> Thông tin</span><span className="checkout-step"><i>3</i> Hoàn tất</span></div></div>
      {message && <p className="notice error" role="alert">{message}</p>}
      {!checkout && !message && <p className="loading">Đang tải thông tin đơn hàng...</p>}
      {checkout && checkout.items.length === 0 && <section className="empty-panel"><h2>Giỏ hàng đang trống</h2><a className="primary-link" href="/#/products">Tiếp tục mua sắm</a></section>}
      {checkout && checkout.items.length > 0 && <div className="checkout-grid"><div><form id="checkoutForm" onSubmit={submit}>
        <section className="checkout-panel"><div className="checkout-panel-head"><h2><i className="ti ti-user" aria-hidden="true"/> Thông tin người nhận</h2></div><div className="checkout-fields"><label className="checkout-field">Họ và tên<input name="fullName" defaultValue={checkout.recipient.fullName || ''} placeholder="Nguyễn Văn A" required/></label><label className="checkout-field">Số điện thoại<input name="phone" type="tel" defaultValue={checkout.recipient.phone || ''} placeholder="0909 123 456" required/></label></div></section>
        <section className="checkout-panel"><div className="checkout-panel-head"><h2><i className="ti ti-map-pin" aria-hidden="true"/> Địa chỉ giao hàng</h2></div><div className="address-row"><label className="checkout-field">Địa chỉ nhận hàng<input name="address" value={address} onChange={(event) => setAddress(event.target.value)} placeholder="Nhập địa chỉ hoặc chọn trên bản đồ" required/></label><button type="button" className="map-button" onClick={() => setMapOpen(true)}><i className="ti ti-map-2" aria-hidden="true"/> Chọn trên bản đồ</button></div><label className="checkout-field note-field">Ghi chú<textarea name="note" placeholder="Ví dụ: Giao giữa giờ hành chính, gọi trước khi giao..."/></label><p className="muted-note">Bạn có thể nhập tay hoặc nhấn “Chọn trên bản đồ” để xác định vị trí chính xác hơn.</p>{coordinates && <p className="coordinate-note">Vị trí đã chọn: {coordinates.lat.toFixed(6)}, {coordinates.lng.toFixed(6)}</p>}</section>
        <section className="checkout-panel"><div className="checkout-panel-head"><h2><i className="ti ti-credit-card" aria-hidden="true"/> Phương thức thanh toán</h2></div><div className="payment-list"><label className={`payment-option ${paymentMethod === 'COD' ? 'active' : ''}`}><input type="radio" name="paymentMethod" value="COD" checked={paymentMethod === 'COD'} onChange={() => setPaymentMethod('COD')}/><div className="payment-info"><div className="payment-title"><i className="ti ti-truck-delivery" aria-hidden="true"/> Thanh toán khi nhận hàng</div><div className="payment-desc">Thanh toán bằng tiền mặt hoặc chuyển khoản trực tiếp khi giao hàng.</div></div></label><label className={`payment-option ${paymentMethod === 'TRANSFER' ? 'active' : ''}`}><input type="radio" name="paymentMethod" value="TRANSFER" checked={paymentMethod === 'TRANSFER'} onChange={() => setPaymentMethod('TRANSFER')}/><div className="payment-info"><div className="payment-title"><i className="ti ti-qrcode" aria-hidden="true"/> Chuyển khoản ngân hàng</div><div className="payment-desc">Quét mã QR để xác nhận thanh toán nhanh chóng.</div>{paymentMethod === 'TRANSFER' && <div className="checkout-qr-preview"><img src={previewQr} alt="QR thanh toán"/><div className="checkout-qr-grid"><div><span>Ngân hàng</span><strong>Vietcombank</strong></div><div><span>Số tài khoản</span><strong>0123456789</strong></div><div><span>Chủ tài khoản</span><strong>SHOPMART JSC</strong></div><div><span>Số tiền</span><strong>{money.format(checkout.subtotal)}</strong></div></div><div className="qr-note"><i className="ti ti-info-circle" aria-hidden="true"/> Nội dung chuyển khoản có mã đơn hàng</div></div>}</div></label></div></section>
      </form></div><aside className="checkout-summary"><h2>Đơn hàng của bạn</h2><div className="checkout-summary-list">{checkout.items.map((item) => <div className="checkout-summary-item" key={item.productId}><div className="checkout-thumb">{item.imageUrl ? <img src={item.imageUrl} alt={item.name}/> : <i className="ti ti-shopping-bag" aria-hidden="true"/>}</div><div className="checkout-summary-info"><span>{item.name}</span><small>SL: {item.quantity}</small></div><strong>{money.format(item.price * item.quantity)}</strong></div>)}</div><div className="checkout-summary-total"><div><span>Tạm tính</span><strong>{money.format(checkout.subtotal)}</strong></div><div><span>Phí vận chuyển</span><strong>Miễn phí</strong></div><div className="total"><span>Tổng cộng</span><strong>{money.format(checkout.subtotal)}</strong></div></div><button type="submit" form="checkoutForm" className="checkout-button" disabled={busy}>{busy ? 'Đang đặt hàng...' : 'Đặt hàng'}</button></aside></div>}
      <MapPickerModal open={mapOpen} onClose={() => setMapOpen(false)} onConfirm={(selectedAddress, selectedCoordinates) => { setAddress(selectedAddress); setCoordinates(selectedCoordinates); setMapOpen(false); }}/>
      </div>
    </PageFrame>);
}

export default CheckoutPage;