package hudson.plugins.android_emulator;

import hudson.Util;
import hudson.model.BuildListener;
import hudson.remoting.Callable;
import hudson.util.ArgumentListBuilder;
import hudson.util.StreamCopyThread;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

class EmulatorConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String avdName;
    private AndroidPlatform osVersion;
    private ScreenDensity screenDensity;
    private ScreenResolution screenResolution;
    private String deviceLocale;
    private String sdCardSize;
    private final boolean wipeData;
    private final boolean showWindow;

    public EmulatorConfig(String avdName, boolean wipeData, boolean showWindow) {
        this.avdName = avdName;
        this.wipeData = wipeData;
        this.showWindow = showWindow;
    }

    public EmulatorConfig(String osVersion, String screenDensity, String screenResolution,
            String deviceLocale, String sdCardSize, boolean wipeData, boolean showWindow)
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
        this.screenDensity = ScreenDensity.valueOf(screenDensity);
        this.screenResolution = ScreenResolution.valueOf(screenResolution);
        this.deviceLocale = deviceLocale;
        this.sdCardSize = sdCardSize;
        this.wipeData = wipeData;
        this.showWindow = showWindow;
    }

    public static final EmulatorConfig create(String avdName, String osVersion, String screenDensity,
            String screenResolution, String deviceLocale, String sdCardSize, boolean wipeData,
            boolean showWindow) {
        if (Util.fixEmptyAndTrim(avdName) == null) {
            return new EmulatorConfig(osVersion, screenDensity, screenResolution, deviceLocale,
                    sdCardSize, wipeData, showWindow);
        }

        return new EmulatorConfig(avdName, wipeData, showWindow);
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

    public boolean shouldWipeData() {
        return wipeData;
    }

    public boolean shouldShowWindow() {
        return showWindow;
    }

    /**
     * Gets a task that ensures that an Android AVD exists for this instance's configuration.
     *
     * @param homeDir  The path to the current user's home directory where ".android" should live.
     * @param isUnix  Whether the target system is sane.
     * @return A Callable that will handle the detection/creation of an appropriate AVD.
     */
    public Callable<Boolean, AndroidEmulatorException> getEmulatorCreationTask(String androidHome, boolean isUnix, BuildListener listener) {
        return new EmulatorCreationTask(androidHome, isUnix, listener);
    }

    private File getAvdHome(final File homeDir) {
        return new File(homeDir, ".android/avd/");
    }

    private File getAvdDirectory(final File homeDir) {
        return new File(getAvdHome(homeDir), getAvdName() +".avd");
    }

    private Map<String,String> parseAvdConfigFile(File homeDir) throws IOException {
        File configFile = new File(getAvdDirectory(homeDir), "config.ini");

        FileReader fileReader = new FileReader(configFile);
        BufferedReader reader = new BufferedReader(fileReader);

        String line;
        Map<String,String> values = new HashMap<String,String>();
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0 || line.charAt(0) == '#') {
                continue;
            }
            String[] parts = line.split("=", 2);
            values.put(parts[0], parts[1]);
        }

        return values;
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
    public String getCommandArguments() {
        StringBuilder sb = new StringBuilder("-no-boot-anim");

        // Set basics
        if (!isNamedEmulator()) {
            sb.append(" -prop persist.sys.language=");
            sb.append(getDeviceLanguage());
            sb.append(" -prop persist.sys.country=");
            sb.append(getDeviceCountry());
        }
        sb.append(" -avd ");
        sb.append(getAvdName());

        // Options
        if (shouldWipeData()) {
            sb.append(" -wipe-data");
        }
        if (!shouldShowWindow()) {
            sb.append(" -no-window");
        }

        return sb.toString();
    }

    /**
     * A task that locates or creates an AVD based on our local state.
     *
     * Returns <code>TRUE</code> if an AVD already existed with these properties, otherwise returns
     * <code>FALSE</code> if an AVD was newly created, and throws an IOException if the given AVD
     * or parts required to generate a new AVD were not found.
     */
    private final class EmulatorCreationTask implements Callable<Boolean, AndroidEmulatorException> {

        private static final long serialVersionUID = 1L;
        private final String androidHome;
        private final boolean isUnix;

        private final BuildListener listener;
        private transient PrintStream logger;

        public EmulatorCreationTask(String androidHome, boolean isUnix, BuildListener listener) {
            this.androidHome = androidHome;
            this.isUnix = isUnix;
            this.listener = listener;
        }

        @Override
        public Boolean call() throws AndroidEmulatorException {
            if (logger == null) {
                logger = listener.getLogger();
            }

            // Locate the base directory where Android SDK data (such as AVDs) should be kept
            // From git://android.git.kernel.org/platform/external/qemu.git/android/utils/bufprint.c
            String homeDir = System.getenv("ANDROID_SDK_HOME");
            if (homeDir == null) {
                if (isUnix) {
                    homeDir = System.getenv("HOME");
                    if (homeDir == null) {
                        homeDir = "/tmp";
                    }
                } else {
                    // The emulator checks Win32 "CSIDL_PROFILE", which should equal USERPROFILE
                    homeDir = System.getenv("USERPROFILE");
                    if (homeDir == null) {
                        // Otherwise fall back to user.home (which should equal USERPROFILE anyway)
                        homeDir = System.getProperty("user.home");
                    }
                }
            }
            final File avdDirectory = getAvdDirectory(new File(homeDir));

            // Can't do anything if a named emulator doesn't exist
            if (isNamedEmulator() && !avdDirectory.exists()) {
                throw new EmulatorDiscoveryException(Messages.AVD_DOES_NOT_EXIST(avdName));
            }

            // Check whether AVD needs to be created, or whether an existing AVD needs an SD card
            boolean createSdCard = false;
            if (avdDirectory.exists()) {
                // There's nothing to do if no SD card is required, or one already exists
                File sdCardFile = new File(getAvdDirectory(homeDir), "sdcard.img");
                if (getSdCardSize() == null || sdCardFile.exists()) {
                    return true;
                }

                createSdCard = true;
                AndroidEmulator.log(logger, Messages.ADDING_SD_CARD(sdCardSize, getAvdName()));
            } else {
                AndroidEmulator.log(logger, Messages.CREATING_AVD(avdDirectory));
            }

            // We can't continue if we don't know where to find emulator images or tools
            if (androidHome == null) {
                throw new EmulatorCreationException(Messages.SDK_NOT_SPECIFIED());
            }
            final File sdkRoot = new File(androidHome);
            if (!sdkRoot.exists()) {
                throw new EmulatorCreationException(Messages.SDK_NOT_FOUND(androidHome));
            }

            // If we're only here to create an SD card, do so and return
            if (createSdCard) {
                if (!createSdCard(homeDir)) {
                    throw new EmulatorCreationException(Messages.SD_CARD_CREATION_FAILED(), e);
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

                return true;
            }

            // Build up basic arguments to `android` command
            final String androidCmd = isUnix ? "android" : "android.bat";
            final StringBuilder args = new StringBuilder(100);
            args.append("create avd ");
            if (sdCardSize != null) {
                args.append("-c ");
                args.append(sdCardSize);
                args.append(" ");
            }
            args.append("-s ");
            args.append(screenResolution.getSkinName());
            args.append(" -n ");
            args.append(getAvdName());
            ArgumentListBuilder builder = Utils.getToolCommand(androidHome, androidCmd, args.toString());

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

            // Check whether everything went ok
            if (!avdCreated) {
                // TODO: Clear up
                // TODO: Could potentially parse the "not valid target" message and show a nicer one
                final String errOutput = stderr.toString();
                if (errOutput.length() != 0) {
                    AndroidEmulator.log(logger, stderr.toString(), true);
                }
                throw new EmulatorCreationException(Messages.AVD_CREATION_FAILED());
            }

            // Parse newly-created AVD's config
            Map<String, String> configValues;
            try {
                configValues = parseAvdConfigFile(homeDir);
            } catch (IOException e) {
                throw new EmulatorCreationException(Messages.AVD_CONFIG_NOT_READABLE(), e);
            }

            // TODO: Insert/replace any hardware properties we want to override
//            configValues.put("vm.heapSize", "4");
//            configValues.put("hw.ramSize", "64");

            // Update config file
            try {
                writeAvdConfigFile(homeDir, configValues);
            } catch (IOException e) {
                throw new EmulatorCreationException(Messages.AVD_CONFIG_NOT_WRITEABLE(), e);
            }

            // Done!
            return false;
        }

        private boolean createSdCard(File homeDir) {
            // Build command: mksdcard 32M /home/foo/.android/avd/whatever.avd/sdcard.img
            final String androidCmd = isUnix ? "mksdcard" : "mksdcard.exe";
            ArgumentListBuilder builder = Utils.getToolCommand(androidHome, androidCmd, null);
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

}
