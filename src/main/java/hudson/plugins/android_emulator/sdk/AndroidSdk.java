package hudson.plugins.android_emulator.sdk;

import com.google.common.annotations.VisibleForTesting;
import hudson.Util;
import hudson.plugins.android_emulator.Constants;
import hudson.plugins.android_emulator.util.ConfigFileUtils;
import hudson.util.VersionNumber;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

public class AndroidSdk implements Serializable {

    private static final long serialVersionUID = 1L;

    /** First version in which snapshots were supported. */
    private static final int SDK_TOOLS_SNAPSHOTS = 9;

    /** First version in which we can automatically install individual SDK components. */
    private static final int SDK_AUTO_INSTALL = 14;

    /** First version in which we can programmatically install system images. */
    private static final int SDK_SYSTEM_IMAGE_INSTALL = 17;

    /** First version that recognises the "sys-img-[arch]-[tag]-[api]" format. */
    private static final int SDK_SYSTEM_IMAGE_NEW_FORMAT = 23;

    /** First version that ships the Android Emulator 2.0. */
    private static final int SDK_EMULATOR_V2 = 25;

    /**
     * First version where the 'android' command is deprecated/removed
     * and fully replaced by 'avdmanager' and 'sdkmanager'
     */
    private static final String SDK_TOOLS_ANDROID_CMD_DEPRECATED = "25.3";

    /** First version that comes with the emulator 2.0 supporting the needed -ports, etc commands. */
    private static final int SDK_EMULATOR_V2_USABLE = 26;

    private final String sdkRoot;
    private final String sdkHome;
    private String sdkToolsVersion;

    public AndroidSdk(String root, String home) throws IOException {
        this.sdkRoot = root;
        this.sdkHome = home;
        if (hasKnownRoot()) {
            determineVersion();
        }
    }

    private void determineVersion() throws IOException {
        // Determine SDK tools version
        File toolsPropFile = new File(sdkRoot, "tools/source.properties");
        Map<String, String> toolsProperties;
        toolsProperties = ConfigFileUtils.parseConfigFile(toolsPropFile);
        sdkToolsVersion = Util.fixEmptyAndTrim(toolsProperties.get("Pkg.Revision"));
    }

    public boolean hasKnownRoot() {
        return this.sdkRoot != null;
    }

    public String getSdkRoot() {
        return this.sdkRoot;
    }

    public boolean hasKnownHome() {
        return this.sdkHome != null;
    }

    public String getSdkHome() {
        return this.sdkHome;
    }

    public String getSdkToolsVersion() {
        return this.sdkToolsVersion;
    }

    @VisibleForTesting
    void setSdkToolsVersion(String version) {
        sdkToolsVersion = version;
    }

    /** @return The major version number of the SDK tools being used, or {@code 0} if unknown. */
    public int getSdkToolsMajorVersion() {
        if (sdkToolsVersion == null) {
            return 0;
        }
        // We create this object on-demand rather than holding on to it, as VersionNumber is not Serializable
        return new VersionNumber(sdkToolsVersion).digit(0);
    }

    /**
     * Determines if the AndroidSdk supports creation of snapshots to enable persistence,
     * currently in Android Emulator v2.0 the usage of snapshots leads to an error on creation
     * of the virtual device. So this option is currently disabled.
     * @return {@code true} if this SDK supports snapshots in AVD creation
     */
    public boolean supportsSnapshots() {
        return getSdkToolsMajorVersion() >= SDK_TOOLS_SNAPSHOTS && !supportsEmulatorV2Full();
    }

    public boolean supportsComponentInstallation() {
        return getSdkToolsMajorVersion() >= SDK_AUTO_INSTALL;
    }

    public boolean supportsSystemImageInstallation() {
        return getSdkToolsMajorVersion() >= SDK_SYSTEM_IMAGE_INSTALL;
    }

    public boolean supportsSystemImageNewFormat() {
        return getSdkToolsMajorVersion() >= SDK_SYSTEM_IMAGE_NEW_FORMAT;
    }

    /** @return {@code true} if this SDK ships the Android Emulator 2.0. */
    public boolean supportsEmulatorV2() {
        return getSdkToolsMajorVersion() >= SDK_EMULATOR_V2;
    }

    /**
     * @return {@code true} if this SDK ships the Android Emulator 2.0 which supports all needed
     *  command line options used by the plugin.
     */
    public boolean supportsEmulatorV2Full() {
        return getSdkToolsMajorVersion() >= SDK_EMULATOR_V2_USABLE;
    }

    /** @return {@code true} if this SDK has an emulator that supports the "-engine" flag. */
    public boolean supportsEmulatorEngineFlag() {
        return supportsEmulatorV2();
    }

    /**
     * @return {@code true} if this SDK has an emulator that supports the "-engine" flag (uses the Android
     * Emulator 2.0) but the version does not support the needed CLI arguments (-ports, -prop, report-console).
     */
    public boolean forceClassicEmulatorEngine() {
        return supportsEmulatorEngineFlag() && !supportsEmulatorV2Full();
    }

    public boolean isAndroidCmdDeprecated() {
        return !useLegacySdkStructure();
    }

    public boolean isOlderThanDefaultDownloadVersion() {
        if (sdkToolsVersion == null) {
            return true;
        }
        final VersionNumber sdk = new VersionNumber(sdkToolsVersion);
        return sdk.isOlderThan(new VersionNumber(Constants.SDK_TOOLS_DEFAULT_VERSION));
    }

    public boolean useLegacySdkStructure() {
        if (sdkToolsVersion == null) {
            return false;
        }
        final VersionNumber sdk = new VersionNumber(sdkToolsVersion);
        return sdk.isOlderThan(new VersionNumber(SDK_TOOLS_ANDROID_CMD_DEPRECATED));
    }

    /** {@return true} if we should explicitly select a non-64-bit emulator executable for snapshot-related tasks. */
    public boolean requiresAndroidBug34233Workaround() {
        if (sdkToolsVersion == null) {
            return true;
        }
        final VersionNumber sdk = new VersionNumber(sdkToolsVersion);
        return sdk.isNewerThan(new VersionNumber("20.0.3")) && sdk.isOlderThan(new VersionNumber("22.6"));
    }

}
