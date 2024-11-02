package net.kremianskii.hubby.endpoint;

import net.kremianskii.hubby.Endpoint;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static java.util.Objects.requireNonNull;

public final class RemoteTcpSocket extends Endpoint {
    public final InetSocketAddress address;

    private SocketChannel socket;
    private SelectionKey readSelKey;
    private SelectionKey writeSelKey;

    public RemoteTcpSocket(InetSocketAddress address) {
        this.address = requireNonNull(address, "address must not be null");
    }

    @Override
    public synchronized void open(Selector readSelector, Selector writeSelector) throws IOException {
        socket = SocketChannel.open();
        socket.connect(address);
        socket.configureBlocking(false);
        readSelKey = socket.register(readSelector, OP_READ, this);
        writeSelKey = socket.register(writeSelector, OP_WRITE, this);
    }

    @Override
    public synchronized int read(ByteBuffer buf) throws IOException {
        return socket.read(buf);
    }

    @Override
    public synchronized void write(ByteBuffer buf) throws IOException {
        socket.write(buf);
    }

    @Override
    public synchronized void close() throws IOException {
        if (writeSelKey != null) {
            writeSelKey.cancel();
            writeSelKey = null;
        }
        if (readSelKey != null) {
            readSelKey.cancel();
            readSelKey = null;
        }
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    @Override
    public String toString() {
        return "remote:" + address;
    }
}
