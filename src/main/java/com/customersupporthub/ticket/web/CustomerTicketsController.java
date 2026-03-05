package com.customersupporthub.ticket.web;

import com.customersupporthub.identity.api.CurrentUser;
import com.customersupporthub.identity.api.CurrentUserProvider;
import com.customersupporthub.ticket.application.TicketService;
import com.customersupporthub.ticket.infra.TicketEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
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
@RequestMapping("/api/customer/tickets")
@Validated
class CustomerTicketsController {

  private final CurrentUserProvider currentUserProvider;
  private final TicketService ticketService;

  CustomerTicketsController(CurrentUserProvider currentUserProvider, TicketService ticketService) {
    this.currentUserProvider = currentUserProvider;
    this.ticketService = ticketService;
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
    TicketEntity t = ticketService.createTicket(cu.id(), req.title(), req.description());
    return ResponseEntity.status(HttpStatus.CREATED).body(TicketResponse.from(t));
  }

  @GetMapping
  @PreAuthorize("hasRole('CUSTOMER')")
  ResponseEntity<List<TicketResponse>> myTickets() {
    CurrentUser cu = currentUserProvider.get();
    List<TicketEntity> tickets = ticketService.listTicketsForCustomer(cu.id());
    return ResponseEntity.ok(tickets.stream().map(TicketResponse::from).toList());
  }
}
