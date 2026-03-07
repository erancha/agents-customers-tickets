package com.agentscustomerstickets.users.infra.remote;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Remote users integration client configuration for phase 2.
 * <p>Creates the internal {@link RestClient} only when
 * {@code users.integration.mode=remote}.</p>
 * <p>Local execution path uses in-process adapters instead:
 * {@code users.infra.UserDirectoryAdapter} and {@code users.infra.UserManagementAdapter}.</p>
 */
@Configuration
class RemoteUsersClientConfig {

   /**
    * Builds an internal REST client for users-service calls.
    *
    * @param builder RestClient builder provided by Spring
    * @param baseUrl users-service base URL
    * @param internalApiKey shared internal API key sent as {@code X-Internal-Api-Key}
    * @return configured {@link RestClient} for users-service integration
    */
   @Bean
   @ConditionalOnProperty(name = "users.integration.mode", havingValue = "remote")
   RestClient usersInternalRestClient(
         RestClient.Builder builder,
         @Value("${users.integration.base-url}") String baseUrl,
         @Value("${users.integration.internal-api-key}") String internalApiKey) {
      return builder
            .baseUrl(baseUrl)
            .defaultHeader("X-Internal-Api-Key", internalApiKey)
            .build();
   }
}