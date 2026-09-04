package com.project.thuongmaidientu.Controller;

import com.project.thuongmaidientu.Model.Product;
import com.project.thuongmaidientu.Model.ProductReview;
import com.project.thuongmaidientu.Model.User;
import com.project.thuongmaidientu.Repository.CategoryRepository;
import com.project.thuongmaidientu.Repository.ProductRepository;
import com.project.thuongmaidientu.Repository.ProductReviewRepository;
import com.project.thuongmaidientu.Service.CartService;
import com.project.thuongmaidientu.Service.ProductSearchService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ProductController {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductReviewRepository productReviewRepository;
    private final CartService cartService;
    private final ProductSearchService productSearchService;

    public ProductController(ProductRepository productRepository,
                             CategoryRepository categoryRepository,
                             ProductReviewRepository productReviewRepository,
                             CartService cartService,
                             ProductSearchService productSearchService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productReviewRepository = productReviewRepository;
        this.cartService = cartService;
        this.productSearchService = productSearchService;
    }

    @GetMapping("/products")
    public String list(@RequestParam(required = false) Long categoryId,
                        @RequestParam(required = false) String keyword,
                        Model model, HttpSession session) {
        List<Product> products = productRepository.findAll();
        var selectedCategory = categoryRepository.findById(categoryId).orElse(null);

        if (categoryId != null) {
            products = products.stream()
                    .filter(p -> p.getCategory() != null && categoryId.equals(p.getCategory().getId()))
                    .collect(Collectors.toList());
        }

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            products = products.stream()
                    .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(kw))
                    .collect(Collectors.toList());
        }

        model.addAttribute("products", products);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("selectedCategory", selectedCategory);
        model.addAttribute("cartItems", cartService.getItems(session));
        model.addAttribute("cartSubtotal", cartService.getSubtotal(session));
        model.addAttribute("cartCount", cartService.getCount(session));
        model.addAttribute("currentUser", getCurrentUser(session));
        return "product";
    }

    @GetMapping("/products/search")
    @ResponseBody
    public List<Product> search(@RequestParam(required = false, defaultValue = "") String keyword) {
        return productSearchService.search(keyword);
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Long id, Model model, HttpSession session) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        List<Product> related = List.of();
        if (product.getCategory() != null) {
            related = productRepository.findTop4ByCategoryIdAndIdNot(product.getCategory().getId(), id);
        }

        User currentUser = getCurrentUser(session);
        ProductReview myReview = currentUser != null
                ? productReviewRepository.findByProductAndUser(product, currentUser).orElse(null)
                : null;

        model.addAttribute("product", product);
        model.addAttribute("relatedProducts", related);
        model.addAttribute("cartItems", cartService.getItems(session));
        model.addAttribute("cartSubtotal", cartService.getSubtotal(session));
        model.addAttribute("cartCount", cartService.getCount(session));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("myReview", myReview);
        return "product-detail";
    }

    @PostMapping("/products/{id}/rate")
    public String rateProduct(@PathVariable Long id,
                             @RequestParam(name = "ratingValue", defaultValue = "0") int ratingValue,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("message", "Bạn cần đăng nhập để đánh giá sản phẩm.");
            return "redirect:/auth?mode=login";
        }

        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            redirectAttributes.addFlashAttribute("message", "Không tìm thấy sản phẩm để đánh giá.");
            return "redirect:/products";
        }

        if (ratingValue < 1 || ratingValue > 5) {
            redirectAttributes.addFlashAttribute("message", "Vui lòng chọn mức đánh giá từ 1 đến 5 sao.");
            return "redirect:/products/" + id;
        }

        ProductReview existingReview = productReviewRepository.findByProductAndUser(product, currentUser).orElse(null);
        if (existingReview != null) {
            redirectAttributes.addFlashAttribute("message", "Bạn đã đánh giá sản phẩm này rồi. Mỗi tài khoản chỉ được đánh giá 1 lần.");
            return "redirect:/products/" + id;
        }

        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setUser(currentUser);
        review.setRating(ratingValue);
        productReviewRepository.save(review);

        long reviewCount = productReviewRepository.countByProduct(product);
        Double averageRating = productReviewRepository.findAverageRatingByProductId(product.getId());
        double normalizedAverage = averageRating != null ? averageRating : 0.0;

        product.setRating(Math.round(normalizedAverage * 10.0) / 10.0);
        product.setReviewCount((int) reviewCount);
        productRepository.save(product);

        redirectAttributes.addFlashAttribute("message", "Cảm ơn bạn đã đánh giá " + ratingValue + " sao cho sản phẩm.");
        return "redirect:/products/" + id;
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
}