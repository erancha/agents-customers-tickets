package com.agentscustomerstickets.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

   @GetMapping({ "/", "/health" })
   public Map<String, String> health(HttpServletRequest request) {
      return Map.of(
            "status", "UP",
            "timestamp", Instant.now().toString(),
            "ipAddress", resolveClientIp(request));
   }

   private String resolveClientIp(HttpServletRequest request) {
      String forwardedFor = request.getHeader("X-Forwarded-For");
      if (forwardedFor != null && !forwardedFor.isBlank()) {
         // Use first IP when multiple proxies append values.
         return normalizeIp(forwardedFor.split(",")[0].trim());
      }

      String realIp = request.getHeader("X-Real-IP");
      if (realIp != null && !realIp.isBlank()) {
         return normalizeIp(realIp);
      }

      return normalizeIp(request.getRemoteAddr());
   }

   private String normalizeIp(String ip) {
      if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
         return "127.0.0.1";
      }

      return ip;
   }
}
