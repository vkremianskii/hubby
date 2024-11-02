package net.kremianskii.hubby;

import net.kremianskii.hubby.Utils.ThrowingRunnable;
import org.junit.jupiter.api.Test;

import static net.kremianskii.hubby.Utils.unchecked;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UtilsTests {
    @Test
    void uncheckedWrapsThrownExceptionInRuntimeException() {
        var thrown = new Exception();
        ThrowingRunnable runnable = () -> {
            throw thrown;
        };
        var wrapped = assertThrows(RuntimeException.class, () -> unchecked(runnable).run());
        assertSame(thrown, wrapped.getCause());
    }
}
