package com.vetautet.controller.resource;

import com.vetautet.application.dto.BenchmarkOrderAcceptedResponse;
import com.vetautet.application.dto.BenchmarkOrderRequest;
import com.vetautet.application.dto.BenchmarkOrderStatsResponse;
import com.vetautet.application.service.benchmark.BenchmarkOrderAppService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/benchmark")
public class BenchmarkController {

    private final BenchmarkOrderAppService benchmarkOrderAppService;

    public BenchmarkController(BenchmarkOrderAppService benchmarkOrderAppService) {
        this.benchmarkOrderAppService = benchmarkOrderAppService;
    }

    @GetMapping("/plain")
    public String plain() {
        return "ok";
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of(
                "status", "ok",
                "timestamp", Instant.now().toString()
        );
    }

    @PostMapping("/test-orders/async")
    public ResponseEntity<BenchmarkOrderAcceptedResponse> enqueueAsyncOrder(@RequestBody(required = false) BenchmarkOrderRequest request) {
        BenchmarkOrderRequest safeRequest = request != null ? request : new BenchmarkOrderRequest();
        return ResponseEntity.ok()
                .body(benchmarkOrderAppService.enqueueAsyncOrder(safeRequest));
    }

    @PostMapping("/test-orders/prepare")
    public ResponseEntity<Map<String, Object>> prepareTestOrders(@RequestParam("ticketRef") Long ticketRef,
                                                                 @RequestParam("stock") long stock) {
        benchmarkOrderAppService.prepare(ticketRef, stock);
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "ticketRef", ticketRef,
                "stock", stock
        ));
    }

    @GetMapping("/test-orders/stats")
    public ResponseEntity<BenchmarkOrderStatsResponse> getTestOrderStats(@RequestParam(value = "ticketRef", required = false) Long ticketRef) {
        return ResponseEntity.ok(benchmarkOrderAppService.getStats(ticketRef));
    }

    @DeleteMapping("/test-orders/reset")
    public ResponseEntity<Map<String, Object>> resetTestOrders(@RequestParam(value = "ticketRef", required = false) Long ticketRef) {
        benchmarkOrderAppService.reset(ticketRef);
        return ResponseEntity.ok(Map.of("status", "ok", "ticketRef", ticketRef));
    }
}
