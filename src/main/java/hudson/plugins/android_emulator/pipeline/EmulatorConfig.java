package hudson.plugins.android_emulator.pipeline;

import hudson.plugins.android_emulator.model.AndroidPlatform;
import hudson.plugins.android_emulator.model.ScreenDensity;
import hudson.plugins.android_emulator.model.ScreenResolution;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;

public class EmulatorConfig implements Serializable {

    // required
    private AndroidPlatform platform;

    // default: Nexus 5X?
    // First: Nexus 4 - 320dpi (xhdpi)
    private ScreenDensity screenDensity;

    // default: Nexus 5X? too RAM-hungry, perhaps - if so; Nexus 4? Need to test with QEMU1...
    // First: Nexus 4 - 768x1280
    private ScreenResolution screenResolution;

    // default: en_US
    private String locale;

    // default: 256MB
    private String sdCardSize;

    // default: x86
    private String abi;

    public EmulatorConfig(String platform) {
        // TODO: Validate at construction time? Would probably be nice to keep validation contained in this class...
        // Alternatively, a validate() method on this object, to be called later
        setPlatform(platform);

        // For now, set some defaults
        setScreenDensity("320");
        setScreenResolution("768x1280");
        setLocale("en_US");
        setAbi("x86"); // fucked if we're on a mac
        setSdCardSize("256M");
    }

    private void setPlatform(String platform) {
        this.platform = AndroidPlatform.valueOf(platform);
    }

    @Nonnull
    public AndroidPlatform getPlatform() {
        return platform;
    }

    private void setScreenDensity(String density) {
        this.screenDensity = ScreenDensity.valueOf(density);
    }

    public ScreenDensity getScreenDensity() {
        return screenDensity;
    }

    private void setScreenResolution(String resolution) {
        this.screenResolution = ScreenResolution.valueOf(resolution);
    }

    public ScreenResolution getScreenResolution() {
        return screenResolution;
    }

    private void setLocale(String locale) {
        this.locale = locale;
    }

    @Nonnull
    public String getLocale() {
        return locale;
    }

    private void setSdCardSize(String sdCardSize) {
        this.sdCardSize = sdCardSize;
    }

    @Nullable
    public String getSdCardSize() {
        return sdCardSize;
    }

    private void setAbi(String abi) {
        this.abi = abi;
    }

    @Nullable
    public String getAbi() {
        return abi;
    }

    public String getAvdName() {
        String locale = this.locale.replace('_', '-');
        String density = this.screenDensity.toString();
        String resolution = this.screenResolution.toString();
        String platform = this.platform.getTargetName().replaceAll("[^a-zA-Z0-9._-]", "_");
        String abi = "";
        if (this.abi != null && this.platform.requiresAbi()) {
            abi = "_" + this.abi.replaceAll("[^a-zA-Z0-9._-]", "-");
        }
        return String.format("jenkins_%s_%s_%s_%s%s", locale, density, resolution, platform, abi);
    }

    public static class Builder {
        private String platform;
        private String screenDensity;
        private String screenResolution;
        private String locale;
        private String abi;

        public Builder(String platform) {
            this.platform = platform;
        }

        public Builder setPlatform(String platform) {
            this.platform = platform;
            return this;
        }

        public Builder setScreenDensity(String screenDensity) {
            this.screenDensity = screenDensity;
            return this;
        }

        public Builder setScreenResolution(String screenResolution) {
            this.screenResolution = screenResolution;
            return this;
        }

        public Builder setLocale(String locale) {
            this.locale = locale;
            return this;
        }

        public Builder setAbi(String abi) {
            this.abi = abi;
            return this;
        }

        public EmulatorConfig build() {
            // TODO: Basic validation of required fields here / in constructor?
            EmulatorConfig c = new EmulatorConfig(platform);
            c.setScreenDensity(screenDensity);
            c.setScreenResolution(screenResolution);
            c.setLocale(locale);
            c.setAbi(abi);
            return c;
        }

    }

}
