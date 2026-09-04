package com.project.thuongmaidientu.Controller;

import com.project.thuongmaidientu.Model.CartItem;
import com.project.thuongmaidientu.Model.Order;
import com.project.thuongmaidientu.Model.User;
import com.project.thuongmaidientu.Service.CartService;
import com.project.thuongmaidientu.Service.OrderService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;

    public CheckoutController(CartService cartService, OrderService orderService) {
        this.cartService = cartService;
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

    // ---------- Hiển thị trang thanh toán ----------
    @GetMapping
    public String showCheckout(Model model, HttpSession session, RedirectAttributes ra, HttpServletResponse response) {
        // Không cho trình duyệt lưu cache trang này, tránh việc bấm Back/Forward
        // hiển thị lại giỏ hàng cũ (bfcache) sau khi vừa thêm sản phẩm mới.
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        User user = currentUser(session);
        if (user == null) {
            ra.addFlashAttribute("message", "Bạn cần đăng nhập để thanh toán");
            return "redirect:/auth?mode=login";
        }

        List<CartItem> cartItems = cartService.getItems(session);
        if (cartItems.isEmpty()) {
            ra.addFlashAttribute("message", "Giỏ hàng của bạn đang trống");
            return "redirect:/cart";
        }

        BigDecimal subtotal = cartService.getSubtotal(session);

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartSubtotal", subtotal);
        model.addAttribute("cartCount", cartService.getCount(session));

        // Điền sẵn thông tin người nhận nếu user đã có sẵn trong hồ sơ
        model.addAttribute("prefillFullName", user.getFullName());
        model.addAttribute("prefillPhone", user.getPhone());
        model.addAttribute("prefillAddress", user.getAddress());

        return "checkout";
    }

    // ---------- Xử lý đặt hàng ----------
    @PostMapping
    public String placeOrder(@RequestParam String fullName,
                              @RequestParam String phone,
                              @RequestParam String address,
                              @RequestParam(required = false) String note,
                              @RequestParam(required = false) Double lat,
                              @RequestParam(required = false) Double lng,
                              @RequestParam String paymentMethod,
                              HttpSession session,
                              RedirectAttributes ra) {

        User user = currentUser(session);
        if (user == null) {
            ra.addFlashAttribute("message", "Bạn cần đăng nhập để đặt hàng");
            return "redirect:/auth?mode=login";
        }

        try {
            Order order = orderService.placeOrder(session, user, fullName, phone, address, note, lat, lng, paymentMethod);

            if ("TRANSFER".equals(order.getPaymentMethod())) {
                // Chuyển khoản -> sang trang quét mã QR, chờ webhook ngân hàng xác nhận
                return "redirect:/checkout/payment/" + order.getOrderCode();
            }
            // COD -> coi như đặt hàng thành công ngay
            return "redirect:/checkout/success/" + order.getOrderCode();

        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("message", ex.getMessage());
            return "redirect:/cart";
        }
    }

    // ---------- Trang chờ quét QR chuyển khoản ----------
    @GetMapping("/payment/{orderCode}")
    public String paymentPending(@PathVariable String orderCode, Model model, HttpSession session, RedirectAttributes ra) {
        User user = currentUser(session);
        if (user == null) {
            ra.addFlashAttribute("message", "Bạn cần đăng nhập để xem đơn hàng");
            return "redirect:/auth?mode=login";
        }

        Optional<Order> orderOpt = orderService.findByOrderCode(orderCode);
        if (orderOpt.isEmpty() || orderOpt.get().getUser() == null
                || !orderOpt.get().getUser().getId().equals(user.getId())) {
            ra.addFlashAttribute("message", "Không tìm thấy đơn hàng");
            return "redirect:/";
        }

        Order order = orderOpt.get();

        // Đơn không phải chuyển khoản, hoặc đã thanh toán rồi -> chuyển thẳng sang trang thành công
        if (!"TRANSFER".equals(order.getPaymentMethod()) || Boolean.TRUE.equals(order.getPaid())) {
            return "redirect:/checkout/success/" + order.getOrderCode();
        }

        model.addAttribute("order", order);
        model.addAttribute("cartCount", cartService.getCount(session));
        model.addAttribute("qrUrl", orderService.buildVietQrUrl(order));
        model.addAttribute("bankId", orderService.getBankId());
        model.addAttribute("bankAccountNo", orderService.getBankAccountNo());
        model.addAttribute("bankAccountName", orderService.getBankAccountName());

        return "payment-pending";
    }

    // ---------- Trang xác nhận sau khi đặt hàng ----------
    @GetMapping("/success/{orderCode}")
    public String orderSuccess(@PathVariable String orderCode, Model model, HttpSession session, RedirectAttributes ra) {
        User user = currentUser(session);
        if (user == null) {
            ra.addFlashAttribute("message", "Bạn cần đăng nhập để xem đơn hàng");
            return "redirect:/auth?mode=login";
        }

        Optional<Order> orderOpt = orderService.findByOrderCode(orderCode);
        if (orderOpt.isEmpty() || orderOpt.get().getUser() == null
                || !orderOpt.get().getUser().getId().equals(user.getId())) {
            ra.addFlashAttribute("message", "Không tìm thấy đơn hàng");
            return "redirect:/";
        }

        Order order = orderOpt.get();
        model.addAttribute("order", order);
        model.addAttribute("cartCount", cartService.getCount(session));

        return "order-success";
    }
}