package hudson.plugins.android_emulator.emulator;

import hudson.plugins.android_emulator.util.Utils;
import jenkins.security.MasterToSlaveCallable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.net.Socket;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EmulatorCommandTask extends MasterToSlaveCallable<EmulatorCommandResult, IOException>
        implements EmulatorOutputReader.ReaderCallback, EmulatorCommandSender.SendCallback, EmulatorOutputMatcher.MatcherCallback {

    private static final Pattern TOKEN_FILE_PATH_REGEXP = Pattern.compile("[^<]*<auth_token> in[^']*'([^']+)'",
            Pattern.DOTALL | Pattern.MULTILINE);
    private static final Pattern OK_REGEXP = Pattern.compile(".*OK.*");

    private static final String CMD_QUIT = "quit";
    private static final String CMD_AUTH_FORMAT = "auth %s";

    private static final String IP_ADDR_LOCALHOST = "127.0.0.1";
    private static final long TIMEOUT_MS = 5000L;

    private final int port;
    private final String command;

    private transient EmulatorOutputReader reader;
    private transient EmulatorCommandSender sender;
    private transient EmulatorCommandResult result;
    private transient EmulatorOutputMatcher outputMatcher;
    private transient String authTokenPath;
    private transient boolean initSuccessful;
    private transient long startedAt;

    @SuppressWarnings("hiding")
    public EmulatorCommandTask(int port, String command) {
        this.port = port;
        this.command = command;
    }

    @SuppressWarnings("null")
    public EmulatorCommandResult call() throws IOException {
        startedAt = now();
        Socket socket = null;
        try {
            result = new EmulatorCommandResult();
            setupMatcher();
            socket = new Socket(IP_ADDR_LOCALHOST, port);
            setupConnection(socket);
            waitForInitialBanner();
            if (authTokenPath != null) {
                String token = readAuthToken(authTokenPath);
                if (token != null) {
                    sender.send(String.format(Locale.US, CMD_AUTH_FORMAT, token));
                }
            }
            sender.send(command);
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
            sender.send(CMD_QUIT);
            result.setSuccess(true);
            waitForReaderClosing();
        } catch (Exception e) {
            result.appendOutput(e.getMessage() + "\r\n");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            result.appendOutput(sw.toString());
        } finally {
            cleanUp(socket);
        }
        return result;
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
        reader.start();
        sender = new EmulatorCommandSender(socket.getOutputStream());
        sender.setSendCallback(this);
        sender.start();
    }

    private void waitForInitialBanner() throws TimeoutException {
        while (!initSuccessful) {
            try {
                checkForTimeout();
                Thread.sleep(10);
            } catch (InterruptedException ignored) {

            }
        }
    }

    private void waitForReaderClosing() throws TimeoutException {
        while (reader.isRunning()) {
            try {
                checkForTimeout();
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void cleanUp(@Nullable Socket socket) {
        if (reader != null) {
            reader.finish();
        }
        if (sender != null) {
            sender.finish();
        }
        Utils.closeStream(socket);
    }

    private long now() {
        return System.currentTimeMillis();
    }

    private void checkForTimeout() throws TimeoutException {
        if (now() - startedAt > TIMEOUT_MS) {
            throw new TimeoutException();
        }
    }

    private static final long serialVersionUID = 1L;

    @Nullable
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
        result.appendOutput("[EMULATOR] << " + line + "\r\n");
        outputMatcher.onLineRead(line);
    }

    @Override
    public void onMatchFound(@Nonnull Pattern pattern, @Nonnull Matcher matcher) {
        if (pattern == TOKEN_FILE_PATH_REGEXP) {
            if (matcher.groupCount() > 0) {
                authTokenPath = matcher.group(1);
            }
        } else if (pattern == OK_REGEXP) {
            this.initSuccessful = true;
        }
    }

    @Override
    public void onCommandSent(@Nonnull String command) {
        String commandToPrint = command;
        if (commandToPrint.startsWith("auth")) {
            commandToPrint = "auth ****\r\n";
        }
        result.appendOutput("[EMULATOR] >> " + commandToPrint);
    }

    private static class TimeoutException extends Exception {

    }
}
