package com.agentscustomerstickets.users.application;

import com.agentscustomerstickets.shared.error.ResourceNotFoundException;
import com.agentscustomerstickets.users.api.User;
import com.agentscustomerstickets.users.api.UserDirectory;
import com.agentscustomerstickets.users.api.UserManagement;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

   private final UserDirectory userDirectory;
   private final UserManagement userManagement;

   UserProfileService(UserDirectory userDirectory, UserManagement userManagement) {
      this.userDirectory = userDirectory;
      this.userManagement = userManagement;
   }

   public User getUser(@NonNull Long userId) {
      return userDirectory.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
   }

   public User updateProfile(@NonNull Long userId, @NonNull String fullName, @NonNull String email) {
      return userManagement.updateProfile(userId, fullName, email);
   }
}
