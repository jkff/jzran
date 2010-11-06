package net.jzran;

import java.io.IOException;
import java.io.InputStream;

public abstract class SeekableInputStream extends InputStream {
    public abstract void seek(long offset) throws IOException;
}
