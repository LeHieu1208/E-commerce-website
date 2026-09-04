package com.project.thuongmaidientu.Controller;

import com.project.thuongmaidientu.Model.Category;
import com.project.thuongmaidientu.Model.Product;
import com.project.thuongmaidientu.Model.User;
import com.project.thuongmaidientu.Repository.BannerRepository;
import com.project.thuongmaidientu.Repository.CategoryRepository;
import com.project.thuongmaidientu.Repository.ProductRepository;
import com.project.thuongmaidientu.Service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Controller
public class HomeController {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final BannerRepository bannerRepository;

    public HomeController(CategoryRepository categoryRepository,
                          ProductRepository productRepository,
                          CartService cartService,
                          BannerRepository bannerRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.cartService = cartService;
        this.bannerRepository = bannerRepository;
    }

    @GetMapping({"/", "/main"})
    public String home(Model model, HttpSession session) {
        User currentUser = getCurrentUser(session);
        List<Category> categories = categoryRepository.findAll();
        List<Product> products = productRepository.findAll();

        List<CategoryProductGroup> categoryGroups = new ArrayList<>();
        for (Category category : categories) {
            List<Product> categoryProducts = products.stream()
                    .filter(product -> product.getCategory() != null
                            && Objects.equals(product.getCategory().getId(), category.getId()))
                    .toList();
            if (!categoryProducts.isEmpty()) {
                categoryGroups.add(new CategoryProductGroup(category, categoryProducts));
            }
        }

        model.addAttribute("categories", categories);
        model.addAttribute("products", products);
        model.addAttribute("categoryGroups", categoryGroups);
        model.addAttribute("cartItems", cartService.getItems(session));
        model.addAttribute("cartSubtotal", cartService.getSubtotal(session));
        model.addAttribute("cartCount", cartService.getCount(session));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("banners", bannerRepository.findAllByOrderByIdAsc());
        return "main";
    }

    private User getCurrentUser(HttpSession session) {
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

    public static class CategoryProductGroup {
        private final Category category;
        private final List<Product> products;

        public CategoryProductGroup(Category category, List<Product> products) {
            this.category = category;
            this.products = products;
        }

        public Category getCategory() {
            return category;
        }

        public List<Product> getProducts() {
            return products;
        }
    }
}