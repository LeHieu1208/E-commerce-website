package com.project.thuongmaidientu.Model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, unique = true, length = 20)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // ---------- Thông tin người nhận ----------
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(columnDefinition = "TEXT")
    private String note; // ghi chú của khách

    private Double lat;
    private Double lng;

    // ---------- Thanh toán ----------
    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod; // COD | TRANSFER

    @Column(nullable = false)
    private Boolean paid = false;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // Mã giao dịch ngân hàng do webhook (Casso/SePay...) trả về, dùng để chống xử lý trùng
    @Column(name = "transaction_ref", length = 100, unique = true)
    private String transactionRef;

    // ---------- Tiền ----------
    @Column(name = "items_subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal itemsSubtotal;

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    // ---------- Vòng đời đơn hàng (dành cho trang admin sau này) ----------
    @Column(nullable = false, length = 30)
    private String status; // CHO_XAC_NHAN, CHO_THANH_TOAN, DA_THANH_TOAN, DA_XAC_NHAN, DANG_GIAO, HOAN_THANH, DA_HUY

    @Column(name = "tracking_code", length = 100)
    private String trackingCode; // mã vận đơn, admin nhập sau

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote; // ghi chú nội bộ, không hiển thị cho khách

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
    }

    public static OrderBuilder builder() {
        return new OrderBuilder();
    }

    public static class OrderBuilder {
        private final Order o = new Order();

        public OrderBuilder id(Long id) { o.id = id; return this; }
        public OrderBuilder orderCode(String v) { o.orderCode = v; return this; }
        public OrderBuilder user(User v) { o.user = v; return this; }
        public OrderBuilder fullName(String v) { o.fullName = v; return this; }
        public OrderBuilder phone(String v) { o.phone = v; return this; }
        public OrderBuilder address(String v) { o.address = v; return this; }
        public OrderBuilder note(String v) { o.note = v; return this; }
        public OrderBuilder lat(Double v) { o.lat = v; return this; }
        public OrderBuilder lng(Double v) { o.lng = v; return this; }
        public OrderBuilder paymentMethod(String v) { o.paymentMethod = v; return this; }
        public OrderBuilder paid(Boolean v) { o.paid = v; return this; }
        public OrderBuilder itemsSubtotal(BigDecimal v) { o.itemsSubtotal = v; return this; }
        public OrderBuilder shippingFee(BigDecimal v) { o.shippingFee = v; return this; }
        public OrderBuilder discountAmount(BigDecimal v) { o.discountAmount = v; return this; }
        public OrderBuilder totalAmount(BigDecimal v) { o.totalAmount = v; return this; }
        public OrderBuilder status(String v) { o.status = v; return this; }
        public OrderBuilder items(List<OrderItem> v) { o.items = v == null ? new ArrayList<>() : v; return this; }

        public Order build() { return o; }
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) {
            this.status = "CHO_XAC_NHAN";
        }
        if (this.paid == null) {
            this.paid = false;
        }
        if (this.shippingFee == null) this.shippingFee = BigDecimal.ZERO;
        if (this.discountAmount == null) this.discountAmount = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public Boolean getPaid() { return paid; }
    public void setPaid(Boolean paid) { this.paid = paid; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
    public BigDecimal getItemsSubtotal() { return itemsSubtotal; }
    public void setItemsSubtotal(BigDecimal itemsSubtotal) { this.itemsSubtotal = itemsSubtotal; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }
    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items == null ? new ArrayList<>() : items; }
}
