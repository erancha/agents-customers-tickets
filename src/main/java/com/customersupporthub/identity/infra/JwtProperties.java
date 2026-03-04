package com.customersupporthub.identity.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
    String issuer,
    String secret,
    long accessTokenMinutes
) {
}
