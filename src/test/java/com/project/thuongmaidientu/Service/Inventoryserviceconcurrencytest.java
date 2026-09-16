package com.project.thuongmaidientu.Service;

import com.project.thuongmaidientu.Model.Category;
import com.project.thuongmaidientu.Model.Product;
import com.project.thuongmaidientu.Repository.CategoryRepository;
import com.project.thuongmaidientu.Repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Bài test quan trọng nhất chứng minh cơ chế pessimistic locking trong
 * InventoryService ngăn được oversell.
 *
 * Kịch bản: 1 sản phẩm chỉ còn 5 trong kho, 20 "khách hàng" (thread) cùng lúc
 * cố mua 1 sản phẩm mỗi người. Kỳ vọng: đúng 5 thread thành công, 15 thread
 * còn lại nhận lỗi hết hàng, và tồn kho cuối cùng CHÍNH XÁC bằng 0 - không âm,
 * không dương thừa.
 */
@SpringBootTest
class InventoryServiceConcurrencyTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Long productId;
    private static final int INITIAL_STOCK = 5;
    private static final int NUMBER_OF_CONCURRENT_BUYERS = 20;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.save(
                Category.builder().name("Danh mục test concurrency").build());

        Product product = productRepository.save(Product.builder()
                .name("Sản phẩm giới hạn - test concurrency")
                .price(BigDecimal.valueOf(100_000))
                .stockQuantity(INITIAL_STOCK)
                .category(category)
                .build());

        productId = product.getId();
    }

    @Test
    void trungTuBanNhieuThreadCungLuc_khongDuocBanVuotTonKho() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_CONCURRENT_BUYERS);
        CountDownLatch readyLatch = new CountDownLatch(NUMBER_OF_CONCURRENT_BUYERS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(NUMBER_OF_CONCURRENT_BUYERS);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < NUMBER_OF_CONCURRENT_BUYERS; i++) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await(); // đảm bảo tất cả thread bắn request gần như cùng lúc
                    inventoryService.deductStock(productId, 1);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet(); // hết hàng - đây là hành vi ĐÚNG mong đợi
                } finally {
                    doneLatch.countDown();
                }
                return null;
            });
        }

        readyLatch.await();
        startLatch.countDown(); // phát lệnh xuất phát cho cả 20 thread cùng lúc
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        Product finalProduct = productRepository.findById(productId).orElseThrow();

        assertEquals(INITIAL_STOCK, successCount.get(), "Số đơn thành công phải đúng bằng tồn kho ban đầu");
        assertEquals(NUMBER_OF_CONCURRENT_BUYERS - INITIAL_STOCK, failCount.get(), "Số đơn còn lại phải báo hết hàng");
        assertEquals(0, finalProduct.getStockQuantity(), "Tồn kho cuối cùng phải về đúng 0, không âm không dương thừa");
    }
}