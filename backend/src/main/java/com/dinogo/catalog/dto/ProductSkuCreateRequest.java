package com.dinogo.catalog.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSkuCreateRequest {

    private String spec1Name;
    private String spec1Value;

    private String spec2Name;
    private String spec2Value;

    private BigDecimal price;
    private Integer stock;
}