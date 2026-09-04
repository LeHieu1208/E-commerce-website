package com.project.thuongmaidientu.Controller;

import com.project.thuongmaidientu.Model.Product;
import com.project.thuongmaidientu.Model.User;
import com.project.thuongmaidientu.Model.WishList;
import com.project.thuongmaidientu.Repository.ProductRepository;
import com.project.thuongmaidientu.Repository.WishListRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/wishlist")
public class WishlistController {
    private final WishListRepository wishListRepository;
    private final ProductRepository productRepository;

    public WishlistController(WishListRepository wishListRepository, ProductRepository productRepository) {
        this.wishListRepository = wishListRepository;
        this.productRepository = productRepository;
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
    public String viewWishlist(Model model, HttpSession session, RedirectAttributes ra) {
        User user = currentUser(session);
        if (user == null) {
            ra.addFlashAttribute("message", "Bạn cần đăng nhập để xem danh sách yêu thích");
            return "redirect:/auth?mode=login";
        }

        List<WishList> items = wishListRepository.findByUserId(user.getId());
        model.addAttribute("wishlistItems", items);
        model.addAttribute("currentUser", user);
        return "wishlist";
    }

    @PostMapping("/add")
    public String addToWishlist(@RequestParam Long productId, HttpSession session, RedirectAttributes ra) {
        User user = currentUser(session);
        if (user == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập trước khi thêm vào yêu thích");
            return "redirect:/auth?mode=login";
        }

        if (wishListRepository.findByUserIdAndProductId(user.getId(), productId).isEmpty()) {
            Product product = productRepository.findById(productId).orElse(null);
            if (product != null) {
                WishList entry = WishList.builder().user(user).product(product).build();
                wishListRepository.save(entry);
            }
        }

        ra.addFlashAttribute("message", "Đã thêm vào danh sách yêu thích");
        return "redirect:/";
    }

    @PostMapping("/remove")
    public String removeFromWishlist(@RequestParam Long productId, HttpSession session, RedirectAttributes ra) {
        User user = currentUser(session);
        if (user == null) {
            ra.addFlashAttribute("message", "Bạn cần đăng nhập để xóa khỏi yêu thích");
            return "redirect:/auth?mode=login";
        }

        wishListRepository.findByUserIdAndProductId(user.getId(), productId)
                .ifPresent(wishListRepository::delete);

        ra.addFlashAttribute("message", "Đã xóa khỏi danh sách yêu thích");
        return "redirect:/wishlist";
    }
}
