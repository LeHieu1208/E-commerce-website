package com.project.thuongmaidientu.Model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wishlists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishList {

    public static WishListBuilder builder() {
        return new WishListBuilder();
    }

    public static class WishListBuilder {
        private Long id;
        private User user;
        private Product product;

        public WishListBuilder id(Long id) { this.id = id; return this; }
        public WishListBuilder user(User user) { this.user = user; return this; }
        public WishListBuilder product(Product product) { this.product = product; return this; }

        public WishList build() {
            return new WishList(id, user, product);
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
}