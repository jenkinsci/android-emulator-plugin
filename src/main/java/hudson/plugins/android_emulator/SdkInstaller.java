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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Semaphore;

import org.apache.commons.lang.StringUtils;

public class SdkInstaller {

    /** Recent version of the Android SDK that will be installed. */
    private static final int SDK_VERSION = 16;

    /** Filename to write some metadata to about our automated installation. */
    private static final String SDK_INFO_FILENAME = ".jenkins-install-info";

    /** Map of nodes to locks, to ensure only one executor attempts SDK installation at once. */
    private static final Map<Node, Semaphore> mutexByNode = new WeakHashMap<Node, Semaphore>();

    /**
     * Downloads and installs the Android SDK on the machine we're executing on.
     *
     * @return An {@code AndroidSdk} object for the newly-installed SDK.
     */
    public static AndroidSdk install(Launcher launcher, BuildListener listener)
            throws SdkInstallationException, IOException, InterruptedException {
        Semaphore semaphore = acquireLock();
        try {
            return doInstall(launcher, listener);
        } finally {
            semaphore.release();
        }
    }

    private static AndroidSdk doInstall(Launcher launcher, BuildListener listener)
            throws SdkInstallationException, IOException, InterruptedException {
        // We should install the SDK on the current build machine
        Node node = Computer.currentComputer().getNode();

        // Install the SDK if required
        String androidHome;
        try {
            androidHome = installBasicSdk(listener, node, launcher.isUnix()).getRemote();
        } catch (IOException e) {
            throw new SdkInstallationException(Messages.SDK_DOWNLOAD_FAILED(), e);
        } catch (SdkUnavailableException e) {
            throw new SdkInstallationException(Messages.SDK_DOWNLOAD_FAILED(), e);
        }

        // Check whether we need to install the SDK components
        if (!isSdkInstallComplete(node, androidHome)) {
            PrintStream logger = listener.getLogger();
            log(logger, Messages.INSTALLING_REQUIRED_COMPONENTS());
            AndroidSdk sdk = new AndroidSdk(androidHome);
            installComponent(logger, launcher, sdk, "platform-tool", "tool");

            // If we made it this far, confirm completion by writing our our metadata file
            getInstallationInfoFilename(node).write(String.valueOf(SDK_VERSION), "UTF-8");

            // As this SDK will not be used manually, opt out of the stats gathering;
            // this also prevents the opt-in dialog from popping up during execution
            optOutOfSdkStatistics(launcher, listener);
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
        final FilePath installDir = Utils.getSdkInstallDirectory(node);

        // Get the OS-specific download URL for the SDK
        AndroidInstaller installer = AndroidInstaller.fromNode(node);
        final URL downloadUrl = installer.getUrl(SDK_VERSION);

        // Download the SDK, if required
        boolean wasNowInstalled = installDir.act(new FileCallable<Boolean>() {
            public Boolean invoke(File f, VirtualChannel channel)
                    throws InterruptedException, IOException {
                String msg = Messages.DOWNLOADING_SDK_FROM(downloadUrl);
                return installDir.installIfNecessaryFrom(downloadUrl, listener, msg);
            }
            private static final long serialVersionUID = 1L;
        });

        if (wasNowInstalled) {
            // If the SDK was required, pull files up from the intermediate directory
            installDir.listDirectories().get(0).moveAllChildrenTo(installDir);

            // Java's ZipEntry doesn't preserve the executable bit...
            if (installer == AndroidInstaller.MAC_OS_X) {
                setPermissions(installDir.child("tools"));
            }

            // Success!
            log(listener.getLogger(), Messages.BASE_SDK_INSTALLED());
        }

        return installDir;
    }

    /**
     * Installs the given SDK component(s) into the given installation.
     *
     * @param logger Logs things.
     * @param launcher Used to launch tasks on the remote node.
     * @param sdkRoot Root of the SDK installation to install components for.
     * @param components Name of the component(s) to install.
     */
    private static void installComponent(PrintStream logger, Launcher launcher, AndroidSdk sdk,
            String... components) throws IOException, InterruptedException {
        String proxySettings = getProxySettings();

        String list = StringUtils.join(components, ',');
        log(logger, Messages.INSTALLING_SDK_COMPONENTS(list.toString()));
        String upgradeArgs = String.format("update sdk -o -u %s -t %s", proxySettings, list);

        // TODO: We need to be able to kill the command spawned if the build is interrupted!
        Utils.runAndroidTool(launcher, logger, logger, sdk, Tool.ANDROID, upgradeArgs, null);
    }

    /**
     * Installs the platform for an emulator config into the given SDK installation, if necessary.
     *
     * @param logger Logs things.
     * @param launcher Used to launch tasks on the remote node.
     * @param sdk SDK installation to install components for.
     * @param emuConfig Specifies the platform to be installed.
     */
    static void installDependencies(PrintStream logger, Launcher launcher,
            AndroidSdk sdk, EmulatorConfig emuConfig) throws IOException, InterruptedException {
        // Get AVD platform from emulator config
        String platform = getPlatformForEmulator(launcher, emuConfig);

        // Install platform and any dependencies it may have
        installPlatform(logger, launcher, sdk, platform);
    }

    /**
     * Installs the given platform and its dependencies into the given installation, if necessary.
     *
     * @param logger Logs things.
     * @param launcher Used to launch tasks on the remote node.
     * @param sdk SDK installation to install components for.
     * @param platform Specifies the platform to be installed.
     */
    public static void installPlatform(PrintStream logger, Launcher launcher,
            AndroidSdk sdk, String platform) throws IOException, InterruptedException {
        // Check whether this platform is already installed
        if (isPlatformInstalled(logger, launcher, sdk, platform)) {
            return;
        }

        // Check whether we are capable of installing individual components
        log(logger, Messages.PLATFORM_INSTALL_REQUIRED(platform));
        if (!launcher.isUnix() && platform.contains(":") && sdk.getSdkToolsVersion() < 16) {
            // SDK add-ons can't be installed on Windows until r16 due to http://b.android.com/18868
            log(logger, Messages.SDK_ADDON_INSTALLATION_UNSUPPORTED());
            return;
        }
        if (!sdk.supportsComponentInstallation()) {
            log(logger, Messages.SDK_COMPONENT_INSTALLATION_UNSUPPORTED());
            return;
        }

        // Automated installation of ABIs (required for android-14+) is not possible until r17, so
        // we should warn the user that we can't automatically set up an AVD with older SDK Tools.
        // See http://b.android.com/21880
        if ((platform.endsWith("14") || platform.endsWith("15")) && !sdk.supportsSystemImageInstallation()) {
            log(logger, Messages.ABI_INSTALLATION_UNSUPPORTED(), true);
        }

        // Determine which individual component(s) need to be installed for this platform
        List<String> components = getSdkComponentsForPlatform(logger, platform);
        if (components == null || components.size() == 0) {
            return;
        }

        // If a platform expanded to multiple dependencies (e.g. "GoogleMaps:7" -> android-7 + Maps)
        // then check whether we really need to install android-7, as it may already be installed
        if (components.size() > 1) {
            for (Iterator<String> it = components.iterator(); it.hasNext(); ) {
                String component = it.next();
                if (isPlatformInstalled(logger, launcher, sdk, component)) {
                    it.remove();
                }
            }
        }

        // Grab the lock and attempt installation
        Semaphore semaphore = acquireLock();
        try {
            installComponent(logger, launcher, sdk, components.toArray(new String[0]));
        } finally {
            semaphore.release();
        }
    }

    private static boolean isPlatformInstalled(PrintStream logger, Launcher launcher,
            AndroidSdk sdk, String platform) throws IOException, InterruptedException {
        ByteArrayOutputStream targetList = new ByteArrayOutputStream();
        // Preferably we'd use the "--compact" flag here, but it wasn't added until r12
        Utils.runAndroidTool(launcher, targetList, logger, sdk, Tool.ANDROID, "list target", null);
        return targetList.toString().contains('"'+ platform +'"');
    }

    private static List<String> getSdkComponentsForPlatform(PrintStream logger, String platform) {
        // If it's a straightforward case like "android-10", there are no further dependencies
        if (!platform.contains(":")) {
            return Collections.singletonList(platform);
        }

        // Otherwise, figure out the component name and its platform dependency
        String parts[] = platform.toLowerCase().split(":");
        if (parts.length != 3) {
            log(logger, Messages.SDK_ADDON_FORMAT_UNRECOGNISED(platform));
            return null;
        }

        // Add dependent platform
        List<String> components = new ArrayList<String>();
        components.add(String.format("android-%s", parts[2]));

        // Add system image, if required
        // Even if a system image doesn't exist for this platform, the installer silently ignores it
        try {
            if (Integer.parseInt(parts[2]) >= 14) {
                components.add(String.format("sysimg-%s", parts[2]));
            }
        } catch (NumberFormatException e) {
            log(logger, Messages.SDK_ADDON_NAME_INCORRECT(platform));
        }

        // Determine addon name
        String component = String.format("addon-%s-%s-%s", parts[1], parts[0], parts[2]);
        component = component.replaceAll("[^a-z0-9_-]+", "_").replaceAll("_+", "_");
        components.add(component);

        return components;
    }

    /**
     * Determines the Android platform for the given emulator configuration.<br>
     * This is a string like "android-10" or "Google Inc.:Google APIs:4".
     *
     * @param launcher Used to launch tasks on the remote node.
     * @param emuConfig The emulator whose target platform we want to determine.
     * @return The platform, or {@code null} if it could not be determined.
     */
    private static String getPlatformForEmulator(Launcher launcher, final EmulatorConfig emuConfig)
            throws IOException, InterruptedException {
        // For existing, named emulators, get the target from the metadata file
        if (emuConfig.isNamedEmulator()) {
            return getPlatformFromExistingEmulator(launcher, emuConfig);
        }

        // Otherwise, use the configured platform
        return emuConfig.getOsVersion().getTargetName();
    }

    /**
     * Determines the Android platform for an existing emulator, via its metadata config file.
     *
     * @param launcher Used to access files on the remote node.
     * @param emuConfig The emulator whose target platform we want to determine.
     * @return The platform identifier.
     */
    private static String getPlatformFromExistingEmulator(final Launcher launcher,
            final EmulatorConfig emuConfig) throws IOException, InterruptedException {
        return launcher.getChannel().call(new Callable<String, IOException>() {
            public String call() throws IOException {
                File metadataFile = emuConfig.getAvdMetadataFile(launcher.isUnix());
                Map<String, String> metadata = Utils.parseConfigFile(metadataFile);
                return metadata.get("target");
            }
            private static final long serialVersionUID = 1L;
        });
    }

    /**
     * Writes the configuration file required to opt out of SDK usage statistics gathering.
     *
     * @param launcher Used for running tasks on the remote node.
     * @param listener Used to access logger.
     */
    private static void optOutOfSdkStatistics(Launcher launcher, BuildListener listener) {
        Callable<Void, Exception> optOutTask = new StatsOptOutTask(launcher.isUnix(), listener);
        try {
            launcher.getChannel().call(optOutTask);
        } catch (Exception e) {
            log(listener.getLogger(), "SDK statistics opt-out failed.", e);
        }
    }

    /**
     * Acquires an exclusive lock for the machine we're executing on.
     * <p>
     * The lock only has one permit, meaning that other executors on the same node which want to
     * install SDK components will block here until the lock is released by another executor.
     *
     * @return The semaphore for the current machine, which must be released once finished with.
     */
    private static Semaphore acquireLock() throws InterruptedException {
        // Retrieve the lock for this node
        Semaphore semaphore;
        final Node node = Computer.currentComputer().getNode();
        synchronized (node) {
            semaphore = mutexByNode.get(node);
            if (semaphore == null) {
                semaphore = new Semaphore(1);
                mutexByNode.put(node, semaphore);
            }
        }

        // Block until the lock is available
        semaphore.acquire();
        return semaphore;
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
    private static boolean isSdkInstallComplete(Node node, final String sdkRoot)
            throws IOException, InterruptedException {
        // Validation needs to run on the remote node
        ValidationResult result = node.getChannel().call(new Callable<ValidationResult, InterruptedException>() {
            public ValidationResult call() throws InterruptedException {
                return Utils.validateAndroidHome(new File(sdkRoot), false);
            }
            private static final long serialVersionUID = 1L;
        });

        if (result.isFatal()) {
            // No, we're missing some tools
            return false;
        }

        // SDK is complete if we got as far as writing the metadata file
        return getInstallationInfoFilename(node).exists();
    }

    /** Gets the path of our installation metadata file for the given node. */
    private static final FilePath getInstallationInfoFilename(Node node) {
        return Utils.getSdkInstallDirectory(node).child(SDK_INFO_FILENAME);
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

    /** Helper to run SDK statistics opt-out task on a remote node. */
    private static final class StatsOptOutTask implements Callable<Void, Exception> {

        private static final long serialVersionUID = 1L;
        private final boolean isUnix;

        private final BuildListener listener;
        private transient PrintStream logger;

        public StatsOptOutTask(boolean isUnix, BuildListener listener) {
            this.isUnix = isUnix;
            this.listener = listener;
        }

        public Void call() throws Exception {
            if (logger == null) {
                logger = listener.getLogger();
            }

            final File homeDir = Utils.getHomeDirectory(isUnix);
            final File androidDir = new File(homeDir, ".android");
            androidDir.mkdirs();

            File configFile = new File(androidDir, "ddms.cfg");
            PrintWriter out;
            try {
                out = new PrintWriter(configFile);
                out.println("pingOptIn=false");
                out.flush();
                out.close();
            } catch (FileNotFoundException e) {
                log(logger, "Failed to automatically opt out of SDK statistics gathering.", e);
            }

            return null;
        }
    }

    /** Helper for getting platform-specific SDK installation information. */
    enum AndroidInstaller {

        LINUX("linux", "tgz"),
        MAC_OS_X("macosx", "zip"),
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
            throw new SdkUnavailableException(Messages.SDK_UNAVAILABLE(prop));
        }

        static final class SdkUnavailableException extends Exception {
            private SdkUnavailableException(String message) {
                super(message);
            }
            private static final long serialVersionUID = 1L;
        }
    }

}
