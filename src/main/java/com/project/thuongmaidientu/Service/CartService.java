package com.project.thuongmaidientu.Service;

import com.project.thuongmaidientu.Model.Cart;
import com.project.thuongmaidientu.Model.CartItem;
import com.project.thuongmaidientu.Model.Product;
import com.project.thuongmaidientu.Model.User;
import com.project.thuongmaidientu.Repository.CartItemRepository;
import com.project.thuongmaidientu.Repository.CartRepository;
import com.project.thuongmaidientu.Repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    private User getCurrentUser(HttpSession session) {
        Object user = session.getAttribute("currentUser");
        if (user instanceof User currentUser) {
            return currentUser;
        }
        Object legacy = session.getAttribute("loggedUser");
        if (legacy instanceof User loggedUser) {
            return loggedUser;
        }
        return null;
    }

    @Transactional
    public Cart getOrCreateCart(HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            throw new IllegalStateException("Bạn cần đăng nhập để dùng giỏ hàng");
        }

        List<Cart> carts = cartRepository.findAllByUser(currentUser);
        if (carts.isEmpty()) {
            return createNewCart(currentUser);
        }

        Cart canonicalCart = carts.get(0);
        if (carts.size() > 1) {
            mergeDuplicateCarts(currentUser, canonicalCart, carts.subList(1, carts.size()));
        }
        return canonicalCart;
    }

    private Cart createNewCart(User user) {
        Cart newCart = Cart.builder().user(user).build();
        return cartRepository.save(newCart);
    }

    @Transactional
    protected void mergeDuplicateCarts(User user, Cart canonicalCart, List<Cart> duplicateCarts) {
        if (duplicateCarts == null || duplicateCarts.isEmpty()) {
            return;
        }

        for (Cart duplicateCart : duplicateCarts) {
            if (duplicateCart == null || duplicateCart.getId() == null) {
                continue;
            }

            for (CartItem item : duplicateCart.getItems()) {
                if (item == null || item.getProduct() == null) {
                    continue;
                }

                CartItem existing = cartItemRepository.findByCartAndProduct(canonicalCart, item.getProduct())
                        .orElse(null);

                if (existing != null) {
                    existing.setQuantity(existing.getQuantity() + item.getQuantity());
                    cartItemRepository.save(existing);
                } else {
                    item.setCart(canonicalCart);
                    cartItemRepository.save(item);
                    canonicalCart.getItems().add(item);
                }
            }

            cartItemRepository.deleteAll(duplicateCart.getItems());
            cartRepository.delete(duplicateCart);
        }
    }

    @Transactional
    public void add(HttpSession session, Long productId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            quantity = 1;
        }

        Cart cart = getOrCreateCart(session);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));

        CartItem existingItem = cartItemRepository.findByCartAndProduct(cart, product).orElse(null);
        int requestedQuantity = quantity + (existingItem != null ? existingItem.getQuantity() : 0);
        int availableStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;

        if (requestedQuantity > availableStock) {
            requestedQuantity = availableStock;
        }
        if (requestedQuantity < 1) {
            throw new IllegalStateException("Sản phẩm đã hết hàng");
        }

        if (existingItem != null) {
            existingItem.setQuantity(requestedQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(requestedQuantity)
                    .build();
            cartItemRepository.save(newItem);
            cart.getItems().add(newItem);
        }
    }

    @Transactional
    public void updateQuantity(HttpSession session, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(session);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));

        CartItem item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không có trong giỏ hàng"));

        if (quantity == null || quantity <= 0) {
            cartItemRepository.delete(item);
            return;
        }

        int availableStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        item.setQuantity(Math.min(quantity, availableStock));
        cartItemRepository.save(item);
    }

    @Transactional
    public void remove(HttpSession session, Long productId) {
        Cart cart = getOrCreateCart(session);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));

        cartItemRepository.findByCartAndProduct(cart, product)
                .ifPresent(cartItemRepository::delete);
    }

    @Transactional
    public List<CartItem> getItems(HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return Collections.emptyList();
        }

        List<Cart> carts = cartRepository.findAllByUser(currentUser);
        if (carts.isEmpty()) {
            return Collections.emptyList();
        }

        Cart canonicalCart = carts.get(0);
        if (carts.size() > 1) {
            mergeDuplicateCarts(currentUser, canonicalCart, carts.subList(1, carts.size()));
        }

        return cartItemRepository.findByCartOrderByIdAsc(canonicalCart);
    }

    @Transactional
    public int getCount(HttpSession session) {
        return getItems(session).stream().mapToInt(CartItem::getQuantity).sum();
    }

    @Transactional
    public BigDecimal getSubtotal(HttpSession session) {
        return getItems(session).stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void clearCart(HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return;
        }

        List<Cart> carts = cartRepository.findAllByUser(currentUser);
        if (carts.isEmpty()) {
            return;
        }

        Cart canonicalCart = carts.get(0);
        for (Cart cart : carts) {
            if (cart == null) {
                continue;
            }
            cartItemRepository.deleteAll(cart.getItems());
            cart.getItems().clear();
            if (!cart.getId().equals(canonicalCart.getId())) {
                cartRepository.delete(cart);
            }
        }
    }
}