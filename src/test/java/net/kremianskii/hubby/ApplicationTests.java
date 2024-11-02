package net.kremianskii.hubby;

import net.kremianskii.hubby.TestFixtures.TcpClient;
import net.kremianskii.hubby.TestFixtures.TcpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

import static java.net.InetAddress.getLoopbackAddress;
import static net.kremianskii.hubby.Application.main;
import static net.kremianskii.hubby.Utils.unchecked;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ApplicationTests {
    @Test
    void runsSingleRouteWithLocalAndRemoteEndpoints() throws Exception {
        var request = "Hello, world!".getBytes();
        var response = "Hello there.".getBytes();

        var leftLatch = new CountDownLatch(1);
        var leftThread = new Thread(unchecked(() -> {
            try (var client = new TcpClient(new InetSocketAddress(getLoopbackAddress(), 8700))) {
                client.send(request);
                assertArrayEquals(response, client.receive(8192));
            }
            leftLatch.countDown();
        }));
        leftThread.start();

        var rightLatch = new CountDownLatch(1);
        var rightThread = new Thread(unchecked(() -> {
            try (var server = new TcpServer(new InetSocketAddress(getLoopbackAddress(), 8800));
                 var client = server.accept()) {
                assertArrayEquals(request, client.receive(8192));
                client.send(response);
            }
            rightLatch.countDown();
        }));
        rightThread.start();

        var mainThread = new Thread(unchecked(() -> {
            var args = new String[]{
                    "local:127.0.0.1:8700",
                    "remote:127.0.0.1:8800"};
            main(args);
        }));
        mainThread.start();

        leftLatch.await();
        rightLatch.await();
        leftThread.join();
        rightThread.join();
        mainThread.interrupt();
        mainThread.join();
    }
}
