package hudson.plugins.android_emulator.emulator;

import hudson.plugins.android_emulator.util.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

class EmulatorOutputReader extends Thread {

    private static int readerIndex = 0;
    private final InputStream inputStream;
    private ReaderCallback callback = ReaderCallback.STUB;
    private BufferedReader reader;
    private boolean running;

    EmulatorOutputReader(@Nonnull InputStream inputStream) {
        super();
        setName("EmulatorOutputReader " + readerIndex++);
        this.inputStream = inputStream;
    }

    void setReaderCallback(@Nullable ReaderCallback callback) {
        if (callback == null) {
            this.callback = ReaderCallback.STUB;
        } else {
            this.callback = callback;
        }
    }

    @Override
    public void run() {
        running = true;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                processLine(line);
            }
        } catch (Exception ignored) {

        } finally {
            running = false;
            finish();
        }
    }

    private void processLine(@Nonnull String line) {
        callback.onLineRead(line);
    }

    void finish() {
        Utils.closeStream(inputStream);
        Utils.closeStream(reader);
    }

    public boolean isRunning() {
        return running;
    }

    interface ReaderCallback {
        void onLineRead(@Nonnull String line);

        ReaderCallback STUB = new ReaderCallback() {
            @Override
            public void onLineRead(@Nonnull String line) {

            }
        };
    }
}
