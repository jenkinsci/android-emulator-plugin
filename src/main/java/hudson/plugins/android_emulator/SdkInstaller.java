package hudson.plugins.android_emulator;

import static hudson.plugins.android_emulator.AndroidEmulator.log;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.plugins.android_emulator.SdkInstaller.AndroidInstaller.SdkUnavailableException;
import hudson.plugins.android_emulator.ValidationResult.Type;
import hudson.remoting.Callable;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Locale;

class SdkInstaller {

    /** Recent version of the Android SDK that will be installed. */
    private static final int SDK_VERSION = 11;

    /** Filename to write some metadata to about our automated installation. */
    private static final String SDK_INFO_FILENAME = ".jenkins-install-info";

    static AndroidSdk install(Launcher launcher, BuildListener listener)
            throws SdkInstallationException, IOException, InterruptedException {
        // We should install the SDK on the current build machine
        Node node = Computer.currentComputer().getNode();

        // Install the SDK if required
        String androidHome;
        try {
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
            installComponents(logger, launcher, androidHome);

            // Check whether, after all the downloading, we actually have a complete SDK installation
            if (!isSdkInstallComplete(node, androidHome)) {
                return null;
            }
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
    private static FilePath installBasicSdk(BuildListener listener, Node node, boolean isUnix)
            throws SdkUnavailableException, IOException, InterruptedException {
        // Locate where the SDK should be installed to on this node
        FilePath installDir = getSdkInstallDirectory(node);

        // Get the OS-specific download URL for the SDK
        AndroidInstaller installer = AndroidInstaller.fromNode(node);
        String downloadUrl = installer.getUrl(SDK_VERSION);
//        downloadUrl = "file:///tmp/android-sdk_r10-mac_x86.zip"; // TODO: Testing

        // Download the SDK, if required
        URL url = new URL(downloadUrl);
        String message = "Downloading and installing Android SDK from "+ downloadUrl;
        boolean wasNowInstalled = installDir.installIfNecessaryFrom(url, listener, message);
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
     * Installs the SDK Tools, Platform Tools, and Platforms into the given installation.
     *
     * @param logger Logs things.
     * @param launcher Used to launch tasks on the remote node.
     * @param sdkRoot Root of the SDK installation to install components for.
     */
    private static void installComponents(PrintStream logger, Launcher launcher, String sdkRoot)
            throws IOException, InterruptedException {
        // Get proxy settings to pass to the 'android' command
        String proxySettings = getProxySettings();

        AndroidSdk sdk = new AndroidSdk(sdkRoot);
        // TODO: Add "extra" component, behind a boolean preference?
        // TODO: Oh yeah, it's not possible to install the Google API via `android`...
        for (String component : "platform-tool,tool,platform".split(",")) {
            log(logger, String.format("Installing the '%s' SDK component...", component));
            String upgradeArgs = String.format("update sdk -u %s -t %s", proxySettings, component);
            Utils.runAndroidTool(launcher, logger, logger, sdk, Tool.ANDROID, upgradeArgs, null);
        }

        // If we made it this far, confirm completion by writing our our metadata file
        Node node = Computer.currentComputer().getNode();
        getInstallationInfoFilename(node).write(String.valueOf(SDK_VERSION), "UTF-8");
    }

    private static String getProxySettings() {
        // TODO: This needs to run on the remote node and fetch System.getprop("http[s].proxyHost")
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
        if (result.getType() != Type.OK) {
            // No, we're missing either some tools or platforms
            System.out.println("SDK is not complete: missing tools or platforms");
            getInstallationInfoFilename(node).delete();
            return false;
        }

        // The tools and the platforms directory might exist, but it's possible we haven't finished
        // downloading all the platforms. The presence of our metadata file confirms completion.
        System.out.println("SDK metadata file exists? "+ getInstallationInfoFilename(node).exists());
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

        String getUrl(int version) {
            return String.format(PATTERN, version, platform, extension);
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
