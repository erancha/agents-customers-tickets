package com.agentscustomerstickets.shared.error;

/**
 * Signals authentication failure due to invalid username/password.
 */
public class InvalidCredentialsException extends RuntimeException {

   public InvalidCredentialsException(String message) {
      super(message);
   }
}
