package net.jzran;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created on: 05.11.10 22:49
 */
public abstract class SeekableInputStream extends InputStream {
    public abstract void seek(long offset) throws IOException;
}
