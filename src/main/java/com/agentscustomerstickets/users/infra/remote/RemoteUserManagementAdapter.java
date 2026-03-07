package com.agentscustomerstickets.users.infra.remote;

import com.agentscustomerstickets.shared.error.ConflictException;
import com.agentscustomerstickets.shared.error.ResourceNotFoundException;
import com.agentscustomerstickets.users.api.Role;
import com.agentscustomerstickets.users.api.User;
import com.agentscustomerstickets.users.api.UserManagement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Remote implementation of {@link UserManagement} for phase 2.
 * <p>Delegates write-side users operations to the internal users-service REST API.</p>
 * <p>Local execution counterpart: {@code users.infra.UserManagementAdapter}.</p>
 */
@Component
@ConditionalOnProperty(name = "users.integration.mode", havingValue = "remote")
class RemoteUserManagementAdapter implements UserManagement {

   private final RestClient usersInternalRestClient;

   RemoteUserManagementAdapter(RestClient usersInternalRestClient) {
      this.usersInternalRestClient = usersInternalRestClient;
   }

   record CreateUserRequest(
         String username,
         String rawPassword,
         Role role,
         Long agentId,
         String fullName,
         String email) {
   }

   record UpdateProfileRequest(String fullName, String email) {
   }

   @Override
   public User createUser(
         @NonNull String username,
         @NonNull String rawPassword,
         @NonNull Role role,
         Long agentId,
         @NonNull String fullName,
         @NonNull String email) {
      try {
         return usersInternalRestClient.post()
               .uri("/internal/users")
               .body(new CreateUserRequest(username, rawPassword, role, agentId, fullName, email))
               .retrieve()
               .body(User.class);
      } catch (RestClientResponseException ex) {
         throw mapRemoteError(ex);
      }
   }

   @Override
   public User updateProfile(
         @NonNull Long userId,
         @NonNull String fullName,
         @NonNull String email) {
      try {
         return usersInternalRestClient.put()
               .uri("/internal/users/{id}/profile", userId)
               .body(new UpdateProfileRequest(fullName, email))
               .retrieve()
               .body(User.class);
      } catch (RestClientResponseException ex) {
         throw mapRemoteError(ex);
      }
   }

   private RuntimeException mapRemoteError(RestClientResponseException ex) {
      HttpStatusCode statusCode = ex.getStatusCode();
      if (statusCode.value() == 404) {
         return new ResourceNotFoundException("User not found");
      }
      if (statusCode.value() == 409) {
         return new ConflictException("Conflict");
      }
      if (statusCode.value() == 400) {
         return new IllegalArgumentException("Invalid users service request");
      }

      return new IllegalStateException("Users service request failed with status " + statusCode.value(), ex);
   }
}