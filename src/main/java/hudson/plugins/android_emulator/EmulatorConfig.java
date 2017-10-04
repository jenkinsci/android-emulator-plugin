package hudson.plugins.android_emulator;

import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.plugins.android_emulator.AndroidEmulator.HardwareProperty;
import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.sdk.Tool;
import hudson.plugins.android_emulator.sdk.cli.SdkCliCommand;
import hudson.plugins.android_emulator.sdk.cli.SdkCliCommandFactory;
import hudson.plugins.android_emulator.util.ConfigFileUtils;
import hudson.plugins.android_emulator.util.StdoutReader;
import hudson.plugins.android_emulator.util.Utils;
import hudson.remoting.Callable;
import hudson.util.ArgumentListBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Map;
import java.util.regex.Pattern;
import jenkins.security.MasterToSlaveCallable;

import org.apache.commons.lang.exception.ExceptionUtils;

class EmulatorConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String avdName;
    private AndroidPlatform osVersion;
    private ScreenDensity screenDensity;
    private ScreenResolution screenResolution;
    private String deviceLocale;
    private String sdCardSize;
    private String targetAbi;
    private String deviceDefinition;
    private boolean wipeData;
    private final boolean showWindow;
    private final boolean useSnapshots;
    private final String commandLineOptions;
    private final String androidSdkHome;
    private final String executable;
    private final String avdNameSuffix;

    private EmulatorConfig(String avdName, boolean wipeData, boolean showWindow,
            boolean useSnapshots, String commandLineOptions, String androidSdkHome, String executable, String
            avdNameSuffix) {
        this.avdName = avdName;
        this.wipeData = wipeData;
        this.showWindow = showWindow;
        this.useSnapshots = useSnapshots;
        this.commandLineOptions = commandLineOptions;
        this.androidSdkHome = androidSdkHome;
        this.executable = executable;
        this.avdNameSuffix = avdNameSuffix;
    }

    private EmulatorConfig(String osVersion, String screenDensity, String screenResolution,
            String deviceLocale, String sdCardSize, boolean wipeData, boolean showWindow,
            boolean useSnapshots, String commandLineOptions, String targetAbi, String deviceDefinition,
            String androidSdkHome, String executable, String avdNameSuffix)
                throws IllegalArgumentException {
        if (osVersion == null || screenDensity == null || screenResolution == null) {
            throw new IllegalArgumentException("Valid OS version and screen properties must be supplied.");
        }

        // Normalise incoming variables
        int targetLength = osVersion.length();
        if (targetLength > 2 && osVersion.startsWith("\"") && osVersion.endsWith("\"")) {
            osVersion = osVersion.substring(1, targetLength - 1);
        }
        screenDensity = screenDensity.toLowerCase();
        if (screenResolution.matches("(?i)"+ Constants.REGEX_SCREEN_RESOLUTION_ALIAS)) {
            screenResolution = screenResolution.toUpperCase();
        } else if (screenResolution.matches("(?i)"+ Constants.REGEX_SCREEN_RESOLUTION)) {
            screenResolution = screenResolution.toLowerCase();
        }
        if (deviceLocale != null && deviceLocale.length() > 4) {
            deviceLocale = deviceLocale.substring(0, 2).toLowerCase() +"_"
                + deviceLocale.substring(3).toUpperCase();
        }

        this.osVersion = AndroidPlatform.valueOf(osVersion);
        if (this.osVersion == null) {
            throw new IllegalArgumentException(
                    "OS version not recognised: " + osVersion);
        }
        this.screenDensity = ScreenDensity.valueOf(screenDensity);
        if (this.screenDensity == null) {
            throw new IllegalArgumentException(
                    "Screen density not recognised: " + screenDensity);
        }
        this.screenResolution = ScreenResolution.valueOf(screenResolution);
        if (this.screenResolution == null) {
            throw new IllegalArgumentException(
                    "Screen resolution not recognised: " + screenResolution);
        }
        this.deviceLocale = deviceLocale;
        this.sdCardSize = sdCardSize;
        this.wipeData = wipeData;
        this.showWindow = showWindow;
        this.useSnapshots = useSnapshots;
        this.commandLineOptions = commandLineOptions;
        if (targetAbi != null && targetAbi.startsWith("default/")) {
            targetAbi = targetAbi.replace("default/", "");
        }
        this.targetAbi = targetAbi;
        this.deviceDefinition = deviceDefinition;
        this.androidSdkHome = androidSdkHome;
        this.executable = executable;
        this.avdNameSuffix = avdNameSuffix;
    }

    public static final EmulatorConfig create(String avdName, String osVersion, String screenDensity,
            String screenResolution, String deviceLocale, String sdCardSize, boolean wipeData,
            boolean showWindow, boolean useSnapshots, String commandLineOptions, String targetAbi,
            String deviceDefinition, String androidSdkHome, String executable, String avdNameSuffix) {
        if (Util.fixEmptyAndTrim(avdName) == null) {
            return new EmulatorConfig(osVersion, screenDensity, screenResolution, deviceLocale, sdCardSize, wipeData,
                    showWindow, useSnapshots, commandLineOptions, targetAbi, deviceDefinition, androidSdkHome, executable, avdNameSuffix);
        }

        return new EmulatorConfig(avdName, wipeData, showWindow, useSnapshots, commandLineOptions, androidSdkHome, executable,
                avdNameSuffix);
    }

    public static final String getAvdName(String avdName, String osVersion, String screenDensity,
            String screenResolution, String deviceLocale, String targetAbi, String deviceDefinition,
            String avdNameSuffix) {
        try {
            return create(avdName, osVersion, screenDensity, screenResolution, deviceLocale, null, false, false, false,
                    null, targetAbi, deviceDefinition, null, null, avdNameSuffix).getAvdName();
        } catch (IllegalArgumentException e) {}
        return null;
    }

    public boolean isNamedEmulator() {
        return avdName != null && osVersion == null;
    }

    public String getAvdName() {
        if (isNamedEmulator()) {
            return avdName;
        }

        return getGeneratedAvdName();
    }

    private String getGeneratedAvdName() {
        String locale = getDeviceLocale().replace('_', '-');
        String density = screenDensity.toString();
        String resolution = screenResolution.toString();
        String platform = osVersion.getTargetName().replaceAll("[^a-zA-Z0-9._-]", "_");
        String abi = "";
        if (targetAbi != null && osVersion.requiresAbi()) {
            abi = "_" + targetAbi.replaceAll("[^a-zA-Z0-9._-]", "-");
        }
        String deviceDef = "";
        if (deviceDefinition != null && !deviceDefinition.isEmpty()) {
            deviceDef = "_" + deviceDefinition.replaceAll("[^a-zA-Z0-9._-]", "-");
        }
        String suffix = "";
        if (avdNameSuffix != null) {
            suffix = "_" + avdNameSuffix.replaceAll("[^a-zA-Z0-9._-]", "-");
        }

        return String.format("hudson_%s_%s_%s_%s%s%s%s", locale, density, resolution, platform, abi, deviceDef, suffix);
    }

    public AndroidPlatform getOsVersion() {
        return osVersion;
    }

    public String getTargetAbi() { return targetAbi; }

    public String getDeviceDefinition() {
        return deviceDefinition;
    }

    public ScreenDensity getScreenDensity() {
        return screenDensity;
    }

    public ScreenResolution getScreenResolution() {
        return screenResolution;
    }

    public String getDeviceLocale() {
        if (deviceLocale == null) {
            return Constants.DEFAULT_LOCALE;
        }
        return deviceLocale;
    }

    public String getDeviceLanguage() {
        return getDeviceLocale().substring(0, 2);
    }

    public String getDeviceCountry() {
        return getDeviceLocale().substring(3);
    }

    public String getSdCardSize() {
        return sdCardSize;
    }

    public void setShouldWipeData() {
        wipeData = true;
    }

    public boolean shouldWipeData() {
        return wipeData;
    }

    public boolean shouldShowWindow() {
        return showWindow;
    }

    public boolean shouldUseSnapshots() {
        return useSnapshots;
    }

    public Tool getExecutable() {
        for (Tool t : Tool.EMULATORS) {
            if (t.executable.equals(executable)) {
                return t;
            }
        }
        return Tool.EMULATOR;
    }

    /**
     * Gets a task that ensures that an Android AVD exists for this instance's configuration.
     *
     * @param androidSdk  The Android SDK to use.
     * @param listener The listener to use for logging.
     * @return A Callable that will handle the detection/creation of an appropriate AVD.
     */
    public Callable<Boolean, AndroidEmulatorException> getEmulatorCreationTask(AndroidSdk androidSdk,
                                                                               BuildListener listener) {
        return new EmulatorCreationTask(androidSdk, listener);
    }

    /**
     * Gets a task that updates the hardware properties of the AVD for this instance.
     *
     *
     * @param hardwareProperties  The hardware properties to update the AVD with.
     * @param listener The listener to use for logging.
     * @return A Callable that will update the config of the current AVD.
     */
    public Callable<Void, IOException> getEmulatorConfigTask(HardwareProperty[] hardwareProperties,
                                                             BuildListener listener) {
        return new EmulatorConfigTask(hardwareProperties, listener);
    }

    /**
     * Gets a task that writes an empty emulator auth file to the machine where the AVD will run.
     *
     * @return A Callable that will write an empty auth file.
     */
    public Callable<Void, IOException> getEmulatorAuthFileTask() {
        return new EmulatorAuthFileTask();
    }

    /**
     * Gets a task that deletes the AVD corresponding to this instance's configuration.
     *
     *
     * @param listener The listener to use for logging.
     * @return A Callable that will delete the AVD with for this configuration.
     */
    public Callable<Boolean, Exception> getEmulatorDeletionTask(TaskListener listener) {
        return new EmulatorDeletionTask(listener);
    }

    private File getAvdHome(final File homeDir) {
        return new File(homeDir, ".android/avd/");
    }

    private File getAvdDirectory(final File homeDir) {
        return new File(getAvdHome(homeDir), getAvdName() +".avd");
    }

    public File getAvdMetadataFile() {
        final File homeDir = Utils.getAndroidSdkHomeDirectory(androidSdkHome);
        return new File(getAvdHome(homeDir), getAvdName() + ".ini");
    }

    private File getAvdConfigFile(File homeDir) {
        return new File(getAvdDirectory(homeDir), "config.ini");
    }

    private Map<String,String> parseAvdConfigFile(File homeDir) throws IOException {
        File configFile = getAvdConfigFile(homeDir);
        return ConfigFileUtils.parseConfigFile(configFile);
    }

    private void writeAvdConfigFile(File homeDir, Map<String,String> values) throws IOException {
        final File configFile = getAvdConfigFile(homeDir);
        ConfigFileUtils.writeConfigFile(configFile, values);
    }

    /**
     * Sets or overwrites a key-value pair in the AVD config file.
     *
     * @param homeDir AVD home directory.
     * @param key Key to set.
     * @param value Value to set.
     * @throws EmulatorCreationException If reading or writing the file failed.
     */
    private void setAvdConfigValue(File homeDir, String key, String value)
            throws EmulatorCreationException {
        Map<String, String> configValues;
        try {
            configValues = parseAvdConfigFile(homeDir);
            configValues.put(key, value);
            writeAvdConfigFile(homeDir, configValues);
        } catch (IOException e) {
            throw new EmulatorCreationException(Messages.AVD_CONFIG_NOT_READABLE(), e);
        }
    }

    /**
     * Gets the command line arguments to pass to "emulator" based on this instance.
     *
     * @return A string of command line arguments.
     */
    public String getCommandArguments(SnapshotState snapshotState, final AndroidSdk androidSdk,
            int userPort, int adbPort, int callbackPort, int consoleTimeout) {
        StringBuilder sb = new StringBuilder();

        // Stick to using the original version of the emulator for now, as otherwise we can't use
        // the "-ports" command line flag, which we need to stay outside of the regular port range,
        // nor can we use the "-prop" or "-report-console" command line flags that we require.
        //
        // See Android bugs 37085830, 37086012, 37090815 and 37090817
        if (androidSdk.forceClassicEmulatorEngine()) {
            sb.append(" -engine classic");
        }

        // screen resolution not supported at creation time in Android Emulator 2.0
        // so it is added here as skin on emulator start
        if (androidSdk.supportsEmulatorV2() && getScreenResolution() != null) {
            sb.append(String.format(" -skin %s", getScreenResolution().getDimensionString()));
        }

        // Tell the emulator to use certain ports
        sb.append(String.format(" -ports %s,%s", userPort, adbPort));

        // Ask the emulator to report to us on the given port, once initial startup is complete
        sb.append(String.format(" -report-console tcp:%s,max=%s", callbackPort, consoleTimeout));

        // Set the locale to be used at startup
        if (!isNamedEmulator()) {
            sb.append(" -prop persist.sys.language=");
            sb.append(getDeviceLanguage());
            sb.append(" -prop persist.sys.country=");
            sb.append(getDeviceCountry());
        }

        // Set the ID of the AVD we want to start
        sb.append(" -avd ");
        sb.append(getAvdName());

        // Snapshots
        if (snapshotState == SnapshotState.BOOT) {
            // For builds after initial snapshot setup, start directly from the "jenkins" snapshot
            sb.append(" -snapshot "+ Constants.SNAPSHOT_NAME);
            sb.append(" -no-snapshot-save");
        } else if (androidSdk.supportsSnapshots()) {
            // For the first boot, or snapshot-free builds, do not load any snapshots that may exist
            sb.append(" -no-snapshot-load");
            sb.append(" -no-snapshot-save");
        }

        // Options
        if (shouldWipeData()) {
            sb.append(" -wipe-data");
        }
        if (!shouldShowWindow()) {
            sb.append(" -no-window");
        }
        if (commandLineOptions != null) {
            sb.append(" ");
            sb.append(commandLineOptions);
        }

        return sb.toString();
    }

    /**
     * Determines whether a snapshot image has already been created for this emulator.
     *
     * @throws IOException If execution of the emulator command fails.
     * @throws InterruptedException If execution of the emulator command is interrupted.
     */
    public boolean hasExistingSnapshot(Launcher launcher, AndroidSdk androidSdk)
            throws IOException, InterruptedException {
        final PrintStream logger = launcher.getListener().getLogger();

        // List available snapshots for this emulator
        ByteArrayOutputStream listOutput = new ByteArrayOutputStream();

        final SdkCliCommand sdkEmulatorListSnapshotsCmd = SdkCliCommandFactory.getCommandsForSdk(androidSdk)
                .getEmulatorListSnapshotsCommand(getAvdName(), androidSdk.requiresAndroidBug34233Workaround());
        Utils.runAndroidTool(launcher, listOutput, logger, androidSdk, sdkEmulatorListSnapshotsCmd, null);

        // Check whether a Jenkins snapshot was listed in the output
        return Pattern.compile(Constants.REGEX_SNAPSHOT).matcher(listOutput.toString()).find();
    }

    /**
     * A task that locates or creates an AVD based on our local state.
     *
     * Returns {@code TRUE} if an AVD already existed with these properties, otherwise returns
     * {@code FALSE} if an AVD was newly created, and throws an AndroidEmulatorException if the
     * given AVD or parts required to generate a new AVD were not found.
     */
    private final class EmulatorCreationTask extends MasterToSlaveCallable<Boolean, AndroidEmulatorException> {

        private static final long serialVersionUID = 1L;
        private final AndroidSdk androidSdk;

        private final BuildListener listener;
        private transient PrintStream logger;

        public EmulatorCreationTask(AndroidSdk androidSdk, BuildListener listener) {
            this.androidSdk = androidSdk;
            this.listener = listener;
        }

        public Boolean call() throws AndroidEmulatorException {
            if (logger == null) {
                logger = listener.getLogger();
            }

            final File homeDir = Utils.getAndroidSdkHomeDirectory(androidSdk.getSdkHome());
            final File avdDirectory = getAvdDirectory(homeDir);
            final boolean emulatorExists = getAvdConfigFile(homeDir).exists();

            // Can't do anything if a named emulator doesn't exist
            if (isNamedEmulator() && !emulatorExists) {
                throw new EmulatorDiscoveryException(Messages.AVD_DOES_NOT_EXIST(avdName, avdDirectory));
            }

            // Check whether AVD needs to be created
            boolean createSdCard = false;
            boolean createSnapshot = false;
            File snapshotsFile = new File(getAvdDirectory(homeDir), "snapshots.img");
            if (emulatorExists) {
                // AVD exists: check whether there's anything still to be set up
                File sdCardFile = new File(getAvdDirectory(homeDir), "sdcard.img");
                boolean sdCardRequired = getSdCardSize() != null;

                // Check if anything needs to be done for snapshot-enabled builds
                if (shouldUseSnapshots() && androidSdk.supportsSnapshots()) {
                    if (!snapshotsFile.exists()) {
                        createSnapshot = true;
                    }

                    // We should ensure that we start out with a clean SD card for the build
                    if (sdCardRequired && sdCardFile.exists()) {
                        sdCardFile.delete();
                    }
                }

                // Flag that we need to generate an SD card, if there isn't one existing
                if (sdCardRequired && !sdCardFile.exists()) {
                    createSdCard = true;
                }

                // If everything is ready, then return
                if (!createSdCard && !createSnapshot) {
                    return true;
                }
            } else {
                AndroidEmulator.log(logger, Messages.CREATING_AVD(avdDirectory));
            }

            // We can't continue if we don't know where to find emulator images or tools
            if (!androidSdk.hasKnownRoot()) {
                throw new EmulatorCreationException(Messages.SDK_NOT_SPECIFIED());
            }
            final File sdkRoot = new File(androidSdk.getSdkRoot());
            if (!sdkRoot.exists()) {
                throw new EmulatorCreationException(Messages.SDK_NOT_FOUND(androidSdk.getSdkRoot()));
            }

            // If we need to initialise snapshot support for an existing emulator, do so
            if (createSnapshot) {
                // Copy the snapshots file into place
                File snapshotDir = new File(sdkRoot, "tools/lib/emulator");
                Util.copyFile(new File(snapshotDir, "snapshots.img"), snapshotsFile);

                // Update the AVD config file mark snapshots as enabled
                setAvdConfigValue(homeDir, "snapshot.present", "true");
            }

            // If we need create an SD card for an existing emulator, do so
            if (createSdCard) {
                AndroidEmulator.log(logger, Messages.ADDING_SD_CARD(sdCardSize, getAvdName()));
                if (!createSdCard(homeDir)) {
                    throw new EmulatorCreationException(Messages.SD_CARD_CREATION_FAILED());
                }

                // Update the AVD config file
                setAvdConfigValue(homeDir, "sdcard.size", sdCardSize);
            }

            // Return if everything is now ready for use
            if (emulatorExists) {
                return true;
            }

            final SdkCliCommand sdkCreateAvdCmd = SdkCliCommandFactory.getCommandsForSdk(androidSdk)
                    .getCreatedAvdCommand(getAvdName(), androidSdk.supportsSnapshots(),
                            sdCardSize, screenResolution.getSkinName(), deviceDefinition,
                            osVersion.getTargetName(), osVersion.getPackagePathOfSystemImage(targetAbi),
                            osVersion.getTagFromAbiString(targetAbi));
            boolean isUnix = !Functions.isWindows();
            ArgumentListBuilder builder = Utils.getToolCommand(androidSdk, isUnix, sdkCreateAvdCmd);

            // avdmanager requires target version and target ABI as package path, so ABI is required
            if (Tool.AVDMANAGER.equals(sdkCreateAvdCmd.getTool())) {
                if (targetAbi == null || targetAbi.isEmpty()) {
                    AndroidEmulator.log(logger, Messages.ABI_REQUIRED());
                    throw new EmulatorCreationException(Messages.AVD_CREATION_FAILED());
                }
            } else if (targetAbi != null && osVersion.requiresAbi()) {
                // This is an unpleasant side-effect of there being an ABI for android-10,
                // and that Google renamed the image after its initial release from Intel...
                // Ideally, as stated in AndroidPlatform#requiresAbi, we should preferably check
                // via the "android list target" command whether an ABI is actually required.
                if (osVersion.getSdkLevel() != 10 || targetAbi.equals("armeabi")
                        || targetAbi.equals("x86")) {
                    builder.add("--abi");
                    builder.add(targetAbi);
                }
            }

            // Log command line used, for info
            AndroidEmulator.log(logger, builder.toStringWithQuote());

            // Run!
            final Process process;
            try {
                ProcessBuilder procBuilder = new ProcessBuilder(builder.toList());
                if (androidSdk.hasKnownHome()) {
                    procBuilder.environment().put(Constants.ENV_VAR_ANDROID_SDK_HOME, androidSdk.getSdkHome());
                }
                // Stderr and Stdout can be fetched via getOutputStream
                procBuilder.redirectErrorStream(true);
                process = procBuilder.start();
            } catch (IOException ex) {
                throw new EmulatorCreationException(Messages.AVD_CREATION_FAILED());
            }

            final OutputStream procstdin = process.getOutputStream();
            final StdoutReader procstdout = new StdoutReader(process.getInputStream());

            // Command may prompt us whether we want to further customise the AVD.
            // Just "press" Enter to continue with the selected target's defaults.
            try {
                int waitCnt = 0;
                while (process.isAlive()) {
                    final String line = procstdout.readLine();
                    if (line != null && !line.isEmpty()) {
                        AndroidEmulator.log(logger, line, true);

                        if (line.contains("custom hardware")) {
                            procstdin.write("no\r\n".getBytes());
                            procstdin.flush();
                        } else if (line.contains("list targets")) {
                            AndroidEmulator.log(logger, Messages.INVALID_AVD_TARGET(osVersion.getTargetName()));
                        } else if (line.contains("more than one ABI")) {
                            AndroidEmulator.log(logger, Messages.MORE_THAN_ONE_ABI(osVersion.getTargetName()), true);
                        }
                    } else {
                        // Write CRLF, if required
                        if (waitCnt++ > 5) {
                            waitCnt = 0;

                            AndroidEmulator.log(logger, "> Process took a while, may wait for input.", true);
                            AndroidEmulator.log(logger, "> <SENDING ENTER>", true);

                            try {
                                procstdin.write("\r\n".getBytes());
                                procstdin.flush();
                            } catch (IOException ioex) {
                                AndroidEmulator.log(logger, "> " + ioex.getMessage(), true);
                            }
                        }

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                        }
                    }
                }

                // Wait for happy ending
                process.waitFor();
            } catch (IOException e) {
                AndroidEmulator.log(logger, ExceptionUtils.getFullStackTrace(e), true);
                throw new EmulatorCreationException(Messages.AVD_CREATION_ABORTED(), e);
            } catch (InterruptedException e) {
                AndroidEmulator.log(logger, ExceptionUtils.getFullStackTrace(e), true);
                throw new EmulatorCreationException(Messages.AVD_CREATION_INTERRUPTED(), e);
            } finally {
                process.destroy();
            }

            // print the rest of stdout (for debugging purposes)
            final String output = procstdout.readContent();
            if (output != null && !output.isEmpty()) {
                    AndroidEmulator.log(logger, output, true);
            }

            // Do a sanity check to ensure the AVD was really created
            if (getAvdConfigFile(homeDir).exists()) {
                // Set the screen density
                setAvdConfigValue(homeDir, "hw.lcd.density", String.valueOf(getScreenDensity().getDpi()));
            } else {
                AndroidEmulator.log(logger, Messages.AVD_CREATION_FAILED());
                throw new EmulatorCreationException(Messages.AVD_CREATION_FAILED());
            }

            // Done!
            return false;
        }

        private boolean createSdCard(File homeDir) {
            final String absoluteSdCardName = new File(getAvdDirectory(homeDir), "sdcard.img").getAbsolutePath();
            final SdkCliCommand mksdcardCmd = SdkCliCommandFactory.getCommandsForSdk(androidSdk)
                    .getCreateSdkCardCommand(absoluteSdCardName, sdCardSize);

            ArgumentListBuilder builder = Utils.getToolCommand(androidSdk, !Functions.isWindows(), mksdcardCmd);

            // Run!
            try {
                ProcessBuilder procBuilder = new ProcessBuilder(builder.toList());
                if (androidSdkHome != null) {
                    procBuilder.environment().put(Constants.ENV_VAR_ANDROID_SDK_HOME, androidSdkHome);
                }
                procBuilder.start().waitFor();
            } catch (InterruptedException ex) {
                return false;
            } catch (IOException ex) {
                return false;
            }

            return true;
        }
    }

    /**
     * A task that updates the hardware properties of this AVD config.
     *
     * Throws an IOException if the AVD's config could not be read or written.
     */
    private final class EmulatorConfigTask extends MasterToSlaveCallable<Void, IOException> {

        private static final long serialVersionUID = 1L;

        private final HardwareProperty[] hardwareProperties;
        private final BuildListener listener;
        private transient PrintStream logger;

        public EmulatorConfigTask(HardwareProperty[] hardwareProperties, BuildListener listener) {
            this.hardwareProperties = hardwareProperties;
            this.listener = listener;
        }

        public Void call() throws IOException {
            if (logger == null) {
                logger = listener.getLogger();
            }

            final File homeDir = Utils.getAndroidSdkHomeDirectory(androidSdkHome);

            // Parse the AVD's config
            Map<String, String> configValues;
            configValues = parseAvdConfigFile(homeDir);

            // Insert any hardware properties we want to override
            AndroidEmulator.log(logger, Messages.SETTING_HARDWARE_PROPERTIES());
            for (HardwareProperty prop : hardwareProperties) {
                AndroidEmulator.log(logger, String.format("%s: %s", prop.key, prop.value), true);
                configValues.put(prop.key, prop.value);
            }

            // Update config file
            writeAvdConfigFile(homeDir, configValues);

            return null;
        }
    }

    /** Writes an empty emulator auth file. */
    private final class EmulatorAuthFileTask extends MasterToSlaveCallable<Void, IOException> {

        private static final long serialVersionUID = 1L;

        public Void call() throws IOException {
            // Create an empty auth file to prevent the emulator telnet interface from requiring authentication
            final File userHome = Utils.getHomeDirectory();
            if (userHome != null) {
                try {
                    FilePath authFile = new FilePath(userHome).child(".emulator_console_auth_token");
                    authFile.write("", "UTF-8");
                } catch (IOException e) {
                    throw new IOException(String.format("Failed to write auth file to %s.", userHome, e));
                } catch (InterruptedException e) {
                    throw new IOException(String.format("Interrupted while writing auth file to %s.", userHome, e));
                }
            }

            return null;
        }

    }

    /** A task that deletes the AVD corresponding to our local state. */
    private final class EmulatorDeletionTask extends MasterToSlaveCallable<Boolean, Exception> {

        private static final long serialVersionUID = 1L;

        private final TaskListener listener;
        private transient PrintStream logger;

        public EmulatorDeletionTask(TaskListener listener) {
            this.listener = listener;
        }

        public Boolean call() throws Exception {
            if (logger == null) {
                logger = listener.getLogger();
            }

            // Check whether the AVD exists
            final File homeDir = Utils.getAndroidSdkHomeDirectory(androidSdkHome);
            final File avdDirectory = getAvdDirectory(homeDir);
            final boolean emulatorExists = avdDirectory.exists();
            if (!emulatorExists) {
                AndroidEmulator.log(logger, Messages.AVD_DIRECTORY_NOT_FOUND(avdDirectory));
                return false;
            }

            // Recursively delete the contents
            new FilePath(avdDirectory).deleteRecursive();

            // Delete the metadata file
            getAvdMetadataFile().delete();

            // Success!
            return true;
        }

    }

}
