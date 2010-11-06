package net.jzran;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;

import java.io.File;

interface ZLib extends Library {
    static class Helper {
        public static ZLib loadLibrary() {
            File res = getZlibFile().getAbsoluteFile();
            if(!res.exists()) {
                throw new RuntimeException(
                        "Library " + res + " doesn't exist, " +
                        "override property jzran.zlib.file, " +
                        "for example: java -Djzran.zlib.file=/path/to/zlib-mac.dylib");
            }
            try {
                return (ZLib) Native.loadLibrary(res.getAbsolutePath(), ZLib.class);
            } catch(Throwable t) {
                throw new RuntimeException("Library " + res + " exists but could not be loaded");
            }
        }

        private static File getZlibFile() {
            String overridden = System.getProperty("jzran.zlib.file");
            if(overridden != null) {
                return new File(overridden);
            }
            if(Platform.isWindows()) {
                return new File("lib/" + (Platform.is64Bit() ? "zlibwapi-1.2.5-64.dll" : "zlibwapi-1.2.5-32.dll"));
            } else if(Platform.isLinux()) {
                return new File("lib/" + (Platform.is64Bit() ? "libz-64.so.1.2.5" : "libz-32.so.1.2.5"));
            } else {
                throw new UnsupportedOperationException(
                        "Versions of zlib are bundled only for Windows and Linux for now. " +
                        "Report to the developers or override system property jzran.zlib.file, " +
                        "for example: java -Djzran.zlib.file=/path/to/zlib-mac.dylib");
            }
        }
    }

    public static ZLib INSTANCE = Helper.loadLibrary();

    int inflatePrime(z_stream stream, int bits, int value);

    int inflateSetDictionary(z_stream stream, byte[] dictionary, int length);

    int inflateInit2_(z_stream stream, int windowBits, String version, int streamSize);

    int inflate(z_stream stream, int flush);

    int inflateEnd(z_stream stream);

    String zlibVersion();

    public static final int Z_OK = 0;
    public static final int Z_STREAM_END = 1;
    public static final int Z_NEED_DICT = 2;
    public static final int Z_ERRNO = -1;
    public static final int Z_STREAM_ERROR = -2;
    public static final int Z_DATA_ERROR = -3;
    public static final int Z_MEM_ERROR = -4;
    public static final int Z_BUF_ERROR = -5;
    public static final int Z_VERSION_ERROR = -6;

    public static final int Z_BLOCK = 5;
    public static final int Z_NO_FLUSH = 0;

    public static final int STREAM_SIZE = new z_stream().size();

}
