package com.customersupporthub.identity.api;

import org.springframework.lang.NonNull;

import com.customersupporthub.identity.domain.Role;

public record CurrentUser(@NonNull Long id, String username, Role role) {
}
