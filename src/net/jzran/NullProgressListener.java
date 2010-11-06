package net.jzran;

public class NullProgressListener implements ProgressListener<Long> {
    public boolean reportProgress(Long progress) {
        return true;
    }
}
