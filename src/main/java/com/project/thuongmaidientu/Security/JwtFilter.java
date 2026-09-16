package com.project.thuongmaidientu.Security;

import com.project.thuongmaidientu.Model.User;
import com.project.thuongmaidientu.Repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Đọc JWT, ưu tiên lấy từ cookie ACCESS_TOKEN (vì đây là ứng dụng Thymeleaf
 * render phía server - trình duyệt không tự gắn header Authorization khi
 * load trang như một SPA sẽ làm). Vẫn hỗ trợ đọc từ header "Authorization:
 * Bearer ..." để tương thích nếu sau này có client API/mobile gọi vào.
 *
 * Sau khi xác thực JWT thành công, filter nạp lại User vào CẢ HAI nơi:
 *  - SecurityContextHolder: để Spring Security (@PreAuthorize, hasRole trong
 *    SecurityConfig) hoạt động đúng.
 *  - HttpSession attribute "currentUser"/"loggedUser": để toàn bộ controller
 *    hiện tại (CartController, CheckoutController, AdminController...) vốn
 *    đang đọc currentUser từ session tiếp tục chạy đúng mà KHÔNG cần sửa lại.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    public JwtFilter(JwtUtils jwtUtils, UserRepository userRepository) {
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromCookie(request);
        if (token == null) {
            token = extractTokenFromHeader(request);
        }

        if (token != null && jwtUtils.validateToken(token)) {
            String email = jwtUtils.getUsernameFromToken(token);
            User user = userRepository.findByEmail(email).orElse(null);

            if (user != null) {
                String userRole = user.getRole() != null ? user.getRole() : "ROLE_USER";
                var authorities = Collections.singletonList(new SimpleGrantedAuthority(userRole));

                var auth = new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

                // Giữ tương thích ngược: nạp lại User vào session cho các
                // controller cũ vẫn đang đọc session.getAttribute("currentUser").
                request.getSession().setAttribute("currentUser", user);
                request.getSession().setAttribute("loggedUser", user);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (JwtUtils.COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}