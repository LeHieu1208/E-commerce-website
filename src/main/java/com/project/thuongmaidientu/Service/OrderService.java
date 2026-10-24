package com.project.thuongmaidientu.Service;

import com.project.thuongmaidientu.Model.CartItem;
import com.project.thuongmaidientu.Model.Order;
import com.project.thuongmaidientu.Model.OrderItem;
import com.project.thuongmaidientu.Model.User;
import com.project.thuongmaidientu.Repository.OrderRepository;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    // Regex tìm mã đơn hàng (VD: DH250822153000123) nằm trong nội dung chuyển khoản
    private static final Pattern ORDER_CODE_PATTERN = Pattern.compile("DH\\d{6,}");

    public static final String CHO_XAC_NHAN = "CHO_XAC_NHAN";
    public static final String CHO_THANH_TOAN = "CHO_THANH_TOAN";
    public static final String DA_THANH_TOAN = "DA_THANH_TOAN";
    public static final String DA_XAC_NHAN = "DA_XAC_NHAN";
    public static final String DANG_GIAO = "DANG_GIAO";
    public static final String HOAN_THANH = "HOAN_THANH";
    public static final String DA_HUY = "DA_HUY";

    /**
     * Máy trạng thái của đơn hàng: trạng thái hiện tại -> các trạng thái được phép chuyển tới.
     * HOAN_THANH và DA_HUY là trạng thái cuối, không đi tiếp được.
     */
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            CHO_XAC_NHAN, Set.of(DA_XAC_NHAN, DA_HUY),
            CHO_THANH_TOAN, Set.of(DA_THANH_TOAN, DA_HUY),
            DA_THANH_TOAN, Set.of(DA_XAC_NHAN, DA_HUY),
            DA_XAC_NHAN, Set.of(DANG_GIAO, DA_HUY),
            DANG_GIAO, Set.of(HOAN_THANH, DA_HUY),
            HOAN_THANH, Set.of(),
            DA_HUY, Set.of());

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final InventoryService inventoryService;

    @Value("${payment.webhook.secret:}")
    private String webhookSecret;

    /** Chỉ bật khi chạy demo/dev: cho phép webhook không cần secret. Mặc định TẮT (fail-closed). */
    @Value("${payment.webhook.allow-unsigned:false}")
    private boolean allowUnsignedWebhook;

    // Thông tin ngân hàng nhận thanh toán - dùng để sinh mã QR VietQR. Cấu hình qua biến môi trường.
    @Value("${payment.bank.id:VCB}")
    private String bankId;
    @Value("${payment.bank.account-no:0123456789}")
    private String bankAccountNo;
    @Value("${payment.bank.account-name:SHOPMART JSC}")
    private String bankAccountName;

    public OrderService(OrderRepository orderRepository, CartService cartService, InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
        this.inventoryService = inventoryService;
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
        String status = "TRANSFER".equals(normalizedMethod) ? CHO_THANH_TOAN : CHO_XAC_NHAN;

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
        // InventoryService.deductStock chạy REQUIRES_NEW nên mỗi lần trừ kho đã tự commit; rollback của
        // transaction cha KHÔNG cuốn theo. Vì vậy mọi lỗi sau khi đã trừ kho (hết hàng, lỗi lưu đơn,
        // lỗi xóa giỏ...) đều phải hoàn kho thủ công (compensating transaction).
        List<CartItem> deductedSoFar = new ArrayList<>();

        try {
            for (CartItem cartItem : cartItems) {
                inventoryService.deductStock(cartItem.getProduct().getId(), cartItem.getQuantity());
                deductedSoFar.add(cartItem);

                // Giá khách thực trả = giá sau giảm (cùng giá đang hiển thị ở danh mục/giỏ hàng)
                BigDecimal itemPrice = cartItem.getProduct().getDiscountedPrice();
                OrderItem orderItem = OrderItem.builder()
                        .product(cartItem.getProduct())
                        .quantity(cartItem.getQuantity())
                        .price(itemPrice)
                        .build();
                order.addItem(orderItem);
                subtotal = subtotal.add(itemPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            }

            BigDecimal shippingFee = BigDecimal.ZERO; // hiện miễn phí ship, có thể tính theo địa chỉ sau này
            BigDecimal discount = BigDecimal.ZERO;    // chỗ để gắn mã giảm giá sau này

            order.setItemsSubtotal(subtotal);
            order.setShippingFee(shippingFee);
            order.setDiscountAmount(discount);
            order.setTotalAmount(subtotal.add(shippingFee).subtract(discount));

            Order saved = orderRepository.save(order);

            // Đặt hàng thành công -> xóa giỏ hàng ngay (hàng đã được "giữ chỗ")
            cartService.clearCart(session);
            return saved;
        } catch (RuntimeException ex) {
            for (CartItem deducted : deductedSoFar) {
                try {
                    inventoryService.restoreStock(deducted.getProduct().getId(), deducted.getQuantity());
                } catch (RuntimeException restoreError) {
                    log.error("Không hoàn được kho cho sản phẩm {}: {}", deducted.getProduct().getId(), restoreError.getMessage());
                }
            }
            throw ex;
        }
    }

    // ---------- Đổi trạng thái đơn (admin) ----------
    /**
     * Đổi trạng thái theo máy trạng thái. Khóa dòng đơn để không đua với webhook/job hết hạn.
     * Khi hủy đơn: cộng lại tồn kho trong CÙNG transaction.
     *
     * @throws NoSuchElementException   không có đơn
     * @throws IllegalArgumentException trạng thái không tồn tại
     * @throws IllegalStateException    chuyển trạng thái không hợp lệ
     */
    @Transactional
    public Order updateStatus(Long orderId, String newStatus, String trackingCode, String adminNote, String cancelReason) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn hàng"));
        if (newStatus == null || !TRANSITIONS.containsKey(newStatus)) {
            throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ.");
        }
        String current = order.getStatus();
        boolean changed = !newStatus.equals(current);
        if (changed && !TRANSITIONS.getOrDefault(current, Set.of()).contains(newStatus)) {
            throw new IllegalStateException("Không thể chuyển đơn từ \"" + current + "\" sang \"" + newStatus + "\".");
        }

        if (trackingCode != null && !trackingCode.isBlank()) order.setTrackingCode(trackingCode.trim());
        if (adminNote != null) order.setAdminNote(adminNote);
        if (changed) applyTransition(order, newStatus, cancelReason);
        return orderRepository.save(order);
    }

    /** Dùng cho job: hủy đơn chuyển khoản quá hạn, nhưng chỉ khi vẫn đang chờ thanh toán (kiểm tra lại dưới khóa). */
    @Transactional
    public boolean expireIfUnpaid(Long orderId) {
        Optional<Order> found = orderRepository.findByIdForUpdate(orderId);
        if (found.isEmpty()) return false;
        Order order = found.get();
        if (!CHO_THANH_TOAN.equals(order.getStatus()) || Boolean.TRUE.equals(order.getPaid())) return false;
        applyTransition(order, DA_HUY, "Quá hạn thanh toán");
        orderRepository.save(order);
        return true;
    }

    private void applyTransition(Order order, String newStatus, String cancelReason) {
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(newStatus);
        switch (newStatus) {
            case DA_THANH_TOAN -> {
                order.setPaid(true);
                if (order.getPaidAt() == null) order.setPaidAt(now);
            }
            case DA_XAC_NHAN -> { if (order.getConfirmedAt() == null) order.setConfirmedAt(now); }
            case DANG_GIAO -> { if (order.getShippedAt() == null) order.setShippedAt(now); }
            case HOAN_THANH -> {
                if (order.getCompletedAt() == null) order.setCompletedAt(now);
                // Đơn COD hoàn thành coi như đã thu tiền
                if ("COD".equals(order.getPaymentMethod()) && !Boolean.TRUE.equals(order.getPaid())) {
                    order.setPaid(true);
                    order.setPaidAt(now);
                }
            }
            case DA_HUY -> {
                order.setCancelledAt(now);
                if (cancelReason != null && !cancelReason.isBlank()) order.setCancelReason(cancelReason.trim());
                for (OrderItem item : order.getItems()) {
                    if (item.getProduct() != null && item.getQuantity() != null) {
                        inventoryService.restoreStockInCurrentTransaction(item.getProduct().getId(), item.getQuantity());
                    }
                }
            }
            default -> { }
        }
    }

    private String generateOrderCode() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        // Hậu tố ngẫu nhiên 3 chữ số: 2 đơn cùng giây không còn trùng mã (cột order_code là UNIQUE)
        return "DH" + timestamp + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    // ---------- Sinh URL ảnh QR chuyển khoản (VietQR.io) ----------
    public String buildVietQrUrl(Order order) {
        String info = UriUtils.encode("Thanh toan don hang " + order.getOrderCode(), StandardCharsets.UTF_8);
        String accountName = UriUtils.encode(bankAccountName, StandardCharsets.UTF_8);
        long amount = order.getTotalAmount().longValue();

        return "https://img.vietqr.io/image/" + bankId + "-" + bankAccountNo + "-compact2.png"
                + "?amount=" + amount
                + "&addInfo=" + info
                + "&accountName=" + accountName;
    }

    public String getBankId() { return bankId; }
    public String getBankAccountNo() { return bankAccountNo; }
    public String getBankAccountName() { return bankAccountName; }

    /**
     * Kiểm tra secret của webhook (Casso/SePay...). Fail-closed: chưa cấu hình payment.webhook.secret
     * thì TỪ CHỐI, trừ khi bật rõ ràng payment.webhook.allow-unsigned=true (chỉ cho dev/demo).
     * So sánh theo thời gian không đổi để tránh timing attack.
     */
    public boolean isWebhookSecretValid(String providedSecret) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            if (allowUnsignedWebhook) {
                log.warn("Webhook đang chạy KHÔNG có secret (payment.webhook.allow-unsigned=true) - chỉ dùng cho dev/demo");
            }
            return allowUnsignedWebhook;
        }
        if (providedSecret == null) return false;
        return MessageDigest.isEqual(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                providedSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Xử lý webhook báo có tiền vào. Khớp mã đơn trong nội dung chuyển khoản + đủ số tiền -> đánh dấu đã thanh toán.
     * Khóa dòng đơn nên 2 lần gọi webhook đồng thời (retry) chỉ xử lý được một lần.
     *
     * @return Order đã cập nhật, hoặc rỗng nếu không khớp / đã xử lý / đơn không còn chờ thanh toán
     */
    @Transactional
    public Optional<Order> confirmTransferPayment(String content, BigDecimal amount, String transactionRef) {
        if (content == null || amount == null) {
            return Optional.empty();
        }

        if (transactionRef != null && !transactionRef.isBlank()
                && orderRepository.existsByTransactionRef(transactionRef)) {
            return Optional.empty();
        }

        Matcher matcher = ORDER_CODE_PATTERN.matcher(content.toUpperCase());
        if (!matcher.find()) {
            return Optional.empty();
        }
        String orderCode = matcher.group();

        Optional<Order> orderOpt = orderRepository.findByOrderCodeForUpdate(orderCode);
        if (orderOpt.isEmpty()) {
            return Optional.empty();
        }
        Order order = orderOpt.get();

        if (!"TRANSFER".equals(order.getPaymentMethod()) || Boolean.TRUE.equals(order.getPaid())) {
            return Optional.empty();
        }

        // Đơn đã bị hủy (hết hạn giữ hàng) mà tiền vẫn về: không tự "sống lại" đơn vì kho đã được hoàn
        if (DA_HUY.equals(order.getStatus())) {
            log.warn("Nhận {} cho đơn {} đã hủy (mã GD {}) - cần hoàn tiền thủ công", amount, orderCode, transactionRef);
            return Optional.empty();
        }
        if (!CHO_THANH_TOAN.equals(order.getStatus())) {
            return Optional.empty();
        }

        // Số tiền chuyển phải đủ (cho phép thừa)
        if (amount.compareTo(order.getTotalAmount()) < 0) {
            return Optional.empty();
        }

        order.setPaid(true);
        order.setPaidAt(LocalDateTime.now());
        order.setTransactionRef(transactionRef);
        order.setStatus(DA_THANH_TOAN);

        return Optional.of(orderRepository.save(order));
    }

    public Optional<Order> findByOrderCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode);
    }

    public List<Order> findOrdersOfUser(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }
}