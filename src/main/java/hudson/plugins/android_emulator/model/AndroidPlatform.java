package hudson.plugins.android_emulator.model;

import hudson.Util;
import hudson.plugins.android_emulator.util.Utils;

import java.io.Serializable;

public class AndroidPlatform implements Serializable {

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
    public static final AndroidPlatform[] ALL = new AndroidPlatform[] { SDK_1_1, SDK_1_5, SDK_1_6, SDK_2_0,
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
