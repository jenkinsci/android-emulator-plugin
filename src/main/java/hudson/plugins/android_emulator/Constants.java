package hudson.plugins.android_emulator;

import hudson.Util;

import java.io.Serializable;

interface Constants {

    /** The locale to which Android emulators default if not otherwise specified. */
    static final String DEFAULT_LOCALE = "en_US";

    static final String[] EMULATOR_LOCALES = {
        "cs_CZ", "de_AT", "de_CH", "de_DE", "de_LI", "en_AU", "en_CA", "en_GB",
        "en_NZ", "en_SG", "en_US", "fr_BE", "fr_CA", "fr_CH", "fr_FR", "it_CH",
        "it_IT", "ja_JP", "ko_KR", "nl_BE", "nl_NL", "pl_PL", "ru_RU", "zh_TW"
    };

    static final String REGEX_AVD_NAME = "[a-zA-Z0-9._-]+";
    static final String REGEX_LOCALE = "[a-z]{2}_[A-Z]{2}";
    static final String REGEX_SCREEN_DENSITY = "[0-9]{2,4}|(?i)[hlm]dpi";
    static final String REGEX_SCREEN_RESOLUTION = "[0-9]{3,4}x[0-9]{3,4}";
    static final String REGEX_SCREEN_RESOLUTION_ALIAS = "([HQ]|F?WQ?)VGA";
    static final String REGEX_SCREEN_RESOLUTION_FULL = REGEX_SCREEN_RESOLUTION_ALIAS +"|"+ REGEX_SCREEN_RESOLUTION;
    static final String REGEX_SD_CARD_SIZE = "(?i)([0-9]{1,12}) ?([KM])[B]?";

}

class AndroidPlatform implements Serializable {

    private static final long serialVersionUID = 1L;

    static final AndroidPlatform SDK_1_1 = new AndroidPlatform("1.1", 2);
    static final AndroidPlatform SDK_1_5 = new AndroidPlatform("1.5", 3);
    static final AndroidPlatform SDK_1_6 = new AndroidPlatform("1.6", 4);
    static final AndroidPlatform SDK_2_0 = new AndroidPlatform("2.0", 5);
    static final AndroidPlatform SDK_2_0_1 = new AndroidPlatform("2.0.1", 6);
    static final AndroidPlatform SDK_2_1 = new AndroidPlatform("2.1", 7);
    static final AndroidPlatform SDK_2_2 = new AndroidPlatform("2.2", 8);
    static final AndroidPlatform[] PRESETS = new AndroidPlatform[] { SDK_1_1, SDK_1_5, SDK_1_6,
                                                                     SDK_2_0, SDK_2_0_1, SDK_2_1,
                                                                     SDK_2_2 };

    private final String name;
    private final int level;

    private AndroidPlatform(String name, int level) {
        this.name = name;
        this.level = level;
    }

    private AndroidPlatform(String name) {
        this(name, -1);
    }

    public static AndroidPlatform valueOf(String version) {
        if (Util.fixEmptyAndTrim(version) == null) {
            return null;
        }

        for (AndroidPlatform preset : PRESETS) {
            if (version.equals(preset.name) || version.equals(preset.level +"")) {
                return preset;
            }
        }

        return new AndroidPlatform(version);
    }

    public boolean isCustomPlatform() {
        return level == -1;
    }

    public String getTargetName() {
        if (isCustomPlatform()) {
            return name;
        }

        return "android-"+ level;
    }

    public String getOldTargetName() {
        return "android-"+ name;
    }

    public int getSdkLevel() {
        return level;
    }

    @Override
    public String toString() {
        return name;
    };

}

class ScreenDensity implements Serializable {

    private static final long serialVersionUID = 1L;

    static final ScreenDensity LOW = new ScreenDensity(120, "ldpi");
    static final ScreenDensity MEDIUM = new ScreenDensity(160, "mdpi");
    static final ScreenDensity HIGH = new ScreenDensity(240, "hdpi");
    static final ScreenDensity[] PRESETS = new ScreenDensity[] { LOW, MEDIUM, HIGH };

    private final int dpi;
    private final String alias;

    private ScreenDensity(int dpi, String alias) {
        this.dpi = dpi;
        this.alias = alias;
    }

    private ScreenDensity(String density) {
        this(Integer.parseInt(density), null);
    }

    public static ScreenDensity valueOf(String density) {
        if (Util.fixEmptyAndTrim(density) == null) {
            return null;
        } else {
            density = density.toLowerCase();
        }

        for (ScreenDensity preset : PRESETS) {
            if (density.equals(preset.alias) || density.equals(preset.toString())) {
                return preset;
            }
        }

        // Return custom value, if things look valid
        try {
            Integer.parseInt(density);
        } catch (NumberFormatException ex) {
            return null;
        }
        return new ScreenDensity(density);
    }

    public boolean isCustomDensity() {
        return alias == null;
    }

    public int getDpi() {
        return dpi;
    }

    @Override
    public String toString() {
        return Integer.toString(dpi);
    };

}

class ScreenResolution implements Serializable {

    private static final long serialVersionUID = 1L;

    static final ScreenResolution QVGA = new ScreenResolution(240, 320, "QVGA", "QVGA",
            ScreenDensity.LOW);
    static final ScreenResolution WQVGA = new ScreenResolution(240, 400, "WQVGA", "WQVGA400",
            ScreenDensity.LOW);
    static final ScreenResolution FWQVGA = new ScreenResolution(240, 432, "FWQVGA", "WQVGA432",
            ScreenDensity.LOW);
    static final ScreenResolution HVGA = new ScreenResolution(320, 480, "HVGA", "HVGA",
            ScreenDensity.MEDIUM);
    static final ScreenResolution WVGA = new ScreenResolution(480, 800, "WVGA", "WVGA800",
            ScreenDensity.MEDIUM, ScreenDensity.HIGH);
    static final ScreenResolution FWVGA = new ScreenResolution(480, 854, "FWVGA", "WVGA854",
            ScreenDensity.MEDIUM, ScreenDensity.HIGH);
    static final ScreenResolution[] PRESETS = new ScreenResolution[] { QVGA, WQVGA, FWQVGA,
                                                                       HVGA, WVGA, FWVGA };

    private final int width;
    private final int height;
    private final String alias;
    private final String skinName;
    private final ScreenDensity[] densities;

    private ScreenResolution(int width, int height, String alias, String skinName,
            ScreenDensity... applicableDensities) {
        this.width = width;
        this.height = height;
        this.alias = alias;
        this.skinName = skinName;
        this.densities = applicableDensities;
    }

    private ScreenResolution(int width, int height) {
        this(width, height, null, null, (ScreenDensity[]) null);
    }

    public static ScreenResolution valueOf(String resolution) {
        if (Util.fixEmptyAndTrim(resolution) == null) {
            return null;
        }

        // Try matching against aliases
        for (ScreenResolution preset : PRESETS) {
            if (resolution.equalsIgnoreCase(preset.alias)) {
                return preset;
            }
        }

        // Check for pixel values
        resolution = resolution.toLowerCase();
        if (!resolution.matches(Constants.REGEX_SCREEN_RESOLUTION)) {
            return null;
        }

        // Try matching against pixel values
        int index = resolution.indexOf('x');
        int width = 0;
        int height = 0;
        try {
            width = Integer.parseInt(resolution.substring(0, index));
            height = Integer.parseInt(resolution.substring(index+1));
        } catch (NumberFormatException ex) {
            return null;
        }
        for (ScreenResolution preset : PRESETS) {
            if (width == preset.width && height == preset.height) {
                return preset;
            }
        }

        // Return custom value
        return new ScreenResolution(width, height);
    }

    public boolean isCustomResolution() {
        return alias == null;
    }

    public String getSkinName() {
        if (isCustomResolution()) {
            return getDimensionString();
        }

        return skinName;
    }

    public ScreenDensity[] getApplicableDensities() {
        return densities;
    }

    public String getDimensionString() {
        return width +"x"+ height;
    }

    @Override
    public String toString() {
        if (isCustomResolution()) {
            return getDimensionString();
        }

        return alias;
    };

}