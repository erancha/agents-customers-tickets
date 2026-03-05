package com.customersupporthub.ticket.application;

import com.customersupporthub.identity.domain.Role;
import com.customersupporthub.identity.infra.UserEntity;
import com.customersupporthub.identity.infra.UserRepository;
import com.customersupporthub.shared.error.ResourceNotFoundException;
import com.customersupporthub.ticket.infra.TicketEntity;
import com.customersupporthub.ticket.infra.TicketRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

  private final TicketRepository ticketRepository;
  private final UserRepository userRepository;

  TicketService(TicketRepository ticketRepository, UserRepository userRepository) {
    this.ticketRepository = ticketRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public TicketEntity createTicket(Long customerId, String title, String description) {
    UserEntity customer = userRepository.findById(customerId)
        .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

    if (customer.getRole() != Role.CUSTOMER) {
      throw new IllegalArgumentException("Only customers can create tickets");
    }

    if (customer.getAgentId() == null) {
      throw new IllegalArgumentException("Customer is not assigned to an agent");
    }

    TicketEntity t = new TicketEntity();
    t.setCustomerId(customerId);
    t.setAgentId(customer.getAgentId());
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
