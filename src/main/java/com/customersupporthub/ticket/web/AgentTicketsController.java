package com.customersupporthub.ticket.web;

import com.customersupporthub.identity.api.CurrentUser;
import com.customersupporthub.identity.api.CurrentUserProvider;
import com.customersupporthub.identity.domain.Role;
import com.customersupporthub.ticket.application.TicketService;
import com.customersupporthub.ticket.infra.TicketEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/tickets")
@Validated
class AgentTicketsController {

  private final CurrentUserProvider currentUserProvider;
  private final TicketService ticketService;

  AgentTicketsController(CurrentUserProvider currentUserProvider, TicketService ticketService) {
    this.currentUserProvider = currentUserProvider;
    this.ticketService = ticketService;
  }

  record TicketResponse(Long id, Long customerId, Long agentId, String title, String description, String status,
      Instant createdAt) {
    static TicketResponse from(TicketEntity t) {
      return new TicketResponse(t.getId(), t.getCustomerId(), t.getAgentId(), t.getTitle(), t.getDescription(),
          t.getStatus(), t.getCreatedAt());
    }
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
  ResponseEntity<List<TicketResponse>> search(
      @RequestParam(name = "agentId", required = false) Long agentId,
      @RequestParam(name = "customerId", required = false) Long customerId) {
    CurrentUser cu = currentUserProvider.get();
    List<TicketEntity> tickets;
    if (cu.role() == Role.ADMIN) {
      tickets = ticketService.listAllTickets(agentId, customerId);
    } else {
      tickets = ticketService.listTicketsForAgent(cu.id(), customerId);
    }
    return ResponseEntity.ok(tickets.stream().map(TicketResponse::from).toList());
  }
}
