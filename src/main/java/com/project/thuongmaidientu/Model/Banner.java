package com.project.thuongmaidientu.Model;

import jakarta.persistence.*;

/**
 * Banner quảng cáo hiển thị xoay vòng ở trang chủ.
 * Khi bấm vào banner sẽ điều hướng tới "link" (VD: một danh mục đang giảm giá).
 */
@Entity
@Table(name = "banners")
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    // Đường dẫn khi bấm vào banner, VD: /products?categoryId=3
    @Column(nullable = false, length = 255)
    private String link;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    public Banner() {
    }

    public Banner(Long id, String title, String link, String imageUrl) {
        this.id = id;
        this.title = title;
        this.link = link;
        this.imageUrl = imageUrl;
    }

    public static BannerBuilder builder() { return new BannerBuilder(); }

    public static class BannerBuilder {
        private Long id;
        private String title;
        private String link;
        private String imageUrl;

        public BannerBuilder id(Long id) { this.id = id; return this; }
        public BannerBuilder title(String title) { this.title = title; return this; }
        public BannerBuilder link(String link) { this.link = link; return this; }
        public BannerBuilder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }

        public Banner build() {
            return new Banner(id, title, link, imageUrl);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
