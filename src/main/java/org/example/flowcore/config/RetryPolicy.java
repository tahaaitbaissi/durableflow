package org.example.flowcore.config;

import java.time.Duration;
import java.util.Set;

public record RetryPolicy(
        int maxAttempts,
        Duration initialInterval,
        double backoffCoefficient,
        Duration maxInterval,
        Set<String> nonRetryableExceptionTypes
) {
    public Duration nextDelay(int attempt) {
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be >= 1");
        }

        double delayMillis = initialInterval.toMillis()
                * Math.pow(backoffCoefficient, attempt - 1);

        long cappedMillis = Math.min(
                (long) delayMillis,
                maxInterval.toMillis()
        );

        return Duration.ofMillis(cappedMillis);
    }
}
