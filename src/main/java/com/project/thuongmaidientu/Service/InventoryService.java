package com.project.thuongmaidientu.Service;

import com.project.thuongmaidientu.Model.Product;
import com.project.thuongmaidientu.Repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * === BÀI TOÁN CONCURRENCY ===
 * Trước đây OrderService.placeOrder() không hề trừ stockQuantity, nên nhiều
 * người có thể "mua" cùng 1 sản phẩm cuối cùng vô hạn lần mà tồn kho không
 * bao giờ giảm. Service này vá lỗi đó, đồng thời xử lý đúng race condition:
 * nếu 2 request cùng đọc stock=1, cùng thấy đủ hàng, cùng trừ -> bán vượt
 * kho (oversell). Giải pháp: PESSIMISTIC WRITE LOCK (SELECT ... FOR UPDATE)
 * - request thứ 2 đọc cùng dòng sản phẩm sẽ bị chặn (block) cho tới khi
 * request thứ 1 commit/rollback xong, đảm bảo đọc-kiểm tra-ghi tuần tự.
 */
@Service
public class InventoryService {

    private final ProductRepository productRepository;

    public InventoryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * REQUIRES_NEW: chạy trong transaction riêng, tách khỏi transaction lớn
     * của OrderService.placeOrder(). Nhờ vậy lock trên dòng Product chỉ bị giữ
     * trong thời gian ngắn nhất có thể (chỉ trong lúc trừ kho), không kéo dài
     * theo toàn bộ luồng tạo đơn hàng.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deductStock(Long productId, int quantity) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new IllegalStateException("Sản phẩm không còn tồn tại"));

        int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        if (currentStock < quantity) {
            throw new IllegalStateException(
                    "Sản phẩm \"" + product.getName() + "\" không đủ hàng (còn " + currentStock + ", cần " + quantity + ")");
        }

        product.setStockQuantity(currentStock - quantity);
        productRepository.save(product);
    }

    /**
     * Hoàn kho - dùng khi hủy đơn, hoặc khi 1 sản phẩm khác trong cùng đơn
     * hàng bị hết hàng giữa chừng (compensating transaction, xem OrderService).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restoreStock(Long productId, int quantity) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new IllegalStateException("Sản phẩm không còn tồn tại"));

        int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        product.setStockQuantity(currentStock + quantity);
        productRepository.save(product);
    }
}
