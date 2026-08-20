package com.dinogo.sales.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.sales.dto.payment.PaymentCapabilityResponse;

@RestController
@RequestMapping("/api/payments")
public class PaymentCapabilityController {

    private final boolean simulationEnabled;

    public PaymentCapabilityController(
            @Value("${app.payment.simulation-enabled:false}") boolean simulationEnabled) {
        this.simulationEnabled = simulationEnabled;
    }

    @GetMapping("/capabilities")
    public PaymentCapabilityResponse getCapabilities() {
        return new PaymentCapabilityResponse(simulationEnabled);
    }
}
