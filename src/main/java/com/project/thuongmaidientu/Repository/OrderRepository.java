package com.project.thuongmaidientu.Repository;

import com.project.thuongmaidientu.Model.Order;
import com.project.thuongmaidientu.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(String orderCode);
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    boolean existsByTransactionRef(String transactionRef);

    // ---------- Dùng cho trang quản trị (Admin) ----------
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByStatusOrderByCreatedAtDesc(String status);
}