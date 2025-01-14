package gg.moonflower.etched.api.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Tracks download progress for any input stream.
 *
 * @author Ocelot
 */
public class ProgressTrackingInputStream extends InputStream {

    private final InputStream parent;
    private final long size;
    private final DownloadProgressListener listener;
    private int read;

    public ProgressTrackingInputStream(InputStream parent, long size, DownloadProgressListener listener) {
        this.parent = parent;
        this.size = size;
        this.listener = listener;
        this.listener.progressStartDownload(size / 1024.0F / 1024.0F);
    }

    @Override
    public int read() throws IOException {
        int result = this.parent.read();
        if (result != -1) {
            this.read++;
            this.listener.progressStage((float) this.read / (float) this.size);
        }
        return result;
    }

    @Override
    public int available() throws IOException {
        return this.parent.available();
    }

    @Override
    public void close() throws IOException {
        this.parent.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        this.parent.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        this.parent.reset();
    }

    @Override
    public boolean markSupported() {
        return this.parent.markSupported();
    }
}
