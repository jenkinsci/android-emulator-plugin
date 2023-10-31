package jenkins.plugin.android.emulator.sdk.pipeline;

import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.collect.ImmutableSet;

import edu.umd.cs.findbugs.annotations.NonNull;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import jenkins.plugin.android.emulator.sdk.cli.ADBCLIBuilder;
import jenkins.plugin.android.emulator.sdk.cli.CLICommand;
import jenkins.plugin.android.emulator.sdk.home.HomeLocator;
import jenkins.plugin.android.emulator.tools.AndroidSDKInstallation;

public class ADBStep extends AbstractCLIStep {
    private class ADBStepExecution extends AbstractCLIStepExecution {
        private static final long serialVersionUID = 1L;

        protected ADBStepExecution(StepContext context) {
            super(emulatorTool, homeLocationStrategy, context);
        }

        @Override
        protected Void doRun(AndroidSDKInstallation sdk, TaskListener listener, EnvVars env) throws Exception {
            FilePath adb = sdk.getToolLocator().getADB(getContext().get(Launcher.class));

            String[] argumentsExp = env.expand(arguments.replaceAll("[\t\r\n]+", " ")).split("\\s+");
            CLICommand<Void> cli = ADBCLIBuilder.with(adb) //
                    .arguments(argumentsExp) //
                    .withEnv(env);
            return quiet ? cli.execute() : cli.execute(listener);
        }

    }

    private static final long serialVersionUID = -1557453962312014910L;

    @DataBoundConstructor
    public ADBStep(@NonNull String emulatorTool, @NonNull HomeLocator homeLocationStrategy, @NonNull String arguments) {
        super(emulatorTool, homeLocationStrategy, arguments);
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new ADBStepExecution(context);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {
        @Override
        public String getFunctionName() {
            return "adb";
        }

        @Override
        public String getDisplayName() {
            return Messages.ADBStep_displayName();
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(FilePath.class, TaskListener.class, Launcher.class, EnvVars.class);
        }
    }
}
