package com.customersupporthub.identity.web;

import com.customersupporthub.identity.api.CurrentUser;
import com.customersupporthub.identity.api.CurrentUserProvider;
import com.customersupporthub.identity.application.IdentityService;
import com.customersupporthub.identity.domain.Role;
import com.customersupporthub.identity.infra.UserEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@Validated
public class MeController {

  private final CurrentUserProvider currentUserProvider;
  private final IdentityService identityService;

  public MeController(CurrentUserProvider currentUserProvider, IdentityService identityService) {
    this.currentUserProvider = currentUserProvider;
    this.identityService = identityService;
  }

  public record MeResponse(Long id, String username, Role role, String fullName, String email) {
    public static MeResponse from(UserEntity u) {
      return new MeResponse(u.getId(), u.getUsername(), u.getRole(), u.getFullName(), u.getEmail());
    }
  }

  public record UpdateMeRequest(
      @NotBlank @Size(max = 200) String fullName,
      @NotBlank @Email @Size(max = 200) String email
  ) {
  }

  @GetMapping
  public ResponseEntity<MeResponse> me() {
    CurrentUser cu = currentUserProvider.get();
    UserEntity u = identityService.requireUser(cu.id());
    return ResponseEntity.ok(MeResponse.from(u));
  }

  @PutMapping
  @PreAuthorize("hasAnyRole('AGENT','CUSTOMER','ADMIN')")
  public ResponseEntity<MeResponse> updateMe(@Valid @RequestBody UpdateMeRequest req) {
    CurrentUser cu = currentUserProvider.get();
    UserEntity u = identityService.updateProfile(cu.id(), req.fullName(), req.email());
    return ResponseEntity.ok(MeResponse.from(u));
  }
}
