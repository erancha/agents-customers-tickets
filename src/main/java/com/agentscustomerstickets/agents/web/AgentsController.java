package com.agentscustomerstickets.agents.web;

import com.agentscustomerstickets.agents.application.AgentsService;
import com.agentscustomerstickets.users.api.User;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agents")
@Validated
class AgentsController {

  private final AgentsService agentsService;

  AgentsController(AgentsService agentsService) {
    this.agentsService = agentsService;
  }

  record CreateAgentRequest(
      @NotBlank @Size(max = 100) String username,
      @NotBlank @Size(min = 6, max = 200) String password,
      @NotBlank @Size(max = 200) String fullName,
      @NotBlank @Email @Size(max = 200) String email) {
  }

  record AgentResponse(Long id, String username, Role role, String fullName, String email) {
    static AgentResponse from(User user) {
      return new AgentResponse(user.id(), user.username(), user.role(), user.fullName(), user.email());
    }
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  ResponseEntity<AgentResponse> createAgent(@Valid @RequestBody CreateAgentRequest req) {
    User created = agentsService.createAgent(req.username(), req.password(), req.fullName(), req.email());
    return ResponseEntity.status(HttpStatus.CREATED).body(AgentResponse.from(created));
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  ResponseEntity<List<AgentResponse>> listAgents() {
    List<User> agents = agentsService.listAgents();
    return ResponseEntity.ok(agents.stream().map(AgentResponse::from).toList());
  }
}
