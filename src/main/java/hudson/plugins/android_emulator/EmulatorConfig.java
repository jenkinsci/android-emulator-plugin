package hudson.plugins.android_emulator;

import hudson.FilePath;
import hudson.Util;
import hudson.remoting.Callable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;

class EmulatorConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String avdName;
    private AndroidPlatform osVersion;
    private ScreenDensity screenDensity;
    private ScreenResolution screenResolution;
    private String deviceLocale;

    public EmulatorConfig(String avdName) {
        this.avdName = avdName;
    }

    public EmulatorConfig(String osVersion,
            String screenDensity, String screenResolution, String deviceLocale) {
        this.osVersion = AndroidPlatform.valueOf(osVersion);
        this.screenDensity = ScreenDensity.valueOf(screenDensity);
        this.screenResolution = ScreenResolution.valueOf(screenResolution);
        this.deviceLocale = deviceLocale;
    }

    public static final EmulatorConfig create(String avdName, String osVersion,
            String screenDensity, String screenResolution, String deviceLocale) {
        if (Util.fixEmptyAndTrim(avdName) == null) {
            return new EmulatorConfig(osVersion, screenDensity, screenResolution, deviceLocale);
        }

        return new EmulatorConfig(avdName);
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
        String platform = osVersion.getTargetName();
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

    /**
     * Gets a task that ensures that an Android AVD exists for this instance's configuration.
     *
     * @param homeDir  The path to the current user's home directory where ".android" should live.
     * @return A Callable that will handle the detection/creation of an appropriate AVD.
     */
    public Callable<Boolean, IOException> getEmulatorCreationTask(String androidHome) {
        return new EmulatorCreationTask(androidHome);
    }

    private File getAvdHome(final File homeDir) {
        return new File(homeDir, ".android/avd/");
    }

    private File getAvdDirectory(final File homeDir) {
        return new File(getAvdHome(homeDir), getAvdName() +".avd");
    }

    private void writeAvdMetadataFile(final File homeDir) throws FileNotFoundException {
        File configFile = new File(getAvdHome(homeDir), getAvdName() +".ini");

        PrintWriter out = new PrintWriter(configFile);
        out.print("target=");
        out.println(osVersion.getTargetName());
        out.print("path=");
        out.println(getAvdDirectory(homeDir).toString());
        out.flush();
        out.close();
    }

    private void writeAvdConfigFile(File homeDir, String targetName) throws FileNotFoundException {
        StringBuilder sb = new StringBuilder();

        sb.append("image.sysdir.1=");
        sb.append("platforms/");
        sb.append(targetName);
        sb.append("/images/");
        sb.append("\r\n");

        sb.append("sdcard.size=");
        sb.append("16M\r\n");

        sb.append("skin.name=");
        sb.append(screenResolution.toString());
        sb.append("\r\n");

        sb.append("skin.path=");
        if (!screenResolution.isCustomResolution()) {
            sb.append("platforms/");
            sb.append(targetName);
            sb.append("/skins/");
        }
        sb.append(screenResolution.toString());
        sb.append("\r\n");

        sb.append("hw.lcd.density=");
        sb.append(screenDensity.getDpi());
        sb.append("\r\n");

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

        if (!isNamedEmulator()) {
            sb.append(" -prop persist.sys.language=");
            sb.append(getDeviceLanguage());
            sb.append(" -prop persist.sys.country=");
            sb.append(getDeviceCountry());
        }
        sb.append(" -avd ");
        sb.append(getAvdName());

        return sb.toString();
    }

    /**
     * A task that locates or creates an AVD based on our local state.
     *
     * Returns <code>TRUE</code> if an AVD already existed with these properties, otherwise returns
     * <code>FALSE</code> if an AVD was newly created, and throws an IOException if the given AVD
     * or parts required to generate a new AVD were not found.
     */
    private final class EmulatorCreationTask implements Callable<Boolean, IOException> {

        private static final long serialVersionUID = 1L;
        private final String androidHome;

        public EmulatorCreationTask(String androidHome) {
            this.androidHome = androidHome;
        }

        @Override
        public Boolean call() throws IOException {
            final File homeDir = new File(System.getProperty("user.home"));
            final File avdDirectory = getAvdDirectory(homeDir);

            // There's nothing to do if the directory exists
            if (avdDirectory.exists()) {
                return true;
            } else if (isNamedEmulator()) {
                throw new FileNotFoundException(Messages.AVD_DOES_NOT_EXIST(avdName));
            }

            // We can't continue if we don't know where to find emulator images
            if (androidHome == null) {
                throw new FileNotFoundException(Messages.SDK_NOT_SPECIFIED());
            }
            final File sdkRoot = new File(androidHome);
            if (!sdkRoot.exists()) {
                throw new FileNotFoundException(Messages.SDK_NOT_FOUND(androidHome));
            }

            // Detect which style of target name is being used
            final File platformsRoot = new File(sdkRoot, "platforms");
            String targetName = null;
            for (String target : new String[] { osVersion.getTargetName(), osVersion.getOldTargetName() }) {
                File tmp = new File(platformsRoot, target);
                System.out.println("");
                if (tmp.exists()) {
                    targetName = target;
                    break;
                }
            }

            // Can't go ahead if desired platform isn't available at all
            if (targetName == null) {
                throw new FileNotFoundException(Messages.PLATFORM_NOT_FOUND(osVersion.getTargetName()));
            }

            // Create base directory
            avdDirectory.mkdirs();

            // Copy platform image
            File platformImage = new File(platformsRoot, targetName +"/images/userdata.img");
            if (!platformImage.exists()) {
                throw new FileNotFoundException(Messages.PLATFORM_IMAGE_NOT_FOUND(platformImage.toString()));
            }
            try {
                new FilePath(platformImage).copyTo(new FilePath(avdDirectory).child("userdata.img"));
            } catch (InterruptedException e) {
                // Rollback
                try {
                    new FilePath(avdDirectory).deleteRecursive();
                } catch (InterruptedException e2) {}
                e.printStackTrace();
                throw new IOException(Messages.AVD_CREATION_INTERRUPTED());
            }

            // Create metadata in AVD home
            writeAvdMetadataFile(homeDir);

            // Create config in AVD directory
            writeAvdConfigFile(homeDir, targetName);

            return false;
        }

    }

}
