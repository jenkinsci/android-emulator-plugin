package hudson.plugins.android_emulator.emulator;

import hudson.plugins.android_emulator.util.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.OutputStream;
import java.io.PrintWriter;

class EmulatorCommandSender extends Thread {

    private static int senderIndex = 0;

    private final OutputStream outputStream;
    private PrintWriter writer;
    private SendCallback sendCallback = SendCallback.STUB;

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

    @Override
    public void run() {
        writer = new PrintWriter(outputStream);
    }

    void finish() {
        Utils.closeStream(writer);
        Utils.closeStream(outputStream);
    }

    void send(@Nonnull String command) {
        PrintWriter local = writer;
        if (local != null) {
            sendCommand(local, command + "\r\n");
        }
    }

    private void sendCommand(@Nonnull PrintWriter printWriter, @Nonnull String command) {
        printWriter.write(command);
        printWriter.flush();
        sendCallback.onCommandSent(command);
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
