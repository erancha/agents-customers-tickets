package com.agentscustomerstickets.customers.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.agentscustomerstickets.users.api.User;
import com.agentscustomerstickets.users.api.UserDirectory;
import com.agentscustomerstickets.users.api.Role;
import com.agentscustomerstickets.shared.error.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Unit test: isolate CustomerService behavior without starting Spring or touching a real database.
// Mockito provides a fake UserDirectory so we can control findById(...) results deterministically.
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

  @Mock
  UserDirectory userDirectory;

  @InjectMocks
  CustomerService customerService;

  /**
   * Expects requireCustomer to throw ResourceNotFoundException when the user does not exist (i.e., the user was not created yet).
   */
  @Test
  void requireCustomerThrowsWhenUserMissing() {
    when(userDirectory.findById(123L)).thenReturn(Optional.empty());

    ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> customerService.requireCustomer(123L));
    assertEquals("Customer not found", ex.getMessage());
  }

  /**
   * Expects requireCustomer to throw ResourceNotFoundException when the user exists (i.e., was created) but does not have the CUSTOMER role (e.g., is an agent).
   */
  @Test
  void requireCustomerThrowsWhenUserIsNotCustomerRole() {
    User user = new User(10L, "agent1", Role.AGENT, null, "Agent One", "a1@example.com");

    when(userDirectory.findById(10L)).thenReturn(Optional.of(user));

    ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> customerService.requireCustomer(10L));
    assertEquals("Customer not found", ex.getMessage());
  }

  /**
   * Expects requireCustomer to return the user when the user exists and has the CUSTOMER role.
   */
  @Test
  void requireCustomerReturnsUserWhenRoleIsCustomer() {
    User user = new User(11L, "customer1", Role.CUSTOMER, 1L, "Customer One", "c1@example.com");

    when(userDirectory.findById(11L)).thenReturn(Optional.of(user));

    User out = customerService.requireCustomer(11L);
    assertSame(user, out);
  }
}
