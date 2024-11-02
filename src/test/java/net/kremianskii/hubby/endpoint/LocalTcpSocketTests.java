package net.kremianskii.hubby.endpoint;

import net.kremianskii.hubby.TestFixtures.TcpClient;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.util.concurrent.CountDownLatch;

import static java.net.InetAddress.getLoopbackAddress;
import static net.kremianskii.hubby.Utils.unchecked;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class LocalTcpSocketTests {
    @Test
    void readsToAndWritesFromLocalTcpSocket() throws Exception {
        var address = new InetSocketAddress(getLoopbackAddress(), 8100);
        var request = "Hello, world!".getBytes();

        var latch = new CountDownLatch(1);
        var clientThread = new Thread(unchecked(() -> {
            try (var client = new TcpClient(address)) {
                client.send(request);
                assertArrayEquals(request, client.receive(8192));
            }
            latch.countDown();
        }));
        clientThread.start();

        var buf = ByteBuffer.allocate(8192);
        try (var readSelector = Selector.open();
             var writeSelector = Selector.open();
             var endpoint = new LocalTcpSocket(address)) {
            endpoint.open(readSelector, writeSelector);
            while (true) {
                readSelector.select();
                writeSelector.select();
                if (readSelector.selectedKeys().isEmpty() || writeSelector.selectedKeys().isEmpty()) {
                    continue;
                }
                endpoint.read(buf);
                buf.flip();
                endpoint.write(buf);
                break;
            }
        }

        latch.await();
        clientThread.join();
    }
}
