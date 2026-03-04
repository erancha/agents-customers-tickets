package com.customersupporthub.customer.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.customersupporthub.identity.domain.Role;
import com.customersupporthub.identity.infra.UserEntity;
import com.customersupporthub.identity.infra.UserRepository;
import com.customersupporthub.shared.error.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Unit test: isolate CustomerService behavior without starting Spring or touching a real database.
// Mockito provides a fake UserRepository so we can control findById(...) results deterministically.
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

  @Mock
  UserRepository userRepository;

  @InjectMocks
  CustomerService customerService;

  @Test
  void requireCustomerThrowsWhenUserMissing() {
    when(userRepository.findById(123L)).thenReturn(Optional.empty());

    ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
        () -> customerService.requireCustomer(123L));
    assertEquals("Customer not found", ex.getMessage());
  }

  @Test
  void requireCustomerThrowsWhenUserIsNotCustomerRole() {
    UserEntity u = new UserEntity();
    u.setId(10L);
    u.setRole(Role.AGENT);

    when(userRepository.findById(10L)).thenReturn(Optional.of(u));

    ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
        () -> customerService.requireCustomer(10L));
    assertEquals("Customer not found", ex.getMessage());
  }

  @Test
  void requireCustomerReturnsUserWhenRoleIsCustomer() {
    UserEntity u = new UserEntity();
    u.setId(11L);
    u.setRole(Role.CUSTOMER);

    when(userRepository.findById(11L)).thenReturn(Optional.of(u));

    UserEntity out = customerService.requireCustomer(11L);
    assertSame(u, out);
  }
}
