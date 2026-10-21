package com.project.thuongmaidientu.Controller;

import com.project.thuongmaidientu.Model.User;
import com.project.thuongmaidientu.Repository.UserRepository;
import com.project.thuongmaidientu.Security.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthApiController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
                                   HttpServletResponse response,
                                   HttpSession session) {
        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("message", "Email hoặc mật khẩu không đúng"));
        }

        issueTokenCookie(user, response);
        session.setAttribute("currentUser", user);
        session.setAttribute("loggedUser", user);
        return ResponseEntity.ok(toUserResponse(user));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request,
                                      HttpServletResponse response,
                                      HttpSession session) {
        if (request.fullName() == null || request.fullName().isBlank()
                || request.email() == null || request.email().isBlank()
                || request.password() == null || request.password().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng điền đầy đủ thông tin"));
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email đã tồn tại"));
        }

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(request.email().trim())
                .password(passwordEncoder.encode(request.password()))
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
        issueTokenCookie(user, response);
        session.setAttribute("currentUser", user);
        session.setAttribute("loggedUser", user);
        return ResponseEntity.ok(toUserResponse(user));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        User user = currentUser(session);
        return user == null
                ? ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"))
                : ResponseEntity.ok(toUserResponse(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session, HttpServletResponse response) {
        session.invalidate();
        ResponseCookie cookie = ResponseCookie.from(JwtUtils.COOKIE_NAME, "")
                .httpOnly(true).secure(false).path("/").maxAge(0).sameSite("Lax").build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.noContent().build();
    }

    private User currentUser(HttpSession session) {
        Object user = session.getAttribute("currentUser");
        if (user instanceof User currentUser) return currentUser;
        Object legacy = session.getAttribute("loggedUser");
        return legacy instanceof User loggedUser ? loggedUser : null;
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }

    private void issueTokenCookie(User user, HttpServletResponse response) {
        String token = jwtUtils.generateToken(user.getEmail(), user.getRole());
        ResponseCookie cookie = ResponseCookie.from(JwtUtils.COOKIE_NAME, token)
                .httpOnly(true).secure(false).path("/")
                .maxAge(jwtUtils.getExpirationSeconds()).sameSite("Lax").build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public record LoginRequest(String email, String password) { }
    public record RegisterRequest(String fullName, String email, String password) { }
    public record UserResponse(Long id, String fullName, String email, String role) { }
}