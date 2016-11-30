package hudson.plugins.android_emulator.emulator;

import hudson.plugins.android_emulator.util.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentLinkedQueue;

class EmulatorCommandSender extends Thread implements ResourceWithFinish {

    private static int senderIndex = 0;

    private final OutputStream outputStream;
    private PrintWriter printWriter;
    private SendCallback sendCallback = SendCallback.STUB;
    private final ConcurrentLinkedQueue<String> cmdQueue = new ConcurrentLinkedQueue<String>();
    private boolean running = true;

    EmulatorCommandSender(@Nonnull OutputStream outputStream) {
        this.outputStream = outputStream;
        setName("EmulatorCommandSender " + senderIndex++);
    }

    void setSendCallback(@Nullable SendCallback callback) {
        if (callback == null) {
            this.sendCallback = SendCallback.STUB;
        } else {
            this.sendCallback = callback;
        }
    }

    void send(@Nonnull String command) {
        if (!isRunning()) {
            throw new IllegalStateException("Attempted to send command when sender is already stopped");
        }
        cmdQueue.add(command);
    }

    @Override
    public void run() {
        printWriter = new PrintWriter(outputStream);
        try {
            while (running) {
                String cmd = cmdQueue.poll();
                if (cmd != null) {
                    sendCommandIfWriterExists(printWriter, cmd);
                }
                waitFor(10);
            }
        } catch (IOException ignored) {
        } finally {
            finish();
        }
    }

    private void sendCommandIfWriterExists(@Nullable PrintWriter writer, final @Nonnull String cmd) throws IOException {
        String cmdToSend = cmd;
        if (!cmdToSend.endsWith("\r\n")) {
            cmdToSend = cmdToSend + "\r\n";
        }
        if (writer != null) {
            sendCommand(writer, cmdToSend);
            waitFor(50);
        }
    }

    private void sendCommand(@Nonnull PrintWriter printWriter, @Nonnull String command) throws IOException {
        printWriter.write(command);
        printWriter.flush();
        if (printWriter.checkError()) {
            throw new IOException("Failed to send command " + command);
        }
        sendCallback.onCommandSent(command);
    }

    private void waitFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public boolean isFinished() {
        return !running;
    }

    @Override
    public void finish() {
        Utils.closeStream(printWriter);
        Utils.closeStream(outputStream);
        running = false;
    }

    @Nonnull
    @Override
    public String getResourceName() {
        return "Emulator command sender";
    }

    boolean isRunning() {
        return running;
    }

    interface SendCallback {
        void onCommandSent(@Nonnull String command);

        SendCallback STUB = new SendCallback() {
            @Override
            public void onCommandSent(@Nonnull String command) {

            }
        };
    }
}
