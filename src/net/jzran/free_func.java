package net.jzran;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

/**
 * Created on: 27.03.2010 19:35:44
 */
interface free_func extends Callback {
    void invoke(Pointer opaque, Pointer address);
}
