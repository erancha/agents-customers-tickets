package com.customersupporthub.identity.api;

import com.customersupporthub.identity.domain.Role;

public record CurrentUser(Long id, String username, Role role) {
}
