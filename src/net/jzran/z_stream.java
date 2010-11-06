package net.jzran;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class z_stream extends Structure {
    public Pointer next_in;
    public int avail_in;
    public NativeLong total_in;

    public Pointer next_out;
    public int avail_out;
    public NativeLong total_out;

    public Pointer msg;

    public Pointer state;

    public alloc_func zalloc;
    public free_func zfree;
    public Pointer opaque;

    public int data_type;
    public NativeLong adler;
    public NativeLong reserved;
}
