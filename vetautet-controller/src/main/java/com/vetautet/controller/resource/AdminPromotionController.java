package com.vetautet.controller.resource;

import com.vetautet.application.dto.PromotionRequest;
import com.vetautet.application.dto.PromotionResponse;
import com.vetautet.application.service.promotion.PromotionAppService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/promotions")
public class AdminPromotionController {

    private final PromotionAppService promotionAppService;

    public AdminPromotionController(PromotionAppService promotionAppService) {
        this.promotionAppService = promotionAppService;
    }

    @GetMapping
    public ResponseEntity<List<PromotionResponse>> getAll(
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

    @PostMapping
    public ResponseEntity<PromotionResponse> create(@RequestBody PromotionRequest request) {
        return ResponseEntity.ok(promotionAppService.createPromotion(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromotionResponse> update(@PathVariable Long id, @RequestBody PromotionRequest request) {
        return ResponseEntity.ok(promotionAppService.updatePromotion(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        promotionAppService.deletePromotion(id);
        return ResponseEntity.noContent().build();
    }
}
