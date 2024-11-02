package net.kremianskii.hubby;

import net.kremianskii.hubby.TestFixtures.TcpClient;
import net.kremianskii.hubby.TestFixtures.TcpServer;
import net.kremianskii.hubby.endpoint.LocalTcpSocket;
import net.kremianskii.hubby.endpoint.RemoteTcpSocket;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

import static java.net.InetAddress.getLoopbackAddress;
import static net.kremianskii.hubby.Utils.unchecked;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class RouteTests {
    @Test
    void exchangesDataBetweenTwoEndpoints() throws InterruptedException {
        var request = "Hello, world!".getBytes();
        var response = "Hello there.".getBytes();

        var leftLatch = new CountDownLatch(1);
        var leftAddress = new InetSocketAddress(getLoopbackAddress(), 8300);
        var leftThread = new Thread(unchecked(() -> {
            try (var client = new TcpClient(leftAddress)) {
                client.send(request);
                assertArrayEquals(response, client.receive(8192));
                leftLatch.countDown();
            }
        }));
        leftThread.start();

        var rightLatch = new CountDownLatch(1);
        var rightAddress = new InetSocketAddress(getLoopbackAddress(), 8400);
        var rightThread = new Thread(unchecked(() -> {
            try (var server = new TcpServer(rightAddress);
                 var client = server.accept()) {
                assertArrayEquals(request, client.receive(8192));
                client.send(response);
                rightLatch.countDown();
            }
        }));
        rightThread.start();

        var leftEnd = new LocalTcpSocket(leftAddress);
        var rightEnd = new RemoteTcpSocket(rightAddress);
        var route = new Route("test", leftEnd, rightEnd);
        route.start();

        leftLatch.await();
        rightLatch.await();
        leftThread.join();
        rightThread.join();
        route.close();
    }

    @Test
    void reopensBothEndsWhenOneEndFails() throws Exception {
        var leftLatch = new CountDownLatch(1);
        var leftAddress = new InetSocketAddress(getLoopbackAddress(), 8500);
        var leftThread = new Thread(unchecked(() -> {
            try (var server = new TcpServer(leftAddress)) {
                try (var client = server.accept()) {
                }
                try (var client = server.accept()) {
                }
                leftLatch.countDown();
            }
        }));
        leftThread.start();

        var rightLatch = new CountDownLatch(1);
        var rightAddress = new InetSocketAddress(getLoopbackAddress(), 8600);
        var rightThread = new Thread(unchecked(() -> {
            try (var server = new TcpServer(rightAddress)) {
                try (var client = server.accept()) {
                }
                try (var client = server.accept()) {
                }
                rightLatch.countDown();
            }
        }));
        rightThread.start();

        var leftEnd = new RemoteTcpSocket(leftAddress);
        var rightEnd = new RemoteTcpSocket(rightAddress);
        var route = new Route("test", leftEnd, rightEnd);
        route.start();

        leftLatch.await();
        rightLatch.await();
        leftThread.join();
        rightThread.join();
        route.close();
    }
}
