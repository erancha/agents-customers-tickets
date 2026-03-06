package com.agentscustomerstickets.shared.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

   @GetMapping("/")
   public Map<String, String> root() {
      return Map.of("status", "UP", "service", "agents-customers-tickets");
   }

   @GetMapping("/status")
   public Map<String, String> status() {
      return Map.of("status", "UP");
   }

   @GetMapping("/health")
   public Map<String, String> health() {
      return Map.of("status", "UP");
   }
}
