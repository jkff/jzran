package net.jzran;

import org.junit.Test;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import static junit.framework.Assert.assertEquals;

public class RandomAccessGZipTest {
    @Test
    public void testGzipByteArray() throws Exception {
        TestPair testPair = new TestPair();
        testCorrectness(testPair.buf, new ByteArraySeekableInputStream(testPair.zipped, 0, testPair.zipped.length));
    }

    @Test
    public void testGzipByteBuffer() throws Exception {
        TestPair testPair = new TestPair();
        testCorrectness(testPair.buf, new ByteBufferSeekableInputStream(ByteBuffer.wrap(testPair.zipped)));
    }

    @Test
    public void testGzipFile() throws Exception {
        TestPair testPair = new TestPair();
        File tmp = File.createTempFile("zip-test", ".zip");
        tmp.deleteOnExit();
        OutputStream os = new FileOutputStream(tmp);
        os.write(testPair.zipped);
        testCorrectness(testPair.buf, new FileSeekableInputStream(new RandomAccessFile(tmp, "r")));
    }

    private void testCorrectness(byte[] buf, SeekableInputStream basis) throws Exception {
        RandomAccessGZip.Index index = RandomAccessGZip.index(basis, 1048576, new ProgressListener<Long>() {
            public boolean reportProgress(Long progress) {
                return true;
            }
        });

        ByteArrayOutputStream indexData = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(indexData);
        oos.writeObject(index);
        oos.close();
        indexData.close();
        byte[] indexBytes = indexData.toByteArray();

        index = (RandomAccessGZip.Index) new ObjectInputStream(new ByteArrayInputStream(indexBytes)).readObject();

        assertEquals(buf.length, index.decompressedSize());

        Random r = new Random(56738138L);
        for(int i = 0; i < 100; ++i) {
            int origin = r.nextInt(buf.length);
            byte[] dest = new byte[Math.min(100, buf.length - origin)];
            int n = index.read(basis, origin, dest, 0, dest.length);
            for(int j = 0; j < Math.min(n, dest.length); ++j) {
                assertEquals(dest[j], buf[origin+j]);
            }
        }
    }

    private class TestPair {
        private final byte[] buf;
        private final byte[] zipped;

        private TestPair() throws IOException {
            buf = new byte[10485739];
            new Random().nextBytes(buf);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream zos = new GZIPOutputStream(baos);
            zos.write(buf);
            zos.close();
            baos.close();
            zipped = baos.toByteArray();
        }
    }
}
