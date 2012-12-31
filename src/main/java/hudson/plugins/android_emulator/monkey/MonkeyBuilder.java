package hudson.plugins.android_emulator.monkey;

import static hudson.plugins.android_emulator.AndroidEmulator.log;
import hudson.Extension;
import hudson.Functions;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.plugins.android_emulator.Messages;
import hudson.plugins.android_emulator.builder.AbstractBuilder;
import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.sdk.Tool;
import hudson.plugins.android_emulator.util.Utils;
import hudson.tasks.Builder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Random;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

public class MonkeyBuilder extends AbstractBuilder {

    /** File to write monkey results to if none specified. */
    private static final String DEFAULT_OUTPUT_FILENAME = "monkey.txt";

    /** Number of events to execute if nothing was specified. */
    private static final int DEFAULT_EVENT_COUNT = 100;

    /** File to write monkey results to. */
    @Exported
    public final String filename;

    /** Package ID to restrict the monkey to. */
    @Exported
    public final String packageId;

    /** How many events to perform. */
    @Exported
    public final int eventCount;

    /** How many milliseconds between each event. */
    @Exported
    public final int throttleMs;

    /** Seed value for the random number generator. Number, "random", or "timestamp". */
    @Exported
    public final String seed;

    @DataBoundConstructor
    public MonkeyBuilder(String filename, String packageId, Integer eventCount, Integer throttleMs, String seed) {
        this.filename = Util.fixEmptyAndTrim(filename);
        this.packageId = packageId;
        this.eventCount = eventCount == null ? DEFAULT_EVENT_COUNT : Math.abs(eventCount);
        this.throttleMs = throttleMs == null ? 0 : Math.abs(throttleMs);
        this.seed = seed == null ? "" : seed.trim().toLowerCase();
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        final PrintStream logger = listener.getLogger();

        // Discover Android SDK
        AndroidSdk androidSdk = getAndroidSdk(build, launcher, listener);
        if (androidSdk == null) {
            return false;
        }

        // Set up arguments to adb
        final String deviceIdentifier = getDeviceIdentifier(build, listener);
        final String expandedPackageId = Utils.expandVariables(build, listener, this.packageId);
        final long seedValue = parseSeed(seed);
        String args = String.format("%s shell monkey -v -v -p %s -s %d --throttle %d %d",
                deviceIdentifier, expandedPackageId, seedValue, throttleMs, eventCount);

        // Determine output filename
        String outputFile;
        if (filename == null) {
            outputFile = DEFAULT_OUTPUT_FILENAME;
        } else {
            outputFile = Utils.expandVariables(build, listener, filename);
        }

        // Start monkeying around
        OutputStream monkeyOutput = build.getWorkspace().child(outputFile).write();
        try {
            log(logger, Messages.STARTING_MONKEY(expandedPackageId, eventCount, seedValue));
            Utils.runAndroidTool(launcher, build.getEnvironment(TaskListener.NULL), monkeyOutput,
                    logger, androidSdk, Tool.ADB, args, null);
        } finally {
            if (monkeyOutput != null) {
                monkeyOutput.close();
            }
        }

        return true;
    }

    private static long parseSeed(String seed) {
        long seedValue;
        if ("random".equals(seed)) {
            seedValue = new Random().nextLong();
        } else if ("timestamp".equals(seed)) {
            seedValue = System.currentTimeMillis();
        } else if (seed != null) {
            try {
                seedValue = Long.parseLong(seed);
            } catch (NumberFormatException e) {
                seedValue = 0;
            }
        } else {
            seedValue = 0;
        }
        return seedValue;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Builder> implements Serializable {

        private static final long serialVersionUID = 1L;

        public DescriptorImpl() {
            super(MonkeyBuilder.class);
            load();
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) {
            save();
            return true;
        }

        @Override
        public String getHelpFile() {
            return Functions.getResourcePath() + "/plugin/android-emulator/help-runMonkey.html";
        }

        @Override
        public String getDisplayName() {
            return Messages.RUN_MONKEY();
        }

    }

}
