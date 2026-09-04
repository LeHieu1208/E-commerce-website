package com.project.thuongmaidientu.Controller;

import com.project.thuongmaidientu.Model.User;
import com.project.thuongmaidientu.Repository.UserRepository;
import jakarta.servlet.http.HttpSession;
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

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
                        HttpSession session,
                        RedirectAttributes ra) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            session.setAttribute("currentUser", user);
            session.setAttribute("loggedUser", user);
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
                           HttpSession session,
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

        session.setAttribute("currentUser", newUser);
        session.setAttribute("loggedUser", newUser);
        ra.addFlashAttribute("message", "Đăng ký thành công");
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes ra) {
        session.invalidate();
        ra.addFlashAttribute("message", "Đăng xuất thành công");
        return "redirect:/";
    }
}