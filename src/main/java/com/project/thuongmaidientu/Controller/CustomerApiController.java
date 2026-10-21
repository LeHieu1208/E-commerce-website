package com.project.thuongmaidientu.Controller;

import com.project.thuongmaidientu.Model.CartItem;
import com.project.thuongmaidientu.Model.Order;
import com.project.thuongmaidientu.Model.Product;
import com.project.thuongmaidientu.Model.User;
import com.project.thuongmaidientu.Model.WishList;
import com.project.thuongmaidientu.Repository.OrderRepository;
import com.project.thuongmaidientu.Repository.ProductRepository;
import com.project.thuongmaidientu.Repository.UserRepository;
import com.project.thuongmaidientu.Repository.WishListRepository;
import com.project.thuongmaidientu.Service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class CustomerApiController {

    private final CartService cartService;
    private final WishListRepository wishListRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public CustomerApiController(CartService cartService,
                                 WishListRepository wishListRepository,
                                 ProductRepository productRepository,
                                 UserRepository userRepository,
                                 OrderRepository orderRepository) {
        this.cartService = cartService;
        this.wishListRepository = wishListRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/api/cart")
    public CartResponse cart(HttpSession session) {
        List<CartLine> items = cartService.getItems(session).stream()
                .map(CustomerApiController::toCartLine).toList();
        return new CartResponse(items, cartService.getSubtotal(session), cartService.getCount(session));
    }

    @PostMapping("/api/cart/items")
    public ResponseEntity<?> addCartItem(@RequestBody CartCommand command, HttpSession session) {
        try {
            cartService.add(session, command.productId(), command.quantity());
            return ResponseEntity.ok(cart(session));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.badRequest().body(new MessageResponse(exception.getMessage()));
        }
    }

    @PutMapping("/api/cart/items/{productId}")
    public ResponseEntity<?> updateCartItem(@PathVariable Long productId,
                                            @RequestBody QuantityCommand command,
                                            HttpSession session) {
        try {
            cartService.updateQuantity(session, productId, command.quantity());
            return ResponseEntity.ok(cart(session));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.badRequest().body(new MessageResponse(exception.getMessage()));
        }
    }

    @DeleteMapping("/api/cart/items/{productId}")
    public CartResponse removeCartItem(@PathVariable Long productId, HttpSession session) {
        cartService.remove(session, productId);
        return cart(session);
    }

    @GetMapping("/api/wishlist")
    public List<WishProduct> wishlist(HttpSession session) {
        User user = currentUser(session);
        return wishListRepository.findByUserId(user.getId()).stream()
                .map(item -> toWishProduct(item.getProduct())).toList();
    }

    @PostMapping("/api/wishlist/items/{productId}")
    public ResponseEntity<?> addWishlistItem(@PathVariable Long productId, HttpSession session) {
        User user = currentUser(session);
        if (wishListRepository.findByUserIdAndProductId(user.getId(), productId).isEmpty()) {
            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) return ResponseEntity.notFound().build();
            wishListRepository.save(WishList.builder().user(user).product(product).build());
        }
        return ResponseEntity.ok(wishlist(session));
    }

    @DeleteMapping("/api/wishlist/items/{productId}")
    public List<WishProduct> removeWishlistItem(@PathVariable Long productId, HttpSession session) {
        User user = currentUser(session);
        wishListRepository.findByUserIdAndProductId(user.getId(), productId).ifPresent(wishListRepository::delete);
        return wishlist(session);
    }

    @GetMapping("/api/profile")
    public ProfileResponse profile(HttpSession session) {
        User user = currentUser(session);
        User saved = userRepository.findById(user.getId()).orElse(user);
        List<OrderSummary> orders = orderRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(CustomerApiController::toOrderSummary).toList();
        return new ProfileResponse(new UserProfile(saved.getFullName(), saved.getEmail(), saved.getPhone(), saved.getAddress()), orders);
    }

    @PutMapping("/api/profile")
    public ResponseEntity<?> updateProfile(@RequestBody ProfileCommand command, HttpSession session) {
        User user = currentUser(session);
        User saved = userRepository.findById(user.getId()).orElse(user);
        saved.setFullName(command.fullName());
        saved.setPhone(command.phone());
        saved.setAddress(command.address());
        userRepository.save(saved);
        session.setAttribute("currentUser", saved);
        session.setAttribute("loggedUser", saved);
        List<OrderSummary> orders = orderRepository.findByUserOrderByCreatedAtDesc(saved).stream()
                .map(CustomerApiController::toOrderSummary).toList();
        return ResponseEntity.ok(new ProfileResponse(
                new UserProfile(saved.getFullName(), saved.getEmail(), saved.getPhone(), saved.getAddress()), orders));
    }

    private User currentUser(HttpSession session) {
        Object user = session.getAttribute("currentUser");
        if (user instanceof User currentUser) return currentUser;
        Object legacy = session.getAttribute("loggedUser");
        return legacy instanceof User loggedUser ? loggedUser : null;
    }

    private static CartLine toCartLine(CartItem item) {
        Product product = item.getProduct();
        return new CartLine(product.getId(), product.getName(), product.getImageUrl(), product.getPrice(),
                item.getQuantity(), product.getStockQuantity());
    }

    private static WishProduct toWishProduct(Product product) {
        return new WishProduct(product.getId(), product.getName(), product.getDescription(), product.getImageUrl(), product.getPrice());
    }

    private static OrderSummary toOrderSummary(Order order) {
        return new OrderSummary(order.getOrderCode(), order.getCreatedAt(), order.getStatus(), order.getTotalAmount(), order.getPaymentMethod());
    }

    public record CartCommand(Long productId, Integer quantity) { }
    public record QuantityCommand(Integer quantity) { }
    public record CartLine(Long productId, String name, String imageUrl, BigDecimal price, Integer quantity, Integer stockQuantity) { }
    public record CartResponse(List<CartLine> items, BigDecimal subtotal, int count) { }
    public record WishProduct(Long id, String name, String description, String imageUrl, BigDecimal price) { }
    public record ProfileCommand(String fullName, String phone, String address) { }
    public record UserProfile(String fullName, String email, String phone, String address) { }
    public record OrderSummary(String orderCode, LocalDateTime createdAt, String status, BigDecimal totalAmount, String paymentMethod) { }
    public record ProfileResponse(UserProfile user, List<OrderSummary> orders) { }
    public record MessageResponse(String message) { }
}