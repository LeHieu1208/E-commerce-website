package com.project.thuongmaidientu.Model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wishlists")
@Getter
@Setter

public class WishList {

    // Explicit constructor so javac can resolve line 28 without relying on Lombok
    public WishList(Long id, User user, Product product) {
        this.id = id;
        this.user = user;
        this.product = product;
    }

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

    public WishList() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}