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
import hudson.util.ForkOutputStream;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.regex.Pattern;

public class InstallBuilder extends AbstractBuilder {

    /** Maximum time to wait, in milliseconds, for an APK to install. */
    private static final int INSTALL_TIMEOUT = 2 * 60 * 1000;

    /** Path to the APK to be installed, relative to the workspace. */
    private final String apkFile;

    /** Whether the APK should be uninstalled from the device before installation. */
    private final boolean uninstallFirst;

    /** Whether to fail the build if installation isn't successful. */
    private final boolean failOnInstallFailure;

    @DataBoundConstructor
    @SuppressWarnings("hiding")
    public InstallBuilder(String apkFile, boolean uninstallFirst, boolean failOnInstallFailure) {
        this.apkFile = Util.fixEmptyAndTrim(apkFile);
        this.uninstallFirst = uninstallFirst;
        this.failOnInstallFailure = failOnInstallFailure;
    }

    public String getApkFile() {
        return apkFile;
    }

    public boolean shouldUninstallFirst() {
        return uninstallFirst;
    }

    public boolean shouldFailBuildOnFailure() {
        return failOnInstallFailure;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        final PrintStream logger = listener.getLogger();

        // Discover Android SDK
        AndroidSdk androidSdk = getAndroidSdk(build, launcher, listener);
        if (androidSdk == null) {
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

        // Wait for package manager to become ready
        AndroidEmulator.log(logger, Messages.WAITING_FOR_CORE_PROCESS());
        boolean ready = waitForCoreProcess(build, launcher, androidSdk, deviceIdentifier);
        if (!ready) {
            AndroidEmulator.log(logger, Messages.CORE_PROCESS_DID_NOT_START());
        }

        // Uninstall APK first, if requested
        if (shouldUninstallFirst()) {
            uninstallApk(build, launcher, logger, androidSdk, deviceIdentifier, apkPath);
        }

        // Execute installation
        AndroidEmulator.log(logger, Messages.INSTALLING_APK(apkPath.getName()));
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ForkOutputStream forkStream = new ForkOutputStream(logger, stdout);
        String args = String.format("%s install -r \"%s\"", deviceIdentifier, apkPath.getName());
        Utils.runAndroidTool(launcher, build.getEnvironment(TaskListener.NULL), forkStream, logger,
                androidSdk, Tool.ADB, args, apkPath.getParent(), INSTALL_TIMEOUT);

        Pattern p = Pattern.compile("^Success$", Pattern.MULTILINE);
        boolean success = p.matcher(stdout.toString()).find();
        if (!success && failOnInstallFailure) {
            return false;
        }
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
