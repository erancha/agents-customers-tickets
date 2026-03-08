package com.agentscustomerstickets.shared.error;

import java.time.Instant;

record ApiErrorResponse(
                Instant timestamp,
                int status,
                String message) {
}
