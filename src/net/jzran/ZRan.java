package net.jzran;

import com.sun.jna.Memory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * This file is a nearly identical reimplementation of 'zran.c'
 * from the examples to zlib 1.2.3.
 */
class ZRan {
    private static final ZLib Z = ZLib.INSTANCE;

    private static final int WINSIZE = 32768;
    private static final int CHUNK = 65536;

    static class Point implements Serializable {
        private long out;
        private long in;
        private int bits;
        private byte[] window;

        private Point(long out, long in, int bits) {
            this.out = out;
            this.in = in;
            this.bits = bits;
            this.window = new byte[WINSIZE];
        }
    }

    static void addpoint(List<Point> index, int bits, long in, long out, int left, Memory window) {
        Point p = new Point(out, in, bits);
        index.add(p);
        if (left != 0)
            System.arraycopy(window.getByteArray(0, WINSIZE), WINSIZE - left, p.window, 0, left);
        if (left < WINSIZE)
            System.arraycopy(window.getByteArray(0, WINSIZE), 0, p.window, left, WINSIZE - left);
    }

    static List<Point> build_index(InputStream in, long span, long[] decompressedSize, ProgressListener<Long> listener)
            throws IOException {
        int ret;
        long totin, totout;  /* our own total counters to avoid 4GB limit */
        long last;                 /* totout value of last access Point */
        List<Point> index;       /* access points being generated */
        z_stream strm = new z_stream();
        Memory input = new Memory(CHUNK);
        ByteBuffer bb = input.getByteBuffer(0, CHUNK);
        Memory window = new Memory(WINSIZE);
        byte[] buf = new byte[CHUNK];

        /* initialize inflate */
        strm.zalloc = null;
        strm.zfree = null;
        strm.opaque = null;
        strm.avail_in = 0;
        strm.next_in = null;
        ret = Z.inflateInit2_(strm, 47, Z.zlibVersion(), ZLib.STREAM_SIZE);      /* automatic zlib or gzip decoding */
        try {
            if (ret != ZLib.Z_OK)
                throw new IOException("zlib error: " + ret);

            /* inflate the input, maintain a sliding window, and build an index -- this
  also validates the integrity of the compressed data using the check
  information at the end of the gzip or zlib stream */
            totin = totout = last = 0;
            index = new ArrayList<Point>();               /* will be allocated by first addpoint() */
            strm.avail_out = 0;
            do {
                if (!listener.reportProgress(totin))
                    return null;

                /* get some compressed data from input file */
                strm.avail_in = in.read(buf, 0, CHUNK);
                if (strm.avail_in == -1)
                    throw new IOException("zlib: data error");
                bb.position(0);
                bb.put(buf, 0, strm.avail_in);
                strm.next_in = input;

                /* process all of that, or until end of stream */
                do {
                    /* reset sliding window if necessary */
                    if (strm.avail_out == 0) {
                        strm.avail_out = WINSIZE;
                        strm.next_out = window;
                    }

                    /* inflate until out of input, output, or at end of block --
             update the total input and output counters */
                    totin += strm.avail_in;
                    totout += strm.avail_out;
                    ret = Z.inflate(strm, ZLib.Z_BLOCK);      /* return at end of block */
                    totin -= strm.avail_in;
                    totout -= strm.avail_out;
                    if (ret == ZLib.Z_NEED_DICT)
                        ret = ZLib.Z_DATA_ERROR;
                    if (ret == ZLib.Z_MEM_ERROR || ret == ZLib.Z_DATA_ERROR)
                        throw new IOException("zlib error: " + ret);
                    if (ret == ZLib.Z_STREAM_END)
                        break;

                    /* if at end of block, consider adding an index entry (note that if
                      data_type indicates an end-of-block, then all of the
                      uncompressed data from that block has been delivered, and none
                      of the compressed data after that block has been consumed,
                      except for up to seven bits) -- the totout == 0 provides an
                      entry Point after the zlib or gzip header, and assures that the
                      index always has at least one access Point; we avoid creating an
                      access Point after the last block by checking bit 6 of data_type
                    */
                    if ((0 != (strm.data_type & 128)) && (0 == (strm.data_type & 64)) &&
                            (totout == 0 || totout - last > span)) {
                        addpoint(index, strm.data_type & 7, totin, totout, strm.avail_out, window);
                        last = totout;
                    }
                } while (strm.avail_in != 0);
            } while (ret != ZLib.Z_STREAM_END);

            decompressedSize[0] = totout;
            return index;
        } finally {
            Z.inflateEnd(strm);
        }
    }

    private static Memory discard = new Memory(WINSIZE);

    static class Extractor {
        private final byte[] bbuf = new byte[CHUNK];
        private final Memory input = new Memory(CHUNK);
        private final ByteBuffer bb = input.getByteBuffer(0, CHUNK);
        private final z_stream stream;
        private final SeekableInputStream inStream;

        Extractor(SeekableInputStream inStream, List<Point> index, long offset) throws IOException {
            this.inStream = inStream;

            Point here = findIndexPoint(index, offset);

            z_stream strm = new z_stream();
            strm.zalloc = null;
            strm.zfree = null;
            strm.opaque = null;
            strm.avail_in = 0;
            strm.next_in = null;
            int init = Z.inflateInit2_(
                    strm, -15, Z.zlibVersion(), ZLib.STREAM_SIZE);
            if (init != ZLib.Z_OK)
                throw new IOException("zlib error: " + init);
            this.stream = strm;

            inStream.seek(here.in - ((here.bits != 0) ? 1 : 0));
            if (here.bits != 0) {
                int prime = inStream.read();
                if (prime == -1)
                    throw new IOException("End of stream");
                Z.inflatePrime(stream, here.bits, prime >>> (8 - here.bits));
            }
            Z.inflateSetDictionary(stream, here.window, WINSIZE);

            for (long rem = offset - here.out; rem > 0; ) {
                rem -= extract(discard, (rem > WINSIZE) ? WINSIZE : (int) rem);
            }
        }

        public void close() {
            Z.inflateEnd(stream);
        }

        public int extract(Memory buf, int len) throws IOException {
            stream.next_out = buf;
            stream.avail_out = len;
            if (stream.avail_in == 0) {
                int nr = inStream.read(bbuf);
                if (nr == -1)
                    throw new IOException("End of stream");
                bb.position(0);
                bb.put(bbuf, 0, nr);
                stream.next_in = input;
                stream.avail_in = nr;
            }
            int ret = Z.inflate(stream, ZLib.Z_NO_FLUSH);       /* normal inflate */
            switch (ret) {
                case ZLib.Z_NEED_DICT:
                case ZLib.Z_MEM_ERROR:
                case ZLib.Z_DATA_ERROR:
                    throw new IOException("zlib error: " + ret);
            }
            return len - stream.avail_out;
        }

        private static Point findIndexPoint(List<Point> index, long offset) {
            for (int j = 0; j < index.size() - 1; ++j) {
                if (index.get(j + 1).out > offset)
                    return index.get(j);
            }
            return index.get(index.size() - 1);
        }
    }

}
