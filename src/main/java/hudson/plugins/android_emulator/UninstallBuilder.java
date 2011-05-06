package hudson.plugins.android_emulator;

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.plugins.android_emulator.builder.AbstractBuilder;
import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.util.Utils;
import hudson.tasks.Builder;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class UninstallBuilder extends AbstractBuilder {

    /** Package ID of the APK to be uninstalled. */
    private final String packageId;

    @DataBoundConstructor
    @SuppressWarnings("hiding")
    public UninstallBuilder(String packageId) {
        this.packageId = Util.fixEmptyAndTrim(packageId);
    }

    public String getPackageId() {
        return packageId;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        final PrintStream logger = listener.getLogger();

        // Discover Android SDK
        AndroidSdk androidSdk = getAndroidSdk(build, launcher, listener);
        if (androidSdk == null) {
            AndroidEmulator.log(logger, Messages.SDK_TOOLS_NOT_FOUND());
            return false;
        }

        // Check whether a value was provided
        final String packageId = getPackageId();
        if (Util.fixEmptyAndTrim(packageId) == null) {
            AndroidEmulator.log(logger, Messages.PACKAGE_ID_NOT_SPECIFIED());
            return false;
        }

        // Expand package ID value
        String expandedPackageId = Utils.expandVariables(build, listener, packageId);

        // Execute uninstallation
        String deviceIdentifier = getDeviceIdentifier(build, listener);
        uninstallApk(build, launcher, logger, androidSdk, deviceIdentifier, expandedPackageId);

        // TODO: Evaluate success/failure and fail the build (if the user said we should do so)
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Builder> implements Serializable {

        private static final long serialVersionUID = 1L;

        public DescriptorImpl() {
            super(UninstallBuilder.class);
            load();
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) {
            save();
            return true;
        }

        @Override
        public String getHelpFile() {
            return "/plugin/android-emulator/help-uninstallPackage.html";
        }

        @Override
        public String getDisplayName() {
            return Messages.UNINSTALL_ANDROID_PACKAGE();
        }

    }

}
