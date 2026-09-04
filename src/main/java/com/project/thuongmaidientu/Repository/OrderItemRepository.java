package com.project.thuongmaidientu.Repository;

import com.project.thuongmaidientu.Model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
