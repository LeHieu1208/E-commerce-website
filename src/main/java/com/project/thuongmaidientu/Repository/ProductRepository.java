package com.project.thuongmaidientu.Repository;

import com.project.thuongmaidientu.Model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findTop4ByCategoryIdAndIdNot(Long categoryId, Long id);

    /**
     * Khóa dòng sản phẩm (SELECT ... FOR UPDATE) cho tới khi transaction hiện
     * tại commit/rollback. Dùng khi trừ kho lúc đặt hàng để tránh 2 request
     * đọc cùng 1 giá trị stockQuantity rồi cùng trừ, dẫn tới bán vượt tồn kho
     * (oversell) khi nhiều người mua cùng lúc 1 sản phẩm sắp hết hàng.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}