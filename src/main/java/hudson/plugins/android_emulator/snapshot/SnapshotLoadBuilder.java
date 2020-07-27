package hudson.plugins.android_emulator.snapshot;

import hudson.Functions;
import hudson.model.Descriptor;
import hudson.plugins.android_emulator.Messages;
import hudson.tasks.Builder;

import java.io.Serializable;

import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

public class SnapshotLoadBuilder extends AbstractSnapshotBuilder {

    @DataBoundConstructor
    public SnapshotLoadBuilder(String name) {
        super(name);
    }

    @Override
    protected String getSnapshotAction() {
        return "load";
    }

    @Override
    protected String getLogMessage(String name, int port) {
        return Messages.LOADING_SNAPSHOT(name, port);
    }

    //@Extension
    public static class DescriptorImpl extends Descriptor<Builder> implements Serializable {

        private static final long serialVersionUID = 1L;

        public DescriptorImpl() {
            super(SnapshotLoadBuilder.class);
            load();
        }

        @Override
        public String getHelpFile() {
            return Functions.getResourcePath() + "/plugin/android-emulator/help-snapshotLoad.html";
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return Messages.LOAD_EMULATOR_SNAPSHOT();
        }

    }

}
