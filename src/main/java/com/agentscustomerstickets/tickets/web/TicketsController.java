package com.agentscustomerstickets.tickets.web;

import com.agentscustomerstickets.users.api.CurrentUser;
import com.agentscustomerstickets.users.api.CurrentUserProvider;
import com.agentscustomerstickets.users.api.Role;
import com.agentscustomerstickets.tickets.application.TicketsService;
import com.agentscustomerstickets.tickets.infra.TicketEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
@Validated
class TicketsController {

  private final CurrentUserProvider currentUserProvider;
  private final TicketsService ticketsService;

  TicketsController(CurrentUserProvider currentUserProvider, TicketsService ticketsService) {
    this.currentUserProvider = currentUserProvider;
    this.ticketsService = ticketsService;
  }

  record CreateTicketRequest(
      @NotBlank @Size(max = 200) String title,
      @NotBlank @Size(max = 4000) String description) {
  }

  record TicketResponse(Long id, Long customerId, Long agentId, String title, String description, String status,
      Instant createdAt) {
    static TicketResponse from(TicketEntity t) {
      return new TicketResponse(t.getId(), t.getCustomerId(), t.getAgentId(), t.getTitle(), t.getDescription(),
          t.getStatus(), t.getCreatedAt());
    }
  }

  @PostMapping
  @PreAuthorize("hasRole('CUSTOMER')")
  ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest req) {
    CurrentUser cu = currentUserProvider.get();
    TicketEntity t = ticketsService.createTicket(cu.id(), req.title(), req.description());
    return ResponseEntity.status(HttpStatus.CREATED).body(TicketResponse.from(t));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('CUSTOMER','AGENT','ADMIN')")
  ResponseEntity<List<TicketResponse>> list(
      @RequestParam(name = "agentId", required = false) Long agentId,
      @RequestParam(name = "customerId", required = false) Long customerId) {
    CurrentUser cu = currentUserProvider.get();
    List<TicketEntity> tickets;

    if (cu.role() == Role.CUSTOMER) {
      // Customers cannot use filtering parameters
      if (agentId != null || customerId != null) {
        throw new AccessDeniedException("Customers cannot filter tickets");
      }
      // Customers only see their own tickets
      tickets = ticketsService.listTicketsForCustomer(cu.id());
    } else if (cu.role() == Role.ADMIN) {
      // Admins can search all tickets
      tickets = ticketsService.listAllTickets(agentId, customerId);
    } else {
      // Agents cannot use agentId parameter (they see tickets for their own agent ID)
      if (agentId != null) {
        throw new AccessDeniedException("Agents cannot filter by agentId");
      }
      // Agents see their assigned tickets
      tickets = ticketsService.listTicketsForAgent(cu.id(), customerId);
    }
    return ResponseEntity.ok(tickets.stream().map(TicketResponse::from).toList());
  }
}
