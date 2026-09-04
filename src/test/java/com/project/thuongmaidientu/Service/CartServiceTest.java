package com.project.thuongmaidientu.Service;

import com.project.thuongmaidientu.Model.Cart;
import com.project.thuongmaidientu.Model.CartItem;
import com.project.thuongmaidientu.Model.Product;
import com.project.thuongmaidientu.Model.User;
import com.project.thuongmaidientu.Repository.CartItemRepository;
import com.project.thuongmaidientu.Repository.CartRepository;
import com.project.thuongmaidientu.Repository.ProductRepository;
import com.project.thuongmaidientu.Repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CartServiceTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void getItemsMergesDuplicateCartsForSameUser() {
        String email = "carttest-" + UUID.randomUUID() + "@example.com";
        User user = userRepository.save(User.builder()
                .email(email)
                .password("123456")
                .fullName("Test User")
                .role("ROLE_USER")
                .build());

        Product product1 = productRepository.save(Product.builder()
                .name("Laptop A")
                .price(new BigDecimal("20000000"))
                .stockQuantity(10)
                .build());

        Product product2 = productRepository.save(Product.builder()
                .name("Laptop B")
                .price(new BigDecimal("30000000"))
                .stockQuantity(10)
                .build());

        Cart firstCart = cartRepository.save(Cart.builder().user(user).build());
        cartItemRepository.save(CartItem.builder().cart(firstCart).product(product1).quantity(1).build());

        Cart secondCart = cartRepository.save(Cart.builder().user(user).build());
        cartItemRepository.save(CartItem.builder().cart(secondCart).product(product2).quantity(2).build());

        HttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", user);

        List<CartItem> items = cartService.getItems(session);

        assertThat(items).hasSize(2);
        assertThat(cartRepository.findAllByUser(user)).hasSize(1);
    }
}
