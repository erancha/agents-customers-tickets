package com.agentscustomerstickets.users.application;

import com.agentscustomerstickets.users.api.User;
import org.springframework.lang.NonNull;

public interface CurrentUserRecordProvider {
  @NonNull
  User getCurrentUserRecord();
}
