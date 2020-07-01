package hudson.plugins.android_emulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jenkins.security.MasterToSlaveCallable;

/**
 * Task that will wait, up to a certain timeout, for an inbound connection from the emulator,
 * informing us on which port it is running.
 */
public final class ReceiveEmulatorPortTask
        extends MasterToSlaveCallable<Integer, InterruptedException> {

    private static final long serialVersionUID = 1L;

    private final int port;
    private final int timeout;

    /**
     * @param port The local TCP port to listen on.
     * @param timeout How many milliseconds to wait for an emulator connection before giving up.
     */
    public ReceiveEmulatorPortTask(int port, int timeout) {
        this.port = port;
        this.timeout = timeout;
    }

    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    public Integer call() throws InterruptedException {
        // TODO: Find a better way to allow the build to be interrupted.
        // ServerSocket#accept() blocks and cannot be interrupted, which means that any
        // attempts to stop the build will fail.  The best we can do here is to set the
        // SO_TIMEOUT, so at least if an emulator fails to start, we won't wait here forever
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setSoTimeout(timeout);

            // Wait for the emulator to connect to us
            Socket completed = socket.accept();

            // Parse and return the port number the emulator sent us
            try (InputStream is = completed.getInputStream()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                return Integer.parseInt(reader.readLine());
            } catch (NumberFormatException ignore) {
            }
        } catch (IOException ignore) {
        }

        // Timed out
        return -1;
    }
}