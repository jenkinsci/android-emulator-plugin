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
import hudson.plugins.android_emulator.Constants;
import jenkins.model.Jenkins;
import jenkins.plugin.android.emulator.sdk.cli.CLICommand;
import jenkins.plugin.android.emulator.sdk.cli.SDKManagerCLIBuilder;
import jenkins.plugin.android.emulator.sdk.home.HomeLocator;
import jenkins.plugin.android.emulator.tools.AndroidSDKInstallation;

public class SDKManagerStep extends AbstractCLIStep {
    private class SDKManagerStepExecution extends AbstractCLIStepExecution {
        private static final long serialVersionUID = 1L;

        protected SDKManagerStepExecution(StepContext context) {
            super(emulatorTool, homeLocationStrategy, context);
        }

        @Override
        protected Void doRun(AndroidSDKInstallation sdk, TaskListener listener, EnvVars env) throws Exception {
            FilePath sdkManager = sdk.getToolLocator().getSDKManager(getContext().get(Launcher.class));

            String[] argumentsExp = env.expand(arguments.replaceAll("[\t\r\n]+", " ")).split("\\s+");
            CLICommand<Void> cli = SDKManagerCLIBuilder.with(sdkManager) //
                    .proxy(Jenkins.get().proxy) //
                    .sdkRoot(env.get(Constants.ENV_VAR_ANDROID_SDK_ROOT)) //
                    .arguments(argumentsExp) //
                    .withEnv(env);
            return quiet ? cli.execute() : cli.execute(listener);
        }

    }

    private static final long serialVersionUID = -1557453962312014910L;

    @DataBoundConstructor
    public SDKManagerStep(@Nonnull String emulatorTool, @Nonnull HomeLocator homeLocationStrategy, @Nonnull String arguments) {
        super(emulatorTool, homeLocationStrategy, arguments);
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new SDKManagerStepExecution(context);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {
        @Override
        public String getFunctionName() {
            return "sdkmanager";
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
