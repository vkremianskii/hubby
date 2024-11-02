package net.kremianskii.hubby;

import net.kremianskii.hubby.Backoff.Exponential;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.time.Duration;
import java.util.Arrays;
import java.util.logging.Logger;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.util.Arrays.copyOfRange;
import static java.util.Objects.requireNonNull;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static net.kremianskii.hubby.Utils.unchecked;

public final class Route implements AutoCloseable {
    private final String name;
    private final Endpoint leftEnd;
    private final Endpoint rightEnd;
    private final Logger logger;

    private Thread thread;

    public Route(String name, Endpoint leftEnd, Endpoint rightEnd) {
        this.name = requireNonNull(name, "name must not be null");
        this.leftEnd = requireNonNull(leftEnd, "leftEnd must not be null");
        this.rightEnd = requireNonNull(rightEnd, "rightEnd must not be null");
        this.logger = getLogger(Route.class.getName());
    }

    public synchronized void start() {
        logger.info("Starting route: name=" + name + ", left=" + leftEnd + ", right=" + rightEnd);
        thread = new Thread(unchecked(this::threadFun), "route-" + name);
        thread.start();
    }

    private void threadFun() throws IOException, InterruptedException {
        var buf = ByteBuffer.allocate(8192);
        try (var readSelector = Selector.open();
             var writeSelector = Selector.open()) {
            while (!currentThread().isInterrupted()) {
                try {
                    openEndpoint(leftEnd, readSelector, writeSelector);
                    openEndpoint(rightEnd, readSelector, writeSelector);
                    while (!currentThread().isInterrupted()) {
                        endsExchange(readSelector, writeSelector, buf);
                    }
                } catch (IOException | EndpointStreamEnded e) {
                    if (e instanceof IOException) {
                        logger.log(WARNING, "Error in route thread", e);
                    }
                    closeEndpoint(leftEnd);
                    closeEndpoint(rightEnd);
                }
            }
        }
    }

    private void openEndpoint(Endpoint endpoint,
                              Selector readSelector,
                              Selector writeSelector) throws IOException, InterruptedException {
        var attempt = 0;
        var backoff = new Exponential();
        while (true) {
            try {
                endpoint.open(readSelector, writeSelector);
                break;
            } catch (IOException e) {
                if (attempt == 2) {
                    throw e;
                }
                sleep(backoff.delay(attempt++));
            }
        }
        logger.info("Endpoint open: " + endpoint);
    }

    private void closeEndpoint(Endpoint endpoint) {
        try {
            leftEnd.close();
        } catch (IOException ignored) {
        }
        logger.info("Endpoint closed: " + endpoint);
    }

    private void endsExchange(Selector readSelector,
                              Selector writeSelector,
                              ByteBuffer buf) throws IOException, EndpointStreamEnded {
        readSelector.select();
        writeSelector.select();
        var readSelected = selectedEndpoints(readSelector);
        var writeSelected = selectedEndpoints(writeSelector);
        if (readSelected.left && writeSelected.right) {
            endsExchange(leftEnd, rightEnd, buf);
        }
        if (writeSelected.left && readSelected.right) {
            endsExchange(rightEnd, leftEnd, buf);
        }
    }

    private SelectedEndpoints selectedEndpoints(Selector selector) {
        var iterator = selector.selectedKeys().iterator();
        var leftSelected = false;
        var rightSelected = false;
        while (iterator.hasNext()) {
            var key = iterator.next();
            var attachment = key.attachment();
            if (attachment == leftEnd) {
                leftSelected = true;
            } else if (attachment == rightEnd) {
                rightSelected = true;
            }
            iterator.remove();
        }
        return new SelectedEndpoints(leftSelected, rightSelected);
    }

    private void endsExchange(Endpoint from, Endpoint to, ByteBuffer buf) throws IOException, EndpointStreamEnded {
        buf.clear();
        int bytesRead = from.read(buf);
        if (bytesRead == -1) {
            throw new EndpointStreamEnded();
        }
        if (logger.isLoggable(FINE)) {
            var data = Arrays.toString(copyOfRange(buf.array(), 0, bytesRead));
            logger.fine("Data read: endpoint=" + from + ", size=" + bytesRead + ", data=" + data);
        } else {
            logger.info("Data read: endpoint=" + from + ", size=" + bytesRead);
        }
        buf.flip();
        to.write(buf);
        logger.info("Data written: endpoint=" + to + ", size=" + bytesRead);
    }

    @Override
    public synchronized void close() {
        if (thread == null) return;
        if (thread.isAlive()) {
            thread.interrupt();
            try {
                thread.join(Duration.ofSeconds(10));
            } catch (InterruptedException ignored) {
            }
        }
        thread = null;
    }

    private record SelectedEndpoints(boolean left, boolean right) {
    }

    private static class EndpointStreamEnded extends Exception {
    }
}
