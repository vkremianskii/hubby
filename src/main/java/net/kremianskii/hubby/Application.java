package net.kremianskii.hubby;

import net.kremianskii.hubby.endpoint.LocalTcpSocket;
import net.kremianskii.hubby.endpoint.RemoteTcpSocket;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public final class Application {
    public static void main(String[] args) throws Exception {
        try {
            var routes = routes(args);
            run(routes);
            System.exit(0);
        } catch (InvalidCmdLineArgs e) {
            printUsage();
            System.exit(1);
        }
    }

    private static List<Route> routes(String[] args) {
        if (args.length != 2) {
            throw new InvalidCmdLineArgs();
        }
        var leftEnd = endpoint(args[0]);
        var rightEnd = endpoint(args[1]);
        return List.of(new Route("main", leftEnd, rightEnd));
    }

    private static Endpoint endpoint(String arg) {
        var strings = arg.split(":");
        if (strings.length != 3) {
            throw new InvalidCmdLineArgs();
        }
        var host = inetAddress(strings[1]);
        var port = port(strings[2]);
        var address = new InetSocketAddress(host, port);
        return switch (strings[0]) {
            case "local" -> new LocalTcpSocket(address);
            case "remote" -> new RemoteTcpSocket(address);
            default -> throw new InvalidCmdLineArgs();
        };
    }

    private static InetAddress inetAddress(String host) {
        try {
            return InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new InvalidCmdLineArgs();
        }
    }

    private static int port(String s) {
        try {
            return Integer.parseUnsignedInt(s);
        } catch (NumberFormatException e) {
            throw new InvalidCmdLineArgs();
        }
    }

    private static void run(List<Route> routes) throws Exception {
        routes.forEach(Route::start);
        var latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            routes.forEach(Route::close);
            latch.countDown();
        }));
        latch.await();
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar hubby.jar ENDPOINT ENDPOINT\n" +
                " ENDPOINT={ local | remote }:host:port");
    }

    private static class InvalidCmdLineArgs extends RuntimeException {
    }
}
