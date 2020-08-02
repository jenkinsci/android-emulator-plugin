package hudson.plugins.android_emulator.snapshot;

import static hudson.plugins.android_emulator.AndroidEmulator.log;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.android_emulator.builder.AbstractBuilder;
import hudson.plugins.android_emulator.util.Utils;

import java.io.IOException;
import java.io.PrintStream;

import org.kohsuke.stapler.export.Exported;

public abstract class AbstractSnapshotBuilder extends AbstractBuilder {

    private static final int DEFAULT_TIMEOUT_MS = 2 * 60 * 1000;

    /** Name of the snapshot involved. */
    @Exported
    public final String name;

    protected AbstractSnapshotBuilder(String name) {
        this.name = name;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        final PrintStream logger = listener.getLogger();

        // Expand snapshot name
        final String snapshotName = Utils.expandVariables(build, listener, name);

        // Get AVD port
        final int port = getDeviceTelnetPort(build, listener);

        // Send telnet command: "avd snapshot $action $name"
        log(logger, getLogMessage(snapshotName, port));
        String command = String.format("avd snapshot %s %s", getSnapshotAction(), snapshotName);
        return Utils.sendEmulatorCommand(launcher, logger, port, command, getCommandTimeout());
    }

    /* Retrieves the time in which the snapshot command should complete, in milliseconds. */
    protected int getCommandTimeout() {
        return DEFAULT_TIMEOUT_MS;
    }

    /* Retrieves the snapshot action to execute (i.e. "load" or "save"). */
    protected abstract String getSnapshotAction();

    /* Retrieves the log message to print when performing the snapshot action. */
    protected abstract String getLogMessage(String snapshotName, int avdPort);

}
