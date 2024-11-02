package net.kremianskii.hubby;

import java.time.Duration;

import static java.lang.Math.min;
import static java.lang.Math.pow;

public sealed interface Backoff permits Backoff.Exponential {
    final class Exponential implements Backoff {
        private final double baseMillis;
        private final double factorMillis;
        private final double maxMillis;

        public Exponential() {
            this(1000, 3, 10000);
        }

        public Exponential(double baseMillis, double factorMillis, double maxMillis) {
            this.baseMillis = baseMillis;
            this.factorMillis = factorMillis;
            this.maxMillis = maxMillis;
        }

        public Duration delay(int attempt) {
            return Duration.ofMillis((int) min(baseMillis * pow(factorMillis, attempt), maxMillis));
        }
    }
}
