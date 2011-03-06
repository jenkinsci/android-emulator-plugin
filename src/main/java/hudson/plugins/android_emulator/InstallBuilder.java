package hudson.plugins.android_emulator;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Builder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class InstallBuilder extends Builder {

    /** Environment variable set by the plugin to specify the serial of the started AVD. */
    private static final String DEVICE_SERIAL_VARIABLE = "ANDROID_AVD_DEVICE";

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
        String androidHome = Utils.expandVariables(build, listener, Utils.getConfiguredAndroidHome());
        EnvVars envVars = Utils.getEnvironment(build, listener);
        String discoveredAndroidHome = Utils.discoverAndroidHome(launcher, envVars, androidHome);
        AndroidSdk androidSdk = Utils.getAndroidSdk(launcher, discoveredAndroidHome);
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
        String deviceIdentifier;
        String deviceSerialToken = String.format("$%s", DEVICE_SERIAL_VARIABLE);
        String deviceSerial = Utils.expandVariables(build, listener, deviceSerialToken);
        if (deviceSerial.equals(deviceSerialToken)) {
            // No emulator was started by this plugin; assume only one device is attached
            deviceIdentifier = "";
        } else {
            // Use the serial of the emulator started by this plugin
            deviceIdentifier = String.format("-s %s", deviceSerial);
        }

        // Uninstall APK first, if requested
        FilePath apkDirectory = apkPath.getParent();
        if (shouldUninstallFirst()) {
            // Run aapt command on APK to be installed
            ByteArrayOutputStream aaptOutput = new ByteArrayOutputStream();
            String args = String.format("dump badging %s", apkPath.getName());
            Utils.runAndroidTool(launcher, aaptOutput, logger, androidSdk, Tool.AAPT, args, apkDirectory);

            // Determine package ID from aapt output
            String packageId = null;
            String aaptResult = aaptOutput.toString();
            if (aaptResult.length() > 0) {
                Matcher matcher = Pattern.compile("package: +name='([^']+)'").matcher(aaptResult);
                if (matcher.find()) {
                    packageId = matcher.group(1);
                }
            }

            // Execute uninstallation
            if (packageId == null) {
                AndroidEmulator.log(logger, Messages.COULD_NOT_DETERMINE_APK_PACKAGE(apkPath.getName()));
            } else {
                AndroidEmulator.log(logger, Messages.UNINSTALLING_APK(packageId));
                String adbArgs = String.format("%s uninstall %s", deviceIdentifier, packageId);
                Utils.runAndroidTool(launcher, logger, logger, androidSdk, Tool.ADB, adbArgs, null);
            }
        }

        // Execute installation
        AndroidEmulator.log(logger, Messages.INSTALLING_APK(apkPath.getName()));
        String args = String.format("%s install -r %s", deviceIdentifier, apkPath.getName());
        Utils.runAndroidTool(launcher, logger, logger, androidSdk, Tool.ADB, args, apkDirectory);

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
            return "/plugin/android-emulator/help-installPackage.html";
        }

        @Override
        public String getDisplayName() {
            return Messages.INSTALL_ANDROID_PACKAGE();
        }

    }

}
