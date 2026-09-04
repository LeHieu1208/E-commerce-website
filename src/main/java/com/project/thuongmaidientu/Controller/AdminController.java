package com.project.thuongmaidientu.Controller;

import com.project.thuongmaidientu.Model.Banner;
import com.project.thuongmaidientu.Model.Category;
import com.project.thuongmaidientu.Model.Order;
import com.project.thuongmaidientu.Model.OrderItem;
import com.project.thuongmaidientu.Model.Product;
import com.project.thuongmaidientu.Model.User;
import com.project.thuongmaidientu.Repository.BannerRepository;
import com.project.thuongmaidientu.Repository.CategoryRepository;
import com.project.thuongmaidientu.Repository.OrderRepository;
import com.project.thuongmaidientu.Repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Trang quản trị (Admin) dành cho chủ shop:
 * - Dashboard tổng quan doanh thu, đơn hàng
 * - Quản lý đơn hàng (xem, cập nhật trạng thái, vận đơn)
 * - Quản lý sản phẩm (thêm/sửa/xóa, tồn kho)
 * - Báo cáo doanh thu theo khoảng thời gian
 *
 * Bảo vệ bằng session (giống các trang khác trong hệ thống): chỉ user có
 * role = ROLE_ADMIN mới truy cập được, ngược lại sẽ bị chuyển hướng.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final List<String> STATUS_ORDER = List.of(
            "CHO_XAC_NHAN", "CHO_THANH_TOAN", "DA_THANH_TOAN", "DA_XAC_NHAN",
            "DANG_GIAO", "HOAN_THANH", "DA_HUY"
    );

    private static final Map<String, String> STATUS_LABELS = Map.of(
            "CHO_XAC_NHAN", "Chờ xác nhận",
            "CHO_THANH_TOAN", "Chờ thanh toán",
            "DA_THANH_TOAN", "Đã thanh toán",
            "DA_XAC_NHAN", "Đã xác nhận",
            "DANG_GIAO", "Đang giao",
            "HOAN_THANH", "Hoàn thành",
            "DA_HUY", "Đã hủy"
    );

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BannerRepository bannerRepository;

    public AdminController(OrderRepository orderRepository,
                            ProductRepository productRepository,
                            CategoryRepository categoryRepository,
                            BannerRepository bannerRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.bannerRepository = bannerRepository;
    }

    // ================= Helpers =================

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

    private boolean isAdmin(User user) {
        return user != null && "ROLE_ADMIN".equals(user.getRole());
    }

    /** Trả về admin đang đăng nhập, hoặc null nếu chưa đăng nhập / không phải admin. */
    private User requireAdmin(HttpSession session) {
        User user = currentUser(session);
        return isAdmin(user) ? user : null;
    }

    private boolean isRevenueCounted(Order o) {
        return Boolean.TRUE.equals(o.getPaid()) || "HOAN_THANH".equals(o.getStatus());
    }

    private void addCommonAttributes(Model model, User admin) {
        model.addAttribute("currentUser", admin);
        model.addAttribute("statusLabels", STATUS_LABELS);
        model.addAttribute("statusList", STATUS_ORDER);
    }

    // ================= Dashboard =================

    @GetMapping({"", "/"})
    public String dashboard(HttpSession session, Model model, RedirectAttributes ra) {
        User admin = requireAdmin(session);
        if (admin == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập bằng tài khoản quản trị");
            return "redirect:/auth?mode=login";
        }

        List<Order> allOrders = orderRepository.findAllByOrderByCreatedAtDesc();
        LocalDate today = LocalDate.now();

        BigDecimal revenueToday = BigDecimal.ZERO;
        BigDecimal revenueMonth = BigDecimal.ZERO;
        BigDecimal revenueTotal = BigDecimal.ZERO;
        long pendingOrders = 0;
        long completedOrders = 0;
        long cancelledOrders = 0;

        for (Order o : allOrders) {
            if (o.getStatus() != null) {
                if (o.getStatus().equals("HOAN_THANH")) completedOrders++;
                if (o.getStatus().equals("DA_HUY")) cancelledOrders++;
                if (o.getStatus().equals("CHO_XAC_NHAN") || o.getStatus().equals("CHO_THANH_TOAN")
                        || o.getStatus().equals("DA_THANH_TOAN") || o.getStatus().equals("DA_XAC_NHAN")
                        || o.getStatus().equals("DANG_GIAO")) {
                    pendingOrders++;
                }
            }
            if (isRevenueCounted(o) && o.getTotalAmount() != null && o.getCreatedAt() != null) {
                revenueTotal = revenueTotal.add(o.getTotalAmount());
                LocalDate created = o.getCreatedAt().toLocalDate();
                if (created.equals(today)) {
                    revenueToday = revenueToday.add(o.getTotalAmount());
                }
                if (created.getMonthValue() == today.getMonthValue() && created.getYear() == today.getYear()) {
                    revenueMonth = revenueMonth.add(o.getTotalAmount());
                }
            }
        }

        // Doanh thu 14 ngày gần nhất, dùng cho biểu đồ
        List<String> chartLabels = new ArrayList<>();
        List<BigDecimal> chartValues = new ArrayList<>();
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd/MM");
        for (int i = 13; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            BigDecimal dayTotal = BigDecimal.ZERO;
            for (Order o : allOrders) {
                if (isRevenueCounted(o) && o.getTotalAmount() != null && o.getCreatedAt() != null
                        && o.getCreatedAt().toLocalDate().equals(day)) {
                    dayTotal = dayTotal.add(o.getTotalAmount());
                }
            }
            chartLabels.add(day.format(dayFmt));
            chartValues.add(dayTotal);
        }

        // Top 5 sản phẩm bán chạy (dựa trên các đơn chưa bị hủy)
        Map<Long, String> productNames = new LinkedHashMap<>();
        Map<Long, Integer> soldQty = new LinkedHashMap<>();
        Map<Long, BigDecimal> soldRevenue = new LinkedHashMap<>();
        for (Order o : allOrders) {
            if ("DA_HUY".equals(o.getStatus())) continue;
            for (OrderItem item : o.getItems()) {
                if (item.getProduct() == null) continue;
                Long pid = item.getProduct().getId();
                productNames.putIfAbsent(pid, item.getProduct().getName());
                soldQty.merge(pid, item.getQuantity() == null ? 0 : item.getQuantity(), Integer::sum);
                soldRevenue.merge(pid, item.getSubtotal(), BigDecimal::add);
            }
        }
        List<Map<String, Object>> topProducts = new ArrayList<>();
        soldQty.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(5)
                .forEach(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", productNames.get(e.getKey()));
                    row.put("quantity", e.getValue());
                    row.put("revenue", soldRevenue.get(e.getKey()));
                    topProducts.add(row);
                });

        // Sản phẩm sắp hết hàng
        List<Product> lowStock = productRepository.findAll().stream()
                .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() < 5)
                .sorted(Comparator.comparing(Product::getStockQuantity))
                .limit(8)
                .toList();

        addCommonAttributes(model, admin);
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("revenueToday", revenueToday);
        model.addAttribute("revenueMonth", revenueMonth);
        model.addAttribute("revenueTotal", revenueTotal);
        model.addAttribute("totalOrders", allOrders.size());
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("completedOrders", completedOrders);
        model.addAttribute("cancelledOrders", cancelledOrders);
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartValues", chartValues);
        model.addAttribute("topProducts", topProducts);
        model.addAttribute("lowStockProducts", lowStock);
        model.addAttribute("recentOrders", allOrders.stream().limit(8).toList());
        model.addAttribute("totalProducts", productRepository.count());

        return "admin-dashboard";
    }

    // ================= Quản lý đơn hàng =================

    @GetMapping("/orders")
    public String orders(@RequestParam(required = false) String status,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String paymentMethod,
                          HttpSession session, Model model, RedirectAttributes ra) {
        User admin = requireAdmin(session);
        if (admin == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập bằng tài khoản quản trị");
            return "redirect:/auth?mode=login";
        }

        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();

        if (status != null && !status.isBlank()) {
            orders = orders.stream().filter(o -> status.equals(o.getStatus())).toList();
        }
        if (paymentMethod != null && !paymentMethod.isBlank()) {
            orders = orders.stream().filter(o -> paymentMethod.equalsIgnoreCase(o.getPaymentMethod())).toList();
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase();
            orders = orders.stream().filter(o ->
                    (o.getOrderCode() != null && o.getOrderCode().toLowerCase().contains(kw)) ||
                    (o.getFullName() != null && o.getFullName().toLowerCase().contains(kw)) ||
                    (o.getPhone() != null && o.getPhone().toLowerCase().contains(kw))
            ).toList();
        }

        addCommonAttributes(model, admin);
        model.addAttribute("pageTitle", "Đơn hàng");
        model.addAttribute("orders", orders);
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("paymentMethod", paymentMethod);
        return "admin-orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes ra) {
        User admin = requireAdmin(session);
        if (admin == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập bằng tài khoản quản trị");
            return "redirect:/auth?mode=login";
        }

        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            ra.addFlashAttribute("message", "Không tìm thấy đơn hàng");
            return "redirect:/admin/orders";
        }

        addCommonAttributes(model, admin);
        model.addAttribute("order", order);
        model.addAttribute("statusList", STATUS_ORDER);
        model.addAttribute("statusLabels", STATUS_LABELS);
        return "admin-orders";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                     @RequestParam String newStatus,
                                     @RequestParam(required = false) String trackingCode,
                                     @RequestParam(required = false) String adminNote,
                                     @RequestParam(required = false) String cancelReason,
                                     HttpSession session, RedirectAttributes ra) {
        User admin = requireAdmin(session);
        if (admin == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập bằng tài khoản quản trị");
            return "redirect:/auth?mode=login";
        }

        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            ra.addFlashAttribute("message", "Không tìm thấy đơn hàng");
            return "redirect:/admin/orders";
        }

        if (!STATUS_LABELS.containsKey(newStatus)) {
            ra.addFlashAttribute("message", "Trạng thái không hợp lệ");
            return "redirect:/admin/orders/" + id;
        }

        order.setStatus(newStatus);
        if (trackingCode != null && !trackingCode.isBlank()) {
            order.setTrackingCode(trackingCode.trim());
        }
        if (adminNote != null) {
            order.setAdminNote(adminNote);
        }

        LocalDateTime now = LocalDateTime.now();
        switch (newStatus) {
            case "DA_XAC_NHAN" -> { if (order.getConfirmedAt() == null) order.setConfirmedAt(now); }
            case "DANG_GIAO" -> { if (order.getShippedAt() == null) order.setShippedAt(now); }
            case "HOAN_THANH" -> {
                if (order.getCompletedAt() == null) order.setCompletedAt(now);
                // Đơn COD hoàn thành coi như đã thu tiền
                if ("COD".equals(order.getPaymentMethod()) && !Boolean.TRUE.equals(order.getPaid())) {
                    order.setPaid(true);
                    order.setPaidAt(now);
                }
            }
            case "DA_HUY" -> {
                order.setCancelledAt(now);
                if (cancelReason != null && !cancelReason.isBlank()) {
                    order.setCancelReason(cancelReason.trim());
                }
            }
            default -> { }
        }

        orderRepository.save(order);
        ra.addFlashAttribute("message", "Đã cập nhật trạng thái đơn hàng " + order.getOrderCode());
        return "redirect:/admin/orders/" + id;
    }

    // ================= Quản lý sản phẩm =================

    @GetMapping("/products")
    public String products(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) Long categoryId,
                            HttpSession session, Model model, RedirectAttributes ra) {
        User admin = requireAdmin(session);
        if (admin == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập bằng tài khoản quản trị");
            return "redirect:/auth?mode=login";
        }

        List<Product> products = productRepository.findAll();
        if (categoryId != null) {
            products = products.stream()
                    .filter(p -> p.getCategory() != null && categoryId.equals(p.getCategory().getId()))
                    .toList();
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase();
            products = products.stream()
                    .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(kw))
                    .toList();
        }

        addCommonAttributes(model, admin);
        model.addAttribute("pageTitle", "Sản phẩm");
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        return "admin-products";
    }

    @GetMapping("/products/new")
    public String newProductForm(HttpSession session, Model model, RedirectAttributes ra) {
        User admin = requireAdmin(session);
        if (admin == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập bằng tài khoản quản trị");
            return "redirect:/auth?mode=login";
        }

        addCommonAttributes(model, admin);
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("isEdit", false);
        return "admin-products";
    }

    @PostMapping("/products")
    public String createProduct(@RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 @RequestParam BigDecimal price,
                                 @RequestParam(required = false) BigDecimal oldPrice,
                                 @RequestParam(required = false) Integer discountPercent,
                                 @RequestParam(required = false) String imageUrl,
                                 @RequestParam(required = false) Integer stockQuantity,
                                 @RequestParam(required = false) Long categoryId,
                                 HttpSession session, RedirectAttributes ra) {
        User admin = requireAdmin(session);
        if (admin == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập bằng tài khoản quản trị");
            return "redirect:/auth?mode=login";
        }

        Category category = categoryId != null ? categoryRepository.findById(categoryId).orElse(null) : null;
        Product product = Product.builder()
                .name(name)
                .description(description)
                .price(price)
                .oldPrice(oldPrice)
                .discountPercent(discountPercent)
                .imageUrl(imageUrl)
                .stockQuantity(stockQuantity != null ? stockQuantity : 0)
                .rating(0.0)
                .reviewCount(0)
                .category(category)
                .build();
        productRepository.save(product);

        ra.addFlashAttribute("message", "Đã thêm sản phẩm \"" + name + "\"");
        return "redirect:/admin/products";
    }

    @GetMapping("/products/{id}/edit")
    public String editProductForm(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes ra) {
        User admin = requireAdmin(session);
        if (admin == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập bằng tài khoản quản trị");
            return "redirect:/auth?mode=login";
        }

        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            ra.addFlashAttribute("message", "Không tìm thấy sản phẩm");
            return "redirect:/admin/products";
        }

        addCommonAttributes(model, admin);
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("isEdit", true);
        return "admin-products";
    }

    @PostMapping("/products/{id}")
    public String updateProduct(@PathVariable Long id,
                                 @RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 @RequestParam BigDecimal price,
                                 @RequestParam(required = false) BigDecimal oldPrice,
                                 @RequestParam(required = false) Integer discountPercent,
                                 @RequestParam(required = false) String imageUrl,
                                 @RequestParam(required = false) Integer stockQuantity,
                                 @RequestParam(required = false) Long categoryId,
                                 HttpSession session, RedirectAttributes ra) {
        User admin = requireAdmin(session);
        if (admin == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập bằng tài khoản quản trị");
            return "redirect:/auth?mode=login";
        }

        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            ra.addFlashAttribute("message", "Không tìm thấy sản phẩm");
            return "redirect:/admin/products";
        }

        Category category = categoryId != null ? categoryRepository.findById(categoryId).orElse(null) : null;
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setOldPrice(oldPrice);
        product.setDiscountPercent(discountPercent);
        product.setImageUrl(imageUrl);
        product.setStockQuantity(stockQuantity != null ? stockQuantity : 0);
        product.setCategory(category);
        productRepository.save(product);

        ra.addFlashAttribute("message", "Đã cập nhật sản phẩm \"" + name + "\"");
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        User admin = requireAdmin(session);
        if (admin == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập bằng tài khoản quản trị");
            return "redirect:/auth?mode=login";
        }

        try {
            productRepository.deleteById(id);
            ra.addFlashAttribute("message", "Đã xóa sản phẩm");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("message", "Không thể xóa: sản phẩm này đã xuất hiện trong đơn hàng");
        }
        return "redirect:/admin/products";
    }

    // ================= Báo cáo doanh thu =================

    @GetMapping("/revenue")
    public String revenue(@RequestParam(required = false) String from,
                           @RequestParam(required = false) String to,
                           HttpSession session, Model model, RedirectAttributes ra) {
        User admin = requireAdmin(session);
        if (admin == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập bằng tài khoản quản trị");
            return "redirect:/auth?mode=login";
        }

        LocalDate today = LocalDate.now();
        LocalDate fromDate;
        LocalDate toDate;
        try {
            fromDate = (from != null && !from.isBlank()) ? LocalDate.parse(from) : today.minusDays(29);
        } catch (Exception e) {
            fromDate = today.minusDays(29);
        }
        try {
            toDate = (to != null && !to.isBlank()) ? LocalDate.parse(to) : today;
        } catch (Exception e) {
            toDate = today;
        }
        if (fromDate.isAfter(toDate)) {
            LocalDate tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }

        List<Order> allOrders = orderRepository.findAllByOrderByCreatedAtDesc();
        final LocalDate finalFrom = fromDate;
        final LocalDate finalTo = toDate;

        List<Order> inRange = allOrders.stream()
                .filter(o -> isRevenueCounted(o) && o.getCreatedAt() != null)
                .filter(o -> {
                    LocalDate d = o.getCreatedAt().toLocalDate();
                    return !d.isBefore(finalFrom) && !d.isAfter(finalTo);
                })
                .toList();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Order o : inRange) {
            if (o.getTotalAmount() != null) totalRevenue = totalRevenue.add(o.getTotalAmount());
        }
        long orderCount = inRange.size();
        BigDecimal avgOrderValue = orderCount > 0
                ? totalRevenue.divide(BigDecimal.valueOf(orderCount), 0, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Doanh thu theo từng ngày trong khoảng chọn
        List<String> chartLabels = new ArrayList<>();
        List<BigDecimal> chartValues = new ArrayList<>();
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd/MM");
        long days = java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate);
        for (long i = 0; i <= days; i++) {
            LocalDate day = fromDate.plusDays(i);
            BigDecimal dayTotal = BigDecimal.ZERO;
            for (Order o : inRange) {
                if (o.getCreatedAt().toLocalDate().equals(day) && o.getTotalAmount() != null) {
                    dayTotal = dayTotal.add(o.getTotalAmount());
                }
            }
            chartLabels.add(day.format(dayFmt));
            chartValues.add(dayTotal);
        }

        // Doanh thu theo danh mục
        Map<String, BigDecimal> revenueByCategory = new LinkedHashMap<>();
        // Top sản phẩm theo doanh thu
        Map<Long, String> productNames = new LinkedHashMap<>();
        Map<Long, BigDecimal> productRevenue = new LinkedHashMap<>();
        Map<Long, Integer> productQty = new LinkedHashMap<>();

        for (Order o : inRange) {
            for (OrderItem item : o.getItems()) {
                if (item.getProduct() == null) continue;
                Long pid = item.getProduct().getId();
                productNames.putIfAbsent(pid, item.getProduct().getName());
                productRevenue.merge(pid, item.getSubtotal(), BigDecimal::add);
                productQty.merge(pid, item.getQuantity() == null ? 0 : item.getQuantity(), Integer::sum);

                String catName = item.getProduct().getCategory() != null
                        ? item.getProduct().getCategory().getName() : "Khác";
                revenueByCategory.merge(catName, item.getSubtotal(), BigDecimal::add);
            }
        }

        List<Map<String, Object>> topProducts = new ArrayList<>();
        productRevenue.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .forEach(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", productNames.get(e.getKey()));
                    row.put("revenue", e.getValue());
                    row.put("quantity", productQty.get(e.getKey()));
                    topProducts.add(row);
                });

        List<Map<String, Object>> categoryBreakdown = new ArrayList<>();
        revenueByCategory.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", e.getKey());
                    row.put("revenue", e.getValue());
                    categoryBreakdown.add(row);
                });

        addCommonAttributes(model, admin);
        model.addAttribute("pageTitle", "Doanh thu");
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("todayStr", today.toString());
        model.addAttribute("last7Str", today.minusDays(6).toString());
        model.addAttribute("last30Str", today.minusDays(29).toString());
        model.addAttribute("last90Str", today.minusDays(89).toString());
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("orderCount", orderCount);
        model.addAttribute("avgOrderValue", avgOrderValue);
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartValues", chartValues);
        model.addAttribute("topProducts", topProducts);
        model.addAttribute("categoryBreakdown", categoryBreakdown);

        return "admin-revenue";
    }

    // ================= Quản lý danh mục =================

    @GetMapping("/categories")
    public String categories(HttpSession session, Model model, RedirectAttributes ra) {
        User admin = requireAdmin(session);
        if (admin == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập bằng tài khoản quản trị");
            return "redirect:/auth?mode=login";
        }

        List<Category> categories = categoryRepository.findAll();
        List<Product> allProducts = productRepository.findAll();
        Map<Long, Long> productCount = new HashMap<>();
        for (Product p : allProducts) {
            if (p.getCategory() != null) {
                productCount.merge(p.getCategory().getId(), 1L, Long::sum);
            }
        }

        addCommonAttributes(model, admin);
        model.addAttribute("pageTitle", "Danh mục");
        model.addAttribute("categories", categories);
        model.addAttribute("productCount", productCount);
        model.addAttribute("category", new Category());
        model.addAttribute("isEdit", false);
        return "admin-categories";
    }

    @GetMapping("/categories/{id}/edit")
    public String editCategoryForm(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes ra) {
        User admin = requireAdmin(session);
        if (admin == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập bằng tài khoản quản trị");
            return "redirect:/auth?mode=login";
        }

        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) {
            ra.addFlashAttribute("message", "Không tìm thấy danh mục");
            return "redirect:/admin/categories";
        }

        List<Category> categories = categoryRepository.findAll();
        List<Product> allProducts = productRepository.findAll();
        Map<Long, Long> productCount = new HashMap<>();
        for (Product p : allProducts) {
            if (p.getCategory() != null) {
                productCount.merge(p.getCategory().getId(), 1L, Long::sum);
            }
        }

        addCommonAttributes(model, admin);
        model.addAttribute("categories", categories);
        model.addAttribute("productCount", productCount);
        model.addAttribute("category", category);
        model.addAttribute("isEdit", true);
        return "admin-categories";
    }

    @PostMapping("/categories")
    public String createCategory(@RequestParam String name,
                                  @RequestParam(required = false) String icon,
                                  @RequestParam(required = false) String description,
                                  HttpSession session, RedirectAttributes ra) {
        User admin = requireAdmin(session);
        if (admin == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập bằng tài khoản quản trị");
            return "redirect:/auth?mode=login";
        }

        Category category = Category.builder().name(name).icon(icon).description(description).build();
        categoryRepository.save(category);
        ra.addFlashAttribute("message", "Đã thêm danh mục \"" + name + "\"");
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}")
    public String updateCategory(@PathVariable Long id,
                                  @RequestParam String name,
                                  @RequestParam(required = false) String icon,
                                  @RequestParam(required = false) String description,
                                  HttpSession session, RedirectAttributes ra) {
        User admin = requireAdmin(session);
        if (admin == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập bằng tài khoản quản trị");
            return "redirect:/auth?mode=login";
        }

        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) {
            ra.addFlashAttribute("message", "Không tìm thấy danh mục");
            return "redirect:/admin/categories";
        }

        category.setName(name);
        category.setIcon(icon);
        category.setDescription(description);
        categoryRepository.save(category);
        ra.addFlashAttribute("message", "Đã cập nhật danh mục \"" + name + "\"");
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        User admin = requireAdmin(session);
        if (admin == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập bằng tài khoản quản trị");
            return "redirect:/auth?mode=login";
        }

        try {
            categoryRepository.deleteById(id);
            ra.addFlashAttribute("message", "Đã xóa danh mục");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("message", "Không thể xóa: vẫn còn sản phẩm thuộc danh mục này");
        }
        return "redirect:/admin/categories";
    }

    // ================= Quản lý banner quảng cáo =================

    @GetMapping("/banners")
    public String banners(HttpSession session, Model model, RedirectAttributes ra) {
        User admin = requireAdmin(session);
        if (admin == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập bằng tài khoản quản trị");
            return "redirect:/auth?mode=login";
        }

        addCommonAttributes(model, admin);
        model.addAttribute("pageTitle", "Banner");
        model.addAttribute("banners", bannerRepository.findAllByOrderByIdAsc());
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin-banners";
    }

    @GetMapping("/banners/new")
    public String newBannerForm(HttpSession session, Model model, RedirectAttributes ra) {
        User admin = requireAdmin(session);
        if (admin == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập bằng tài khoản quản trị");
            return "redirect:/auth?mode=login";
        }

        addCommonAttributes(model, admin);
        model.addAttribute("banner", new Banner());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("isEdit", false);
        return "admin-banners";
    }

    @GetMapping("/banners/{id}/edit")
    public String editBannerForm(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes ra) {
        User admin = requireAdmin(session);
        if (admin == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập bằng tài khoản quản trị");
            return "redirect:/auth?mode=login";
        }

        Banner banner = bannerRepository.findById(id).orElse(null);
        if (banner == null) {
            ra.addFlashAttribute("message", "Không tìm thấy banner");
            return "redirect:/admin/banners";
        }

        addCommonAttributes(model, admin);
        model.addAttribute("banner", banner);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("isEdit", true);
        return "admin-banners";
    }

    @PostMapping("/banners")
    public String createBanner(@RequestParam String title,
                                @RequestParam String link,
                                @RequestParam String imageUrl,
                                HttpSession session, RedirectAttributes ra) {
        User admin = requireAdmin(session);
        if (admin == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập bằng tài khoản quản trị");
            return "redirect:/auth?mode=login";
        }

        Banner banner = Banner.builder().title(title).link(link).imageUrl(imageUrl).build();
        bannerRepository.save(banner);
        ra.addFlashAttribute("message", "Đã thêm banner \"" + title + "\"");
        return "redirect:/admin/banners";
    }

    @PostMapping("/banners/{id}")
    public String updateBanner(@PathVariable Long id,
                                @RequestParam String title,
                                @RequestParam String link,
                                @RequestParam String imageUrl,
                                HttpSession session, RedirectAttributes ra) {
        User admin = requireAdmin(session);
        if (admin == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập bằng tài khoản quản trị");
            return "redirect:/auth?mode=login";
        }

        Banner banner = bannerRepository.findById(id).orElse(null);
        if (banner == null) {
            ra.addFlashAttribute("message", "Không tìm thấy banner");
            return "redirect:/admin/banners";
        }

        banner.setTitle(title);
        banner.setLink(link);
        banner.setImageUrl(imageUrl);
        bannerRepository.save(banner);
        ra.addFlashAttribute("message", "Đã cập nhật banner \"" + title + "\"");
        return "redirect:/admin/banners";
    }

    @PostMapping("/banners/{id}/delete")
    public String deleteBanner(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        User admin = requireAdmin(session);
        if (admin == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập bằng tài khoản quản trị");
            return "redirect:/auth?mode=login";
        }

        bannerRepository.deleteById(id);
        ra.addFlashAttribute("message", "Đã xóa banner");
        return "redirect:/admin/banners";
    }
}