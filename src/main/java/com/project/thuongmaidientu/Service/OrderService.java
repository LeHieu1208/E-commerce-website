package com.project.thuongmaidientu.Service;

import com.project.thuongmaidientu.Model.CartItem;
import com.project.thuongmaidientu.Model.Order;
import com.project.thuongmaidientu.Model.OrderItem;
import com.project.thuongmaidientu.Model.User;
import com.project.thuongmaidientu.Repository.OrderRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OrderService {

    // Thông tin ngân hàng nhận thanh toán - dùng để sinh mã QR VietQR.
    // Đổi sang tài khoản thật khi triển khai thật.
    private static final String BANK_ID = "VCB";
    private static final String BANK_ACCOUNT_NO = "0123456789";
    private static final String BANK_ACCOUNT_NAME = "SHOPMART JSC";

    // Regex tìm mã đơn hàng (VD: DH250822153000) nằm trong nội dung chuyển khoản
    private static final Pattern ORDER_CODE_PATTERN = Pattern.compile("DH\\d{6,}");

    private final OrderRepository orderRepository;
    private final CartService cartService;

    @Value("${payment.webhook.secret:}")
    private String webhookSecret;

    public OrderService(OrderRepository orderRepository, CartService cartService) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
    }

    // ---------- Tạo đơn hàng từ giỏ hàng ----------
    @Transactional
    public Order placeOrder(HttpSession session, User user, String fullName, String phone, String address,
                             String note, Double lat, Double lng, String paymentMethod) {

        List<CartItem> cartItems = cartService.getItems(session);
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng của bạn đang trống");
        }

        String normalizedMethod = "TRANSFER".equalsIgnoreCase(paymentMethod) ? "TRANSFER" : "COD";
        String status = "TRANSFER".equals(normalizedMethod) ? "CHO_THANH_TOAN" : "CHO_XAC_NHAN";

        Order order = Order.builder()
                .orderCode(generateOrderCode())
                .user(user)
                .fullName(fullName)
                .phone(phone)
                .address(address)
                .note(note)
                .lat(lat)
                .lng(lng)
                .paymentMethod(normalizedMethod)
                .paid(false)
                .status(status)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            BigDecimal itemPrice = cartItem.getProduct().getPrice();
            OrderItem orderItem = OrderItem.builder()
                    .product(cartItem.getProduct())
                    .quantity(cartItem.getQuantity())
                    .price(itemPrice)
                    .build();
            order.addItem(orderItem);
            subtotal = subtotal.add(itemPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        BigDecimal shippingFee = BigDecimal.ZERO; // hiện đang miễn phí ship, có thể tính theo địa chỉ sau này
        BigDecimal discount = BigDecimal.ZERO;    // chỗ để gắn mã giảm giá sau này

        order.setItemsSubtotal(subtotal);
        order.setShippingFee(shippingFee);
        order.setDiscountAmount(discount);
        order.setTotalAmount(subtotal.add(shippingFee).subtract(discount));

        Order saved = orderRepository.save(order);

        // Đặt hàng thành công -> xóa giỏ hàng ngay (kể cả đơn chuyển khoản, vì hàng đã được "giữ chỗ")
        cartService.clearCart(session);

        return saved;
    }

    private String generateOrderCode() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        return "DH" + timestamp;
    }

    // ---------- Sinh URL ảnh QR chuyển khoản (VietQR.io) ----------
    public String buildVietQrUrl(Order order) {
        String info = UriUtils.encode("Thanh toan don hang " + order.getOrderCode(), StandardCharsets.UTF_8);
        String accountName = UriUtils.encode(BANK_ACCOUNT_NAME, StandardCharsets.UTF_8);
        long amount = order.getTotalAmount().longValue();

        return "https://img.vietqr.io/image/" + BANK_ID + "-" + BANK_ACCOUNT_NO + "-compact2.png"
                + "?amount=" + amount
                + "&addInfo=" + info
                + "&accountName=" + accountName;
    }

    public String getBankId() { return BANK_ID; }
    public String getBankAccountNo() { return BANK_ACCOUNT_NO; }
    public String getBankAccountName() { return BANK_ACCOUNT_NAME; }

    /**
     * Kiểm tra secret của webhook (Casso/SePay...). Nếu chưa cấu hình payment.webhook.secret
     * (đang để trống, dùng cho môi trường demo) thì bỏ qua bước kiểm tra này.
     */
    public boolean isWebhookSecretValid(String providedSecret) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return true; // chưa cấu hình -> cho qua (chỉ dùng khi demo/dev)
        }
        return webhookSecret.equals(providedSecret);
    }

    /**
     * Xử lý webhook báo có tiền vào từ Casso/SePay/VietQR Pro...
     * Khớp mã đơn hàng nằm trong nội dung chuyển khoản + đúng số tiền -> đánh dấu đã thanh toán.
     *
     * @param content         nội dung chuyển khoản (addInfo) do ngân hàng trả về
     * @param amount          số tiền thực nhận
     * @param transactionRef  mã giao dịch ngân hàng (để chống xử lý trùng)
     * @return Order đã được cập nhật, hoặc rỗng nếu không khớp được đơn nào
     */
    @Transactional
    public Optional<Order> confirmTransferPayment(String content, BigDecimal amount, String transactionRef) {
        if (content == null || amount == null) {
            return Optional.empty();
        }

        // Chống xử lý trùng nếu ngân hàng gọi webhook lại (retry)
        if (transactionRef != null && !transactionRef.isBlank()
                && orderRepository.existsByTransactionRef(transactionRef)) {
            return Optional.empty();
        }

        Matcher matcher = ORDER_CODE_PATTERN.matcher(content.toUpperCase());
        if (!matcher.find()) {
            return Optional.empty();
        }
        String orderCode = matcher.group();

        Optional<Order> orderOpt = orderRepository.findByOrderCode(orderCode);
        if (orderOpt.isEmpty()) {
            return Optional.empty();
        }

        Order order = orderOpt.get();

        // Chỉ xử lý đơn đang thực sự chờ chuyển khoản, chưa được thanh toán trước đó
        if (!"TRANSFER".equals(order.getPaymentMethod()) || Boolean.TRUE.equals(order.getPaid())) {
            return Optional.empty();
        }

        // Số tiền chuyển phải đủ (cho phép thừa, ví dụ khách chuyển dư)
        if (amount.compareTo(order.getTotalAmount()) < 0) {
            return Optional.empty();
        }

        order.setPaid(true);
        order.setPaidAt(LocalDateTime.now());
        order.setTransactionRef(transactionRef);
        order.setStatus("DA_THANH_TOAN");

        return Optional.of(orderRepository.save(order));
    }

    public Optional<Order> findByOrderCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode);
    }

    public List<Order> findOrdersOfUser(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }
}
