package hudson.plugins.android_emulator;

import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.plugins.android_emulator.builder.AbstractBuilder;
import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.sdk.Tool;
import hudson.plugins.android_emulator.util.Utils;
import hudson.tasks.Builder;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class InstallBuilder extends AbstractBuilder {

    /** Path to the APK to be installed, relative to the workspace. */
    private final String apkFile;

    /** Whether the APK should be uninstalled from the device before installation. */
    private final boolean uninstallFirst;

    @DataBoundConstructor
    @SuppressWarnings("hiding")
    public InstallBuilder(String apkFile, boolean uninstallFirst) {
        this.apkFile = Util.fixEmptyAndTrim(apkFile);
        this.uninstallFirst = uninstallFirst;
    }

    public String getApkFile() {
        return apkFile;
    }

    public boolean shouldUninstallFirst() {
        return uninstallFirst;
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
        final String apkFile = getApkFile();
        if (Util.fixEmptyAndTrim(apkFile) == null) {
            AndroidEmulator.log(logger, Messages.APK_NOT_SPECIFIED());
            return false;
        }

        // Get absolute path to the APK file
        String apkFileExpanded = Utils.expandVariables(build, listener, apkFile);
        FilePath apkPath = build.getWorkspace().child(apkFileExpanded);

        // Check whether the file exists
        boolean exists = apkPath.exists();
        if (!exists) {
            AndroidEmulator.log(logger, Messages.APK_NOT_FOUND(apkPath));
            return false;
        }

        // Determine which device to use
        final String deviceIdentifier = getDeviceIdentifier(build, listener);

        // Uninstall APK first, if requested
        if (shouldUninstallFirst()) {
            uninstallApk(build, launcher, logger, androidSdk, deviceIdentifier, apkPath);
        }

        // Execute installation
        AndroidEmulator.log(logger, Messages.INSTALLING_APK(apkPath.getName()));
        String args = String.format("%s install -r \"%s\"", deviceIdentifier, apkPath.getName());
        Utils.runAndroidTool(launcher, build.getEnvironment(TaskListener.NULL), logger, logger,
                androidSdk, Tool.ADB, args, apkPath.getParent());

        // TODO: Evaluate success/failure and fail the build (if the user said we should do so)
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Builder> implements Serializable {

        private static final long serialVersionUID = 1L;

        public DescriptorImpl() {
            super(InstallBuilder.class);
            load();
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) {
            save();
            return true;
        }

        @Override
        public String getHelpFile() {
            return Functions.getResourcePath() + "/plugin/android-emulator/help-installPackage.html";
        }

        @Override
        public String getDisplayName() {
            return Messages.INSTALL_ANDROID_PACKAGE();
        }

    }

}
