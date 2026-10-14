package org.example.flowcore.config;

import java.time.Duration;

public record ActivityOptions(
        Duration startToCloseTimeout,
        Duration heartbeatTimeout,
        RetryPolicy retryPolicy
) {}
