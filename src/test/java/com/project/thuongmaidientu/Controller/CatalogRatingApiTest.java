package com.project.thuongmaidientu.Controller;

import com.project.thuongmaidientu.Model.Category;
import com.project.thuongmaidientu.Model.Product;
import com.project.thuongmaidientu.Model.User;
import com.project.thuongmaidientu.Repository.CategoryRepository;
import com.project.thuongmaidientu.Repository.ProductRepository;
import com.project.thuongmaidientu.Repository.UserRepository;
import com.project.thuongmaidientu.Security.JwtUtils;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CatalogRatingApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private Long productId;
    private String token;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        User user = User.builder()
                .email("rating-" + suffix + "@test.local")
                .password("unused")
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
        token = jwtUtils.generateToken(user.getEmail(), user.getRole());

        Category category = categoryRepository.save(Category.builder().name("Rating test " + suffix).build());
        Product product = productRepository.save(Product.builder()
                .name("Rating product " + suffix)
                .price(BigDecimal.valueOf(100_000))
                .stockQuantity(3)
                .rating(0.0)
                .reviewCount(0)
                .category(category)
                .build());
        productId = product.getId();
    }

    @Test
    @Transactional
    void ratingRequiresLogin() throws Exception {
        mockMvc.perform(post("/api/catalog/products/{id}/rating", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ratingValue\":5}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void ratingRejectsValuesOutsideOneToFive() throws Exception {
        mockMvc.perform(authenticatedRating(0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vui lòng chọn mức đánh giá từ 1 đến 5 sao."));
    }

    @Test
    @Transactional
    void successfulRatingReturnsUpdatedAggregate() throws Exception {
        mockMvc.perform(authenticatedRating(4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4.0))
                .andExpect(jsonPath("$.reviewCount").value(1))
                .andExpect(jsonPath("$.reviewed").value(true))
                .andExpect(jsonPath("$.message").value("Cảm ơn bạn đã đánh giá 4 sao cho sản phẩm."));
    }

    @Test
    @Transactional
    void userCanRateProductOnlyOnce() throws Exception {
        mockMvc.perform(authenticatedRating(5)).andExpect(status().isOk());

        mockMvc.perform(authenticatedRating(3))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Bạn đã đánh giá sản phẩm này rồi. Mỗi tài khoản chỉ được đánh giá 1 lần."));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authenticatedRating(int rating) {
        return post("/api/catalog/products/{id}/rating", productId)
                .cookie(new Cookie(JwtUtils.COOKIE_NAME, token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ratingValue\":" + rating + "}");
    }
}