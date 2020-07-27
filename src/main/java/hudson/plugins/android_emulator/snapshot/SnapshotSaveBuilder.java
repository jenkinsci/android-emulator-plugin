package hudson.plugins.android_emulator.snapshot;

import hudson.Functions;
import hudson.model.Descriptor;
import hudson.plugins.android_emulator.Messages;
import hudson.tasks.Builder;

import java.io.Serializable;

import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

public class SnapshotSaveBuilder extends AbstractSnapshotBuilder {

    @DataBoundConstructor
    public SnapshotSaveBuilder(String name) {
        super(name);
    }

    @Override
    protected String getSnapshotAction() {
        return "save";
    }

    @Override
    protected String getLogMessage(String name, int port) {
        return Messages.SAVING_SNAPSHOT(name, port);
    }

    //@Extension
    public static class DescriptorImpl extends Descriptor<Builder> implements Serializable {

        private static final long serialVersionUID = 1L;

        public DescriptorImpl() {
            super(SnapshotSaveBuilder.class);
            load();
        }

        @Override
        public String getHelpFile() {
            return Functions.getResourcePath() + "/plugin/android-emulator/help-snapshotSave.html";
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return Messages.SAVE_EMULATOR_SNAPSHOT();
        }

    }

}
