package com.project.thuongmaidientu.Config;

import com.project.thuongmaidientu.Model.*;
import com.project.thuongmaidientu.Repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            BannerRepository bannerRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            // Seed Users nếu chưa có
            if (userRepository.count() == 0) {
                User admin = User.builder()
                        .fullName("admin")
                        .email("admin@gmail.com")
                        .password(passwordEncoder.encode("admin123"))
                        .role("ROLE_ADMIN")
                        .build();

                User customer = User.builder()
                        .fullName("user")
                        .email("user@gmail.com")
                        .password(passwordEncoder.encode("123456"))
                        .role("ROLE_USER")
                        .build();

                userRepository.saveAll(List.of(admin, customer));
            }

            // Seed Categories & Products
            if (categoryRepository.count() == 0) {
                List<Category> categories = categoryRepository.saveAll(List.of(
                        Category.builder().name("Laptop").build(),
                        Category.builder().name("Điện thoại").build(),
                        Category.builder().name("Ipad").build()
                ));

                Product laptop = Product.builder()
                        .name("Laptop Apple M3 16GB 512GB")
                        .description("Laptop Apple M3 16GB 512GB")
                        .price(BigDecimal.valueOf(45000000))
                        .discountPercent(10)
                        .category(categories.get(0))
                        .rating(4.5)
                        .stockQuantity(20)
                        .build();

                Product phone = Product.builder()
                        .name("Smartphone Apple 256GB Titanium")
                        .description("Smartphone Apple 256GB Titanium")
                        .imageUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTQLhPZXNVD_XD9lAwwRA_TAAuDNndpAiIi_xyCyCat-A&s=10")
                        .price(BigDecimal.valueOf(30000000.0))
                        .category(categories.get(1))
                        .discountPercent(10)
                        .rating(3.2)
                        .stockQuantity(15)
                        .build();

                productRepository.saveAll(List.of(laptop, phone));
            }
            // Nếu đã có dữ liệu (categoryRepository.count() != 0) thì không seed/ghi đè gì nữa,
            // dữ liệu hiển thị sẽ luôn lấy từ cơ sở dữ liệu (kể cả khi admin đã chỉnh sửa).
            if (bannerRepository.count() == 0) {
                List<Category> cats = categoryRepository.findAll();
                String laptopLink = !cats.isEmpty() ? "/products?categoryId=" + cats.get(0).getId() : "/products";
                String phoneLink = cats.size() > 1 ? "/products?categoryId=" + cats.get(1).getId() : "/products";

                bannerRepository.saveAll(List.of(
                        Banner.builder()
                                .title("Sale Laptop giảm đến 20%")
                                .link(laptopLink)
                                .imageUrl("https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=1200&q=80")
                                .build(),
                        Banner.builder()
                                .title("Điện thoại flagship giá tốt")
                                .link(phoneLink)
                                .imageUrl("https://images.unsplash.com/photo-1592286927505-1def25115558?w=1200&q=80")
                                .build()
                ));
            }
        };
    }
}