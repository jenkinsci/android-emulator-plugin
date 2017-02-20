package hudson.plugins.android_emulator.util;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class OutputConsumer extends OutputStream {

    private static final char NEWLINE = '\n';
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();


    @Override
    public void write(int b) throws IOException {
        buffer.write(b);
        if (b == NEWLINE) {
            dispatchLineReadFinished();
            clear();
        }
    }

    private void dispatchLineReadFinished() {
        String line = buffer.toString();
        onLineRead(line);
    }

    public abstract void onLineRead(@Nonnull String line);

    public void clear() {
        buffer.reset();
    }
}
