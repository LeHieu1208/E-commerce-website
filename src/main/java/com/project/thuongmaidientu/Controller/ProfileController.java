package com.project.thuongmaidientu.Controller;

import com.project.thuongmaidientu.Model.User;
import com.project.thuongmaidientu.Repository.UserRepository;
import com.project.thuongmaidientu.Service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final UserRepository userRepository;
    private final OrderService orderService;

    public ProfileController(UserRepository userRepository, OrderService orderService) {
        this.userRepository = userRepository;
        this.orderService = orderService;
    }

    private User currentUser(HttpSession session) {
        Object user = session.getAttribute("currentUser");
        if (user instanceof User currentUser) {
            return currentUser;
        }
        Object legacy = session.getAttribute("loggedUser");
        if (legacy instanceof User loggedUser) {
            return loggedUser;
        }
        return null;
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model, RedirectAttributes ra) {
        User user = currentUser(session);
        if (user == null) {
            ra.addFlashAttribute("message", "Bạn cần đăng nhập để xem thông tin cá nhân");
            return "redirect:/auth?mode=login";
        }

        model.addAttribute("user", userRepository.findById(user.getId()).orElse(user));
        model.addAttribute("orders", orderService.findOrdersOfUser(user));
        return "profile";
    }

    @PostMapping("/profile")
    public String saveProfile(@ModelAttribute("user") User form,
                              HttpSession session,
                              RedirectAttributes ra) {
        User current = currentUser(session);
        if (current == null) {
            ra.addFlashAttribute("message", "Bạn cần đăng nhập để cập nhật thông tin");
            return "redirect:/auth?mode=login";
        }

        User user = userRepository.findById(current.getId()).orElse(current);
        user.setFullName(form.getFullName());
        user.setPhone(form.getPhone());
        user.setAddress(form.getAddress());
        userRepository.save(user);

        session.setAttribute("currentUser", user);
        session.setAttribute("loggedUser", user);
        ra.addFlashAttribute("message", "Đã cập nhật thông tin cá nhân");
        return "redirect:/profile";
    }
}
