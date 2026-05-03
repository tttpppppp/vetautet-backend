package com.vetautet.controller.resource;

import com.vetautet.application.dto.PromotionResponse;
import com.vetautet.application.dto.PromotionValidationResponse;
import com.vetautet.application.service.promotion.PromotionAppService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/promotions")
public class PromotionController {

    private final PromotionAppService promotionAppService;

    public PromotionController(PromotionAppService promotionAppService) {
        this.promotionAppService = promotionAppService;
    }

    @GetMapping
    public ResponseEntity<List<PromotionResponse>> getPromotions(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "discount", required = false) String minDiscount,
            @RequestParam(value = "type", required = false) String discountType,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "route", required = false) String route,
            @RequestParam(value = "sort", defaultValue = "newest") String sort) {
        return ResponseEntity.ok(promotionAppService.getPromotions(
                query,
                minDiscount,
                discountType,
                category,
                status,
                route,
                sort
        ));
    }

    @GetMapping("/validate-code")
    public ResponseEntity<PromotionValidationResponse> validatePromotion(
            @RequestParam("code") String code,
            @RequestParam(value = "orderAmount", required = false) BigDecimal orderAmount) {
        return ResponseEntity.ok(promotionAppService.validatePromotion(code, orderAmount));
    }

    @GetMapping("/{code}")
    public ResponseEntity<PromotionResponse> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(promotionAppService.getPromotionByCode(code));
    }
}
