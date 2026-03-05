package com.customersupporthub.customer.web;

import com.customersupporthub.customer.application.CustomerService;
import com.customersupporthub.identity.api.CurrentUser;
import com.customersupporthub.identity.api.CurrentUserProvider;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/customers")
@Validated
class AgentCustomersController {

  private final CurrentUserProvider currentUserProvider;
  private final IdentityService identityService;
  private final CustomerService customerService;
  private final UserRepository userRepository;

  AgentCustomersController(CurrentUserProvider currentUserProvider, IdentityService identityService,
      CustomerService customerService, UserRepository userRepository) {
    this.currentUserProvider = currentUserProvider;
    this.identityService = identityService;
    this.customerService = customerService;
    this.userRepository = userRepository;
  }

  record CreateCustomerRequest(
      @NotBlank @Size(max = 100) String username,
      @NotBlank @Size(min = 6, max = 200) String password,
      @NotBlank @Size(max = 200) String fullName,
      @NotBlank @Email @Size(max = 200) String email,
      Long agentId) {
  }

  record CustomerResponse(Long id, String username, String fullName, String email, Long agentId) {
    static CustomerResponse from(UserEntity u) {
      return new CustomerResponse(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getAgentId());
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

    UserEntity created = identityService.createUser(req.username(), req.password(), Role.CUSTOMER, agentId,
        req.fullName(), req.email());
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
        List<UserEntity> customers = userRepository.findAllByRole(Role.CUSTOMER);
        return ResponseEntity.ok(customers.stream().map(CustomerResponse::from).toList());
      }
      effectiveAgentId = agentId;
    } else {
      effectiveAgentId = cu.id();
    }

    List<UserEntity> customers = customerService.listCustomersForAgent(effectiveAgentId);
    return ResponseEntity.ok(customers.stream().map(CustomerResponse::from).toList());
  }
}
