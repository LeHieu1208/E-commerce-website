package com.project.thuongmaidientu.Controller;

import com.project.thuongmaidientu.Model.CartItem;
import com.project.thuongmaidientu.Model.User;
import com.project.thuongmaidientu.Service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
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

    @GetMapping
    public String viewCart(Model model, HttpSession session, RedirectAttributes ra) {
        User user = currentUser(session);
        if (user == null) {
            ra.addFlashAttribute("message", "Bạn cần đăng nhập để xem giỏ hàng");
            return "redirect:/auth?mode=login";
        }

        List<CartItem> items = cartService.getItems(session);
        BigDecimal subtotal = cartService.getSubtotal(session);

        model.addAttribute("cartItems", items);
        model.addAttribute("cartSubtotal", subtotal);
        model.addAttribute("cartCount", cartService.getCount(session));
        model.addAttribute("currentUser", user);
        return "cart";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(defaultValue = "1") int quantity,
                            HttpSession session,
                            RedirectAttributes ra) {
        User user = currentUser(session);
        if (user == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập trước khi dùng giỏ hàng");
            return "redirect:/auth?mode=login";
        }

        cartService.add(session, productId, quantity);
        ra.addFlashAttribute("message", "Đã thêm sản phẩm vào giỏ hàng");
        return "redirect:/";
    }

    @PostMapping("/update")
    public String updateQuantity(@RequestParam Long productId,
                                 @RequestParam Integer quantity,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        User user = currentUser(session);
        if (user == null) {
            ra.addFlashAttribute("message", "Bạn cần đăng nhập để cập nhật giỏ hàng");
            return "redirect:/auth?mode=login";
        }

        cartService.updateQuantity(session, productId, quantity);
        ra.addFlashAttribute("message", "Đã cập nhật số lượng");
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String remove(@RequestParam Long productId,
                         HttpSession session,
                         RedirectAttributes ra) {
        User user = currentUser(session);
        if (user == null) {
            ra.addFlashAttribute("message", "Bạn cần đăng nhập để xóa sản phẩm khỏi giỏ hàng");
            return "redirect:/auth?mode=login";
        }

        cartService.remove(session, productId);
        ra.addFlashAttribute("message", "Đã xóa sản phẩm khỏi giỏ hàng");
        return "redirect:/cart";
    }
}