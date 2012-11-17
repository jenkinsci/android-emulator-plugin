package hudson.plugins.android_emulator;

import hudson.Util;

import java.io.Serializable;

public interface Constants {

    /** The locale to which Android emulators default if not otherwise specified. */
    static final String DEFAULT_LOCALE = "en_US";

    /** Locales supported: http://developer.android.com/sdk/android-3.0.html#locs */
    static final String[] EMULATOR_LOCALES = {
        "ar_EG", "ar_IL", "bg_BG", "ca_ES", "cs_CZ", "da_DK", "de_AT", "de_CH",
        "de_DE", "de_LI", "el_GR", "en_AU", "en_CA", "en_GB", "en_IE", "en_IN",
        "en_NZ", "en_SG", "en_US", "en_ZA", "es_ES", "es_US", "fi_FI", "fr_BE",
        "fr_CA", "fr_CH", "fr_FR", "he_IL", "hi_IN", "hr_HR", "hu_HU", "id_ID",
        "it_CH", "it_IT", "ja_JP", "ko_KR", "lt_LT", "lv_LV", "nb_NO", "nl_BE",
        "nl_NL", "pl_PL", "pt_BR", "pt_PT", "ro_RO", "ru_RU", "sk_SK", "sl_SI",
        "sr_RS", "sv_SE", "th_TH", "tl_PH", "tr_TR", "uk_UA", "vi_VN", "zh_CN",
        "zh_TW"
    };

    /** Commonly-used hardware properties that can be emulated. */
    static final String[] HARDWARE_PROPERTIES = {
        "hw.accelerometer", "hw.battery", "hw.camera", "hw.dPad", "hw.gps",
        "hw.gsmModem", "hw.keyboard", "hw.ramSize", "hw.sdCard",
        "hw.touchScreen", "hw.trackBall", "vm.heapSize"
    };

    /** Possible abis */
    static final String[] TARGET_ABIS = {
        "armeabi", "armeabi-v7a", "x86", "mips"
    };

    /** Name of the snapshot image we will use. */
    static final String SNAPSHOT_NAME = "jenkins";

    // From hudson.Util.VARIABLE
    static final String REGEX_VARIABLE = "\\$([A-Za-z0-9_]+|\\{[A-Za-z0-9_]+\\}|\\$)";
    static final String REGEX_AVD_NAME = "[a-zA-Z0-9._-]+";
    static final String REGEX_LOCALE = "[a-z]{2}_[A-Z]{2}";
    static final String REGEX_SCREEN_DENSITY = "[0-9]{2,4}|(?i)(x?h|[lm])dpi";
    static final String REGEX_SCREEN_RESOLUTION = "[0-9]{3,4}x[0-9]{3,4}";
    static final String REGEX_SCREEN_RESOLUTION_ALIAS = "(([HQ]|F?W[SQ]?)V|WX)GA(720|800|-[LP])?";
    static final String REGEX_SCREEN_RESOLUTION_FULL = REGEX_SCREEN_RESOLUTION_ALIAS +"|"+ REGEX_SCREEN_RESOLUTION;
    static final String REGEX_SD_CARD_SIZE = "(?i)([0-9]{1,12}) ?([KM])[B]?";
    static final String REGEX_SNAPSHOT = "[0-9]+ +"+ SNAPSHOT_NAME +" +[0-9]+M ";

}

enum SnapshotState {
    NONE,
    INITIALISE,
    BOOT
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
    static final AndroidPlatform SDK_2_3 = new AndroidPlatform("2.3", 9);
    static final AndroidPlatform SDK_2_3_3 = new AndroidPlatform("2.3.3", 10);
    static final AndroidPlatform SDK_3_0 = new AndroidPlatform("3.0", 11);
    static final AndroidPlatform SDK_3_1 = new AndroidPlatform("3.1", 12);
    static final AndroidPlatform SDK_3_2 = new AndroidPlatform("3.2", 13);
    static final AndroidPlatform SDK_4_0 = new AndroidPlatform("4.0", 14);
    static final AndroidPlatform SDK_4_0_3 = new AndroidPlatform("4.0.3", 15);
    static final AndroidPlatform SDK_4_1 = new AndroidPlatform("4.1", 16);
    static final AndroidPlatform SDK_4_2 = new AndroidPlatform("4.2", 17);
    static final AndroidPlatform[] PRESETS = new AndroidPlatform[] { SDK_1_5, SDK_1_6, SDK_2_1,
                                                                     SDK_2_2, SDK_2_3_3, SDK_3_0,
                                                                     SDK_3_1, SDK_3_2, SDK_4_0_3,
                                                                     SDK_4_1, SDK_4_2 };
    static final AndroidPlatform[] ALL = new AndroidPlatform[] { SDK_1_1, SDK_1_5, SDK_1_6, SDK_2_0,
        SDK_2_0_1, SDK_2_1, SDK_2_2, SDK_2_3, SDK_2_3_3, SDK_3_0, SDK_3_1, SDK_3_2, SDK_4_0,
        SDK_4_0_3, SDK_4_1, SDK_4_2 };

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

        for (AndroidPlatform preset : ALL) {
            if (version.equals(preset.name) || version.equals(String.valueOf(preset.level))
                    || version.equals(preset.getTargetName())) {
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
    static final ScreenDensity EXTRA_HIGH = new ScreenDensity(320, "xhdpi");
    static final ScreenDensity TV_720P = new ScreenDensity(213, "tvdpi");
    static final ScreenDensity[] PRESETS = new ScreenDensity[] { LOW, MEDIUM, HIGH, EXTRA_HIGH,
                                                                 TV_720P };

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
    static final ScreenResolution WSVGA = new ScreenResolution(1024, 654, "WSVGA", "WSVGA",
            ScreenDensity.MEDIUM, ScreenDensity.HIGH);
    static final ScreenResolution WXGA_720 = new ScreenResolution(1280, 720, "WXGA720", "WXGA720",
            ScreenDensity.MEDIUM);
    static final ScreenResolution WXGA_800 = new ScreenResolution(1280, 800, "WXGA800", "WXGA800",
            ScreenDensity.MEDIUM);
    static final ScreenResolution WXGA = new ScreenResolution(1280, 800, "WXGA", "WXGA",
            ScreenDensity.MEDIUM);
    static final ScreenResolution[] PRESETS = new ScreenResolution[] { QVGA, WQVGA, FWQVGA, HVGA,
                                                                       WVGA, FWVGA, WSVGA,
                                                                       WXGA_720, WXGA_800, WXGA };

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