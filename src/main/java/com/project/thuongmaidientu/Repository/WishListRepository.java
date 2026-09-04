package com.project.thuongmaidientu.Repository;

import com.project.thuongmaidientu.Model.WishList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishListRepository extends JpaRepository<WishList, Long> {
    List<WishList> findByUserId(Long userId);
    Optional<WishList> findByUserIdAndProductId(Long userId, Long productId);
}
