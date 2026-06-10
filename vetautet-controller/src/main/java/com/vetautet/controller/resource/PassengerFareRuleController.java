package com.vetautet.controller.resource;

import com.vetautet.application.dto.PassengerFareRuleResponse;
import com.vetautet.application.service.fare.PassengerFareRuleAppService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/passenger-fare-rules")
public class PassengerFareRuleController {

    private final PassengerFareRuleAppService passengerFareRuleAppService;

    public PassengerFareRuleController(PassengerFareRuleAppService passengerFareRuleAppService) {
        this.passengerFareRuleAppService = passengerFareRuleAppService;
    }

    @GetMapping
    public ResponseEntity<List<PassengerFareRuleResponse>> getActiveRules() {
        return ResponseEntity.ok(passengerFareRuleAppService.getActiveRules());
    }
}
