package com.project.thuongmaidientu.Controller;

import com.project.thuongmaidientu.Model.CartItem;
import com.project.thuongmaidientu.Model.Order;
import com.project.thuongmaidientu.Model.OrderItem;
import com.project.thuongmaidientu.Model.Product;
import com.project.thuongmaidientu.Model.User;
import com.project.thuongmaidientu.Service.CartService;
import com.project.thuongmaidientu.Service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutApiController {

    private final CartService cartService;
    private final OrderService orderService;

    public CheckoutApiController(CartService cartService, OrderService orderService) {
        this.cartService = cartService;
        this.orderService = orderService;
    }

    @GetMapping
    public CheckoutView checkout(HttpSession session) {
        User user = currentUser(session);
        List<CheckoutLine> items = cartService.getItems(session).stream()
                .map(CheckoutApiController::toCheckoutLine).toList();
        return new CheckoutView(items, cartService.getSubtotal(session),
                new Recipient(user.getFullName(), user.getPhone(), user.getAddress()));
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody PlaceOrderRequest request, HttpSession session) {
        User user = currentUser(session);
        try {
            Order order = orderService.placeOrder(session, user, request.fullName(), request.phone(),
                    request.address(), request.note(), request.lat(), request.lng(), request.paymentMethod());
            return ResponseEntity.ok(toOrderResponse(order));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.badRequest().body(new MessageResponse(exception.getMessage()));
        }
    }

    @GetMapping("/orders/{orderCode}")
    public ResponseEntity<?> order(@PathVariable String orderCode, HttpSession session) {
        User user = currentUser(session);
        return orderService.findByOrderCode(orderCode)
                .filter(order -> order.getUser() != null && order.getUser().getId().equals(user.getId()))
                .<ResponseEntity<?>>map(order -> ResponseEntity.ok(toOrderResponse(order)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private User currentUser(HttpSession session) {
        Object user = session.getAttribute("currentUser");
        if (user instanceof User currentUser) return currentUser;
        Object legacy = session.getAttribute("loggedUser");
        return legacy instanceof User loggedUser ? loggedUser : null;
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderLine> items = order.getItems().stream().map(CheckoutApiController::toOrderLine).toList();
        return new OrderResponse(order.getOrderCode(), order.getFullName(), order.getPhone(), order.getAddress(),
                order.getPaymentMethod(), order.getPaid(), order.getPaidAt(), order.getStatus(), order.getTotalAmount(),
                order.getCreatedAt(), items, "TRANSFER".equals(order.getPaymentMethod())
                        ? orderService.buildVietQrUrl(order) : null,
                orderService.getBankId(), orderService.getBankAccountNo(), orderService.getBankAccountName());
    }

    private static CheckoutLine toCheckoutLine(CartItem item) {
        Product product = item.getProduct();
        return new CheckoutLine(product.getId(), product.getName(), product.getImageUrl(), product.getDiscountedPrice(), item.getQuantity());
    }

    private static OrderLine toOrderLine(OrderItem item) {
        Product product = item.getProduct();
        return new OrderLine(product == null ? "Sản phẩm" : product.getName(), item.getPrice(), item.getQuantity(), item.getSubtotal());
    }

    public record PlaceOrderRequest(String fullName, String phone, String address, String note,
                                    Double lat, Double lng, String paymentMethod) { }
    public record CheckoutLine(Long productId, String name, String imageUrl, BigDecimal price, Integer quantity) { }
    public record Recipient(String fullName, String phone, String address) { }
    public record CheckoutView(List<CheckoutLine> items, BigDecimal subtotal, Recipient recipient) { }
    public record OrderLine(String name, BigDecimal price, Integer quantity, BigDecimal subtotal) { }
    public record OrderResponse(String orderCode, String fullName, String phone, String address,
                               String paymentMethod, Boolean paid, LocalDateTime paidAt, String status,
                               BigDecimal totalAmount, LocalDateTime createdAt, List<OrderLine> items,
                               String qrUrl, String bankId, String bankAccountNo, String bankAccountName) { }
    public record MessageResponse(String message) { }
}