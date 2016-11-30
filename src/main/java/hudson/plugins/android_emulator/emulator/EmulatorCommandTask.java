package hudson.plugins.android_emulator.emulator;

import hudson.plugins.android_emulator.util.Utils;
import jenkins.security.MasterToSlaveCallable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.net.Socket;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EmulatorCommandTask extends MasterToSlaveCallable<EmulatorCommandResult, IOException>
        implements ReaderCallback, EmulatorCommandSender.SendCallback,
        EmulatorOutputMatcher.MatcherCallback, TimeoutConsumer, ReadFinishedListener,
        ResourceWithFinish {

    private static final Pattern TOKEN_FILE_PATH_REGEXP = Pattern.compile("[^<]*<auth_token> in[^']*'([^']+)'",
            Pattern.DOTALL | Pattern.MULTILINE);
    private static final Pattern OK_REGEXP = Pattern.compile(".*OK.*");
    private static final String CMD_QUIT = "quit";
    private static final String CMD_AUTH_FORMAT = "auth %s";
    private static final String IP_ADDR_LOCALHOST = "127.0.0.1";

    private transient EmulatorOutputReader reader;
    private transient EmulatorCommandSender sender;
    private transient EmulatorCommandResult result;
    private transient EmulatorOutputMatcher outputMatcher;
    private transient String authTokenPath;
    private transient EmulatorCommandTimeout timeout;
    private transient Semaphore semaphore;
    private transient boolean running;

    private final int port;
    private final String command;
    private final long timeoutMS;

    @SuppressWarnings("hiding")
    public EmulatorCommandTask(int port, String command, long timeoutMS) {
        this.port = port;
        this.command = command;
        if (timeoutMS <= 0) {
            throw new IllegalArgumentException("Timeout must be > 0");
        }
        this.timeoutMS = timeoutMS;
    }

    @SuppressWarnings("null")
    public EmulatorCommandResult call() throws IOException {
        running = true;
        timeout = new EmulatorCommandTimeout(timeoutMS, this);
        timeout.start();
        semaphore = new Semaphore(1);
        result = new EmulatorCommandResult();
        Socket socket = null;
        try {
            acquireAllPermits();
            setupMatcher();
            socket = new Socket(IP_ADDR_LOCALHOST, port);
            setupConnection(socket);
            sendAuthTokenIfRequested();
            sendRequestedCommand();
            quitConsole();
            result.setSuccess(true);
            waitUntilQuitSent();
        } catch (RunFinishedException e) {
            log("Communication finished " + e.getMessage());
        } catch (Exception e) {
            failWithException(e);
        } finally {
            cleanUp(socket);
        }
        // sanity check
        checkIfAllResourcesFinished(this, reader, sender);
        return result;
    }

    private void acquireAllPermits() {
        // this will make the program wait until first OK is received before sending any command.
        int acquired = semaphore.drainPermits();
        if (acquired != 1) { // sanity check
            throw new IllegalStateException("Invalid number of permits acquired: " + acquired);
        }
    }

    private void setupMatcher() {
        outputMatcher = new EmulatorOutputMatcher();
        outputMatcher.setMatcherCallback(this);
        outputMatcher.reset();
        outputMatcher.registerPattern(TOKEN_FILE_PATH_REGEXP);
        outputMatcher.registerPattern(OK_REGEXP);
    }

    private void setupConnection(@Nonnull Socket socket) throws IOException {
        reader = new EmulatorOutputReader(socket.getInputStream());
        reader.setReaderCallback(this);
        reader.setReadFinishedListener(this);
        reader.start();
        sender = new EmulatorCommandSender(socket.getOutputStream());
        sender.setSendCallback(this);
        sender.start();
    }

    private void sendAuthTokenIfRequested() throws IOException {
        waitUntilCanContinue();
        if (authTokenPath != null) {
            String token = readAuthToken(authTokenPath);
            this.send(String.format(Locale.US, CMD_AUTH_FORMAT, token));
        }
    }

    private void sendRequestedCommand() {
        this.send(this.command);
    }

    private void quitConsole() {
        this.send(CMD_QUIT);
    }

    private void waitUntilQuitSent() {
        // semaphore permit will be released upon receiving confirmation for 'quit' being sent.
        waitUntilCanContinue();
    }

    private void failWithException(@Nonnull Exception e) {
        log(e.toString());
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        log(sw.toString());
        result.setSuccess(false);
    }

    private void send(@Nonnull String command) {
        waitUntilCanContinue();
        if (!semaphore.tryAcquire()) {
            // Only task's main thread should acquire semaphore and permit will be released by read thread.
            // If permit is not available at this point, then most likely there is an error in code which causes other
            // thread to acquire permit.
            throw new IllegalStateException("Failed to acquire semaphore permit.");
        }
        if (running) {
            sender.send(command);
        }
    }

    private void waitUntilCanContinue() {
        while (!canContinue()) {
            waitFor(10);
        }
    }

    private void waitFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    private boolean canContinue() {
        checkForTimeout();
        checkIfResourceFinished(this);
        checkIfResourceFinished(reader);
        checkIfResourceFinished(sender);
        return isSemaphorePermitAvailable();
    }

    private void checkIfResourceFinished(@Nonnull ResourceWithFinish resource) {
        if (resource.isFinished()) {
            throw new RunFinishedException(resource);
        }
    }

    private void checkForTimeout() {
        if (timeout.isTimeoutReached()) {
            failWithException(new TimeoutException());
            result.timedOut = true;
        }
    }

    private boolean isSemaphorePermitAvailable() {
        int permits = semaphore.availablePermits();
        // sanity check. If permits are > 1 then read thread released them more than once.
        // This can lead to situation when send thread is allowed to continue when it shouldn't.
        if (permits > 1) {
            throw new IllegalStateException("Permits count exceeded: " + permits + ". Max should be 1.");
        }
        return permits == 1;
    }

    private void cleanUp(@Nullable Socket socket) {
        running = false;
        if (reader != null) {
            reader.finish();
        }
        if (sender != null) {
            sender.finish();
        }
        timeout.finish();
        Utils.closeStream(socket);
    }

    private static final long serialVersionUID = 1L;

    @Nonnull
    private String readAuthToken(@Nonnull String path) throws IOException {
        FileInputStream in = new FileInputStream(path);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String ret = "";
        String part;
        while ((part = reader.readLine()) != null) {
            ret = ret + part;
        }
        return ret.trim();
    }

    @Override
    public void onLineRead(@Nonnull String line) {
        log("<< " + line);
        outputMatcher.onLineRead(line);
    }

    private void log(@Nonnull String msg) {
        String msgToLog = msg;
        if (!msgToLog.endsWith("\r\n")) {
            msgToLog = msgToLog + "\r\n";
        }
        result.appendOutput("[EMULATOR] ").appendOutput(msgToLog);
    }

    @Override
    public void onMatchFound(@Nonnull EmulatorOutputMatcher emulatorOutputMatcher, @Nonnull Pattern pattern,
                             @Nonnull Matcher matcher) {
        if (pattern == TOKEN_FILE_PATH_REGEXP) {
            if (matcher.groupCount() > 0) {
                authTokenPath = matcher.group(1);
                emulatorOutputMatcher.disablePattern(TOKEN_FILE_PATH_REGEXP);
            }
        } else if (pattern == OK_REGEXP) {
            semaphore.release();
        }
    }

    @Override
    public void onCommandSent(@Nonnull String command) {
        String commandToPrint = maskCommand(command);
        log(">> " + commandToPrint);
        if (command.startsWith(CMD_QUIT)) {
            semaphore.release();
        }
    }

    @Nonnull
    private String maskCommand(@Nonnull String command) {
        String ret;
        if (command.startsWith("auth")) {
            ret = "auth ****";
        } else {
            ret = command;
        }
        return ret;
    }

    @Override
    public void onTimedOut(@Nonnull EmulatorCommandTimeout emulatorCommandTimeout) {
        if (running) {
            finish();
        }
    }

    @Override
    public void onReadFinished(@Nonnull EmulatorOutputReader reader) {
        semaphore.release();
    }

    @Override
    public boolean isFinished() {
        return !running;
    }

    @Override
    public void finish() {
        running = false;
        semaphore.release();
    }

    @Nonnull
    @Override
    public String getResourceName() {
        return "Emulator command task";
    }

    private void checkIfAllResourcesFinished(ResourceWithFinish... resources) {
        for (ResourceWithFinish res : resources) {
            if (res != null && !res.isFinished()) {
                throw new IllegalStateException("Expected resource " + res.getResourceName() + " to be finished");
            }
        }
    }

    private static class RunFinishedException extends RuntimeException {
        private RunFinishedException(@Nonnull ResourceWithFinish resource) {
            super(resource.getResourceName() + " finished running");
        }
    }
}
