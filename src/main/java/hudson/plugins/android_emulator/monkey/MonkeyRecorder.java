package hudson.plugins.android_emulator.monkey;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.android_emulator.AndroidEmulator;
import hudson.plugins.android_emulator.Messages;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.stapler.DataBoundConstructor;

public class MonkeyRecorder extends Recorder {

    /** Default file to read monkey results from. */
    private static final String INPUT_FILENAME = "monkey.txt";

    @DataBoundConstructor
    public MonkeyRecorder() {
        // Nothing to configure, yet
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        // Don't analyse anything if the build failed
        if (build.getResult().isWorseThan(Result.UNSTABLE)) {
            return true;
        }

        // Read monkey results from file
        FilePath monkeyFile = build.getWorkspace().child(INPUT_FILENAME);
        String monkeyOutput = null;
        try {
            monkeyOutput = monkeyFile.readToString();
        } catch (IOException e) {
            AndroidEmulator.log(listener.getLogger(), Messages.NO_MONKEY_OUTPUT(monkeyFile));
        }

        // Parse output and apply it to the build
        MonkeyAction result = parseMonkeyOutput(build, monkeyOutput);
        build.addAction(result);

        return true;
    }

    private MonkeyAction parseMonkeyOutput(AbstractBuild<?, ?> build, String monkeyOutput) {
        // No input, no output
        if (monkeyOutput == null) {
            return new MonkeyAction(MonkeyResult.NothingToParse);
        }

        // If we don't recognise any outcomes, then say so
        MonkeyResult result = MonkeyResult.UnrecognisedFormat;

        // Extract common data
        int totalEventCount = 0;
        Matcher matcher = Pattern.compile(":Monkey: seed=\\d+ count=(\\d+)").matcher(monkeyOutput);
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

            // TODO: Make this configurable
            build.setResult(Result.UNSTABLE);
        }

        return new MonkeyAction(result, eventsCompleted, totalEventCount);
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public String getDisplayName() {
            return Messages.PUBLISH_MONKEY_OUTPUT();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/android-emulator/help-publishMonkeyOutput.html";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

    }

}
