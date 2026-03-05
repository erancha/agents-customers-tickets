package com.customersupporthub.shared.error;

import java.time.Instant;

record ApiErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path
) {
}
