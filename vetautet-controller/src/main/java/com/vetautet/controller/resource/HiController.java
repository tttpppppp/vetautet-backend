package com.vetautet.controller.resource;


import com.vetautet.application.service.event.EventAppService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HiController {
    @Autowired
    EventAppService eventAppService;

    @GetMapping("/hello")
    @CircuitBreaker(name = "helloCircuit")
    @RateLimiter(name = "helloTest")
    public String hello(@RequestParam(value = "fail", defaultValue = "false") boolean fail) {
        if (fail) {
            throw new RuntimeException("HELLO_CIRCUIT_TEST_FAILURE");
        }
        return eventAppService.sayHi("Phuc");
    }
}
