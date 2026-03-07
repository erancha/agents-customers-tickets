package com.agentscustomerstickets.users.infra.remote;

import com.agentscustomerstickets.shared.error.ResourceNotFoundException;
import com.agentscustomerstickets.users.api.Role;
import com.agentscustomerstickets.users.api.User;
import com.agentscustomerstickets.users.api.UserDirectory;
import com.agentscustomerstickets.users.api.UserManagement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal users API exposed for inter-module communication in phase 2.
 * <p>These endpoints are consumed by remote adapters:
 * {@link RemoteUserDirectoryAdapter} and {@link RemoteUserManagementAdapter}.</p>
 * <p>When running in local embedded mode, modules use
 * {@code users.infra.UserDirectoryAdapter} and {@code users.infra.UserManagementAdapter}
 * directly instead of HTTP calls.</p>
 */
@RestController
@RequestMapping("/internal/users")
@Validated
class InternalUsersController {

   private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

   private final UserDirectory userDirectory;
   private final UserManagement userManagement;
   private final String internalApiKey;

   InternalUsersController(
         UserDirectory userDirectory,
         UserManagement userManagement,
         @Value("${users.integration.internal-api-key}") String internalApiKey) {
      this.userDirectory = userDirectory;
      this.userManagement = userManagement;
      this.internalApiKey = internalApiKey;
   }

   record CreateUserRequest(
         @NotBlank @Size(max = 100) String username,
         @NotBlank @Size(max = 200) String rawPassword,
         @NotNull Role role,
         Long agentId,
         @NotBlank @Size(max = 200) String fullName,
         @NotBlank @Email @Size(max = 200) String email) {
   }

   record UpdateProfileRequest(
         @NotBlank @Size(max = 200) String fullName,
         @NotBlank @Email @Size(max = 200) String email) {
   }

   @GetMapping("/{id}")
   User findById(@RequestHeader(name = INTERNAL_API_KEY_HEADER, required = false) String apiKey,
         @PathVariable Long id) {
      authorize(apiKey);
      return userDirectory.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
   }

   @GetMapping("/by-agent/{agentId}")
   List<User> findAllByAgentId(
         @RequestHeader(name = INTERNAL_API_KEY_HEADER, required = false) String apiKey,
         @PathVariable Long agentId) {
      authorize(apiKey);
      return userDirectory.findAllByAgentId(agentId);
   }

   @GetMapping("/by-role/{role}")
   List<User> findAllByRole(
         @RequestHeader(name = INTERNAL_API_KEY_HEADER, required = false) String apiKey,
         @PathVariable Role role) {
      authorize(apiKey);
      return userDirectory.findAllByRole(role);
   }

   @PostMapping
   @ResponseStatus(HttpStatus.CREATED)
   User createUser(@RequestHeader(name = INTERNAL_API_KEY_HEADER, required = false) String apiKey,
         @Valid @RequestBody CreateUserRequest req) {
      authorize(apiKey);
      return userManagement.createUser(
            req.username(),
            req.rawPassword(),
            req.role(),
            req.agentId(),
            req.fullName(),
            req.email());
   }

   @PutMapping("/{id}/profile")
   User updateProfile(
         @RequestHeader(name = INTERNAL_API_KEY_HEADER, required = false) String apiKey,
         @PathVariable Long id,
         @Valid @RequestBody UpdateProfileRequest req) {
      authorize(apiKey);
      return userManagement.updateProfile(id, req.fullName(), req.email());
   }

   private void authorize(String apiKey) {
      if (!Objects.equals(internalApiKey, apiKey)) {
         throw new BadCredentialsException("Unauthorized");
      }
   }

}