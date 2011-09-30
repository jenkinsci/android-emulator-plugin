package hudson.plugins.android_emulator;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.plugins.android_emulator.AndroidEmulator.HardwareProperty;
import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.sdk.Tool;
import hudson.plugins.android_emulator.util.Utils;
import hudson.remoting.Callable;
import hudson.util.ArgumentListBuilder;
import hudson.util.StreamCopyThread;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Map;
import java.util.regex.Pattern;

class EmulatorConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String avdName;
    private AndroidPlatform osVersion;
    private ScreenDensity screenDensity;
    private ScreenResolution screenResolution;
    private String deviceLocale;
    private String sdCardSize;
    private boolean wipeData;
    private final boolean showWindow;
    private final boolean useSnapshots;
    private final String commandLineOptions;

    public EmulatorConfig(String avdName, boolean wipeData, boolean showWindow,
            boolean useSnapshots, String commandLineOptions) {
        this.avdName = avdName;
        this.wipeData = wipeData;
        this.showWindow = showWindow;
        this.useSnapshots = useSnapshots;
        this.commandLineOptions = commandLineOptions;
    }

    public EmulatorConfig(String osVersion, String screenDensity, String screenResolution,
            String deviceLocale, String sdCardSize, boolean wipeData, boolean showWindow,
            boolean useSnapshots, String commandLineOptions)
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
        if (sdCardSize != null) {
            sdCardSize = sdCardSize.toUpperCase().replaceAll("[ B]", "");
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
    }

    public static final EmulatorConfig create(String avdName, String osVersion, String screenDensity,
            String screenResolution, String deviceLocale, String sdCardSize, boolean wipeData,
            boolean showWindow, boolean useSnapshots, String commandLineOptions) {
        if (Util.fixEmptyAndTrim(avdName) == null) {
            return new EmulatorConfig(osVersion, screenDensity, screenResolution, deviceLocale,
                    sdCardSize, wipeData, showWindow, useSnapshots, commandLineOptions);
        }

        return new EmulatorConfig(avdName, wipeData, showWindow, useSnapshots, commandLineOptions);
    }

    public static final String getAvdName(String avdName, String osVersion, String screenDensity,
            String screenResolution, String deviceLocale) {
        try {
            return create(avdName, osVersion, screenDensity, screenResolution, deviceLocale, null,
                    false, false, false, null).getAvdName();
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
        String platform = osVersion.getTargetName().replace(':', '_').replace(' ', '_');
        return String.format("hudson_%s_%s_%s_%s", locale, density, resolution, platform);
    }

    public AndroidPlatform getOsVersion() {
        return osVersion;
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

    /**
     * Gets a task that ensures that an Android AVD exists for this instance's configuration.
     *
     * @param androidSdk  The Android SDK to use.
     * @param isUnix  Whether the target system is sane.
     * @param listener The listener to use for logging.
     * @return A Callable that will handle the detection/creation of an appropriate AVD.
     */
    public Callable<Boolean, AndroidEmulatorException> getEmulatorCreationTask(AndroidSdk androidSdk,
            boolean isUnix, BuildListener listener) {
        return new EmulatorCreationTask(androidSdk, isUnix, listener);
    }

    /**
     * Gets a task that updates the hardware properties of the AVD for this instance.
     *
     * @param hardwareProperties  The hardware properties to update the AVD with.
     * @param isUnix  Whether the target system is sane.
     * @param listener The listener to use for logging.
     * @return A Callable that will update the config of the current AVD.
     */
    public Callable<Void, IOException> getEmulatorConfigTask(HardwareProperty[] hardwareProperties,
            boolean isUnix, BuildListener listener) {
        return new EmulatorConfigTask(hardwareProperties, isUnix, listener);
    }

    /**
     * Gets a task that deletes the AVD corresponding to this instance's configuration.
     *
     * @param isUnix  Whether the target system is sane.
     * @param listener The listener to use for logging.
     * @return A Callable that will delete the AVD with for this configuration.
     */
    public Callable<Boolean, Exception> getEmulatorDeletionTask(boolean isUnix,
            TaskListener listener) {
        return new EmulatorDeletionTask(isUnix, listener);
    }

    private File getHomeDirectory(boolean isUnix) {
        // Locate the base directory where Android SDK data (such as AVDs) should be kept
        // From git://android.git.kernel.org/platform/external/qemu.git/android/utils/bufprint.c
        String homeDirPath = System.getenv("ANDROID_SDK_HOME");
        if (homeDirPath == null) {
            if (isUnix) {
                homeDirPath = System.getenv("HOME");
                if (homeDirPath == null) {
                    homeDirPath = "/tmp";
                }
            } else {
                // The emulator checks Win32 "CSIDL_PROFILE", which should equal USERPROFILE
                homeDirPath = System.getenv("USERPROFILE");
                if (homeDirPath == null) {
                    // Otherwise fall back to user.home (which should equal USERPROFILE anyway)
                    homeDirPath = System.getProperty("user.home");
                }
            }
        }

        return new File(homeDirPath);
    }

    private File getAvdHome(final File homeDir) {
        return new File(homeDir, ".android/avd/");
    }

    private File getAvdDirectory(final File homeDir) {
        return new File(getAvdHome(homeDir), getAvdName() +".avd");
    }

    private File getAvdMetadataFile(final File homeDir) {
        return new File(getAvdHome(homeDir), getAvdName() +".ini");
    }

    private Map<String,String> parseAvdConfigFile(File homeDir) throws IOException {
        File configFile = new File(getAvdDirectory(homeDir), "config.ini");
        return Utils.parseConfigFile(configFile);
    }

    private void writeAvdConfigFile(File homeDir, Map<String,String> values) throws FileNotFoundException {
        StringBuilder sb = new StringBuilder();

        for (String key : values.keySet()) {
            sb.append(key);
            sb.append("=");
            sb.append(values.get(key));
            sb.append("\r\n");
        }

        File configFile = new File(getAvdDirectory(homeDir), "config.ini");
        PrintWriter out = new PrintWriter(configFile);
        out.print(sb.toString());
        out.flush();
        out.close();
    }

    /**
     * Gets the command line arguments to pass to "emulator" based on this instance.
     *
     * @return A string of command line arguments.
     */
    public String getCommandArguments(SnapshotState snapshotState, boolean sdkSupportsSnapshots,
            int userPort, int adbPort) {
        StringBuilder sb = new StringBuilder();

        // Set basics
        sb.append("-no-boot-anim");
        sb.append(String.format(" -ports %s,%s", userPort, adbPort));
        if (!isNamedEmulator()) {
            sb.append(" -prop persist.sys.language=");
            sb.append(getDeviceLanguage());
            sb.append(" -prop persist.sys.country=");
            sb.append(getDeviceCountry());
        }
        sb.append(" -avd ");
        sb.append(getAvdName());

        // Snapshots
        if (snapshotState == SnapshotState.BOOT) {
            // For builds after initial snapshot setup, start directly from the "jenkins" snapshot
            sb.append(" -snapshot "+ Constants.SNAPSHOT_NAME);
            sb.append(" -no-snapshot-save");
        } else if (sdkSupportsSnapshots) {
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
        String args = String.format("-snapshot-list -no-window -avd %s", getAvdName());
        Utils.runAndroidTool(launcher, listOutput, logger, androidSdk, Tool.EMULATOR, args, null);

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
    private final class EmulatorCreationTask implements Callable<Boolean, AndroidEmulatorException> {

        private static final long serialVersionUID = 1L;
        private final AndroidSdk androidSdk;
        private final boolean isUnix;

        private final BuildListener listener;
        private transient PrintStream logger;

        public EmulatorCreationTask(AndroidSdk androidSdk, boolean isUnix, BuildListener listener) {
            this.androidSdk = androidSdk;
            this.isUnix = isUnix;
            this.listener = listener;
        }

        public Boolean call() throws AndroidEmulatorException {
            if (logger == null) {
                logger = listener.getLogger();
            }

            final File homeDir = getHomeDirectory(isUnix);
            final File avdDirectory = getAvdDirectory(homeDir);
            final boolean emulatorExists = avdDirectory.exists();

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
                if (getSdCardSize() != null && !sdCardFile.exists()) {
                    createSdCard = true;
                }
                if (shouldUseSnapshots() && androidSdk.supportsSnapshots() && !snapshotsFile.exists()) {
                    createSnapshot = true;
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
                Map<String, String> configValues;
                try {
                    configValues = parseAvdConfigFile(homeDir);
                    configValues.put("snapshot.present", "true");
                    writeAvdConfigFile(homeDir, configValues);
                } catch (IOException e) {
                    throw new EmulatorCreationException(Messages.AVD_CONFIG_NOT_READABLE(), e);
                }
            }

            // If we need create an SD card for an existing emulator, do so
            if (createSdCard) {
                AndroidEmulator.log(logger, Messages.ADDING_SD_CARD(sdCardSize, getAvdName()));
                if (!createSdCard(homeDir)) {
                    throw new EmulatorCreationException(Messages.SD_CARD_CREATION_FAILED());
                }

                // Update the AVD config file
                Map<String, String> configValues;
                try {
                    configValues = parseAvdConfigFile(homeDir);
                    configValues.put("sdcard.size", sdCardSize);
                    writeAvdConfigFile(homeDir, configValues);
                } catch (IOException e) {
                    throw new EmulatorCreationException(Messages.AVD_CONFIG_NOT_READABLE(), e);
                }
            }

            // Return if everything is now ready for use
            if (emulatorExists) {
                return true;
            }

            // Build up basic arguments to `android` command
            final StringBuilder args = new StringBuilder(100);
            args.append("create avd ");

            // Initialise snapshot support, regardless of whether we will actually use it
            if (androidSdk.supportsSnapshots()) {
                args.append("-a ");
            }

            if (sdCardSize != null) {
                args.append("-c ");
                args.append(sdCardSize);
                args.append(" ");
            }
            args.append("-s ");
            args.append(screenResolution.getSkinName());
            args.append(" -n ");
            args.append(getAvdName());
            ArgumentListBuilder builder = Utils.getToolCommand(androidSdk, isUnix, Tool.ANDROID, args.toString());

            // Tack on quoted platform name at the end, since it can be anything
            builder.add("-t");
            builder.add(osVersion.getTargetName());

            // Run!
            boolean avdCreated = false;
            final Process process;
            try {
                ProcessBuilder procBuilder = new ProcessBuilder(builder.toList());
                process = procBuilder.start();
            } catch (IOException ex) {
                throw new EmulatorCreationException(Messages.AVD_CREATION_FAILED());
            }

            // Redirect process's stderr to a stream, for logging purposes
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            new StreamCopyThread("", process.getErrorStream(), stderr).start();

            // Command may prompt us whether we want to further customise the AVD.
            // Just "press" Enter to continue with the selected target's defaults.
            try {
                boolean processAlive = true;

                // Block until the command outputs something (or process ends)
                final InputStream in = process.getInputStream();
                int len = in.read();
                if (len == -1) {
                    // Check whether the process has exited badly, as sometimes no output is valid.
                    // e.g. When creating an AVD with Google APIs, no user input is requested.
                    if (process.waitFor() != 0) {
                        AndroidEmulator.log(logger, Messages.AVD_CREATION_FAILED());
                        AndroidEmulator.log(logger, stderr.toString(), true);
                        throw new EmulatorCreationException(Messages.AVD_CREATION_FAILED());
                    }
                    processAlive = false;
                }
                in.close();

                // Write CRLF, if required
                if (processAlive) {
                    final OutputStream stream = process.getOutputStream();
                    stream.write('\r');
                    stream.write('\n');
                    stream.flush();
                    stream.close();
                }

                // Wait for happy ending
                if (process.waitFor() == 0) {
                    avdCreated = true;
                }

            } catch (IOException e) {
                throw new EmulatorCreationException(Messages.AVD_CREATION_ABORTED(), e);
            } catch (InterruptedException e) {
                throw new EmulatorCreationException(Messages.AVD_CREATION_INTERRUPTED(), e);
            } finally {
                process.destroy();
            }

            // For reasons unknown, the return code may not be correctly reported on Windows.
            // So check whether stderr contains failure info (useful for other platforms too).
            String errOutput = stderr.toString();
            if (errOutput.toString().contains("list targets")) {
                AndroidEmulator.log(logger, Messages.INVALID_AVD_TARGET(osVersion.getTargetName()));
                avdCreated = false;
                errOutput = null;
            }

            // Check everything went ok
            if (!avdCreated) {
                if (errOutput != null && errOutput.length() != 0) {
                    AndroidEmulator.log(logger, stderr.toString(), true);
                }
                throw new EmulatorCreationException(Messages.AVD_CREATION_FAILED());
            }

            // Done!
            return false;
        }

        private boolean createSdCard(File homeDir) {
            // Build command: mksdcard 32M /home/foo/.android/avd/whatever.avd/sdcard.img
            ArgumentListBuilder builder = Utils.getToolCommand(androidSdk, isUnix, Tool.MKSDCARD, null);
            builder.add(sdCardSize);
            builder.add(new File(getAvdDirectory(homeDir), "sdcard.img"));

            // Run!
            try {
                ProcessBuilder procBuilder = new ProcessBuilder(builder.toList());
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
    private final class EmulatorConfigTask implements Callable<Void, IOException> {

        private static final long serialVersionUID = 1L;
        private final boolean isUnix;

        private final HardwareProperty[] hardwareProperties;
        private final BuildListener listener;
        private transient PrintStream logger;

        public EmulatorConfigTask(HardwareProperty[] hardwareProperties, boolean isUnix, BuildListener listener) {
            this.hardwareProperties = hardwareProperties;
            this.isUnix = isUnix;
            this.listener = listener;
        }

        public Void call() throws IOException {
            if (logger == null) {
                logger = listener.getLogger();
            }

            final File homeDir = getHomeDirectory(isUnix);

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

    /** A task that deletes the AVD corresponding to our local state. */
    private final class EmulatorDeletionTask implements Callable<Boolean, Exception> {

        private static final long serialVersionUID = 1L;
        private final boolean isUnix;

        private final TaskListener listener;
        private transient PrintStream logger;

        public EmulatorDeletionTask(boolean isUnix, TaskListener listener) {
            this.isUnix = isUnix;
            this.listener = listener;
        }

        public Boolean call() throws Exception {
            if (logger == null) {
                logger = listener.getLogger();
            }

            // Check whether the AVD exists
            final File homeDir = getHomeDirectory(isUnix);
            final File avdDirectory = getAvdDirectory(homeDir);
            final boolean emulatorExists = avdDirectory.exists();
            if (!emulatorExists) {
                AndroidEmulator.log(logger, Messages.AVD_DIRECTORY_NOT_FOUND(avdDirectory));
                return false;
            }

            // Recursively delete the contents
            new FilePath(avdDirectory).deleteRecursive();

            // Delete the metadata file
            getAvdMetadataFile(homeDir).delete();

            // Success!
            return true;
        }

    }

}
