package com.agentscustomerstickets.agents.application;

import com.agentscustomerstickets.users.api.Role;
import com.agentscustomerstickets.users.api.User;
import com.agentscustomerstickets.users.api.UserDirectory;
import com.agentscustomerstickets.users.api.UserManagement;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentsService {

   private final UserDirectory userDirectory;
   private final UserManagement userManagement;

   AgentsService(UserDirectory userDirectory, UserManagement userManagement) {
      this.userDirectory = userDirectory;
      this.userManagement = userManagement;
   }

   public User createAgent(String username, String password, String fullName, String email) {
      return userManagement.createUser(username, password, Role.AGENT, null, fullName, email);
   }

   public List<User> listAgents() {
      return userDirectory.findAllByRole(Role.AGENT);
   }
}