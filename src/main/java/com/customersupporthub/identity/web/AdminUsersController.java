package com.customersupporthub.identity.web;

import com.customersupporthub.identity.application.IdentityService;
import com.customersupporthub.identity.domain.Role;
import com.customersupporthub.identity.infra.UserEntity;
import com.customersupporthub.identity.infra.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminUsersController {

  private final IdentityService identityService;
  private final UserRepository userRepository;

  public AdminUsersController(IdentityService identityService, UserRepository userRepository) {
    this.identityService = identityService;
    this.userRepository = userRepository;
  }

  public record CreateAgentRequest(
      @NotBlank @Size(max = 100) String username,
      @NotBlank @Size(min = 6, max = 200) String password,
      @NotBlank @Size(max = 200) String fullName,
      @NotBlank @Email @Size(max = 200) String email) {
  }

  public record UserResponse(Long id, String username, Role role, String fullName, String email) {
    public static UserResponse from(UserEntity u) {
      return new UserResponse(u.getId(), u.getUsername(), u.getRole(), u.getFullName(), u.getEmail());
    }
  }

  @GetMapping("/agents")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<UserResponse>> listAgents() {
    List<UserEntity> agents = userRepository.findAllByRole(Role.AGENT);
    return ResponseEntity.ok(agents.stream().map(UserResponse::from).toList());
  }

  @PostMapping("/agents")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserResponse> createAgent(@Valid @RequestBody CreateAgentRequest req) {
    UserEntity created = identityService.createUser(req.username(), req.password(), Role.AGENT, null, req.fullName(),
        req.email());
    return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(created));
  }
}
