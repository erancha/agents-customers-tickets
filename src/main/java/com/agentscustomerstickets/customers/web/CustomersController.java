package com.agentscustomerstickets.customers.web;

import com.agentscustomerstickets.customers.application.CustomerService;
import com.agentscustomerstickets.users.api.CurrentUser;
import com.agentscustomerstickets.users.api.CurrentUserProvider;
import com.agentscustomerstickets.users.api.User;
import com.agentscustomerstickets.users.api.UserDirectory;
import com.agentscustomerstickets.users.api.UserManagement;
import com.agentscustomerstickets.users.api.Role;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@Validated
class CustomersController {

  private final CurrentUserProvider currentUserProvider;
  private final UserDirectory userDirectory;
  private final UserManagement userManagement;
  private final CustomerService customerService;

  CustomersController(CurrentUserProvider currentUserProvider,
      UserDirectory userDirectory,
      UserManagement userManagement,
      CustomerService customerService) {
    this.currentUserProvider = currentUserProvider;
    this.userDirectory = userDirectory;
    this.userManagement = userManagement;
    this.customerService = customerService;
  }

  record CreateCustomerRequest(
      @NotBlank @Size(max = 100) String username,
      @NotBlank @Size(min = 6, max = 200) String password,
      @NotBlank @Size(max = 200) String fullName,
      @NotBlank @Email @Size(max = 200) String email,
      Long agentId) {
  }

  record CustomerResponse(Long id, String username, String fullName, String email, Long agentId) {
    static CustomerResponse from(User user) {
      return new CustomerResponse(user.id(), user.username(), user.fullName(), user.email(), user.agentId());
    }
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
  ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest req) {
    CurrentUser cu = currentUserProvider.get();
    Long agentId;
    if (cu.role() == Role.ADMIN) {
      if (req.agentId() == null) {
        throw new IllegalArgumentException("agentId is required when ADMIN creates a customer");
      }
      agentId = req.agentId();
    } else {
      agentId = cu.id();
    }

    User created = userManagement.createUser(req.username(), req.password(), Role.CUSTOMER, agentId, req.fullName(),
        req.email());
    return ResponseEntity.status(HttpStatus.CREATED).body(CustomerResponse.from(created));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
  ResponseEntity<List<CustomerResponse>> listCustomers(
      @RequestParam(name = "agentId", required = false) Long agentId) {
    CurrentUser cu = currentUserProvider.get();

    Long effectiveAgentId;
    if (cu.role() == Role.ADMIN) {
      if (agentId == null) {
        List<User> customers = userDirectory.findAllByRole(Role.CUSTOMER);
        return ResponseEntity.ok(customers.stream().map(CustomerResponse::from).toList());
      }
      effectiveAgentId = agentId;
    } else {
      effectiveAgentId = cu.id();
    }

    List<User> customers = customerService.listCustomersForAgent(effectiveAgentId);
    return ResponseEntity.ok(customers.stream().map(CustomerResponse::from).toList());
  }
}
