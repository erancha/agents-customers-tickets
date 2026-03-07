package com.agentscustomerstickets.users.api;

import java.util.List;
import java.util.Optional;
import org.springframework.lang.NonNull;

/**
 * Read-side public contract for querying users from the users module.
 * <p>Other modules should depend on this interface instead of users internals such as repositories or entities.</p>
 */
public interface UserDirectory {

   /**
    * Finds a user by id.
    */
   Optional<User> findById(@NonNull Long id);

   /**
    * Lists all users assigned to a specific agent.
    */
   List<User> findAllByAgentId(@NonNull Long agentId);

   /**
    * Lists all users with a given role.
    */
   List<User> findAllByRole(@NonNull Role role);
}
