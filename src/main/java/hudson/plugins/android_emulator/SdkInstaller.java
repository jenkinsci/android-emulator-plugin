package hudson.plugins.android_emulator;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.plugins.android_emulator.SdkInstaller.AndroidInstaller.SdkUnavailableException;
import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.sdk.cli.SdkCliCommandFactory;
import hudson.plugins.android_emulator.sdk.cli.SdkToolsCommands;
import hudson.plugins.android_emulator.sdk.cli.SdkCliCommand;
import hudson.plugins.android_emulator.util.Utils;
import hudson.plugins.android_emulator.util.ConfigFileUtils;
import hudson.plugins.android_emulator.util.ValidationResult;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;
import jenkins.MasterToSlaveFileCallable;
import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Semaphore;

import static hudson.plugins.android_emulator.AndroidEmulator.log;

public class SdkInstaller {

    /** Filename to write some metadata to about our automated installation. */
    private static final String SDK_INFO_FILENAME = ".jenkins-install-info";

    /** Map of nodes to locks, to ensure only one executor attempts SDK installation at once. */
    private static final Map<Node, Semaphore> mutexByNode = new WeakHashMap<Node, Semaphore>();

    /**
     * Downloads and installs the Android SDK on the machine we're executing on.
     *
     * @return An {@code AndroidSdk} object for the newly-installed SDK.
     */
    public static AndroidSdk install(Launcher launcher, BuildListener listener, String androidSdkHome)
            throws SdkInstallationException, IOException, InterruptedException {
        Semaphore semaphore = acquireLock();
        try {
            return doInstall(launcher, listener, androidSdkHome);
        } finally {
            semaphore.release();
        }
    }

    private static AndroidSdk doInstall(Launcher launcher, BuildListener listener, String androidSdkHome)
            throws SdkInstallationException, IOException, InterruptedException {
        // We should install the SDK on the current build machine
        Node node = Computer.currentComputer().getNode();

        // Install the SDK if required
        String androidHome;
        try {
            androidHome = installBasicSdk(listener, node).getRemote();
        } catch (IOException e) {
            throw new SdkInstallationException(Messages.SDK_DOWNLOAD_FAILED(), e);
        } catch (SdkUnavailableException e) {
            throw new SdkInstallationException(Messages.SDK_DOWNLOAD_FAILED(), e);
        }

        // Check whether we need to install the SDK components
        if (!isSdkInstallComplete(node, androidHome)) {
            PrintStream logger = listener.getLogger();
            log(logger, Messages.INSTALLING_REQUIRED_COMPONENTS());
            AndroidSdk sdk = getAndroidSdkForNode(node, androidHome, androidSdkHome);

            // Upgrade the tools if necessary and add the latest build-tools component
            List<String> components = new ArrayList<String>(5);

            // do not update 'tools', as they were updated above to current compatible version

            // Get the latest platform-tools
            components.add("platform-tool");

            String buildTools = getBuildToolsPackageName(logger, launcher, sdk);
            if (buildTools != null) {
                components.add(buildTools);
            }

            // Add the local maven repos for Gradle
            components.add("extra-android-m2repository");
            components.add("extra-google-m2repository");
            components.add("emulator");

            // Install the lot
            installComponent(logger, launcher, sdk, components);


            // As this SDK will not be used manually, opt out of the stats gathering;
            // this also prevents the opt-in dialog from popping up during execution
            optOutOfSdkStatistics(launcher, listener, androidSdkHome);
        }

        // If we made it this far, confirm completion by writing our our metadata file
        getInstallationInfoFilename(node).write(Constants.SDK_TOOLS_DEFAULT_VERSION, "UTF-8");

        // Create an SDK object now that all the components exist
        return Utils.getAndroidSdk(launcher, androidHome, androidSdkHome);
    }

    @SuppressWarnings("serial")
    private static AndroidSdk getAndroidSdkForNode(Node node, final String androidHome,
            final String androidSdkHome) throws IOException, InterruptedException {
        return node.getChannel().call(new MasterToSlaveCallable<AndroidSdk, IOException>() {
            public AndroidSdk call() throws IOException {
                return new AndroidSdk(androidHome, androidSdkHome);
            }
        });
    }

    private static String getBuildToolsPackageName(PrintStream logger, Launcher launcher, AndroidSdk sdk)
    throws IOException, InterruptedException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        final SdkCliCommand sdkListComponentsCmd = SdkCliCommandFactory.getCommandsForSdk(sdk)
                .getListSdkComponentsCommand();
        Utils.runAndroidTool(launcher, output, logger, sdk, sdkListComponentsCmd, null);
        return Utils.getPatternWithHighestSuffixedVersionNumberInMultiLineInput(output.toString(), "build-tools");
    }

    /**
     * Downloads and extracts the basic Android SDK on a given Node, if it hasn't already been done.
     *
     *
     * @param node Node to install the SDK on.
     * @return Path where the SDK is installed, regardless of whether it was installed right now.
     * @throws SdkUnavailableException If the Android SDK is not available on this platform.
     */
    private static FilePath installBasicSdk(final BuildListener listener, Node node)
            throws SdkUnavailableException, IOException, InterruptedException {
        // Locate where the SDK should be installed to on this node
        final FilePath installDir = Utils.getSdkInstallDirectory(node);

        // Get the OS-specific download URL for the SDK
        AndroidInstaller installer = AndroidInstaller.fromNode(node);
        final URL downloadUrl = installer.getUrl(Constants.SDK_TOOLS_DEFAULT_BUILD_ID);

        final FilePath toolsSubdir = installDir.child("tools");

        // Download the SDK, if required
        boolean wasNowInstalled = installDir.act(new MasterToSlaveFileCallable<Boolean>() {
            public Boolean invoke(File f, VirtualChannel channel)
                    throws InterruptedException, IOException {
                String msg = Messages.DOWNLOADING_SDK_FROM(downloadUrl);
                return toolsSubdir.installIfNecessaryFrom(downloadUrl, listener, msg);
            }
            private static final long serialVersionUID = 1L;
        });

        if (wasNowInstalled) {
            // If the SDK was required, pull files up from the intermediate directory
            toolsSubdir.listDirectories().get(0).moveAllChildrenTo(toolsSubdir);

            // Java's ZipEntry doesn't preserve the executable bit...
            if (installer == AndroidInstaller.MAC_OS_X) {
                setPermissions(toolsSubdir);
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
     * @param sdk Root of the SDK installation to install components for.
     * @param components Name of the component(s) to install.
     */
    private static void installComponent(PrintStream logger, Launcher launcher, AndroidSdk sdk,
            final List<String> components) throws IOException, InterruptedException {
        String proxySettings = getProxySettings();

        // Build the command to install the given component(s)
        log(logger, Messages.INSTALLING_SDK_COMPONENTS(StringUtils.join(components, ',')));
        final SdkCliCommand sdkInstallAndUpdateCmd = SdkCliCommandFactory.getCommandsForSdk(sdk)
                .getSdkInstallAndUpdateCommand(proxySettings, components);
        ArgumentListBuilder cmd = Utils.getToolCommand(sdk, launcher.isUnix(), sdkInstallAndUpdateCmd);
        ProcStarter procStarter = launcher.launch().stderr(logger).readStdout().writeStdin().cmds(cmd);

        final EnvVars env = new EnvVars();
        env.put(Constants.ENV_VAR_ANDROID_USE_SDK_WRAPPER, "y");
        if (sdk.hasKnownHome()) {
            env.put(Constants.ENV_VAR_ANDROID_SDK_HOME, sdk.getSdkHome());
        }
        procStarter = procStarter.envs(env);

        // Run the command and accept any licence requests during installation
        Proc proc = procStarter.start();
        BufferedReader r = new BufferedReader(new InputStreamReader(proc.getStdout()));
        String line;
        while (proc.isAlive() && (line = r.readLine()) != null) {
            logger.println(line);
            if (line.toLowerCase(Locale.ENGLISH).startsWith("license id: ") ||
                    line.toLowerCase(Locale.ENGLISH).startsWith("license android-sdk")) {
                proc.getStdin().write("y\r\n".getBytes());
                proc.getStdin().flush();
            }
        }
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
        final boolean skipSystemImageInstall = emuConfig.isNamedEmulator()
                || !emuConfig.getOsVersion().requiresAbi();
        installPlatform(logger, launcher, sdk, platform, emuConfig.getTargetAbi(), skipSystemImageInstall);
    }

    /**
     * Installs the given platform and its dependencies into the given installation, if necessary.
     *
     * @param logger Logs things.
     * @param launcher Used to launch tasks on the remote node.
     * @param sdk SDK installation to install components for.
     * @param platform Specifies the platform to be installed.
     * @param abi Specifies the ABI to be installed; may be {@code null}.
     * @param skipSystemImageInstall Specifies that the system image does not need to be installed (useful for named emulator)
     */
    public static void installPlatform(PrintStream logger, Launcher launcher, AndroidSdk sdk,
            String platform, String abi, final boolean skipSystemImageInstall) throws IOException, InterruptedException {

        final AndroidPlatform androidPlatform = AndroidPlatform.valueOf(platform);
        if (androidPlatform == null) {
            log(logger, Messages.SDK_PLATFORM_STRING_UNRECOGNISED(platform));
            return;
        }

        // Check whether this platform is already installed
        if (isPlatformInstalled(logger, launcher, sdk, androidPlatform.getName(), abi, skipSystemImageInstall)) {
            return;
        }

        // Check whether we are capable of installing individual components
        log(logger, Messages.PLATFORM_INSTALL_REQUIRED(androidPlatform.getName()));
        if (!launcher.isUnix() && androidPlatform.isCustomPlatform() && sdk.getSdkToolsMajorVersion() < 16) {
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
        if ((androidPlatform.getSdkLevel() == 14 || androidPlatform.getSdkLevel() == 15) && !sdk.supportsSystemImageInstallation()) {
            log(logger, Messages.ABI_INSTALLATION_UNSUPPORTED(), true);
        }

        // Determine which individual component(s) need to be installed for this platform
        List<String> components = getSdkComponentsForPlatform(logger, sdk, androidPlatform, abi, skipSystemImageInstall);
        if (components == null || components.size() == 0) {
            return;
        }

        // If a platform expanded to multiple dependencies (e.g. "GoogleMaps:7" -> android-7 + Maps)
        // then check whether we really need to install android-7, as it may already be installed
        if (components.size() > 1) {
            for (Iterator<String> it = components.iterator(); it.hasNext(); ) {
                String component = it.next();
                if (isPlatformInstalled(logger, launcher, sdk, component, null, true)) {
                    it.remove();
                }
            }
        }

        // Grab the lock and attempt installation
        Semaphore semaphore = acquireLock();
        try {
            installComponent(logger, launcher, sdk, components);
        } finally {
            semaphore.release();
        }
    }

    private static boolean isPlatformInstalled(PrintStream logger, Launcher launcher,
            AndroidSdk sdk, String platform, String abi,
            final boolean skipSystemInstall) throws IOException, InterruptedException {
        ByteArrayOutputStream targetList = new ByteArrayOutputStream();
        final SdkCliCommand sdkListTargets = SdkCliCommandFactory.getCommandsForSdk(sdk)
                .getListExistingTargetsCommand();
        Utils.runAndroidTool(launcher, targetList, logger, sdk, sdkListTargets, null);
        boolean platformInstalled = targetList.toString().contains('"'+ platform +'"');
        if (!platformInstalled) {
            return false;
        }

        if (!skipSystemInstall && abi != null) {
            final ByteArrayOutputStream systemImagesList = new ByteArrayOutputStream();
            final SdkToolsCommands sdkToolsCommand = SdkCliCommandFactory.getCommandsForSdk(sdk);

            final SdkCliCommand sdkListSystemImages = sdkToolsCommand.getListSystemImagesCommand();
            Utils.runAndroidTool(launcher, systemImagesList, logger, sdk, sdkListSystemImages, null);
            final boolean isAbiImageInstalled = sdkToolsCommand.isImageForPlatformAndABIInstalled(
                    systemImagesList.toString(), platform, abi);
            if (!isAbiImageInstalled) {
                return false;
            }
        }

        // Everything we wanted is installed
        return true;
    }

    private static List<String> getSdkComponentsForPlatform(final PrintStream logger,
            final AndroidSdk sdk, final AndroidPlatform androidPlatform, final String abi,
            final boolean skipSystemImageInstall) {
        // Gather list of required components
        List<String> components = new ArrayList<String>();

        // Add dependent platform (eg: 'android-17')
        if (androidPlatform.getSdkLevel() > 0) {
            components.add(androidPlatform.getTargetName());
        }

        // Add system image, if required
        // Even if a system image doesn't exist for this platform, the installer silently ignores it
        if (!skipSystemImageInstall && androidPlatform.getSdkLevel() >= 10 && abi != null) {
            if (sdk.supportsSystemImageNewFormat()) {
                components.add(androidPlatform.getSystemImageName(abi));
            } else {
                components.add(androidPlatform.getSystemImageNameLegacyFormat());
            }
        }

        // If it's a non straightforward case like "Google Inc.:Google APIs:10" we need to add the addon as well
        if (androidPlatform.isCustomPlatform()) {
            components.add(androidPlatform.getAddonName());
        }

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
    private static String getPlatformFromExistingEmulator(Launcher launcher,
            final EmulatorConfig emuConfig) throws IOException, InterruptedException {
        return launcher.getChannel().call(new MasterToSlaveCallable<String, IOException>() {
            public String call() throws IOException {
                File metadataFile = emuConfig.getAvdMetadataFile();
                Map<String, String> metadata = ConfigFileUtils.parseConfigFile(metadataFile);
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
    public static void optOutOfSdkStatistics(Launcher launcher, BuildListener listener, String androidSdkHome) {
        Callable<Void, Exception> optOutTask = new StatsOptOutTask(androidSdkHome, listener);
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
        ValidationResult result = node.getChannel().call(new MasterToSlaveCallable<ValidationResult, InterruptedException>() {
            public ValidationResult call() throws InterruptedException {
                return Utils.validateAndroidHome(new File(sdkRoot), false, false);
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
    private static final class StatsOptOutTask extends MasterToSlaveCallable<Void, Exception> {

        private static final long serialVersionUID = 1L;
        private final String androidSdkHome;

        private final BuildListener listener;
        private transient PrintStream logger;

        public StatsOptOutTask(String androidSdkHome, BuildListener listener) {
            this.androidSdkHome = androidSdkHome;
            this.listener = listener;
        }

        public Void call() throws Exception {
            if (logger == null) {
                logger = listener.getLogger();
            }

            final File homeDir = Utils.getAndroidSdkHomeDirectory(androidSdkHome);
            final File androidDir = new File(homeDir, ".android");
            androidDir.mkdirs();

            File configFile = new File(androidDir, "ddms.cfg");
            PrintWriter out;
            try {
                out = new PrintWriter(configFile);
                out.println("pingOptIn=false");
                out.println("pingId=0");
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

        LINUX("linux", "zip"),
        MAC_OS_X("darwin", "zip"),
        WINDOWS("windows", "zip");

        private static final String PATTERN = "http://dl.google.com/android/repository/sdk-tools-%s-%s.%s";
        private final String platform;
        private final String extension;

        private AndroidInstaller(String platform, String extension) {
            this.platform = platform;
            this.extension = extension;
        }

        URL getUrl(String version) {
            try {
                return new URL(String.format(PATTERN, platform, version, extension));
            } catch (MalformedURLException e) {}
            return null;
        }

        static AndroidInstaller fromNode(Node node) throws SdkUnavailableException,
                IOException, InterruptedException {
            return node.getChannel().call(new MasterToSlaveCallable<AndroidInstaller, SdkUnavailableException>() {
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
