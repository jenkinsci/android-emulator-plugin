package hudson.plugins.android_emulator.model;

import hudson.Util;

import java.io.Serializable;

public class ScreenDensity implements Serializable {

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
    public static final ScreenDensity[] PRESETS = new ScreenDensity[] { LOW, MEDIUM, TV_720P, HIGH,
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
