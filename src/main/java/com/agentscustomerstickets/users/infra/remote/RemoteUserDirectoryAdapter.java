package com.agentscustomerstickets.users.infra.remote;

import com.agentscustomerstickets.shared.error.ResourceNotFoundException;
import com.agentscustomerstickets.users.api.Role;
import com.agentscustomerstickets.users.api.User;
import com.agentscustomerstickets.users.api.UserDirectory;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Remote implementation of {@link UserDirectory} for phase 2.
 * <p>Delegates read-side users operations to the internal users-service REST API.</p>
 * <p>Local execution counterpart: {@code users.infra.UserDirectoryAdapter}.</p>
 */
@Component
@ConditionalOnProperty(name = "users.integration.mode", havingValue = "remote")
class RemoteUserDirectoryAdapter implements UserDirectory {

   private static final ParameterizedTypeReference<List<User>> USER_LIST_TYPE = new ParameterizedTypeReference<>() {
   };

   private final RestClient usersInternalRestClient;

   RemoteUserDirectoryAdapter(RestClient usersInternalRestClient) {
      this.usersInternalRestClient = usersInternalRestClient;
   }

   @Override
   public Optional<User> findById(@NonNull Long id) {
      try {
         User user = usersInternalRestClient.get()
               .uri("/internal/users/{id}", id)
               .retrieve()
               .body(User.class);
         return Optional.ofNullable(user);
      } catch (HttpClientErrorException.NotFound ex) {
         return Optional.empty();
      } catch (RestClientResponseException ex) {
         throw mapRemoteError(ex);
      }
   }

   @Override
   public List<User> findAllByAgentId(@NonNull Long agentId) {
      try {
         List<User> users = usersInternalRestClient.get()
               .uri("/internal/users/by-agent/{agentId}", agentId)
               .retrieve()
               .body(USER_LIST_TYPE);
         return users == null ? List.of() : users;
      } catch (RestClientResponseException ex) {
         throw mapRemoteError(ex);
      }
   }

   @Override
   public List<User> findAllByRole(@NonNull Role role) {
      try {
         List<User> users = usersInternalRestClient.get()
               .uri("/internal/users/by-role/{role}", role.name())
               .retrieve()
               .body(USER_LIST_TYPE);
         return users == null ? List.of() : users;
      } catch (RestClientResponseException ex) {
         throw mapRemoteError(ex);
      }
   }

   private RuntimeException mapRemoteError(RestClientResponseException ex) {
      HttpStatusCode statusCode = ex.getStatusCode();
      if (statusCode.value() == 404) {
         return new ResourceNotFoundException("User not found");
      }

      return new IllegalStateException("Users service request failed with status " + statusCode.value(), ex);
   }
}