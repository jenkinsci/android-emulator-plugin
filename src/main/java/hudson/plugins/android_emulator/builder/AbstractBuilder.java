package hudson.plugins.android_emulator.builder;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.EnvironmentContributingAction;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.plugins.android_emulator.AndroidEmulator;
import hudson.plugins.android_emulator.AndroidEmulator.DescriptorImpl;
import hudson.plugins.android_emulator.Messages;
import hudson.plugins.android_emulator.SdkInstallationException;
import hudson.plugins.android_emulator.SdkInstaller;
import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.sdk.Tool;
import hudson.plugins.android_emulator.util.Utils;
import hudson.remoting.VirtualChannel;
import hudson.tasks.Builder;
import hudson.util.ForkOutputStream;
import net.dongliu.apk.parser.ApkParser;
import net.dongliu.apk.parser.bean.ApkMeta;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import static hudson.plugins.android_emulator.AndroidEmulator.log;

public abstract class AbstractBuilder extends Builder {

    /** Maximum time to wait, in milliseconds, for an APK to be uninstalled. */
    private static final int UNINSTALL_TIMEOUT = 60 * 1000;

    /** Environment variable set by the plugin to specify the serial of the started AVD. */
    private static final String DEVICE_SERIAL_VARIABLE = "ANDROID_AVD_DEVICE";

    /** Environment variable set by the plugin to specify the telnet interface port. */
    private static final String DEVICE_USER_PORT_VARIABLE = "ANDROID_AVD_USER_PORT";

    /**
     * Gets an Android SDK instance, ready for use.
     *
     * @param build The build for which we should retrieve the SDK instance.
     * @param launcher The launcher for the remote node.
     * @param listener The listener used to get the environment variables.
     * @return An Android SDK instance, or {@code null} if none could be found or installed.
     */
    protected static AndroidSdk getAndroidSdk(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws IOException, InterruptedException {
        boolean shouldInstallSdk = true;
        boolean keepInWorkspace = false;
        DescriptorImpl descriptor = Hudson.getInstance().getDescriptorByType(DescriptorImpl.class);
        if (descriptor != null) {
            shouldInstallSdk = descriptor.shouldInstallSdk;
            keepInWorkspace = descriptor.shouldKeepInWorkspace;
        }

        // Get configured, expanded Android SDK root value
        String androidHome = Utils.expandVariables(build, listener,
                Utils.getConfiguredAndroidHome());
        EnvVars envVars = Utils.getEnvironment(build, listener);

        // Retrieve actual SDK root based on given value
        Node node = Computer.currentComputer().getNode();
        String discoveredAndroidHome = Utils.discoverAndroidHome(launcher, node, envVars,
                androidHome);

        // Get Android SDK object from the given root (or locate on PATH)
        final String androidSdkHome = (envVars != null && keepInWorkspace ? envVars
                .get("WORKSPACE") : null);
        AndroidSdk androidSdk = Utils
                .getAndroidSdk(launcher, discoveredAndroidHome, androidSdkHome);

        // Check whether we should install the SDK
        if (androidSdk == null) {
            PrintStream logger = listener.getLogger();
            if (!shouldInstallSdk) {
                // Couldn't find an SDK, don't want to install it, give up
                log(logger, Messages.SDK_TOOLS_NOT_FOUND());
                return null;
            }

            // Ok, let's download and install the SDK
            log(logger, Messages.INSTALLING_SDK());
            try {
                androidSdk = SdkInstaller.install(launcher, listener, null);
            } catch (SdkInstallationException e) {
                log(logger, Messages.SDK_INSTALLATION_FAILED(), e);
                return null;
            }

            // Check whether anything went wrong
            if (androidSdk == null) {
                log(logger, Messages.SDK_INSTALLATION_FAILED());
                return null;
            }
        }

        // Export environment variables
        final String sdkRoot = androidSdk.getSdkRoot();
        build.addAction(new EnvironmentContributingAction() {

            public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars envVars) {
                if (envVars != null) {
                    envVars.put("ANDROID_HOME", sdkRoot);
                }
            }

            public String getUrlName() {
                return null;
            }

            public String getIconFileName() {
                return null;
            }

            public String getDisplayName() {
                return null;
            }
        });

        return androidSdk;
    }

    /**
     * Gets the Android device identifier for this job, defaulting to the AVD started by this plugin.
     *
     * @param build The build for which we should retrieve the SDK instance.
     * @param listener The listener used to get the environment variables.
     * @return The device identifier (defaulting to the value of "<tt>-s $ANDROID_AVD_DEVICE</tt>").
     */
    protected static String getDeviceIdentifier(AbstractBuild<?, ?> build, BuildListener listener) {
        String deviceSerial = expandVariable(build, listener, DEVICE_SERIAL_VARIABLE);
        if (deviceSerial == null) {
            // No emulator was started by this plugin; assume only one device is attached
            return "";
        }

        // Use the serial of the emulator started by this plugin
        return String.format("-s %s", deviceSerial);
    }

    /**
     * Gets the Android device identifier for this job, defaulting to the AVD started by this plugin.
     *
     * @param build The build for which we should retrieve the SDK instance.
     * @param listener The listener used to get the environment variables.
     * @return The device identifier (defaulting to the value of "<tt>-s $ANDROID_AVD_DEVICE</tt>").
     */
    protected static int getDeviceTelnetPort(AbstractBuild<?, ?> build, BuildListener listener) {
        String devicePort = expandVariable(build, listener, DEVICE_USER_PORT_VARIABLE);
        if (devicePort == null) {
            // No emulator was started by this plugin; assume only one device is attached
            return 5554;
        }

        // Use the serial of the emulator started by this plugin
        return Integer.parseInt(devicePort);
    }

    /**
     * Expands the given variable name to its value from the given build and environment.

     * @param build Used to get build-specific variables.
     * @param listener Used to get environment variables.
     * @param variable The name of the variable to expand.
     * @return The value of the expanded variable, or {@code null} if it could not be resolved.
     */
    private static String expandVariable(AbstractBuild<?, ?> build, BuildListener listener,
            String variable) {
        String varFormat = String.format("$%s", variable);
        String value = Utils.expandVariables(build, listener, varFormat);
        if (value.equals(varFormat)) {
            // Variable did not expand to anything
            return null;
        }

        // Return expanded value
        return value;
    }

    /**
     * Waits for the "android.process.acore" process to start, as this is a prerequisite for using the package manager.
     *
     * @return {@code true} if the process has started; {@code false} if it did not start within a reasonable timeout.
     */
    protected boolean waitForCoreProcess(AbstractBuild<?, ?> build, Launcher launcher,
            AndroidSdk androidSdk, String deviceIdentifier) throws IOException, InterruptedException {

        // At this point, the emulator has already supposedly started, yet we may still have to wait a while...
        final int timeout = 120 * 1000;
        final int adbTimeout = 5 * 1000;
        final int sleep = timeout / (int) (Math.sqrt(timeout / 1000) * 2);

        // Run the "ps" command in a loop until we find the desired process
        final String adbArgs = String.format("%s shell ps", deviceIdentifier);
        try {
            long start = System.currentTimeMillis();
            final ByteArrayOutputStream stdout = new ByteArrayOutputStream(8192);
            while (System.currentTimeMillis() < start + timeout) {
                // Clear out any existing stdout output
                stdout.reset();

                // Get the process list from the device
                Utils.runAndroidTool(launcher, build.getEnvironment(TaskListener.NULL), stdout, null, androidSdk,
                        Tool.ADB, adbArgs, null, adbTimeout);

                // Check whether the core process has started
                if (stdout.toString().contains("android.process.acore")) {
                    return true;
                }

                // It hasn't started, so sleep and try again later
                Thread.sleep(sleep);
            }
        } catch (InterruptedException ex) {
            // TODO: Words
            log(launcher.getListener().getLogger(), Messages.INTERRUPTED_DURING_BOOT_COMPLETION());
        } catch (IOException ex) {
            // TODO: Words
            log(launcher.getListener().getLogger(), Messages.COULD_NOT_CHECK_BOOT_COMPLETION());
            ex.printStackTrace(launcher.getListener().getLogger());
        }

        return false;
    }

    /**
     * Uninstalls the Android package corresponding to the given APK file from an Android device.
     *
     * @param build The build for which we should uninstall the package.
     * @param launcher The launcher for the remote node.
     * @param logger Where log output should be redirected to.
     * @param androidSdk The Android SDK to use.
     * @param deviceIdentifier The device from which the package should be removed.
     * @param apkPath The path to the APK file.
     * @return {@code true} iff uninstallation completed successfully.
     * @throws IOException If execution failed.
     * @throws InterruptedException If execution failed.
     */
    protected static boolean uninstallApk(AbstractBuild<?, ?> build, Launcher launcher,
            PrintStream logger, AndroidSdk androidSdk, String deviceIdentifier, FilePath apkPath)
                throws IOException, InterruptedException {
        // Get package ID to uninstall
        String packageId = getPackageIdForApk(apkPath);
        return uninstallApk(build, launcher, logger, androidSdk, deviceIdentifier, packageId);
    }

    /**
     * Uninstalls the given Android package ID from the given Android device.
     *
     * @param build The build for which we should uninstall the package.
     * @param launcher The launcher for the remote node.
     * @param logger Where log output should be redirected to.
     * @param androidSdk The Android SDK to use.
     * @param deviceIdentifier The device from which the package should be removed.
     * @param packageId The ID of the Android package to remove from the given device.
     * @return {@code true} iff uninstallation completed successfully.
     * @throws IOException If execution failed.
     * @throws InterruptedException If execution failed.
     */
    protected static boolean uninstallApk(AbstractBuild<?, ?> build, Launcher launcher,
            PrintStream logger, AndroidSdk androidSdk, String deviceIdentifier, String packageId)
                throws IOException, InterruptedException {
        AndroidEmulator.log(logger, Messages.UNINSTALLING_APK(packageId));

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ForkOutputStream forkStream = new ForkOutputStream(logger, stdout);
        String adbArgs = String.format("%s uninstall %s", deviceIdentifier, packageId);
        Utils.runAndroidTool(launcher, build.getEnvironment(TaskListener.NULL),
                forkStream, logger, androidSdk, Tool.ADB, adbArgs, null, UNINSTALL_TIMEOUT);

        // The package manager simply returns "Success" or "Failure" on stdout
        return stdout.toString().contains("Success");
    }

    /**
     * Determines the package ID of an APK file.
     *
     * @param apkPath The path to the APK file.
     * @return The package ID for the given APK.
     * @throws IOException If execution failed.
     * @throws InterruptedException If execution failed.
     */
    private static String getPackageIdForApk(FilePath apkPath) throws IOException, InterruptedException {
        return apkPath.act(new FilePath.FileCallable<String>() {
            public String invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
                return getApkMetadata(f).getPackageName();
            }
        });
    }

    /** @return The application metadata of the given APK file. */
    private static ApkMeta getApkMetadata(File apk) throws IOException, InterruptedException {
        ApkParser apkParser = new ApkParser(apk);
        try {
            return apkParser.getApkMeta();
        } finally {
            apkParser.close();
        }
    }

}
