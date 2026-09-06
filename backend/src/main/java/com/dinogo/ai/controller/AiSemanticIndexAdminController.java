package com.dinogo.ai.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.ai.dto.SemanticIndexRebuildResponse;
import com.dinogo.ai.service.ProductSemanticIndexService;

@RestController
@RequestMapping("/api/admin/ai-shopping-advisor/index")
public class AiSemanticIndexAdminController {
    private final ProductSemanticIndexService productSemanticIndexService;
    public AiSemanticIndexAdminController(ProductSemanticIndexService productSemanticIndexService) {
        this.productSemanticIndexService = productSemanticIndexService;
    }
    @PostMapping("/rebuild")
    public SemanticIndexRebuildResponse rebuild() { return productSemanticIndexService.rebuild(); }
}
