package net.jzran;

public interface ProgressListener<T> {
    /**
     * Implementor! Return false if you wish to cancel
     * the thing whose progress you're looking at.
     */
    boolean reportProgress(T progress);
}
