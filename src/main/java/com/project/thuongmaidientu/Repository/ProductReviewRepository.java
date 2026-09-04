package com.project.thuongmaidientu.Repository;

import com.project.thuongmaidientu.Model.Product;
import com.project.thuongmaidientu.Model.ProductReview;
import com.project.thuongmaidientu.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    Optional<ProductReview> findByProductAndUser(Product product, User user);
    long countByProduct(Product product);

    @Query("select coalesce(avg(r.rating), 0) from ProductReview r where r.product.id = :productId")
    Double findAverageRatingByProductId(@Param("productId") Long productId);
}
