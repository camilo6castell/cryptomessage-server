package com.cryptomessage.server.config.errors;

import java.time.Instant;

public record ApiError(String error, String message, Instant timestamp) {}