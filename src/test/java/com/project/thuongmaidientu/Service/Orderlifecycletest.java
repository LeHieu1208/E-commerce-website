package com.project.thuongmaidientu.Service;

import com.project.thuongmaidientu.Job.OrderExpiryJob;
import com.project.thuongmaidientu.Model.Order;
import com.project.thuongmaidientu.Model.Product;
import com.project.thuongmaidientu.Model.User;
import com.project.thuongmaidientu.Repository.OrderRepository;
import com.project.thuongmaidientu.Repository.ProductRepository;
import com.project.thuongmaidientu.Repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Vòng đời đơn hàng: giá giảm, hoàn kho khi hủy, máy trạng thái, hết hạn giữ hàng, webhook. */
@SpringBootTest
@ActiveProfiles("test")
class OrderLifecycleTest {

    @Autowired private OrderService orderService;
    @Autowired private CartService cartService;
    @Autowired private OrderExpiryJob orderExpiryJob;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private User newUser() {
        return userRepository.save(User.builder()
                .email("lifecycle-" + UUID.randomUUID() + "@example.com")
                .password("123456").fullName("Lifecycle").role("ROLE_USER").build());
    }

    private Product newProduct(int stock, int discountPercent) {
        return productRepository.save(Product.builder()
                .name("SP lifecycle " + UUID.randomUUID())
                .price(new BigDecimal("1000000"))
                .discountPercent(discountPercent)
                .stockQuantity(stock).build());
    }

    private Order placeOrder(User user, Product product, int qty, String method) {
        HttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", user);
        cartService.add(session, product.getId(), qty);
        return orderService.placeOrder(session, user, "Người nhận", "0900000000", "Hà Nội", null, null, null, method);
    }

    private int stockOf(Product product) {
        return productRepository.findById(product.getId()).orElseThrow().getStockQuantity();
    }

    @Test
    void donHangTinhTheoGiaSauGiamVaTruKho() {
        Product product = newProduct(5, 10);
        Order order = placeOrder(newUser(), product, 2, "COD");

        assertThat(order.getTotalAmount()).isEqualByComparingTo("1800000");
        assertThat(order.getItems().get(0).getPrice()).isEqualByComparingTo("900000");
        assertThat(stockOf(product)).isEqualTo(3);
    }

    @Test
    void huyDonHoanKhoDungMotLan() {
        Product product = newProduct(5, 0);
        Order order = placeOrder(newUser(), product, 2, "COD");
        assertThat(stockOf(product)).isEqualTo(3);

        orderService.updateStatus(order.getId(), "DA_HUY", null, null, "Khách đổi ý");
        assertThat(stockOf(product)).isEqualTo(5);

        // Gọi lại cùng trạng thái không được hoàn kho lần nữa
        orderService.updateStatus(order.getId(), "DA_HUY", null, "ghi chú", null);
        assertThat(stockOf(product)).isEqualTo(5);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getCancelReason()).isEqualTo("Khách đổi ý");
    }

    @Test
    void mayTrangThaiChanChuyenSai() {
        Order order = placeOrder(newUser(), newProduct(5, 0), 1, "COD");

        assertThatThrownBy(() -> orderService.updateStatus(order.getId(), "HOAN_THANH", null, null, null))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> orderService.updateStatus(order.getId(), "TRANG_THAI_LA", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        orderService.updateStatus(order.getId(), "DA_XAC_NHAN", null, null, null);
        orderService.updateStatus(order.getId(), "DANG_GIAO", "VN123", null, null);
        Order done = orderService.updateStatus(order.getId(), "HOAN_THANH", null, null, null);
        assertThat(done.getPaid()).isTrue(); // COD hoàn thành = đã thu tiền

        // Trạng thái cuối không đi tiếp được
        assertThatThrownBy(() -> orderService.updateStatus(order.getId(), "CHO_XAC_NHAN", null, null, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void donChuyenKhoanQuaHanBiHuyVaHoanKho() {
        Product product = newProduct(5, 0);
        Order order = placeOrder(newUser(), product, 3, "TRANSFER");
        assertThat(stockOf(product)).isEqualTo(2);

        jdbcTemplate.update("UPDATE orders SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusHours(1)), order.getId());
        orderExpiryJob.cancelExpiredTransferOrders();

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo("DA_HUY");
        assertThat(stockOf(product)).isEqualTo(5);
    }

    @Test
    void donDaThanhToanKhongBiJobHuy() {
        Product product = newProduct(5, 0);
        Order order = placeOrder(newUser(), product, 1, "TRANSFER");
        orderService.confirmTransferPayment("Thanh toan " + order.getOrderCode(), order.getTotalAmount(), "REF-" + UUID.randomUUID());
        jdbcTemplate.update("UPDATE orders SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusHours(1)), order.getId());

        orderExpiryJob.cancelExpiredTransferOrders();

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo("DA_THANH_TOAN");
        assertThat(stockOf(product)).isEqualTo(4);
    }

    @Test
    void webhookXuLyMotLanVaBoQuaDonDaHuy() {
        Order order = placeOrder(newUser(), newProduct(5, 0), 1, "TRANSFER");
        String ref = "REF-" + UUID.randomUUID();

        assertThat(orderService.confirmTransferPayment(order.getOrderCode(), new BigDecimal("1"), ref)).isEmpty(); // thiếu tiền
        assertThat(orderService.confirmTransferPayment(order.getOrderCode(), order.getTotalAmount(), ref)).isPresent();
        assertThat(orderService.confirmTransferPayment(order.getOrderCode(), order.getTotalAmount(), ref)).isEmpty(); // gọi lại

        Order cancelled = placeOrder(newUser(), newProduct(5, 0), 1, "TRANSFER");
        orderService.updateStatus(cancelled.getId(), "DA_HUY", null, null, "test");
        assertThat(orderService.confirmTransferPayment(cancelled.getOrderCode(), cancelled.getTotalAmount(), "REF-" + UUID.randomUUID())).isEmpty();
    }

    @Test
    void webhookTuChoiKhiSaiHoacThieuSecret() {
        assertThat(orderService.isWebhookSecretValid(null)).isFalse();
        assertThat(orderService.isWebhookSecretValid("sai")).isFalse();
        assertThat(orderService.isWebhookSecretValid("test-secret")).isTrue();
    }
}