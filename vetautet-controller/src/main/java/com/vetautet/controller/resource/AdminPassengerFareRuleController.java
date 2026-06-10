package com.vetautet.controller.resource;

import com.vetautet.application.dto.PassengerFareRuleResponse;
import com.vetautet.application.dto.PassengerFareRuleUpsertRequest;
import com.vetautet.application.service.fare.PassengerFareRuleAppService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/passenger-fare-rules")
public class AdminPassengerFareRuleController {

    private final PassengerFareRuleAppService passengerFareRuleAppService;

    public AdminPassengerFareRuleController(PassengerFareRuleAppService passengerFareRuleAppService) {
        this.passengerFareRuleAppService = passengerFareRuleAppService;
    }

    @GetMapping
    public ResponseEntity<List<PassengerFareRuleResponse>> getAllRules() {
        return ResponseEntity.ok(passengerFareRuleAppService.getAllRules());
    }

    @PutMapping
    public ResponseEntity<List<PassengerFareRuleResponse>> upsertRules(@RequestBody PassengerFareRuleUpsertRequest request) {
        return ResponseEntity.ok(passengerFareRuleAppService.upsertRules(request));
    }
}
