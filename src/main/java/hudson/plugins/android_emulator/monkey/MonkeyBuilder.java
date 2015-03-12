package hudson.plugins.android_emulator.monkey;

import hudson.Extension;
import hudson.Functions;
import hudson.Launcher;
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
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static hudson.Util.fixEmptyAndTrim;
import static hudson.Util.fixNull;
import static hudson.plugins.android_emulator.AndroidEmulator.log;

public class MonkeyBuilder extends AbstractBuilder {

    /** File to write monkey results to if none specified. */
    private static final String DEFAULT_OUTPUT_FILENAME = "monkey.txt";

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

    /** Categories to restrict the monkey to. */
    @Exported
    public final String categories;

    /** Extra command line parameters to pass to monkey. */
    @Exported
    public final String extraParameters;

    @DataBoundConstructor
    public MonkeyBuilder(String filename, String packageId, Integer eventCount, Integer throttleMs, String seed,
            String categories, String extraParameters) {
        this.filename = fixEmptyAndTrim(filename);
        this.packageId = fixEmptyAndTrim(packageId);
        this.eventCount = eventCount == null ? 0 : Math.abs(eventCount);
        this.throttleMs = throttleMs == null ? 0 : Math.abs(throttleMs);
        this.seed = fixEmptyAndTrim(seed) == null ? null : seed.trim().toLowerCase(Locale.ROOT);
        this.categories = fixEmptyAndTrim(categories);
        this.extraParameters = fixEmptyAndTrim(extraParameters);
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

        // Build list of arguments for monkey
        StringBuilder cmdArgs = new StringBuilder();
        List<String> packageNamesLog = new ArrayList<String>();
        final String expandedPackageId = Utils.expandVariables(build, listener, packageId);
        addArguments(expandedPackageId, "-p", cmdArgs, packageNamesLog);

        List<String> categoryNamesLog = new ArrayList<String>();
        final String expandedCategory = Utils.expandVariables(build, listener, categories);
        addArguments(expandedCategory, "-c", cmdArgs, categoryNamesLog);

        final long seedValue = parseSeed(seed);
        final String expandedExtraParams = fixNull(Utils.expandVariables(build, listener, extraParameters));
        final String deviceIdentifier = getDeviceIdentifier(build, listener);
        String args = String.format("%s shell monkey -v -v -s %d --throttle %d %s %s %d", deviceIdentifier, seedValue,
                throttleMs, cmdArgs.toString(), expandedExtraParams, eventCount);

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
            log(logger, Messages.STARTING_MONKEY(packageNamesLog, eventCount, seedValue, categoryNamesLog));
            Utils.runAndroidTool(launcher, build.getEnvironment(TaskListener.NULL), monkeyOutput,
                    logger, androidSdk, Tool.ADB, args, null);
        } finally {
            if (monkeyOutput != null) {
                monkeyOutput.close();
            }
        }

        return true;
    }

    /**
     * Turns a comma-separated string into a list of command line arguments.
     *
     * @param parameters The user-provided argument string, after variable expansion.
     * @param flag The command line flag to insert between each valid argument.
     * @param argsOut StringBuilder to append valid arguments to.
     * @param names List to append valid arguments to for later logging.
     */
    private static void addArguments(String parameters, String flag, StringBuilder argsOut, List<String> names) {
        // No input, nothing to do
        if (parameters == null) {
            return;
        }

        for (String arg : parameters.split(",")) {
            // Ignore empty values, or variables that failed to be expanded
            arg = fixEmptyAndTrim(arg);
            if (arg == null || arg.contains("$")) {
                continue;
            }

            // Append flag and the given argument to the list
            argsOut.append(' ');
            argsOut.append(flag);
            argsOut.append(' ');
            argsOut.append(arg);
            names.add(arg);
        }
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
