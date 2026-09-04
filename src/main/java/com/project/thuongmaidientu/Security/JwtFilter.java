package com.project.thuongmaidientu.Security;

import com.project.thuongmaidientu.Model.User;
import com.project.thuongmaidientu.Repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

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
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtils.validateToken(token)) {
                // Lấy email từ token (hàm getUsernameFromToken trả về subject/email)
                String email = jwtUtils.getUsernameFromToken(token); 
                
                // Tìm User theo email
                User user = userRepository.findByEmail(email).orElse(null);

                if (user != null) {
                    // Đảm bảo role không bị null, nếu chưa gán role sẽ lấy mặc định ROLE_USER
                    String userRole = user.getRole() != null ? user.getRole() : "ROLE_USER";
                    var authorities = Collections.singletonList(new SimpleGrantedAuthority(userRole));
                    
                    // Dùng user.getEmail() làm principal định danh
                    var auth = new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}