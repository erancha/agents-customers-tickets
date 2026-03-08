package com.agentscustomerstickets.users.web;

import com.agentscustomerstickets.users.application.UserProfileService;
import com.agentscustomerstickets.users.api.CurrentUser;
import com.agentscustomerstickets.users.api.CurrentUserProvider;
import com.agentscustomerstickets.users.api.User;
import com.agentscustomerstickets.users.api.Role;
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
  private final UserProfileService userProfileService;

  MeController(CurrentUserProvider currentUserProvider, UserProfileService userProfileService) {
    this.currentUserProvider = currentUserProvider;
    this.userProfileService = userProfileService;
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
    User user = userProfileService.getUser(cu.id());
    return ResponseEntity.ok(MeResponse.from(user));
  }

  @PutMapping
  @PreAuthorize("hasAnyRole('AGENT','CUSTOMER','ADMIN')")
  ResponseEntity<MeResponse> updateMe(@Valid @RequestBody UpdateMeRequest req) {
    CurrentUser cu = currentUserProvider.get();
    User user = userProfileService.updateProfile(cu.id(), req.fullName(), req.email());
    return ResponseEntity.ok(MeResponse.from(user));
  }
}
