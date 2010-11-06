package net.jzran;

import com.sun.jna.Memory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.List;

public class RandomAccessZip {
    public interface Index {
        int read(SeekableInputStream f, long origin, byte[] buf, int offset, int len) throws IOException;

        long decompressedSize();
    }

    private static class IndexImpl implements Index, Serializable {
        private static final int BUF_SIZE = 1048576;

        private List<ZRan.Point> idx;
        private long decompressedSize;
        private final Memory mem;
        private final ByteBuffer memBuf;

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

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.writeObject(this.idx);
            out.writeObject(this.decompressedSize);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            this.idx = (List) in.readObject();
            this.decompressedSize = in.readLong();
        }
    }

    /**
     * Use this method to create an index :)
     *
     * @param span A "checkpoint" will take place every span
     * bytes of decompressed data. A checkpoint takes about
     */
    public static Index index(InputStream input, long span, ProgressListener<Long> listener) throws IOException {
        long[] holder = {0L};
        List<ZRan.Point> idx = ZRan.build_index(input, span, holder, listener);
        return idx == null ? null : new IndexImpl(idx, holder[0]);
    }
}
