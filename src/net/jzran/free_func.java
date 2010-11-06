package net.jzran;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

interface free_func extends Callback {
    void invoke(Pointer opaque, Pointer address);
}
