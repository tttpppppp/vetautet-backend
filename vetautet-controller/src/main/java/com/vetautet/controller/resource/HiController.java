package com.vetautet.controller.resource;


import com.vetautet.application.service.event.EventAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HiController {
    @Autowired
    EventAppService eventAppService;

    @GetMapping("/hello")
    public String hello() {
        return eventAppService.sayHi("Phuc");
    }
}
