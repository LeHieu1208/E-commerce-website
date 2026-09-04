package com.project.thuongmaidientu.Controller;

import com.project.thuongmaidientu.Model.Order;
import com.project.thuongmaidientu.Service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Endpoint nhận webhook báo "có tiền vào" từ dịch vụ trung gian ngân hàng
 * (Casso, SePay, VietQR Pro...). Cấu hình URL này trong dashboard của dịch vụ đó:
 *   POST https://<domain-cua-ban>/api/payment/webhook
 *
 * Hỗ trợ 2 kiểu payload phổ biến:
 *  - SePay: 1 object phẳng {content/description, transferAmount/amount, referenceCode, transferType}
 *  - Casso : { "data": [ { description, amount, tid/referenceCode }, ... ] }
 *
 * Bảo mật: đặt "payment.webhook.secret" trong application.properties rồi cấu hình
 * cùng giá trị đó ở header Authorization hoặc query param "secret" bên phía nhà cung cấp webhook.
 */
@RestController
@RequestMapping("/api/payment")
public class PaymentWebhookController {

    private final OrderService orderService;

    public PaymentWebhookController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody(required = false) Map<String, Object> payload,
                                            @RequestHeader(value = "Authorization", required = false) String authHeader,
                                            @RequestParam(value = "secret", required = false) String secretParam) {

        String providedSecret = secretParam;
        if (providedSecret == null && authHeader != null) {
            providedSecret = authHeader.replaceFirst("(?i)^(Bearer|Apikey)\\s+", "").trim();
        }
        if (!orderService.isWebhookSecretValid(providedSecret)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Sai mã xác thực webhook"));
        }

        if (payload == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Payload rỗng"));
        }

        int matched = 0;

        Object dataField = payload.get("data");
        if (dataField instanceof List<?> list) {
            // Dạng Casso: { "data": [ {...}, {...} ] }
            for (Object item : list) {
                if (item instanceof Map<?, ?> tx) {
                    if (processTransaction(castMap(tx))) matched++;
                }
            }
        } else {
            // Dạng SePay / gói 1 giao dịch phẳng
            if (processTransaction(payload)) matched++;
        }

        return ResponseEntity.ok(Map.of("success", true, "matchedOrders", matched));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object obj) {
        return (Map<String, Object>) obj;
    }

    private boolean processTransaction(Map<String, Object> tx) {
        // Nếu nhà cung cấp có phân biệt tiền vào/ra, chỉ xử lý tiền vào (in / credit)
        Object transferType = firstNonNull(tx.get("transferType"), tx.get("type"));
        if (transferType != null) {
            String t = transferType.toString().toLowerCase();
            if (t.contains("out") || t.contains("debit")) {
                return false;
            }
        }

        String content = stringOf(firstNonNull(tx.get("content"), tx.get("description"), tx.get("addInfo")));
        BigDecimal amount = amountOf(firstNonNull(tx.get("transferAmount"), tx.get("amount")));
        String transactionRef = stringOf(firstNonNull(
                tx.get("referenceCode"), tx.get("transactionID"), tx.get("tid"), tx.get("id")));

        if (content == null || amount == null) {
            return false;
        }

        Optional<Order> result = orderService.confirmTransferPayment(content, amount, transactionRef);
        return result.isPresent();
    }

    private Object firstNonNull(Object... values) {
        for (Object v : values) {
            if (v != null) return v;
        }
        return null;
    }

    private String stringOf(Object v) {
        return v == null ? null : v.toString();
    }

    private BigDecimal amountOf(Object v) {
        if (v == null) return null;
        try {
            if (v instanceof Number number) {
                return BigDecimal.valueOf(number.doubleValue());
            }
            return new BigDecimal(v.toString().replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
