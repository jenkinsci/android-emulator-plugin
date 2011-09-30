package hudson.plugins.android_emulator;

import static hudson.plugins.android_emulator.AndroidEmulator.log;
import hudson.FilePath;
import hudson.Launcher;
import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.plugins.android_emulator.SdkInstaller.AndroidInstaller.SdkUnavailableException;
import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.sdk.Tool;
import hudson.plugins.android_emulator.util.Utils;
import hudson.plugins.android_emulator.util.ValidationResult;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

class SdkInstaller {

    /** Recent version of the Android SDK that will be installed. */
    private static final int SDK_VERSION = 13;

    /** Filename to write some metadata to about our automated installation. */
    private static final String SDK_INFO_FILENAME = ".jenkins-install-info";

    /**
     * Installs the Android SDK on the machine we're executing on.
     *
     * @param launcher
     * @param listener
     * @return
     * @throws SdkInstallationException
     * @throws IOException
     * @throws InterruptedException
     */
    static AndroidSdk install(Launcher launcher, BuildListener listener)
            throws SdkInstallationException, IOException, InterruptedException {
        // We should install the SDK on the current build machine
        Node node = Computer.currentComputer().getNode();

        // Install the SDK if required
        String androidHome;
        try {
            // TODO: Support Windows. However, running "android update sdk -u -t <whatever>" seems
            //       to hang after a successful install (also confirmed manually from cmd.exe)...
            //       Bug filed here: http://b.android.com/18868
            AndroidInstaller installer = AndroidInstaller.fromNode(node);
            if (installer == AndroidInstaller.WINDOWS) {
                throw new SdkInstallationException("Installation isn't currently supported on Windows");
            }
            androidHome = installBasicSdk(listener, node, launcher.isUnix()).getRemote();
        } catch (IOException e) {
            throw new SdkInstallationException("Failed to download Android SDK", e);
        } catch (SdkUnavailableException e) {
            throw new SdkInstallationException("Failed to download Android SDK", e);
        }

        // Check whether we need to install the SDK components
        if (!isSdkInstallComplete(node, androidHome)) {
            PrintStream logger = listener.getLogger();
            log(logger, "Going to install required Android SDK components...");
            AndroidSdk sdk = new AndroidSdk(androidHome);
            installComponent(logger, launcher, sdk, "platform-tool,tool");

            // If we made it this far, confirm completion by writing our our metadata file
            getInstallationInfoFilename(node).write(String.valueOf(SDK_VERSION), "UTF-8");
        }

        // Create an SDK object now that all the components exist
        return Utils.getAndroidSdk(launcher, androidHome);
    }

    /**
     * Downloads and extracts the basic Android SDK on a given Node, if it hasn't already been done.
     *
     * @param node Node to install the SDK on.
     * @param isUnix Whether the node is sane.
     * @return Path where the SDK is installed, regardless of whether it was installed right now.
     * @throws SdkUnavailableException If the Android SDK is not available on this platform.
     */
    private static FilePath installBasicSdk(final BuildListener listener, Node node, boolean isUnix)
            throws SdkUnavailableException, IOException, InterruptedException {
        // Locate where the SDK should be installed to on this node
        final FilePath installDir = getSdkInstallDirectory(node);
        System.out.println(">>> SDK install dir: "+ installDir);

        // Get the OS-specific download URL for the SDK
        AndroidInstaller installer = AndroidInstaller.fromNode(node);
        final URL downloadUrl = installer.getUrl(SDK_VERSION);

        // Download the SDK, if required
        boolean wasNowInstalled = installDir.act(new FileCallable<Boolean>() {
            public Boolean invoke(File f, VirtualChannel channel)
                    throws InterruptedException, IOException {
                String msg = "Downloading and installing Android SDK from "+ downloadUrl;
                return installDir.installIfNecessaryFrom(downloadUrl, listener, msg);
            }
            private static final long serialVersionUID = 1L;
        });

        if (wasNowInstalled) {
            // If the SDK was required, pull files up from the intermediate directory
            installDir.listDirectories().get(0).moveAllChildrenTo(installDir);

            // Java's ZipEntry doesn't preserve the executable bit...
            // TODO: Check if this is actually needed on Linux (tar.gz is probably saner)
            if (isUnix || installer == AndroidInstaller.MAC_OS_X) {
                setPermissions(installDir.child("tools"));
            }

            // Success!
            log(listener.getLogger(), "SDK installed successfully");
        }

        return installDir;
    }

    /**
     * Installs the SDK Tools and Platform Tools into the given installation.
     *
     * @param logger Logs things.
     * @param launcher Used to launch tasks on the remote node.
     * @param sdkRoot Root of the SDK installation to install components for.
     */
    private static void installComponent(PrintStream logger, Launcher launcher, AndroidSdk sdk,
            String component) throws IOException, InterruptedException {
        String proxySettings = getProxySettings();

        log(logger, String.format("Installing the '%s' SDK component...", component));
        String upgradeArgs = String.format("update sdk -o -u %s -t %s", proxySettings, component);
        Utils.runAndroidTool(launcher, logger, logger, sdk, Tool.ANDROID, upgradeArgs, null);
    }

    /**
     * Installs the given platform and its dependencies into the given installation, if necessary.
     *
     * @param logger Logs things.
     * @param launcher Used to launch tasks on the remote node.
     * @param sdkRoot Root of the SDK installation to install components for.
     */
    public static void installPlatform(PrintStream logger, Launcher launcher, AndroidSdk sdk,
            EmulatorConfig emuConfig) throws IOException, InterruptedException {
        // TODO: Get target platform from emuConfig.
        // TODO: If it's a named AVD, do nothing? Or extract target from .ini file?
        String platform = "android-4";

        // TODO: Dependency "resolution", based on add-on SDK version code?
        installComponent(logger, launcher, sdk, platform);
    }

    private static String getProxySettings() {
        // TODO: This needs to run on the remote node and fetch System.getprop("http[s].proxyHost")
        // TODO: Or can/should we integrate with the built-in proxy support (if it's available)
        return "";
    }

    /**
     * Determines whether the Android SDK installation on the given node is complete.
     *
     * @param node The node to check.
     * @param sdkRoot Root directory of the SDK installation to check.
     * @return {@code true} if the basic SDK <b>and</b> all required SDK components are installed.
     */
    private static boolean isSdkInstallComplete(Node node, String sdkRoot)
            throws IOException, InterruptedException {
        // TODO: This needs to run on the remote node!

        ValidationResult result = Utils.validateAndroidHome(new File(sdkRoot), false);
        System.out.println("SDK validation result: "+ result);
        if (result.isFatal()) {
            // No, we're missing some tools
            System.out.println("SDK is not complete: missing tools");
            return false;
        }

        // SDK is complete if we got as far as writing the metadata file
        return getInstallationInfoFilename(node).exists();
    }

    /**
     * Retrieves the path at which the Android SDK shoud be installed on the given node.
     *
     * @param node Node to install the SDK on.
     * @return Path within the tools folder on the node where the SDK should live.
     */
    private static final FilePath getSdkInstallDirectory(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("Must pass a valid Node!");
        }

        // Get the root of the node installation
        FilePath root = node.getRootPath();
        if (root == null) {
            throw new IllegalArgumentException("Node " + node.getDisplayName() + " seems to be offline");
        }
        return root.child("tools").child("android-sdk");
    }

    /** Gets the path of our installation metadata file for the given node. */
    private static final FilePath getInstallationInfoFilename(Node node) {
        return getSdkInstallDirectory(node).child(SDK_INFO_FILENAME);
    }

    /**
     * Recursively flags anything that looks like an Android tools executable, as executable.
     *
     * @param toolsDir The top level Android SDK tools directory.
     */
    private static final void setPermissions(FilePath toolsDir) throws IOException, InterruptedException {
        for (FilePath dir : toolsDir.listDirectories()) {
            setPermissions(dir);
        }
        for (FilePath f : toolsDir.list(new ToolFileFilter())) {
            f.chmod(0755);
        }
    }

    /** Serializable FileFilter that searches for Android SDK tool executables. */
    private static final class ToolFileFilter implements FileFilter, Serializable {
        public boolean accept(File f) {
            // Executables are files, which have no file extension, except for shell scripts
            return f.isFile() && (!f.getName().contains(".") || f.getName().endsWith(".sh"));
        }
        private static final long serialVersionUID = 1L;
    }

    /** Helper for getting platform-specific SDK installation information. */
    enum AndroidInstaller {

        LINUX("linux_x86", "tar.gz"),
        MAC_OS_X("mac_x86", "zip"),
        WINDOWS("windows", "zip");

        private static final String PATTERN = "http://dl.google.com/android/android-sdk_r%d-%s.%s";
        private final String platform;
        private final String extension;

        private AndroidInstaller(String platform, String extension) {
            this.platform = platform;
            this.extension = extension;
        }

        URL getUrl(int version) {
            try {
                return new URL(String.format(PATTERN, version, platform, extension));
            } catch (MalformedURLException e) {}
            return null;
        }

        static AndroidInstaller fromNode(Node node) throws SdkUnavailableException,
                IOException, InterruptedException {
            return node.getChannel().call(new Callable<AndroidInstaller, SdkUnavailableException>() {
                public AndroidInstaller call() throws SdkUnavailableException {
                    return get();
                }
                private static final long serialVersionUID = 1L;
            });
        }

        private static AndroidInstaller get() throws SdkUnavailableException {
            String prop = System.getProperty("os.name");
            String os = prop.toLowerCase(Locale.ENGLISH);
            if (os.contains("linux")) {
                return LINUX;
            }
            if (os.contains("mac")) {
                return MAC_OS_X;
            }
            if (os.contains("windows")) {
                return WINDOWS;
            }
            throw new SdkUnavailableException("The Android SDK is not available for "+ prop);
        }

        static final class SdkUnavailableException extends Exception {
            private SdkUnavailableException(String message) {
                super(message);
            }
            private static final long serialVersionUID = 1L;
        }
    }

}
