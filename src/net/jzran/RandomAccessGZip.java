package net.jzran;

import com.sun.jna.Memory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * The entry point. Use {@link #index(java.io.InputStream,
 * long, ProgressListener)} to create an index.
 *
 * The index is a regular {@link net.jzran.SeekableInputStream}.
 *
 * If you have trouble loading the zlib library, just specify
 * the jzran.zlib.file property, for example
 * <tt>java -Djzran.zlib.file=/path/to/zlib-mac.dylib</tt>.
 */
public class RandomAccessGZip {
    public static class Index extends SeekableInputStream implements Serializable {
        private enum State {
            // inStream is not set
            VOID,
            // inStream is set but no seek was done
            SEMI_OPEN,
            // ready for reading data
            OPEN,
            CLOSED
        }
        // transitions: state -> state + 1

        private static final int BUF_SIZE = 1048576;

        private List<ZRan.Point> idx;
        private long decompressedSize;
        private State state;

        private transient Memory mem;
        private transient ByteBuffer memBuf;
        private transient SeekableInputStream inStream;
        private transient ZRan.Extractor extractor;
        private transient long pos;

        private Index(List<ZRan.Point> idx, long decompressedSize) {
            this.idx = idx;
            this.decompressedSize = decompressedSize;
            this.mem = new Memory(BUF_SIZE);
            this.memBuf = mem.getByteBuffer(0, BUF_SIZE);
        }

        /**
         * Prepare this index for working with the compressed stream.
         * Optionally also seek to an origin.
         *
         * @param inStream compressed data stream.
         * @param offset if greater than zero, then also seek to this
         *   position in the decompressed data.
         */
        public void open(SeekableInputStream inStream, long offset) throws IOException {
            if(state != State.VOID)
                throw new IllegalStateException("Can only call open() once");

            this.inStream = inStream;
            state = State.SEMI_OPEN;
            if(offset > 0)
                seek(offset);
        }

        /**
         * Prepare this index for working with the compressed stream.
         * Do not seek anywhere yet (you'll have to seek somewhere before
         * the stream is usable).
         */
        public void open(SeekableInputStream inStream) throws IOException {
            open(inStream, -1);
        }

        public void seek(long offset) throws IOException {
            if(state == State.VOID)
                throw new IllegalStateException("Call open() before seeking");
            if(state == State.CLOSED)
                throw new IllegalStateException("Stream closed");

            this.pos = offset;
            this.extractor = new ZRan.Extractor(inStream, idx, offset);
            state = State.OPEN;
        }

        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            return read(b)>0 ? b[0] : -1;
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public long skip(long n) throws IOException {
            ensureOpen();
            long toSkip = Math.min(n, available());
            seek(pos + toSkip);
            return toSkip;
        }

        @Override
        public int available() throws IOException {
            ensureOpen();
            return (int) Math.min(Integer.MAX_VALUE, length() - pos);
        }

        @Override
        public void close() throws IOException {
            if(state != State.OPEN)
                throw new IllegalStateException("Can only close an open stream");
            extractor.close();
            state = State.CLOSED;
        }

        public int read(byte[] buf, int offset, int len) throws IOException {
            ensureOpen();
            int n = extractor.extract(mem, Math.min(len, BUF_SIZE));
            memBuf.position(0);
            memBuf.get(buf, offset, n);
            pos += n;
            return n;
        }

        public long length() {
            return decompressedSize;
        }

        private void ensureOpen() {
            if(state != State.OPEN)
                throw new IllegalStateException("Stream must be open");
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.defaultWriteObject();
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
            this.mem = new Memory(BUF_SIZE);
            this.memBuf = mem.getByteBuffer(0, BUF_SIZE);
            this.state = State.VOID;
        }
    }

    /**
     * Use this method to create an index and monitor progress :)
     *
     * @param span A "checkpoint" will take place every span bytes of
     *   decompressed data. A checkpoint takes about 32kb.
     * @param listener It will be frequently notified of how many bytes of
     *   compressed input have been read so far.
     * @return an index, or null if progress listener returns false
     *   along the way ("cancel").
     */
    public static Index index(InputStream input, long span, ProgressListener<Long> listener) throws IOException {
        long[] holder = {0L};
        List<ZRan.Point> idx = ZRan.build_index(input, span, holder, listener);
        return idx == null ? null : new Index(idx, holder[0]);
    }

    /**
     * Use this method to create an index :)
     *
     * @param span A "checkpoint" will take place every span bytes of
     *   decompressed data. A checkpoint takes about 32kb.
     */
    public static Index index(InputStream input, long span) throws IOException {
        return index(input, span, new NullProgressListener());
    }
}
