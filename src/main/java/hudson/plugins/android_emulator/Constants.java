package hudson.plugins.android_emulator;

import hudson.Util;
import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.util.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public interface Constants {

    /** The locale to which Android emulators default if not otherwise specified. */
    static final String DEFAULT_LOCALE = "en_US";

    /** Locales supported: http://developer.android.com/sdk/android-3.0.html#locs */
    @SuppressFBWarnings("MS_OOI_PKGPROTECT")
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
    @SuppressFBWarnings("MS_OOI_PKGPROTECT")
    static final String[] HARDWARE_PROPERTIES = {
        "hw.accelerometer", "hw.battery", "hw.camera", "hw.dPad", "hw.gps",
        "hw.gsmModem", "hw.keyboard", "hw.ramSize", "hw.sdCard",
        "hw.touchScreen", "hw.trackBall", "vm.heapSize"
    };

    /** Common ABIs. */
    @SuppressFBWarnings("MS_OOI_PKGPROTECT")
    static final String[] TARGET_ABIS = {
        "armeabi", "armeabi-v7a", "mips", "x86", "x86_64"
    };

    /** Name of the snapshot image we will use. */
    static final String SNAPSHOT_NAME = "jenkins";

    /**
     * Recent version of the Android SDK that will be installed.
     *
     * The download URL changed from using a version to having
     * a build id. So it's crucial to keep this version in sync
     * with the BUILD_ID variable beneath.
     */
    static final String SDK_TOOLS_DEFAULT_VERSION = "2.1";

    /**
     * Build ID? of the recent version of the Android SDK that will be installed.
     *
     * The download URL changed from using a version to having
     * a build id. So it's crucial to keep this build id in sync
     * with the VERSION variable above.
     */
    static final String SDK_TOOLS_DEFAULT_BUILD_ID = "6609375";

    static boolean isLatestVersion(AndroidSdk sdk) {
        return sdk != null && sdk.hasCommandLineTools() && Constants.SDK_TOOLS_DEFAULT_VERSION.equals(sdk.getSdkToolsVersion());
    }

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

    /** Environment variables the plugin uses **/
    static final String ENV_VAR_ANDROID_ADB_SERVER_PORT = "ANDROID_ADB_SERVER_PORT";
    static final String ENV_VAR_ANDROID_AVD_ADB_PORT = "ANDROID_AVD_ADB_PORT";
    static final String ENV_VAR_ANDROID_AVD_DENSITY = "ANDROID_AVD_DENSITY";
    // Environment variable set by the plugin to specify the serial of the started AVD.
    static final String ENV_VAR_ANDROID_AVD_DEVICE = "ANDROID_AVD_DEVICE";
    static final String ENV_VAR_ANDROID_AVD_LOCALE = "ANDROID_AVD_LOCALE";
    static final String ENV_VAR_ANDROID_AVD_NAME = "ANDROID_AVD_NAME";
    static final String ENV_VAR_ANDROID_AVD_OS = "ANDROID_AVD_OS";
    static final String ENV_VAR_ANDROID_AVD_RESOLUTION = "ANDROID_AVD_RESOLUTION";
    static final String ENV_VAR_ANDROID_AVD_SKIN = "ANDROID_AVD_SKIN";
    // Environment variable set by the plugin to specify the telnet interface port.
    static final String ENV_VAR_ANDROID_AVD_USER_PORT = "ANDROID_AVD_USER_PORT";
    /**
     * @deprecated Use {@link #ENV_VAR_ANDROID_SDK_ROOT} instead if this.
     */
    @Deprecated
    static final String ENV_VAR_ANDROID_HOME = "ANDROID_HOME";
    static final String ENV_VAR_ANDROID_SDK = "ANDROID_SDK";
    static final String ENV_VAR_ANDROID_SDK_HOME = "ANDROID_SDK_HOME";
    /**
     * Sets the path to the SDK installation directory. Once set, the value does
     * not typically change, and can be shared by multiple users on the same
     * machine.
     * <p>
     * Example: C:\AndroidSDK or ~/android-sdk/
     */
    static final String ENV_VAR_ANDROID_SDK_ROOT = "ANDROID_SDK_ROOT";
    static final String ENV_VAR_ANDROID_SERIAL = "ANDROID_SERIAL";
    static final String ENV_VAR_ANDROID_TMP_LOGCAT_FILE = "ANDROID_TMP_LOGCAT_FILE";
    static final String ENV_VAR_ANDROID_USE_SDK_WRAPPER = "USE_SDK_WRAPPER";
    static final String ENV_VAR_JENKINS_ANDROID_HOME = "JENKINS_ANDROID_HOME";
    static final String ENV_VAR_JENKINS_WORKSPACE = "WORKSPACE";
    static final String ENV_VAR_LD_LIBRARY_PATH = "LD_LIBRARY_PATH";
    static final String ENV_VAR_SYSTEM_HOME = "HOME";
    static final String ENV_VAR_SYSTEM_HOMEDRIVE = "HOMEDRIVE";
    static final String ENV_VAR_SYSTEM_HOMEPATH = "HOMEPATH";
    static final String ENV_VAR_SYSTEM_PATH = "PATH";
    static final String ENV_VAR_SYSTEM_USERPROFILE = "USERPROFILE";
    static final String ENV_VAR_PATH_SDK_PLATFORM_TOOLS = "PATH+SDK_PLATFORM_TOOLS";
    static final String ENV_VAR_PATH_SDK_TOOLS = "PATH+SDK_TOOLS";
    static final String ENV_VAR_QEMU_AUDIO_DRV = "QEMU_AUDIO_DRV";

    static final String ENV_VALUE_QEMU_AUDIO_DRV_NONE = "none";
}

enum SnapshotState {
    NONE,
    INITIALISE,
    BOOT
}

enum AndroidPlatformVersions {
    API_LEVEL_1("UNNAMED", "1.0", 1, 0),
    API_LEVEL_2("UNNAMED", "1.1", 2, 0),
    API_LEVEL_3("Cupcake", "1.5", 3, 1),
    API_LEVEL_4("Donut", "1.6", 4, 2),
    API_LEVEL_5("Eclair", "2.0", 5, 2),
    API_LEVEL_6("Eclair", "2.0.1", 6, 2),
    API_LEVEL_7("Eclair", "2.1", 7, 3),
    API_LEVEL_8("Froyo", "2.2", 8, 4),
    API_LEVEL_9("Gingerbread", "2.3", 9, 5),
    API_LEVEL_10("Gingerbread", "2.3.3", 10, 5),
    API_LEVEL_11("Honeycomb", "3.0", 11, 5),
    API_LEVEL_12("Honeycomb", "3.1", 12, 6),
    API_LEVEL_13("Honeycomb", "3.2", 13, 6),
    API_LEVEL_14("Ice Cream Sandwich", "4.0.1", 14, 7),
    API_LEVEL_15("Ice Cream Sandwich", "4.0.3", 15, 8),
    API_LEVEL_16("Jelly Bean", "4.1", 16, 8),
    API_LEVEL_17("Jelly Bean", "4.2", 17, 8),
    API_LEVEL_18("Jelly Bean", "4.3", 18, 8),
    API_LEVEL_19("KitKat", "4.4", 19, 8),
    API_LEVEL_20("KitKat Wear", "4.4W", 20, 8),
    API_LEVEL_21("Lollipop", "5.0", 21, 8),
    API_LEVEL_22("Lollipop", "5.1", 22, 8),
    API_LEVEL_23("Marshmallow", "6.0", 23, 8),
    API_LEVEL_24("Nougat", "7.0", 24, 8),
    API_LEVEL_25("Nougat", "7.1", 25, 8),
    API_LEVEL_26("Oreo", "8.0", 26, 8),
    API_LEVEL_27("Oreo", "8.1", 27, 8),
    API_LEVEL_28("Pie", "9.0", 28, 8),
    API_LEVEL_29("Q", "10.0", 29, 8);

    final String codename;
    final String version;
    final int apiLevel;
    final int ndkLevel;

    AndroidPlatformVersions(final String codename, final String version, final int apiLevel, final int ndkLevel) {
        this.codename = codename;
        this.version = version;
        this.apiLevel = apiLevel;
        this.ndkLevel = ndkLevel;
    }

    /**
     * Retrieves the API-Level for an given Android Version (e.g: '4.2.1'), the given
     * version does not need to be the initial version for the API ('7.1' and '7.1.1'
     * will both return API 25).
     * @return the API level for the given version, or -1 if not found
     */
    public static int getAPILevelForAndroidVersion(final String androidVersion) {
        final AndroidPlatformVersions[] versions = values();
        int api = -1;

        for (int idx = 0; idx < versions.length; idx++) {
            if (Utils.isVersionOlderThan(androidVersion, versions[idx].version)) {
                    break;
            }

            api = versions[idx].apiLevel;

            boolean lastElement = (idx == versions.length - 1);
            if (lastElement && !Utils.equalsVersion(androidVersion, versions[idx].version, 2)) {
                api = -1;
            }
        }
        return api;
    }
}

class AndroidPlatform implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String PLATFORM_NAME_DELIMITER = ":";
    private static final String ANDROID_TARGET_NAME_PREFIX = "android-";

    private final String name;
    private final String platformName;
    private final int level;
    private final boolean isAddon;
    private final String vendorName;

    private AndroidPlatform(final String name,
            final String vendorName, final String platformName, final String apiLevel) {

        this.name = name;
        this.vendorName = vendorName;
        this.isAddon = !platformName.isEmpty();
        this.level = getAPILevelFromString(apiLevel);
        if (isAddon) {
            this.platformName = platformName;
        } else {
            this.platformName = "Android API " + this.level;
        }
    }

    /**
     * Parses the given version which could either be the API-Level (eg: '24' or 'android-23'),
     * an Android version (eg: '4.4') or a fully featured platform definition ('Google Inc.:Google APIs:23')
     * and returns an better managable AndroidPlatform instance.
     *
     * @param version string representation of the platform version
     * @return an {@code AndroidPlatform} instance represents the given platform or {@code null} on parsing error.
     */
    public static AndroidPlatform valueOf(final String version) {
        if (Util.fixEmptyAndTrim(version) == null) {
            return null;
        }

        String[] origNameParts = version.trim().split(PLATFORM_NAME_DELIMITER);

        String vendorName = "";
        String platformName = "";
        String apiLevel = "";

        if (origNameParts.length == 3) {
            vendorName = origNameParts[0];
            platformName = origNameParts[1];
            apiLevel = origNameParts[2];
        } else if (origNameParts.length == 1) {
            apiLevel = origNameParts[0];
        } else {
            return null;
        }

        return new AndroidPlatform(version, vendorName, platformName, apiLevel);
    }

    public boolean isCustomPlatform() {
        return isAddon;
    }

    /**
     * @return {@code true} if this platform requires an ABI to be explicitly specified during
     * emulator creation or using an system-image (and not the image provided via platform).
     */
    public boolean requiresAbi() {
        // TODO: Could be improved / this logic should ideally be moved to emulator creation time...
        // This is a relatively naive approach; e.g. addons for level <= 13 can have ABIs, though
        // the only example seen so far is the Intel x86 level 10 image we explicitly include here..
        // But, since the Intel x86 for SDK 10 is now hosted by Google, we can't rely on the name...
        return level == AndroidPlatformVersions.API_LEVEL_10.apiLevel
                || level >= AndroidPlatformVersions.API_LEVEL_14.apiLevel
                || Util.fixNull(name).contains("Intel Atom x86 System Image");
    }

    public String getTargetName() {
        if (isCustomPlatform() || level < 0) {
            return name;
        }

        return getAndroidTargetName();
    }

    public String getAndroidTargetName() {
        return AndroidPlatform.getTargetName(level);
    }

    public String getName() {
        return name;
    }

    public String getPlatformName() {
        return platformName;
    }

    public String getPlatformId() {
        if (isCustomPlatform()) {
            return getIdFromName(platformName);
        } else {
            return "default";
        }
    }

    public String getVendorName() {
        return vendorName;
    }

    public String getVendorId() {
        return getIdFromName(vendorName);
    }

    public int getSdkLevel() {
        return level;
    }

    public String getAddonName() {
        if (isCustomPlatform()) {
            return String.format("addon-%s-%s-%d", getPlatformId(), getVendorId(), getSdkLevel());
        } else {
            return "";
        }
    }

    /**
     * Trim the string and remove leading and trailing '/', additionally two or more preceding
     * slashes are trimmed to a single slash
     * @param original the string
     * @return the trimmed version of the string
     */
    private String trimAndDeduplicateSlash(final String original) {
        return original.trim().replaceAll("^/*", "").replaceAll("/*$", "").replaceAll("/+", "/");
    }

    /**
     * The ABi-string may consist of an tag and an architecture ('tag/arch') or just the architecture.
     * This method returns the tag from the ABI-string, if no tag is set, the platform id is used, if no
     * custom platform is set, an empty string is returned.
     * To reduce input error, leading and trailing slashes are removed and multiple slashes are treated
     * as single ones.
     *
     * @param abi either 'tag/arch' or 'arch'
     * @return the extracted 'tag', if no tag was given the platform id, if this is not set an empty string
     */
    public String getTagFromAbiString(final String abi) {
        // After trim and deduplicate slash we have either 'tag/arch' or 'arch' (or 'tag/arch/whatever_data')
        // then match everything before the first '/'
        String tagFromAbi = trimAndDeduplicateSlash(Util.fixNull(abi)).replaceAll("^[^/]+$", "").replaceAll("^([^/]*)(.*)", "$1");
        if (tagFromAbi.isEmpty() && isCustomPlatform()) {
            tagFromAbi = getPlatformId();
        }
        return tagFromAbi;
    }

    /**
     * The ABI-string may consist of an tag and an architecture ('tag/arch') or just the architecture.
     * This method returns the tag from the arch-string.
     * To reduce input error, leading and trailing slashes are removed and multiple slashes are treated
     * as single ones.
     *
     * @param abi either 'tag/arch' or 'arch'
     * @return the extracted 'arch' or empty if a empty string was given
     */
    private String getArchFromAbiString(final String abi) {
        // After trim and de-duplication of slashes we have either 'tag/arch' or 'arch' (or 'tag/arch/whatever_data')
        // then remove all data including the first slash (if matched) (we get 'arch', or 'arch/whatever_data')
        // then we remove everything after the first slash (if matched)
        return trimAndDeduplicateSlash(Util.fixNull(abi)).replaceAll("^[^/]+/", "").replaceAll("/.*$", "");
    }

    public String getSystemImageName(final String abi) {
        String tag = getTagFromAbiString(abi);
        if (tag.isEmpty()) {
            tag = "android";
        }
        final String strAbi = getArchFromAbiString(abi);
        return String.format("sys-img-%s-%s-%d", strAbi, tag, getSdkLevel());
    }

    public String getSystemImageNameLegacyFormat() {
        return String.format("sysimg-%d", getSdkLevel());
    }

    public String getPackagePathOfSystemImage(final String abi) {
        if (!requiresAbi()) {
            return String.format("platforms;%s-%d",
                    "android", getSdkLevel());
        }

        String tagFromAbi = getTagFromAbiString(abi);
        if (tagFromAbi.isEmpty()) {
            tagFromAbi = "default";
        }
        return String.format("system-images;%s-%d;%s;%s",
                "android", getSdkLevel(),
                tagFromAbi,
                getArchFromAbiString(abi));
    }

    private String getIdFromName(final String name) {
        // As of SDK r17-ish, we can't always map addon names directly to component id's.
        // But replacing display name "Google Inc." with vendor ID "google" should cover most cases
        return name.toLowerCase().replaceAll(" inc.", "").replaceAll("[^a-z0-9_-]+", "_").replaceAll("_+", "_").replaceAll("_$", "");
    }

    /**
     * Parses the given version which could either be the API-Level (eg: '24' or 'android-23'),
     * an Android version (eg: '4.4') or a fully featured platform definition ('Google Inc.:Google APIs:23')
     * and tries to parse the API-Level from the parameter.
     *
     * @param version string representation of the platform version
     * @return the API-Level or {@code -1} in case of a parsing error.
     */
    private int getAPILevelFromString(final String version) {
        // If an android SDK version (eg: '4') is given or the
        // platform name (eg: 'android-7'), an integer should be left
        final String versionWithoutTarget =
                version.replaceFirst("^(?i)" + ANDROID_TARGET_NAME_PREFIX, "");

        // If a valid version was given, we now have an integer
        // or an android version like '4.4'
        int apiLevel = -1;
        try {
            apiLevel = Integer.parseInt(versionWithoutTarget);
            return apiLevel;
        } catch (NumberFormatException nfex) {
        }

        // the last valid option is a android version number (eg: '7.0')
        return AndroidPlatformVersions.getAPILevelForAndroidVersion(version);
    }

    private static String getTargetName(final int apiLevel) {
        return ANDROID_TARGET_NAME_PREFIX + apiLevel;
    }

    public static String[] getAllPossibleVersionNames() {
        final List<String> names = new ArrayList<String>();
        for (AndroidPlatformVersions version : AndroidPlatformVersions.values()) {
            names.add(version.version);
            names.add(AndroidPlatform.getTargetName(version.apiLevel));
        }

        return names.toArray(new String[names.size()]);
    }

    @Override
    public String toString() {
        return name;
    }

}
