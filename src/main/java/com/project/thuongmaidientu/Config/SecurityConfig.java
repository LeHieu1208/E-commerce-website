package com.project.thuongmaidientu.Config;

import com.project.thuongmaidientu.Security.JwtFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // JWT là nguồn xác thực thật sự (đọc lại mỗi request qua JwtFilter),
                // nên không cần Spring Session lưu trạng thái đăng nhập nữa. Giữ
                // IF_REQUIRED (thay vì STATELESS) vì JwtFilter vẫn dùng session làm
                // bộ nhớ đệm currentUser cho các controller cũ - xem JwtFilter.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        // Tài nguyên tĩnh + ảnh upload - luôn công khai
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**", "/webjars/**").permitAll()
                        // Trang chủ, đăng nhập/đăng ký/đăng xuất - công khai
                        .requestMatchers("/", "/auth", "/auth/**", "/logout").permitAll()
                        // Duyệt sản phẩm công khai (không cần đăng nhập mới xem được)
                        .requestMatchers(HttpMethod.GET, "/products", "/products/search", "/products/{id}").permitAll()
                        // Webhook nhận thông báo chuyển khoản từ bên thứ 3 (Casso/SePay...)
                        // KHÔNG được yêu cầu JWT vì bên gọi không phải trình duyệt người dùng
                        .requestMatchers("/api/payment/webhook").permitAll()
                        // Toàn bộ khu vực quản trị - bắt buộc ROLE_ADMIN
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                        // Các luồng cần đăng nhập: giỏ hàng, thanh toán, hồ sơ, yêu thích, đánh giá
                        .requestMatchers("/cart/**", "/checkout/**", "/profile/**", "/wishlist/**",
                                "/api/orders/**", "/products/*/rate").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex
                        // Chưa đăng nhập mà vào trang cần auth -> đưa về trang login
                        // (thay vì trả 403 trắng mặc định, vì đây là app Thymeleaf
                        // hướng người dùng duyệt web, không phải API thuần).
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (isApiRequest(request)) {
                                response.sendError(401, "Chưa đăng nhập");
                            } else {
                                response.sendRedirect(request.getContextPath() + "/auth?mode=login");
                            }
                        })
                        // Đã đăng nhập nhưng không đủ quyền (vd user thường vào /admin)
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (isApiRequest(request)) {
                                response.sendError(403, "Không có quyền truy cập");
                            } else {
                                response.sendRedirect(request.getContextPath() + "/");
                            }
                        })
                )
                .logout(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }
}