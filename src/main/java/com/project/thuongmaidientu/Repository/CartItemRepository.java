package com.project.thuongmaidientu.Repository;

import com.project.thuongmaidientu.Model.Cart;
import com.project.thuongmaidientu.Model.CartItem;
import com.project.thuongmaidientu.Model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
    List<CartItem> findByCartOrderByIdAsc(Cart cart);
}