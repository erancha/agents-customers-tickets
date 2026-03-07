package com.agentscustomerstickets.users.web;

import com.agentscustomerstickets.users.api.CurrentUser;
import com.agentscustomerstickets.users.api.CurrentUserProvider;
import com.agentscustomerstickets.users.api.User;
import com.agentscustomerstickets.users.api.UserDirectory;
import com.agentscustomerstickets.users.api.UserManagement;
import com.agentscustomerstickets.users.api.Role;
import com.agentscustomerstickets.shared.error.ResourceNotFoundException;
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
class MeController {

  private final CurrentUserProvider currentUserProvider;
  private final UserDirectory userDirectory;
  private final UserManagement userManagement;

  MeController(CurrentUserProvider currentUserProvider,
      UserDirectory userDirectory,
      UserManagement userManagement) {
    this.currentUserProvider = currentUserProvider;
    this.userDirectory = userDirectory;
    this.userManagement = userManagement;
  }

  record MeResponse(Long id, String username, Role role, String fullName, String email) {
    static MeResponse from(User user) {
      return new MeResponse(user.id(), user.username(), user.role(), user.fullName(), user.email());
    }
  }

  record UpdateMeRequest(
      @NotBlank @Size(max = 200) String fullName,
      @NotBlank @Email @Size(max = 200) String email) {
  }

  @GetMapping
  ResponseEntity<MeResponse> me() {
    CurrentUser cu = currentUserProvider.get();
    User user = userDirectory.findById(cu.id())
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    return ResponseEntity.ok(MeResponse.from(user));
  }

  @PutMapping
  @PreAuthorize("hasAnyRole('AGENT','CUSTOMER','ADMIN')")
  ResponseEntity<MeResponse> updateMe(@Valid @RequestBody UpdateMeRequest req) {
    CurrentUser cu = currentUserProvider.get();
    User user = userManagement.updateProfile(cu.id(), req.fullName(), req.email());
    return ResponseEntity.ok(MeResponse.from(user));
  }
}
