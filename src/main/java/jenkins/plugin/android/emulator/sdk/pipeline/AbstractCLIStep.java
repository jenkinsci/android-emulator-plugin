package jenkins.plugin.android.emulator.sdk.pipeline;

import java.io.Serializable;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Util;

import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.plugin.android.emulator.sdk.home.HomeLocator;

public abstract class AbstractCLIStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final String emulatorTool;
    protected final HomeLocator homeLocationStrategy;
    protected final String arguments;
    protected boolean quiet = false;

    public AbstractCLIStep(@NonNull String emulatorTool, @NonNull HomeLocator homeLocationStrategy, @NonNull String arguments) {
        this.emulatorTool = Util.fixEmptyAndTrim(emulatorTool);
        this.homeLocationStrategy = homeLocationStrategy;
        this.arguments = Util.fixEmptyAndTrim(arguments);
    }

    public String getArguments() {
        return arguments;
    }

    public HomeLocator getHomeLocationStrategy() {
        return homeLocationStrategy;
    }

    public String getEmulatorTool() {
        return emulatorTool;
    }

    public boolean isQuiet() {
        return quiet;
    }

    @DataBoundSetter
    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

}