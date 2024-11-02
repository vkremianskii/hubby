package net.kremianskii.hubby;

import net.kremianskii.hubby.Backoff.Exponential;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackoffTests {
    @Test
    void delayBetweenAttemptsGrowsExponentially() {
        var backoff = new Exponential();
        assertEquals(Duration.ofSeconds(1), backoff.delay(0));
        assertEquals(Duration.ofSeconds(3), backoff.delay(1));
        assertEquals(Duration.ofSeconds(9), backoff.delay(2));
        assertEquals(Duration.ofSeconds(10), backoff.delay(3));
    }
}
