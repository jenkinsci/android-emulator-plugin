package hudson.plugins.android_emulator;

import hudson.Util;
import hudson.plugins.android_emulator.util.Utils;

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

    /** Common ABIs. */
    static final String[] TARGET_ABIS = {
        "armeabi", "armeabi-v7a", "mips", "x86", "x86_64"
    };

    /** Name of the snapshot image we will use. */
    static final String SNAPSHOT_NAME = "jenkins";

    // From hudson.Util.VARIABLE
    static final String REGEX_VARIABLE = "\\$([A-Za-z0-9_]+|\\{[A-Za-z0-9_]+\\}|\\$)";
    static final String REGEX_AVD_NAME = "[a-zA-Z0-9._-]+";
    static final String REGEX_LOCALE = "[a-z]{2}_[A-Z]{2}";
    static final String REGEX_SCREEN_DENSITY = "[0-9]{2,4}|(?i)(x?x?h|[lm])dpi";
    static final String REGEX_SCREEN_RESOLUTION = "[0-9]{3,4}x[0-9]{3,4}";
    static final String REGEX_SCREEN_RESOLUTION_ALIAS = "(([HQ]|F?W[SQ]?)V|WX)GA(720|800|-[LP])?";
    static final String REGEX_SCREEN_RESOLUTION_FULL = REGEX_SCREEN_RESOLUTION_ALIAS +"|"+ REGEX_SCREEN_RESOLUTION;
    static final String REGEX_SD_CARD_SIZE = "(?i)([0-9]{1,12}) ?([KM])[B]?";
    static final String REGEX_SNAPSHOT = "[0-9]+ +"+ SNAPSHOT_NAME +" +[0-9.]+[KMGT] ";

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
    static final AndroidPlatform SDK_4_3 = new AndroidPlatform("4.3", 18);
    static final AndroidPlatform SDK_4_4 = new AndroidPlatform("4.4", 19);
    static final AndroidPlatform SDK_4_4W = new AndroidPlatform("4.4W", 20);
    static final AndroidPlatform SDK_5_0 = new AndroidPlatform("5.0", 21);
    static final AndroidPlatform SDK_5_1 = new AndroidPlatform("5.1", 22);
    static final AndroidPlatform SDK_6_0 = new AndroidPlatform("6.0", 23);
    static final AndroidPlatform[] ALL = new AndroidPlatform[] { SDK_1_1, SDK_1_5, SDK_1_6, SDK_2_0,
        SDK_2_0_1, SDK_2_1, SDK_2_2, SDK_2_3, SDK_2_3_3, SDK_3_0, SDK_3_1, SDK_3_2, SDK_4_0,
        SDK_4_0_3, SDK_4_1, SDK_4_2, SDK_4_3, SDK_4_4, SDK_4_4W, SDK_5_0, SDK_5_1, SDK_6_0 };

    private final String name;
    private final int level;
    private final boolean isAddon;

    private AndroidPlatform(String name, int level) {
        this.name = name;
        this.isAddon = level <= 0;
        if (isAddon) {
            level = Utils.getApiLevelFromPlatform(name);
        }
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
        return isAddon;
    }

    /**
     * @return {@code true} if this platform requires an ABI to be explicitly specified during
     * emulator creation.
     */
    public boolean requiresAbi() {
        // TODO: Could be improved / this logic should ideally be moved to emulator creation time...
        // This is a relatively naive approach; e.g. addons for level <= 13 can have ABIs, though
        // the only example seen so far is the Intel x86 level 10 image we explicitly include here..
        // But, since the Intel x86 for SDK 10 is now hosted by Google, we can't rely on the name...
        return level == 10 || level >= 15
                || Util.fixNull(name).contains("Intel Atom x86 System Image");
    }

    public String getTargetName() {
        if (isCustomPlatform()) {
            return name;
        }

        return "android-"+ level;
    }

    public int getSdkLevel() {
        return level;
    }

    @Override
    public String toString() {
        return name;
    }

}

class ScreenDensity implements Serializable {

    private static final long serialVersionUID = 1L;

    static final ScreenDensity LOW = new ScreenDensity(120, "ldpi");
    static final ScreenDensity MEDIUM = new ScreenDensity(160, "mdpi");
    static final ScreenDensity TV_720P = new ScreenDensity(213, "tvdpi");
    static final ScreenDensity HIGH = new ScreenDensity(240, "hdpi");
    static final ScreenDensity EXTRA_HIGH = new ScreenDensity(320, "xhdpi");
    static final ScreenDensity EXTRA_HIGH_400 = new ScreenDensity(400);
    static final ScreenDensity EXTRA_HIGH_420 = new ScreenDensity(420);
    static final ScreenDensity EXTRA_EXTRA_HIGH = new ScreenDensity(480, "xxhdpi");
    static final ScreenDensity EXTRA_EXTRA_HIGH_560 = new ScreenDensity(560);
    static final ScreenDensity EXTRA_EXTRA_EXTRA_HIGH = new ScreenDensity(640, "xxxhdpi");
    static final ScreenDensity[] PRESETS = new ScreenDensity[] { LOW, MEDIUM, TV_720P, HIGH,
            EXTRA_HIGH, EXTRA_HIGH_400, EXTRA_HIGH_420, EXTRA_EXTRA_HIGH, EXTRA_EXTRA_HIGH_560,
            EXTRA_EXTRA_EXTRA_HIGH };

    private final int dpi;
    private final String alias;

    private ScreenDensity(int dpi, String alias) {
        this.dpi = dpi;
        this.alias = alias;
    }

    private ScreenDensity(int density) {
        this(density, null);
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
            int dpi = Integer.parseInt(density);
            return new ScreenDensity(dpi);
        } catch (NumberFormatException ex) {
            return null;
        }
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

    static final ScreenResolution QVGA = new ScreenResolution(240, 320, "QVGA", "QVGA");
    static final ScreenResolution WQVGA = new ScreenResolution(240, 400, "WQVGA", "WQVGA400");
    static final ScreenResolution FWQVGA = new ScreenResolution(240, 432, "FWQVGA", "WQVGA432");
    static final ScreenResolution HVGA = new ScreenResolution(320, 480, "HVGA", "HVGA");
    static final ScreenResolution WVGA = new ScreenResolution(480, 800, "WVGA", "WVGA800");
    static final ScreenResolution FWVGA = new ScreenResolution(480, 854, "FWVGA", "WVGA854");
    static final ScreenResolution WSVGA = new ScreenResolution(1024, 654, "WSVGA", "WSVGA");
    static final ScreenResolution WXGA_720 = new ScreenResolution(1280, 720, "WXGA720", "WXGA720");
    static final ScreenResolution WXGA_800 = new ScreenResolution(1280, 800, "WXGA800", "WXGA800");
    static final ScreenResolution WXGA = new ScreenResolution(1280, 800, "WXGA", "WXGA");
    static final ScreenResolution[] PRESETS = new ScreenResolution[] { QVGA, WQVGA, FWQVGA, HVGA,
                                                                       WVGA, FWVGA, WSVGA,
                                                                       WXGA_720, WXGA_800, WXGA };

    private final int width;
    private final int height;
    private final String alias;
    private final String skinName;

    private ScreenResolution(int width, int height, String alias, String skinName) {
        this.width = width;
        this.height = height;
        this.alias = alias;
        this.skinName = skinName;
    }

    private ScreenResolution(int width, int height) {
        this(width, height, null, null);
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
