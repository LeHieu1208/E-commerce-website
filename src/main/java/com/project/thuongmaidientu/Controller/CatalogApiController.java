package com.project.thuongmaidientu.Controller;

import com.project.thuongmaidientu.Model.Banner;
import com.project.thuongmaidientu.Model.Category;
import com.project.thuongmaidientu.Model.Product;
import com.project.thuongmaidientu.Model.ProductReview;
import com.project.thuongmaidientu.Model.User;
import com.project.thuongmaidientu.Repository.BannerRepository;
import com.project.thuongmaidientu.Repository.CategoryRepository;
import com.project.thuongmaidientu.Repository.ProductRepository;
import com.project.thuongmaidientu.Repository.ProductReviewRepository;
import com.project.thuongmaidientu.Service.CartService;
import com.project.thuongmaidientu.Service.ProductSearchService;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogApiController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BannerRepository bannerRepository;
    private final ProductReviewRepository productReviewRepository;
    private final CartService cartService;
    private final ProductSearchService productSearchService;

    public CatalogApiController(ProductRepository productRepository,
                                 CategoryRepository categoryRepository,
                                 BannerRepository bannerRepository,
                                 ProductReviewRepository productReviewRepository,
                                 CartService cartService,
                                 ProductSearchService productSearchService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.bannerRepository = bannerRepository;
        this.productReviewRepository = productReviewRepository;
        this.cartService = cartService;
        this.productSearchService = productSearchService;
    }

    @GetMapping("/home")
    public HomeResponse home(HttpSession session) {
        User user = currentUser(session);
        return new HomeResponse(
                categoryRepository.findAll().stream().map(CatalogApiController::toCategory).toList(),
                productRepository.findAll().stream().map(CatalogApiController::toProduct).toList(),
                bannerRepository.findAllByOrderByIdAsc().stream().map(CatalogApiController::toBanner).toList(),
                user == null ? null : new UserSummary(user.getFullName(), user.getEmail(), user.getRole()),
                cartService.getCount(session));
    }

            @GetMapping("/products")
            public List<ProductResponse> products(@RequestParam(required = false) Long categoryId,
                              @RequestParam(required = false) String keyword) {
            String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
            return productRepository.findAll().stream()
                .filter(product -> categoryId == null || (product.getCategory() != null
                    && categoryId.equals(product.getCategory().getId())))
                .filter(product -> normalizedKeyword.isEmpty()
                    || (product.getName() != null && product.getName().toLowerCase().contains(normalizedKeyword))
                    || (product.getDescription() != null
                    && product.getDescription().toLowerCase().contains(normalizedKeyword))
                    || (product.getCategory() != null && product.getCategory().getName() != null
                    && product.getCategory().getName().toLowerCase().contains(normalizedKeyword)))
                .map(CatalogApiController::toProduct)
                .toList();
            }

            @GetMapping("/products/{id}")
            public ResponseEntity<ProductDetailResponse> productDetail(@PathVariable Long id, HttpSession session) {
            return productRepository.findById(id)
                .map(product -> {
                    List<ProductResponse> related = product.getCategory() == null
                        ? List.of()
                        : productRepository.findTop4ByCategoryIdAndIdNot(product.getCategory().getId(), id)
                            .stream().map(CatalogApiController::toProduct).toList();
                    User user = currentUser(session);
                    boolean reviewed = user != null && productReviewRepository.findByProductAndUser(product, user).isPresent();
                    return ResponseEntity.ok(new ProductDetailResponse(toProduct(product), related, user != null, reviewed));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
            }
            @GetMapping("/search")
            public List<SearchSuggestion> search(@RequestParam(required = false, defaultValue = "") String keyword) {
                 return productSearchService.search(keyword).stream()
                     .limit(8)
                     .map(p -> new SearchSuggestion(p.getId(), p.getName(),
                          p.getCategory() == null ? null : p.getCategory().getName()))
                     .toList();
}

    @PostMapping("/products/{id}/rating")
    @Transactional
    public ResponseEntity<?> rateProduct(@PathVariable Long id,
                                         @RequestBody RatingRequest request,
                                         HttpSession session) {
        User user = currentUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body(new ApiMessage("Bạn cần đăng nhập để đánh giá sản phẩm."));
        }

        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        if (request.ratingValue() < 1 || request.ratingValue() > 5) {
            return ResponseEntity.badRequest().body(new ApiMessage("Vui lòng chọn mức đánh giá từ 1 đến 5 sao."));
        }
        if (productReviewRepository.findByProductAndUser(product, user).isPresent()) {
            return ResponseEntity.status(409).body(new ApiMessage("Bạn đã đánh giá sản phẩm này rồi. Mỗi tài khoản chỉ được đánh giá 1 lần."));
        }

        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.ratingValue());
        productReviewRepository.save(review);

        long reviewCount = productReviewRepository.countByProduct(product);
        Double average = productReviewRepository.findAverageRatingByProductId(id);
        product.setRating(Math.round((average == null ? 0.0 : average) * 10.0) / 10.0);
        product.setReviewCount((int) reviewCount);
        productRepository.save(product);

        return ResponseEntity.ok(new RatingResponse(
                "Cảm ơn bạn đã đánh giá " + request.ratingValue() + " sao cho sản phẩm.",
                product.getRating(), product.getReviewCount(), true));
    }

    private User currentUser(HttpSession session) {
        Object user = session.getAttribute("currentUser");
        if (user instanceof User currentUser) {
            return currentUser;
        }
        Object legacy = session.getAttribute("loggedUser");
        return legacy instanceof User loggedUser ? loggedUser : null;
    }

    private static CategoryResponse toCategory(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getIcon());
    }

    private static ProductResponse toProduct(Product product) {
        Category category = product.getCategory();
        return new ProductResponse(
            product.getId(), product.getName(), product.getDescription(), product.getPrice(), product.getOldPrice(),
                product.getDiscountedPrice(), product.getDiscountPercent(), product.getImageUrl(),
                product.getIcon(), product.getRating(), product.getReviewCount(), product.getStockQuantity(),
                category == null ? null : toCategory(category));
    }

    private static BannerResponse toBanner(Banner banner) {
        return new BannerResponse(banner.getId(), banner.getTitle(), banner.getLink(), banner.getImageUrl());
    }

    public record HomeResponse(List<CategoryResponse> categories,
                               List<ProductResponse> products,
                               List<BannerResponse> banners,
                               UserSummary currentUser,
                               int cartCount) { }

    public record CategoryResponse(Long id, String name, String icon) { }

    public record ProductResponse(Long id,
                                  String name,
                                  String description,
                                  BigDecimal price,
                                  BigDecimal oldPrice,
                                  BigDecimal discountedPrice,
                                  Integer discountPercent,
                                  String imageUrl,
                                  String icon,
                                  Double rating,
                                  Integer reviewCount,
                                  Integer stockQuantity,
                                  CategoryResponse category) { }

    public record ProductDetailResponse(ProductResponse product, List<ProductResponse> relatedProducts,
                                        boolean loggedIn, boolean reviewed) { }

    public record RatingRequest(int ratingValue) { }

    public record RatingResponse(String message, Double rating, Integer reviewCount, boolean reviewed) { }

    public record ApiMessage(String message) { }

    public record BannerResponse(Long id, String title, String link, String imageUrl) { }

    public record UserSummary(String fullName, String email, String role) { }

    public record SearchSuggestion(Long id, String name, String categoryName) { }
}