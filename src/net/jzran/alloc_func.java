package net.jzran;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;


/**
 * Created on: 27.03.2010 19:33:56
 */
interface alloc_func extends Callback {
    Pointer invoke(Pointer opaque, int items, int size);
}
