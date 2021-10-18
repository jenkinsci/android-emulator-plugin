package jenkins.plugin.android.emulator.sdk.pipeline;

import java.util.Set;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.collect.ImmutableSet;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import jenkins.plugin.android.emulator.sdk.cli.AVDManagerCLIBuilder;
import jenkins.plugin.android.emulator.sdk.cli.CLICommand;
import jenkins.plugin.android.emulator.sdk.home.HomeLocator;
import jenkins.plugin.android.emulator.tools.AndroidSDKInstallation;

public class AVDManagerStep extends AbstractCLIStep {
    private class AVDManagerStepExecution extends AbstractCLIStepExecution {
        private static final long serialVersionUID = 1L;

        protected AVDManagerStepExecution(StepContext context) {
            super(emulatorTool, homeLocationStrategy, context);
        }

        @Override
        protected Void doRun(AndroidSDKInstallation sdk, TaskListener listener, EnvVars env) throws Exception {
            FilePath avdManager = sdk.getToolLocator().getAVDManager(getContext().get(Launcher.class));

            String[] argumentsExp = env.expand(arguments.replaceAll("[\t\r\n]+", " ")).split("\\s+");
            CLICommand<Void> cli = AVDManagerCLIBuilder.with(avdManager) //
                    .arguments(argumentsExp) //
                    .withEnv(env);
            return quiet ? cli.execute() : cli.execute(listener);
        }

    }

    private static final long serialVersionUID = -9142762434729619710L;

    @DataBoundConstructor
    public AVDManagerStep(@Nonnull String emulatorTool, @Nonnull HomeLocator homeLocationStrategy, @Nonnull String arguments) {
        super(emulatorTool, homeLocationStrategy, arguments);
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new AVDManagerStepExecution(context);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {
        @Override
        public String getFunctionName() {
            return "avdmanager";
        }

        @Override
        public String getDisplayName() {
            return Messages.AVDManagerStep_displayName();
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(FilePath.class, TaskListener.class, Launcher.class, EnvVars.class);
        }
    }
}
