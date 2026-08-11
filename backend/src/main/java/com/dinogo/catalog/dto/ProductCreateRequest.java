package com.dinogo.catalog.dto;

import java.math.BigDecimal;

import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductCreateRequest {

    @NotNull(message = "商品分類不可為空")
    private Integer subcategoryId;

    @NotNull(message = "品牌不可為空")
    private Integer brandId;

    @NotBlank(message = "商品名稱不可為空")
    @Size(max = 50, message = "商品名稱不可超過50字")
    private String productName;

    @Size(max = 3000, message = "商品描述不可超過3000字")
    private String description;

    @NotNull(message = "商品價格不可為空")
    @DecimalMin(value = "0.0", inclusive = false, message = "商品價格必須大於0")
    private BigDecimal basePrice;

}