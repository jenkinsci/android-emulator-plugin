package hudson.plugins.android_emulator.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Reads to contents of an InputStream in an background thread,
 * useful to retrieve OutputStream/ErrorStream of a Process, and
 * allows retrieval as String representing a line of the output.
 */
public class StdoutReader
{
    /**
     * InputStream to read data from
     */
    final InputStream stdout;

    /**
     * Buffered data of the InputStream
     */
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    /**
     * Buffered data of the InputStream converted to Strings and split by line
     */
    final ConcurrentLinkedQueue<String> lineStore = new ConcurrentLinkedQueue<String>();

    /**
     * Set if inputStream gets closed, used to determine
     * what do return if data is accessed and no content
     * available.
     */
    final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates a new StdoutReader starting to read the data
     * immediately of the given InputStream in an own thread.
     * @param inputStream InputStream to read the data from
     */
    @SuppressFBWarnings("SC_START_IN_CTOR")
    public StdoutReader(final InputStream inputStream) {
        this.stdout = inputStream;
        if (this.stdout != null) {
            runner.start();
        } else {
            closed.set(true);
        }
    }

    final Thread runner = new Thread(new Runnable() {
        /**
         * Read stream into buffer, if all currently available data
         * is read or a newline is detected, transfer the stored
         * data to the lineStore.
         */
        @Override
        public void run() {
            try {
                boolean waitedForInputOnLastCheck = false;
                while (true) {
                    // if no output is available, wait once, before try to read
                    // and transfer current buffer
                    if (!waitedForInputOnLastCheck && stdout.available() <= 0) {
                        waitedForInputOnLastCheck = true;
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                        continue;
                    }
                    waitedForInputOnLastCheck = false;

                    final byte[] buf = new byte[8192];
                    final int len = stdout.read(buf);
                    if (len > 0) {
                        buffer.write(buf, 0, len);
                    }

                    // if no new output was read or output contains
                    // newline, transfer buffered data to line store
                    if (len == 0 || containsNewline(buf)) {
                        transferByteBufferToLineStore();
                    }
                }
            } catch (IOException e) {
                // Input stream most likely close, so we are done here 
            } finally {
                close();
            }
        }
    });

    /**
     * Close all streams, transfer remaining data from the buffer
     * to the lineStore, so they can still be fetched
     */
    private void close() {
        transferByteBufferToLineStore();

        if (closed.getAndSet(true)) {
            return;
        }

        try {
            stdout.close();
        } catch (IOException ioex) {
        }

        try {
            buffer.close();
        } catch (IOException ioex) {
        }
    }

    /**
     * Checks if the given byte array contains a newline character ('\r' or '\n')
     * @param buffer the byte array to parse
     * @return true if a newline character ('\r' or '\n') was found, false otherwise
     */
    private boolean containsNewline(final byte[] buffer) {
        for (int pos = 0; pos < buffer.length; pos++) {
            if (buffer[pos] == '\r' || buffer[pos] == '\n') {
                return true;
            }
        }
        return false;
    }

    /**
     * Read the currently buffered data and interpret as UTF-8 String,
     * split String by line-break/line-feed and append it to the read lines.
     */
    private void transferByteBufferToLineStore() {
        if (buffer.size() <= 0) {
            return;
        }

        String streamcontent = "<OUTPUT GARBLED: UNSUPPORTED ENCODING>";
        try {
            streamcontent = new String(buffer.toByteArray(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
        buffer.reset();
        lineStore.addAll(Arrays.asList(streamcontent.split("[\r\n]+")));
    }

    /**
     * Retrieve the next line from the bufered InputStream.
     * @return next line of InputStream, if no new data is available empty or null if InputStream is closed.
     */
    public String readLine() {
        if (lineStore.isEmpty()) {
            return (closed.get()) ? null : "";
        }
        return lineStore.poll();
    }

    /**
     * Retrieve the whole content currently buffered, empty lines are suppressed.
     * @return all read lines, if no new data is available empty or null if InputStream is closed.
     */
    public String readContent() {
        if (lineStore.isEmpty() && closed.get()) {
            return null;
        }

        final StringBuilder content = new StringBuilder();
        while (!lineStore.isEmpty()) {
            final String line = lineStore.poll();
            if (line != null && !line.isEmpty()) {
                content.append(line).append("\r\n");
            }
        }

        return content.toString();
    }
}
