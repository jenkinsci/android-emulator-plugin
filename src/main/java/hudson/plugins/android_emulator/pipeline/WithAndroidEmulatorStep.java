package hudson.plugins.android_emulator.pipeline;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

/** Waits for an Android emulator to finish booting, then executes a block. */
public class WithAndroidEmulatorStep extends AbstractStepImpl {

    @Nonnull
    private final EmulatorState emuState;

    @DataBoundConstructor
    public WithAndroidEmulatorStep(EmulatorState state) {
        this.emuState = state;
    }

    @Nonnull
    public EmulatorState getEmulatorState() {
        return emuState;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(WithAndroidEmulatorStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "withAndroidEmulator";
        }

        @Override
        public String getDisplayName() {
            // TODO: Localisation
            return "Run build steps with an Android emulator running";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }

    }

}