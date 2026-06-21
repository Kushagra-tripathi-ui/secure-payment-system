package com.payment.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HEALTH CONTROLLER - Public endpoint for uptime checks (Render, load balancers, etc).
 * No auth required, returns 200 OK with a tiny JSON body.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public String health() {
        return "{\"status\":\"UP\"}";
    }
}