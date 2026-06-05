package com.vetautet.application.service.benchmark;

import com.vetautet.application.dto.BenchmarkOrderAcceptedResponse;
import com.vetautet.application.dto.BenchmarkOrderRequest;
import com.vetautet.application.dto.BenchmarkOrderStatsResponse;

public interface BenchmarkOrderAppService {
    BenchmarkOrderAcceptedResponse enqueueAsyncOrder(BenchmarkOrderRequest request);
    BenchmarkOrderStatsResponse getStats(Long ticketRef);
    void reset(Long ticketRef);
    void prepare(Long ticketRef, long stock);
}
