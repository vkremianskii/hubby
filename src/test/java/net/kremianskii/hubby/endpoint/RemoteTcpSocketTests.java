package net.kremianskii.hubby.endpoint;

import net.kremianskii.hubby.TestFixtures.TcpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.util.concurrent.CountDownLatch;

import static java.lang.Thread.sleep;
import static java.net.InetAddress.getLoopbackAddress;
import static net.kremianskii.hubby.Utils.unchecked;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RemoteTcpSocketTests {
    @Test
    void readsToAndWritesFromRemoteTcpSocket() throws Exception {
        var address = new InetSocketAddress(getLoopbackAddress(), 8200);
        var request = "Hello, world!".getBytes();

        var latch = new CountDownLatch(1);
        var serverThread = new Thread(unchecked(() -> {
            try (var server = new TcpServer(address);
                 var client = server.accept()) {
                client.send(request);
                assertArrayEquals(request, client.receive(8192));
            }
            latch.countDown();
        }));
        serverThread.start();

        sleep(100);
        var buf = ByteBuffer.allocate(8192);
        try (var readSelector = Selector.open();
             var writeSelector = Selector.open();
             var endpoint = new RemoteTcpSocket(address)) {
            endpoint.open(readSelector, writeSelector);
            readSelector.select();
            writeSelector.select();
            assertFalse(readSelector.selectedKeys().isEmpty());
            assertFalse(writeSelector.selectedKeys().isEmpty());
            endpoint.read(buf);
            buf.flip();
            endpoint.write(buf);
        }

        latch.await();
        serverThread.join();
    }
}
