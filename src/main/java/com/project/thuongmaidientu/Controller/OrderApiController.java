package com.project.thuongmaidientu.Controller;

import com.project.thuongmaidientu.Model.Order;
import com.project.thuongmaidientu.Model.User;
import com.project.thuongmaidientu.Service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * API nhẹ để trang chờ thanh toán (payment-pending) polling trạng thái đơn hàng
 * mỗi vài giây, tự động phát hiện khi webhook ngân hàng xác nhận tiền đã vào.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderApiController {

    private final OrderService orderService;

    public OrderApiController(OrderService orderService) {
        this.orderService = orderService;
    }

    private User currentUser(HttpSession session) {
        Object user = session.getAttribute("currentUser");
        if (user instanceof User currentUser) {
            return currentUser;
        }
        Object legacy = session.getAttribute("loggedUser");
        if (legacy instanceof User loggedUser) {
            return loggedUser;
        }
        return null;
    }

    @GetMapping("/{orderCode}/status")
    public ResponseEntity<?> getStatus(@PathVariable String orderCode, HttpSession session) {
        User user = currentUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        }

        Optional<Order> orderOpt = orderService.findByOrderCode(orderCode);
        if (orderOpt.isEmpty() || orderOpt.get().getUser() == null
                || !orderOpt.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy đơn hàng"));
        }

        Order order = orderOpt.get();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderCode", order.getOrderCode());
        body.put("status", order.getStatus());
        body.put("paid", order.getPaid());
        body.put("paymentMethod", order.getPaymentMethod());
        return ResponseEntity.ok(body);
    }
}
