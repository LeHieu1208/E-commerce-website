package com.project.thuongmaidientu.Controller;

import com.project.thuongmaidientu.Model.User;
import com.project.thuongmaidientu.Repository.UserRepository;
import com.project.thuongmaidientu.Security.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @GetMapping("/auth")
    public String auth(HttpSession session, @RequestParam(required = false, defaultValue = "login") String mode, Model model) {
        if (session.getAttribute("currentUser") != null || session.getAttribute("loggedUser") != null) {
            return "redirect:/";
        }
        model.addAttribute("mode", "register".equals(mode) ? "register" : "login");
        return "auth";
    }

    @PostMapping("/auth/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpServletResponse response,
                        RedirectAttributes ra) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            issueTokenCookie(user, response);
            ra.addFlashAttribute("message", "Đăng nhập thành công");
            return "redirect:/";
        }

        ra.addFlashAttribute("message", "Email hoặc mật khẩu không đúng");
        return "redirect:/auth?mode=login";
    }

    @PostMapping("/auth/register")
    public String register(@RequestParam String fullName,
                           @RequestParam String email,
                           @RequestParam String password,
                           HttpServletResponse response,
                           RedirectAttributes ra) {
        if (userRepository.findByEmail(email).isPresent()) {
            ra.addFlashAttribute("message", "Email đã tồn tại");
            return "redirect:/auth?mode=register";
        }

        User newUser = User.builder()
                .fullName(fullName)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role("ROLE_USER")
                .build();
        userRepository.save(newUser);

        issueTokenCookie(newUser, response);
        ra.addFlashAttribute("message", "Đăng ký thành công");
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, HttpServletResponse response, RedirectAttributes ra) {
        session.invalidate();
        clearTokenCookie(response);
        ra.addFlashAttribute("message", "Đăng xuất thành công");
        return "redirect:/";
    }

    /**
     * Sinh JWT (chứa email làm subject + role) và gắn vào cookie HttpOnly.
     * HttpOnly = true để JavaScript phía client không đọc được token (chống XSS
     * đánh cắp token). Secure nên bật true khi deploy thật (chỉ gửi qua HTTPS);
     * để false ở đây vì môi trường dev chạy http://localhost.
     */
    private void issueTokenCookie(User user, HttpServletResponse response) {
        String token = jwtUtils.generateToken(user.getEmail(), user.getRole());

        ResponseCookie cookie = ResponseCookie.from(JwtUtils.COOKIE_NAME, token)
                .httpOnly(true)
                .secure(false) // TODO: đổi thành true khi deploy production dùng HTTPS
                .path("/")
                .maxAge(jwtUtils.getExpirationSeconds())
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(JwtUtils.COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}