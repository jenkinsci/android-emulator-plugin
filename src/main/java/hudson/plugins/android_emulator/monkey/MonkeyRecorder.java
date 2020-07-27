package hudson.plugins.android_emulator.monkey;

import static hudson.plugins.android_emulator.AndroidEmulator.log;
import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.android_emulator.BuildNodeUnavailableException;
import hudson.plugins.android_emulator.Messages;
import hudson.plugins.android_emulator.util.Utils;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

import javax.annotation.Nonnull;

public class MonkeyRecorder extends Recorder {

    /** Default file to read monkey results from. */
    private static final String DEFAULT_INPUT_FILENAME = "monkey.txt";

    /** File to write monkey results to. */
    @Exported
    public final String filename;

    /** Build outcome in case we detect monkey ended prematurely. */
    @Exported
    public final BuildOutcome failureOutcome;

    @DataBoundConstructor
    public MonkeyRecorder(String filename, BuildOutcome failureOutcome) {
        this.filename = Util.fixEmptyAndTrim(filename);
        this.failureOutcome = failureOutcome == null ? BuildOutcome.UNSTABLE : failureOutcome;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        // Don't analyse anything if the build failed
        final Result buildResult = build.getResult();
        if (buildResult != null && buildResult.isWorseThan(Result.UNSTABLE)) {
            return true;
        }

        // Determine input filename
        String inputFile;
        if (filename == null) {
            inputFile = DEFAULT_INPUT_FILENAME;
        } else {
            inputFile = Utils.expandVariables(build, listener, filename);
        }

        // Read monkey results from file
        final FilePath workspace = build.getWorkspace();
        if (workspace == null) {
            throw new BuildNodeUnavailableException();
        }
        if (inputFile == null) {
            throw new FileNotFoundException();
        }
        final FilePath monkeyFile = workspace.child(inputFile);
        String monkeyOutput = null;
        try {
            monkeyOutput = monkeyFile.readToString();
        } catch (IOException e) {
            log(listener.getLogger(), Messages.NO_MONKEY_OUTPUT(monkeyFile));
        }

        // Parse output and apply it to the build
        PrintStream logger = listener.getLogger();
        MonkeyAction result = parseMonkeyOutput(build, logger, monkeyOutput, failureOutcome);
        build.addAction(result);

        return true;
    }


    static MonkeyAction parseMonkeyOutput(AbstractBuild<?, ?> build, PrintStream logger,
            String monkeyOutput, BuildOutcome failureOutcome) {
        // No input, no output
        if (monkeyOutput == null) {
            return new MonkeyAction(MonkeyResult.NothingToParse);
        }

        // If we don't recognise any outcomes, then say so
        MonkeyResult result = MonkeyResult.UnrecognisedFormat;

        // Extract common data
        int totalEventCount = 0;
        Matcher matcher = Pattern.compile(":Monkey: seed=-?\\d+ count=(\\d+)").matcher(monkeyOutput);
        if (matcher.find()) {
            totalEventCount = Integer.parseInt(matcher.group(1));
        }

        // Determine outcome
        int eventsCompleted = 0;
        if (monkeyOutput.contains("// Monkey finished")) {
            result = MonkeyResult.Success;
            eventsCompleted = totalEventCount;
        } else {
            // If it didn't finish, assume failure
            matcher = Pattern.compile("Events injected: (\\d+)").matcher(monkeyOutput);
            if (matcher.find()) {
                eventsCompleted = Integer.parseInt(matcher.group(1));
            }

            // Determine failure type
            matcher = Pattern.compile("// (CRASH|NOT RESPONDING)").matcher(monkeyOutput);
            if (matcher.find()) {
                String reason = matcher.group(1);
                if ("CRASH".equals(reason)) {
                    result = MonkeyResult.Crash;
                } else if ("NOT RESPONDING".equals(reason)) {
                    result = MonkeyResult.AppNotResponding;
                }
            }

            // Set configured build result
            if (failureOutcome == BuildOutcome.IGNORE) {
                log(logger, Messages.MONKEY_IGNORING_RESULT());
            } else {
                log(logger, Messages.MONKEY_SETTING_RESULT(failureOutcome.name()));
                build.setResult(Result.fromString(failureOutcome.name()));
            }
        }

        return new MonkeyAction(result, eventsCompleted, totalEventCount);
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        @Nonnull
        public String getDisplayName() {
            return Messages.PUBLISH_MONKEY_OUTPUT();
        }

        @Override
        public String getHelpFile() {
            return Functions.getResourcePath() + "/plugin/android-emulator/help-publishMonkeyOutput.html";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

    }

}
