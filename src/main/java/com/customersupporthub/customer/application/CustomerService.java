package com.customersupporthub.customer.application;

import com.customersupporthub.identity.domain.Role;
import com.customersupporthub.identity.infra.UserEntity;
import com.customersupporthub.identity.infra.UserRepository;
import com.customersupporthub.shared.error.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

  private final UserRepository userRepository;

  public CustomerService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<UserEntity> listCustomersForAgent(Long agentId) {
    return userRepository.findAllByAgentId(agentId);
  }

  @Transactional(readOnly = true)
  public UserEntity requireCustomer(Long customerId) {
    UserEntity u = userRepository.findById(customerId)
        .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    if (u.getRole() != Role.CUSTOMER) {
      throw new ResourceNotFoundException("Customer not found");
    }
    return u;
  }
}
