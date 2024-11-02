package net.kremianskii.hubby.endpoint;

import net.kremianskii.hubby.Endpoint;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.Duration;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static java.util.Objects.requireNonNull;
import static net.kremianskii.hubby.Utils.unchecked;

public final class LocalTcpSocket extends Endpoint {
    public final InetSocketAddress address;

    private final Object monitor = new Object();

    private ServerSocketChannel serverSocket;
    private Thread acceptThread;
    private SocketChannel clientSocket;
    private SelectionKey readSelKey;
    private SelectionKey writeSelKey;

    public LocalTcpSocket(InetSocketAddress address) {
        this.address = requireNonNull(address, "address must not be null");
    }

    @Override
    public void open(Selector readSelector, Selector writeSelector) throws IOException {
        synchronized (monitor) {
            serverSocket = ServerSocketChannel.open();
            serverSocket.configureBlocking(true);
            serverSocket.bind(address);
            acceptThread = new Thread(unchecked(() -> {
                synchronized (monitor) {
                    clientSocket = serverSocket.accept();
                    clientSocket.configureBlocking(false);
                    readSelKey = clientSocket.register(readSelector, OP_READ, this);
                    writeSelKey = clientSocket.register(writeSelector, OP_WRITE, this);
                    readSelector.wakeup();
                    writeSelector.wakeup();
                }
            }));
            acceptThread.start();
        }
    }

    @Override
    public int read(ByteBuffer buf) throws IOException {
        synchronized (monitor) {
            return clientSocket.read(buf);
        }
    }

    @Override
    public void write(ByteBuffer buf) throws IOException {
        synchronized (monitor) {
            clientSocket.write(buf);
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (monitor) {
            if (writeSelKey != null) {
                writeSelKey.cancel();
                writeSelKey = null;
            }
            if (readSelKey != null) {
                readSelKey.cancel();
                readSelKey = null;
            }
            if (clientSocket != null) {
                clientSocket.close();
                clientSocket = null;
            }
            if (acceptThread != null) {
                if (acceptThread.isAlive()) {
                    acceptThread.interrupt();
                    try {
                        acceptThread.join(Duration.ofSeconds(10));
                    } catch (InterruptedException ignored) {
                    }
                }
                acceptThread = null;
            }
            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
        }
    }

    @Override
    public String toString() {
        return "local:" + address;
    }
}
