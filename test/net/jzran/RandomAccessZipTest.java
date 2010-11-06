package net.jzran;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static junit.framework.Assert.assertEquals;

/**
 * Created on: 05.11.10 23:30
 */
public class RandomAccessZipTest {
    @Test
    public void testGZipByteArray() throws IOException {
        byte[] buf = new byte[10485739];
        new Random().nextBytes(buf);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream zos = new GZIPOutputStream(baos);
        zos.write(buf);
        zos.close();
        byte[] zipped = baos.toByteArray();

        ByteArraySeekableInputStream basis = new ByteArraySeekableInputStream(zipped, 0, zipped.length);
        RandomAccessZip.Index index = RandomAccessZip.index(basis, 15373, new ProgressListener<Long>() {
            public boolean reportProgress(Long progress) {
                return true;
            }
        });

        assertEquals(buf.length, index.decompressedSize());
        Random r = new Random(56738138L);
        for(int i = 0; i < 100; ++i) {
            int origin = r.nextInt(buf.length);
            byte[] dest = new byte[Math.min(100, buf.length - origin)];
            int n = index.read(basis, origin, dest, 0, dest.length);
            for(int j = 0; j < Math.min(n, dest.length); ++j) {
                assertEquals(dest[j], buf[j]);
            }
        }
    }
}
