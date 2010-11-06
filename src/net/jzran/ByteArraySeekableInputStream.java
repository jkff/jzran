package net.jzran;

import java.io.IOException;

public class ByteArraySeekableInputStream extends SeekableInputStream {
    private final byte[] buf;
    private final int offset;
    private final int len;

    private int pos;

    private boolean isClosed;

    public ByteArraySeekableInputStream(byte[] buf, int offset, int len) {
        this.buf = buf;
        this.offset = offset;
        this.len = len;
    }

    private void ensureOpen() {
        if(isClosed) {
            throw new IllegalStateException("Stream closed");
        }
    }

    @Override
    public int read() throws IOException {
        ensureOpen();
        if(pos == offset + len)
            return -1;
        int oldPos = pos;
        pos++;
        return buf[oldPos];
    }

    @Override
    public int read(byte[] b) throws IOException {
        ensureOpen();
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int readLen) throws IOException {
        ensureOpen();
        int rem = Math.min(readLen, offset + len - pos);
        if(rem == 0)
            return -1;
        System.arraycopy(buf, pos, b, off, rem);
        pos += rem;
        return rem;
    }

    @Override
    public long skip(long n) throws IOException {
        ensureOpen();
        if(n < 0)
            return 0;
        int rem = Math.min(
                (int)Math.min(n, Integer.MAX_VALUE),
                offset + len - pos);
        pos += rem;
        return rem;
    }

    @Override
    public int available() throws IOException {
        ensureOpen();
        return offset + len - pos;
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
        pos = offset;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    public void seek(long seekOffset) throws IOException {
        ensureOpen();
        if(seekOffset > len) {
            throw new IllegalArgumentException(
                    "Offset beyond end of buffer: " + seekOffset + " > " + len);
        }
        // Can't be more than Integer.MAX_VALUE because offset + len is less.
        pos = (int)(offset + seekOffset);
    }
}
