package net.jzran;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created on: 05.11.10 22:51
 */
public class ByteBufferSeekableInputStream extends SeekableInputStream {
    private final ByteBuffer buf;
    private long pos;
    private boolean isClosed;

    public ByteBufferSeekableInputStream(ByteBuffer buf) {
        this.buf = buf;
    }

    private void ensureOpen() {
        if(isClosed) {
            throw new IllegalStateException("Stream closed");
        }
    }

    @Override
    public int read() throws IOException {
        ensureOpen();
        return buf.remaining() > 0 ? buf.get() : -1;
    }

    @Override
    public int read(byte[] b) throws IOException {
        ensureOpen();
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        int rem = Math.min(len, buf.remaining());
        buf.get(b, off, rem);
        return (rem==0) ? -1 : rem;
    }

    @Override
    public long skip(long n) throws IOException {
        ensureOpen();
        if(n < 0) {
            return 0;
        }
        int toSkip = Math.min(
                (int)Math.min(n, Integer.MAX_VALUE),
                Integer.MAX_VALUE - buf.position());
        buf.position(buf.position() + toSkip);
        return toSkip;
    }

    @Override
    public int available() throws IOException {
        ensureOpen();
        return buf.limit() - buf.position();
    }

    @Override
    public void close() throws IOException {
        isClosed = true;
    }

    @Override
    public void mark(int readlimit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() throws IOException {
        ensureOpen();
        buf.rewind();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    public void seek(long offset) throws IOException {
        ensureOpen();
        if(offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Offset too large: " + offset + " > " + Integer.MAX_VALUE);
        }
        buf.position((int)offset);
    }

    public long length() throws IOException {
        return buf.limit();
    }
}
