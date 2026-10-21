package com.project.thuongmaidientu.Controller;

import com.project.thuongmaidientu.Model.Banner;
import com.project.thuongmaidientu.Model.Category;
import com.project.thuongmaidientu.Model.Order;
import com.project.thuongmaidientu.Model.OrderItem;
import com.project.thuongmaidientu.Model.Product;
import com.project.thuongmaidientu.Repository.BannerRepository;
import com.project.thuongmaidientu.Repository.CategoryRepository;
import com.project.thuongmaidientu.Repository.OrderRepository;
import com.project.thuongmaidientu.Repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApiController {

    private static final List<String> STATUSES = List.of(
            "CHO_XAC_NHAN", "CHO_THANH_TOAN", "DA_THANH_TOAN", "DA_XAC_NHAN", "DANG_GIAO", "HOAN_THANH", "DA_HUY");

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BannerRepository bannerRepository;

    public AdminApiController(OrderRepository orderRepository,
                              ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              BannerRepository bannerRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.bannerRepository = bannerRepository;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        LocalDate today = LocalDate.now();
        BigDecimal todayRevenue = BigDecimal.ZERO;
        BigDecimal monthRevenue = BigDecimal.ZERO;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        long pending = 0;
        long completed = 0;
        long cancelled = 0;
        for (Order order : orders) {
            if ("HOAN_THANH".equals(order.getStatus())) completed++;
            if ("DA_HUY".equals(order.getStatus())) cancelled++;
            if (isPending(order.getStatus())) pending++;
            if (!revenueCounted(order) || order.getTotalAmount() == null || order.getCreatedAt() == null) continue;
            totalRevenue = totalRevenue.add(order.getTotalAmount());
            LocalDate created = order.getCreatedAt().toLocalDate();
            if (created.equals(today)) todayRevenue = todayRevenue.add(order.getTotalAmount());
            if (created.getMonth() == today.getMonth() && created.getYear() == today.getYear()) {
                monthRevenue = monthRevenue.add(order.getTotalAmount());
            }
        }
        List<DailyRevenue> daily = dailyRevenue(orders, today.minusDays(13), today);
        List<AdminProduct> lowStock = productRepository.findAll().stream()
                .filter(product -> product.getStockQuantity() != null && product.getStockQuantity() < 5)
                .sorted(Comparator.comparing(Product::getStockQuantity))
                .limit(8).map(this::toProduct).toList();
        return new DashboardResponse(todayRevenue, monthRevenue, totalRevenue, orders.size(), pending,
                completed, cancelled, productRepository.count(), daily, lowStock,
                orders.stream().limit(8).map(this::toOrder).toList(), topProducts(orders, false));
    }

    @GetMapping("/products")
    public List<AdminProduct> products(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Long categoryId) {
        String query = keyword == null ? "" : keyword.trim().toLowerCase();
        return productRepository.findAll().stream()
                .filter(product -> categoryId == null || (product.getCategory() != null
                        && categoryId.equals(product.getCategory().getId())))
                .filter(product -> query.isEmpty() || (product.getName() != null && product.getName().toLowerCase().contains(query)))
                .map(this::toProduct).toList();
    }

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@RequestBody ProductRequest request) {
        Product product = new Product();
        applyProduct(product, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toProduct(productRepository.save(product)));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody ProductRequest request) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) return ResponseEntity.notFound().build();
        applyProduct(product, request);
        return ResponseEntity.ok(toProduct(productRepository.save(product)));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            productRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new MessageResponse("Không thể xóa sản phẩm đã xuất hiện trong đơn hàng."));
        }
    }

    @GetMapping("/categories")
    public List<AdminCategory> categories() {
        Map<Long, Long> productCounts = new LinkedHashMap<>();
        productRepository.findAll().stream().filter(product -> product.getCategory() != null)
                .forEach(product -> productCounts.merge(product.getCategory().getId(), 1L, Long::sum));
        return categoryRepository.findAll().stream()
                .map(category -> toCategory(category, productCounts.getOrDefault(category.getId(), 0L))).toList();
    }

    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(@RequestBody CategoryRequest request) {
        Category category = Category.builder().name(request.name()).icon(request.icon()).description(request.description()).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toCategory(categoryRepository.save(category), 0));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody CategoryRequest request) {
        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) return ResponseEntity.notFound().build();
        category.setName(request.name());
        category.setIcon(request.icon());
        category.setDescription(request.description());
        long count = productRepository.findAll().stream().filter(product -> product.getCategory() != null
                && id.equals(product.getCategory().getId())).count();
        return ResponseEntity.ok(toCategory(categoryRepository.save(category), count));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            categoryRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new MessageResponse("Không thể xóa danh mục đang có sản phẩm."));
        }
    }

    @GetMapping("/banners")
    public List<AdminBanner> banners() {
        return bannerRepository.findAllByOrderByIdAsc().stream().map(this::toBanner).toList();
    }

    @PostMapping("/banners")
    public ResponseEntity<?> createBanner(@RequestBody BannerRequest request) {
        Banner banner = Banner.builder().title(request.title()).link(request.link()).imageUrl(request.imageUrl()).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toBanner(bannerRepository.save(banner)));
    }

    @PutMapping("/banners/{id}")
    public ResponseEntity<?> updateBanner(@PathVariable Long id, @RequestBody BannerRequest request) {
        Banner banner = bannerRepository.findById(id).orElse(null);
        if (banner == null) return ResponseEntity.notFound().build();
        banner.setTitle(request.title());
        banner.setLink(request.link());
        banner.setImageUrl(request.imageUrl());
        return ResponseEntity.ok(toBanner(bannerRepository.save(banner)));
    }

    @DeleteMapping("/banners/{id}")
    public ResponseEntity<Void> deleteBanner(@PathVariable Long id) {
        bannerRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/orders")
    public List<AdminOrder> orders(@RequestParam(required = false) String status,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) String paymentMethod) {
        String query = keyword == null ? "" : keyword.trim().toLowerCase();
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(order -> status == null || status.isBlank() || status.equals(order.getStatus()))
                .filter(order -> paymentMethod == null || paymentMethod.isBlank()
                        || paymentMethod.equalsIgnoreCase(order.getPaymentMethod()))
                .filter(order -> query.isEmpty() || contains(order.getOrderCode(), query)
                        || contains(order.getFullName(), query) || contains(order.getPhone(), query))
                .map(this::toOrder).toList();
    }

    @PatchMapping("/orders/{id}")
    public ResponseEntity<?> updateOrder(@PathVariable Long id, @RequestBody OrderUpdate request) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) return ResponseEntity.notFound().build();
        if (!STATUSES.contains(request.newStatus())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Trạng thái đơn hàng không hợp lệ."));
        }
        order.setStatus(request.newStatus());
        if (request.trackingCode() != null && !request.trackingCode().isBlank()) order.setTrackingCode(request.trackingCode().trim());
        if (request.adminNote() != null) order.setAdminNote(request.adminNote());
        LocalDateTime now = LocalDateTime.now();
        switch (request.newStatus()) {
            case "DA_XAC_NHAN" -> { if (order.getConfirmedAt() == null) order.setConfirmedAt(now); }
            case "DANG_GIAO" -> { if (order.getShippedAt() == null) order.setShippedAt(now); }
            case "HOAN_THANH" -> {
                if (order.getCompletedAt() == null) order.setCompletedAt(now);
                if ("COD".equals(order.getPaymentMethod()) && !Boolean.TRUE.equals(order.getPaid())) {
                    order.setPaid(true);
                    order.setPaidAt(now);
                }
            }
            case "DA_HUY" -> {
                order.setCancelledAt(now);
                if (request.cancelReason() != null && !request.cancelReason().isBlank()) order.setCancelReason(request.cancelReason().trim());
            }
            default -> { }
        }
        return ResponseEntity.ok(toOrder(orderRepository.save(order)));
    }

    @GetMapping("/revenue")
    public RevenueResponse revenue(@RequestParam(required = false) LocalDate from,
                                   @RequestParam(required = false) LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        if (start.isAfter(end)) {
            LocalDate swap = start;
            start = end;
            end = swap;
        }
        LocalDate fromDate = start;
        LocalDate toDate = end;
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(this::revenueCounted).filter(order -> order.getCreatedAt() != null)
            .filter(order -> !order.getCreatedAt().toLocalDate().isBefore(fromDate)
                && !order.getCreatedAt().toLocalDate().isAfter(toDate)).toList();
        BigDecimal total = orders.stream().map(Order::getTotalAmount).filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<DailyRevenue> daily = dailyRevenue(orders, fromDate, toDate);
        Map<String, BigDecimal> categories = new LinkedHashMap<>();
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                String category = item.getProduct() != null && item.getProduct().getCategory() != null
                        ? item.getProduct().getCategory().getName() : "Khác";
                categories.merge(category, item.getSubtotal(), BigDecimal::add);
            }
        }
        List<RevenueCategory> categoryRows = categories.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(entry -> new RevenueCategory(entry.getKey(), entry.getValue())).toList();
        BigDecimal average = orders.isEmpty() ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(orders.size()), 0, java.math.RoundingMode.HALF_UP);
        return new RevenueResponse(fromDate, toDate, total, orders.size(), average, daily, categoryRows, topProducts(orders, true));
    }

    private void applyProduct(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setOldPrice(request.oldPrice());
        product.setDiscountPercent(request.discountPercent());
        product.setImageUrl(request.imageUrl());
        product.setStockQuantity(request.stockQuantity() == null ? 0 : request.stockQuantity());
        product.setCategory(request.categoryId() == null ? null : categoryRepository.findById(request.categoryId()).orElse(null));
    }

    private AdminProduct toProduct(Product product) {
        Category category = product.getCategory();
        return new AdminProduct(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getOldPrice(), product.getDiscountPercent(), product.getImageUrl(), product.getStockQuantity(),
                category == null ? null : category.getId(), category == null ? null : category.getName());
    }

    private AdminCategory toCategory(Category category, long count) {
        return new AdminCategory(category.getId(), category.getName(), category.getIcon(), category.getDescription(), count);
    }

    private AdminBanner toBanner(Banner banner) {
        return new AdminBanner(banner.getId(), banner.getTitle(), banner.getLink(), banner.getImageUrl());
    }

    private AdminOrder toOrder(Order order) {
        List<AdminOrderLine> items = order.getItems().stream().map(item -> new AdminOrderLine(
                item.getProduct() == null ? "Sản phẩm đã xóa" : item.getProduct().getName(),
                item.getQuantity(), item.getPrice(), item.getSubtotal())).toList();
        return new AdminOrder(order.getId(), order.getOrderCode(), order.getFullName(), order.getPhone(), order.getAddress(),
                order.getPaymentMethod(), order.getPaid(), order.getStatus(), order.getTotalAmount(), order.getCreatedAt(),
                order.getTrackingCode(), order.getAdminNote(), order.getCancelReason(), items);
    }

    private List<DailyRevenue> dailyRevenue(List<Order> orders, LocalDate start, LocalDate end) {
        List<DailyRevenue> values = new ArrayList<>();
        DateTimeFormatter label = DateTimeFormatter.ofPattern("dd/MM");
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            LocalDate current = date;
            BigDecimal amount = orders.stream().filter(order -> order.getCreatedAt() != null
                    && order.getCreatedAt().toLocalDate().equals(current)).map(Order::getTotalAmount)
                    .filter(value -> value != null).reduce(BigDecimal.ZERO, BigDecimal::add);
            values.add(new DailyRevenue(date.format(label), amount));
        }
        return values;
    }

    private List<SalesProduct> topProducts(List<Order> orders, boolean sortByRevenue) {
        Map<Long, String> names = new LinkedHashMap<>();
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        Map<Long, BigDecimal> revenues = new LinkedHashMap<>();
        for (Order order : orders) {
            if ("DA_HUY".equals(order.getStatus())) continue;
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() == null) continue;
                Long id = item.getProduct().getId();
                names.putIfAbsent(id, item.getProduct().getName());
                quantities.merge(id, item.getQuantity() == null ? 0 : item.getQuantity(), Integer::sum);
                revenues.merge(id, item.getSubtotal(), BigDecimal::add);
            }
        }
        Comparator<Long> comparator = sortByRevenue
            ? Comparator.<Long, BigDecimal>comparing(revenues::get).reversed()
            : Comparator.<Long, Integer>comparing(quantities::get).reversed();
        return quantities.keySet().stream().sorted(comparator).limit(sortByRevenue ? 10 : 5)
                .map(id -> new SalesProduct(names.get(id), quantities.get(id), revenues.get(id))).toList();
    }

    private boolean revenueCounted(Order order) {
        return Boolean.TRUE.equals(order.getPaid()) || "HOAN_THANH".equals(order.getStatus());
    }

    private boolean isPending(String status) {
        return List.of("CHO_XAC_NHAN", "CHO_THANH_TOAN", "DA_THANH_TOAN", "DA_XAC_NHAN", "DANG_GIAO").contains(status);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    public record ProductRequest(String name, String description, BigDecimal price, BigDecimal oldPrice,
                                 Integer discountPercent, String imageUrl, Integer stockQuantity, Long categoryId) { }
    public record AdminProduct(Long id, String name, String description, BigDecimal price, BigDecimal oldPrice,
                               Integer discountPercent, String imageUrl, Integer stockQuantity,
                               Long categoryId, String categoryName) { }
    public record CategoryRequest(String name, String icon, String description) { }
    public record AdminCategory(Long id, String name, String icon, String description, long productCount) { }
    public record BannerRequest(String title, String link, String imageUrl) { }
    public record AdminBanner(Long id, String title, String link, String imageUrl) { }
    public record OrderUpdate(String newStatus, String trackingCode, String adminNote, String cancelReason) { }
    public record AdminOrderLine(String productName, Integer quantity, BigDecimal price, BigDecimal subtotal) { }
    public record AdminOrder(Long id, String orderCode, String fullName, String phone, String address,
                             String paymentMethod, Boolean paid, String status, BigDecimal totalAmount,
                             LocalDateTime createdAt, String trackingCode, String adminNote, String cancelReason,
                             List<AdminOrderLine> items) { }
    public record DailyRevenue(String label, BigDecimal amount) { }
    public record RevenueCategory(String name, BigDecimal amount) { }
        public record SalesProduct(String name, int quantity, BigDecimal revenue) { }
    public record DashboardResponse(BigDecimal revenueToday, BigDecimal revenueMonth, BigDecimal revenueTotal,
                                    long totalOrders, long pendingOrders, long completedOrders, long cancelledOrders,
                                    long totalProducts, List<DailyRevenue> dailyRevenue, List<AdminProduct> lowStock,
                                                                        List<AdminOrder> recentOrders, List<SalesProduct> topProducts) { }
    public record RevenueResponse(LocalDate from, LocalDate to, BigDecimal totalRevenue, long orderCount,
                                  BigDecimal averageOrderValue, List<DailyRevenue> dailyRevenue,
                                                                    List<RevenueCategory> categoryBreakdown, List<SalesProduct> topProducts) { }
    public record MessageResponse(String message) { }
}