package net.jzran;

import com.sun.jna.Memory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * The entry point. Use {@link #index(java.io.InputStream, long, ProgressListener)} to create an index.
 * If you have trouble loading the zlib library, just specify the jzran.zlib.file property,
 * for example <tt>java -Djzran.zlib.file=/path/to/zlib-mac.dylib</tt>.
 */
public class RandomAccessGZip {
    public interface Index extends Serializable {
        /**
         * Given the compressed stream, read 'len' bytes starting at 'origin',
         * write them to 'buf' starting from 'offset'.
         *
         * @return How many bytes have actually been read
         */
        int read(SeekableInputStream f, long origin, byte[] buf, int offset, int len) throws IOException;

        /**
         * How big was the original (decompressed) file.
         */
        long decompressedSize();
    }

    private static class IndexImpl implements Index {
        private static final int BUF_SIZE = 1048576;

        private List<ZRan.Point> idx;
        private long decompressedSize;
        private transient Memory mem;
        private transient ByteBuffer memBuf;

        private IndexImpl(List<ZRan.Point> idx, long decompressedSize) {
            this.idx = idx;
            this.decompressedSize = decompressedSize;
            this.mem = new Memory(BUF_SIZE);
            this.memBuf = mem.getByteBuffer(0, BUF_SIZE);
        }

        public long decompressedSize() {
            return decompressedSize;
        }

        public synchronized int read(SeekableInputStream f, long origin, byte[] buf, int offset, int len) throws IOException {
            int total = 0;
            while (len > 0) {
                int n = ZRan.extract(f, idx, origin, mem, Math.min(len, BUF_SIZE));
                if (n == 0)
                    break;
                total += n;
                memBuf.position(0);
                memBuf.get(buf, offset, n);
                offset += n;
                len -= n;
            }
            return total;
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.defaultWriteObject();
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
            this.mem = new Memory(BUF_SIZE);
            this.memBuf = mem.getByteBuffer(0, BUF_SIZE);
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
        return idx == null ? null : new IndexImpl(idx, holder[0]);
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
