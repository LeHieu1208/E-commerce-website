package com.project.thuongmaidientu.Repository;

import com.project.thuongmaidientu.Model.Order;
import com.project.thuongmaidientu.Model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(String orderCode);
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    boolean existsByTransactionRef(String transactionRef);

    /** Khóa dòng đơn hàng (SELECT ... FOR UPDATE): webhook, job hết hạn và admin không thể đổi trạng thái cùng lúc. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM com.project.thuongmaidientu.Model.Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM com.project.thuongmaidientu.Model.Order o WHERE o.orderCode = :code")
    Optional<Order> findByOrderCodeForUpdate(@Param("code") String code);

    List<Order> findByStatusAndPaymentMethodAndCreatedAtBefore(String status, String paymentMethod, LocalDateTime cutoff);

    // ---------- Dùng cho trang quản trị (Admin) ----------
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByStatusOrderByCreatedAtDesc(String status);
}