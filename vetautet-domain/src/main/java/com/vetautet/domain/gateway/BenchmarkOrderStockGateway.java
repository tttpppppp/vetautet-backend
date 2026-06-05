package com.vetautet.domain.gateway;

public interface BenchmarkOrderStockGateway {
    void prepareStock(Long ticketRef, long stock);
    long getStock(Long ticketRef);
    void clearStock(Long ticketRef);
    int decreaseStockByLua(Long ticketRef, int quantity);
    boolean increaseStock(Long ticketRef, int quantity);
}
