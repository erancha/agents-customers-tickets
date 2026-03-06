package com.agentscustomerstickets.identity.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
record JwtProperties(
        String issuer,
        String secret,
        long accessTokenMinutes) {
}
