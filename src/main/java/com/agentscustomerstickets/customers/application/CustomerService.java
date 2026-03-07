package com.agentscustomerstickets.customers.application;

import com.agentscustomerstickets.users.api.User;
import com.agentscustomerstickets.users.api.UserDirectory;
import com.agentscustomerstickets.users.api.Role;
import com.agentscustomerstickets.shared.error.ResourceNotFoundException;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

  private final UserDirectory userDirectory;

  CustomerService(UserDirectory userDirectory) {
    this.userDirectory = userDirectory;
  }

  @Transactional(readOnly = true)
  public List<User> listCustomersForAgent(Long agentId) {
    return userDirectory.findAllByAgentId(agentId);
  }

  @Transactional(readOnly = true)
  User requireCustomer(@NonNull Long customerId) {
    User user = userDirectory.findById(customerId)
        .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    if (user.role() != Role.CUSTOMER) {
      throw new ResourceNotFoundException("Customer not found");
    }
    return user;
  }
}
