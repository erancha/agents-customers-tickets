package com.agentscustomerstickets.tickets.application;

import com.agentscustomerstickets.users.api.User;
import com.agentscustomerstickets.users.api.UserDirectory;
import com.agentscustomerstickets.users.api.Role;
import com.agentscustomerstickets.shared.error.ResourceNotFoundException;
import com.agentscustomerstickets.tickets.infra.TicketEntity;
import com.agentscustomerstickets.tickets.infra.TicketRepository;
import java.time.Instant;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketsService {

  private final TicketRepository ticketRepository;
  private final UserDirectory userDirectory;

  TicketsService(TicketRepository ticketRepository, UserDirectory userDirectory) {
    this.ticketRepository = ticketRepository;
    this.userDirectory = userDirectory;
  }

  @Transactional
  public TicketEntity createTicket(@NonNull Long customerId, String title, String description) {
    User customer = userDirectory.findById(customerId)
        .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

    if (customer.role() != Role.CUSTOMER) {
      throw new IllegalArgumentException("Only customers can create tickets");
    }

    if (customer.agentId() == null) {
      throw new IllegalArgumentException("Customer is not assigned to an agent");
    }

    TicketEntity t = new TicketEntity();
    t.setCustomerId(customerId);
    t.setAgentId(customer.agentId());
    t.setTitle(title);
    t.setDescription(description);
    t.setStatus("OPEN");
    t.setCreatedAt(Instant.now());
    return ticketRepository.save(t);
  }

  @Transactional(readOnly = true)
  public List<TicketEntity> listTicketsForCustomer(Long customerId) {
    return ticketRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId);
  }

  @Transactional(readOnly = true)
  public List<TicketEntity> listTicketsForAgent(Long agentId, Long customerIdFilter) {
    if (customerIdFilter == null) {
      return ticketRepository.findAllByAgentIdOrderByCreatedAtDesc(agentId);
    }
    return ticketRepository.findAllByAgentIdAndCustomerIdOrderByCreatedAtDesc(agentId, customerIdFilter);
  }

  @Transactional(readOnly = true)
  public List<TicketEntity> listAllTickets(Long agentIdFilter, Long customerIdFilter) {
    if (agentIdFilter == null && customerIdFilter == null) {
      return ticketRepository.findAllByOrderByCreatedAtDesc();
    }
    if (agentIdFilter != null && customerIdFilter == null) {
      return ticketRepository.findAllByAgentIdOrderByCreatedAtDesc(agentIdFilter);
    }
    if (agentIdFilter != null) {
      return ticketRepository.findAllByAgentIdAndCustomerIdOrderByCreatedAtDesc(agentIdFilter, customerIdFilter);
    }
    throw new IllegalArgumentException("Filtering by customerId without agentId is not supported");
  }
}
