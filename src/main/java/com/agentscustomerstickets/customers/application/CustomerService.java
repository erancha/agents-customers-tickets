package com.agentscustomerstickets.customers.application;

import com.agentscustomerstickets.users.api.User;
import com.agentscustomerstickets.users.api.UserDirectory;
import com.agentscustomerstickets.users.api.UserManagement;
import com.agentscustomerstickets.users.api.Role;
import com.agentscustomerstickets.shared.error.ResourceNotFoundException;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

  private final UserDirectory userDirectory;
  private final UserManagement userManagement;

  CustomerService(UserDirectory userDirectory, UserManagement userManagement) {
    this.userDirectory = userDirectory;
    this.userManagement = userManagement;
  }

  public User createCustomer(String username, String password, Long agentId, String fullName, String email) {
    return userManagement.createUser(username, password, Role.CUSTOMER, agentId, fullName, email);
  }

  public List<User> listAllCustomers() {
    return userDirectory.findAllByRole(Role.CUSTOMER);
  }

  public List<User> listCustomersForAgent(Long agentId) {
    return userDirectory.findAllByAgentId(agentId);
  }

  User requireCustomer(@NonNull Long customerId) {
    User user = userDirectory.findById(customerId)
        .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    if (user.role() != Role.CUSTOMER) {
      throw new ResourceNotFoundException("Customer not found");
    }
    return user;
  }
}
