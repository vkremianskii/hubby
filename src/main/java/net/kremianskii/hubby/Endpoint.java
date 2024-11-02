package net.kremianskii.hubby;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;

public abstract class Endpoint implements Closeable {
    public abstract void open(Selector readSelector, Selector writeSelector) throws IOException;

    public abstract int read(ByteBuffer buf) throws IOException;

    public abstract void write(ByteBuffer buf) throws IOException;
}
