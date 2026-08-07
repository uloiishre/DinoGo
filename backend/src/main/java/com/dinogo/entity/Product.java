package com.dinogo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Product", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "seller_id", nullable = false)
    private Integer sellerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id", nullable = false)
    private Subcategory subcategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(name = "product_name", nullable = false, length = 50)
    private String productName;

    @Column(name = "description", length = 3000)
    private String description;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    // 0 = 草稿
    // 1 = 上架
    // 2 = 下架
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Byte status = (byte) 0;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "sold_count", nullable = false)
    @Builder.Default
    private Integer soldCount = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductSku> skus = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (status == null) {
            status = (byte) 0;
        }

        if (viewCount == null) {
            viewCount = 0;
        }

        if (soldCount == null) {
            soldCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}