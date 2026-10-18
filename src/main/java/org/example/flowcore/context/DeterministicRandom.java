package org.example.flowcore.context;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class DeterministicRandom {

    private long state;

    public DeterministicRandom(String workflowId) {
        this.state = hashSeed(workflowId);

        // Avoid the all-zero state.
        if (this.state == 0) {
            this.state = 0x9E3779B97F4A7C15L;
        }
    }

    private static long hashSeed(String value) {
        long hash = 0xcbf29ce484222325L;

        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            hash ^= b & 0xffL;
            hash *= 0x100000001b3L;
        }

        return hash;
    }

    private long nextLong() {
        // SplitMix64-style deterministic generator.
        long z = (state += 0x9E3779B97F4A7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    public UUID nextUUID() {
        long most = nextLong();
        long least = nextLong();

        // Set UUID version 4 and RFC 4122 variant bits.
        most = (most & 0xffffffffffff0fffL)
                | 0x0000000000004000L;
        least = (least & 0x3fffffffffffffffL)
                | 0x8000000000000000L;

        return new UUID(most, least);
    }
}
