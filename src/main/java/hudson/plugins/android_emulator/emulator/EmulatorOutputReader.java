package hudson.plugins.android_emulator.emulator;

import hudson.plugins.android_emulator.util.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class EmulatorOutputReader extends Thread implements ResourceWithFinish {

    private static int readerIndex = 0;
    private final InputStream inputStream;
    private ReaderCallback callback = ReaderCallback.STUB;
    private BufferedReader reader;
    private boolean running = true;
    private ReadFinishedListener readFinishedListener = ReadFinishedListener.STUB;

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

    void setReadFinishedListener(@Nullable ReadFinishedListener readFinishedListener) {
        if (readFinishedListener == null) {
            this.readFinishedListener = ReadFinishedListener.STUB;
        } else {
            this.readFinishedListener = readFinishedListener;
        }
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream));
            while (running) {
                if (reader.ready()) {
                    String line = reader.readLine();
                    processLine(line);
                }
                try {
                    sleep(10);
                } catch (InterruptedException ignored) {
                    // ignored
                }
            }
        } catch (IOException ignored) {

        } finally {
            finish();
        }
    }

    private void processLine(@Nonnull String line) {
        callback.onLineRead(line);
    }

    @Override
    public boolean isFinished() {
        return !running;
    }

    @Override
    public void finish() {
        Utils.closeStream(inputStream);
        Utils.closeStream(reader);
        running = false;
        readFinishedListener.onReadFinished(this);
    }

    @Nonnull
    @Override
    public String getResourceName() {
        return "Emulator output reader";
    }

    public boolean isRunning() {
        return running;
    }

}
