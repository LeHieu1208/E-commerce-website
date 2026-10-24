package com.project.thuongmaidientu.Job;

import com.project.thuongmaidientu.Model.Order;
import com.project.thuongmaidientu.Repository.OrderRepository;
import com.project.thuongmaidientu.Service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Giữ hàng có thời hạn: đơn chuyển khoản không được thanh toán trong payment.hold-minutes phút
 * sẽ bị hủy và kho được hoàn, tránh việc đơn "ma" khóa tồn kho vĩnh viễn.
 */
@Component
public class OrderExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(OrderExpiryJob.class);

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Value("${payment.hold-minutes:15}")
    private long holdMinutes;

    public OrderExpiryJob(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${payment.expiry-check-ms:60000}", initialDelayString = "${payment.expiry-check-ms:60000}")
    public void cancelExpiredTransferOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(holdMinutes);
        List<Order> candidates = orderRepository.findByStatusAndPaymentMethodAndCreatedAtBefore(
                OrderService.CHO_THANH_TOAN, "TRANSFER", cutoff);
        int cancelled = 0;
        for (Order order : candidates) {
            try {
                // Gọi qua proxy: mỗi đơn một transaction riêng, kiểm tra lại trạng thái dưới khóa
                if (orderService.expireIfUnpaid(order.getId())) cancelled++;
            } catch (RuntimeException ex) {
                log.error("Không hủy được đơn quá hạn {}: {}", order.getOrderCode(), ex.getMessage());
            }
        }
        if (cancelled > 0) log.info("Đã hủy {} đơn chuyển khoản quá hạn và hoàn kho", cancelled);
    }
}