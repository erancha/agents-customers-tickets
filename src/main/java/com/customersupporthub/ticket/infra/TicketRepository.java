package com.customersupporthub.ticket.infra;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<TicketEntity, Long> {
  List<TicketEntity> findAllByCustomerIdOrderByCreatedAtDesc(Long customerId);

  List<TicketEntity> findAllByAgentIdOrderByCreatedAtDesc(Long agentId);

  List<TicketEntity> findAllByAgentIdAndCustomerIdOrderByCreatedAtDesc(Long agentId, Long customerId);

  List<TicketEntity> findAllByOrderByCreatedAtDesc();
}
