package net.jzran;

import java.io.IOException;
import java.io.RandomAccessFile;

public class FileSeekableInputStream extends SeekableInputStream {
    private final RandomAccessFile raf;

    public FileSeekableInputStream(RandomAccessFile raf) {
        this.raf = raf;
    }

    @Override
    public int read() throws IOException {
        return raf.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return raf.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return raf.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return n < 0 ? 0 : raf.skipBytes((int) Math.min(n, Integer.MAX_VALUE));
    }

    @Override
    public int available() throws IOException {
        return (int)Math.min(raf.length() - raf.getFilePointer(), Integer.MAX_VALUE);
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }

    @Override
    public void mark(int readlimit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() throws IOException {
        raf.seek(0);
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    public void seek(long offset) throws IOException {
        raf.seek(offset);
    }

    public long length() throws IOException {
        return raf.length();
    }
}
