package hudson.plugins.android_emulator;

import hudson.Util;

public class ScreenDensity {

    public static final ScreenDensity LOW = new ScreenDensity(120, "ldpi");
    public static final ScreenDensity MEDIUM = new ScreenDensity(160, "mdpi");
    public static final ScreenDensity TV_720P = new ScreenDensity(213, "tvdpi");
    public static final ScreenDensity HIGH = new ScreenDensity(240, "hdpi");
    public static final ScreenDensity EXTRA_HIGH = new ScreenDensity(320, "xhdpi");
    public static final ScreenDensity EXTRA_HIGH_400 = new ScreenDensity(400);
    public static final ScreenDensity EXTRA_HIGH_420 = new ScreenDensity(420);
    public static final ScreenDensity EXTRA_EXTRA_HIGH = new ScreenDensity(480, "xxhdpi");
    public static final ScreenDensity EXTRA_EXTRA_HIGH_560 = new ScreenDensity(560);
    public static final ScreenDensity EXTRA_EXTRA_EXTRA_HIGH = new ScreenDensity(640, "xxxhdpi");
    private static final ScreenDensity[] PRESETS = new ScreenDensity[] { LOW, MEDIUM, TV_720P, HIGH,
            EXTRA_HIGH, EXTRA_HIGH_400, EXTRA_HIGH_420, EXTRA_EXTRA_HIGH, EXTRA_EXTRA_HIGH_560,
            EXTRA_EXTRA_EXTRA_HIGH };

    public static ScreenDensity[] values() {
        return PRESETS;
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

    private final int dpi;
    private final String alias;

    private ScreenDensity(int dpi, String alias) {
        this.dpi = dpi;
        this.alias = alias;
    }

    private ScreenDensity(int density) {
        this(density, null);
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
    }

}