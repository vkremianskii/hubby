package net.kremianskii.hubby;

import net.kremianskii.hubby.Backoff.Exponential;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static java.lang.Thread.sleep;
import static java.util.Arrays.copyOfRange;

public final class TestFixtures {
    public static final class TcpClient implements AutoCloseable {
        private Socket socket;

        TcpClient(Socket socket) {
            this.socket = socket;
        }

        public TcpClient(InetSocketAddress address) throws IOException, InterruptedException {
            connect(address);
        }

        private void connect(InetSocketAddress address) throws IOException, InterruptedException {
            var attempt = 0;
            var backoff = new Exponential();
            while (true) {
                try {
                    socket = new Socket();
                    socket.connect(address);
                    break;
                } catch (IOException e) {
                    if (attempt == 2) {
                        throw e;
                    }
                    sleep(backoff.delay(attempt++));
                }
            }
        }

        public void send(byte[] data) throws IOException {
            socket.getOutputStream().write(data);
        }

        public byte[] receive(int buflen) throws IOException {
            var buf = new byte[buflen];
            var bytesRead = socket.getInputStream().read(buf);
            return copyOfRange(buf, 0, bytesRead);
        }

        @Override
        public void close() throws IOException {
            socket.close();
            socket = null;
        }
    }

    public static final class TcpServer implements AutoCloseable {
        private final ServerSocket serverSocket;

        public TcpServer(InetSocketAddress address) throws IOException {
            serverSocket = new ServerSocket();
            serverSocket.bind(address);
        }

        public TcpClient accept() throws IOException {
            return new TcpClient(serverSocket.accept());
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
        }
    }
}
