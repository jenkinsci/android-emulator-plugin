package hudson.plugins.android_emulator.sdk;

import com.google.common.annotations.VisibleForTesting;
import hudson.Util;
import hudson.plugins.android_emulator.util.Utils;
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
        toolsProperties = Utils.parseConfigFile(toolsPropFile);
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

    public boolean supportsSnapshots() {
        return getSdkToolsMajorVersion() >= SDK_TOOLS_SNAPSHOTS;
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

    /** {@return true} if we should explicitly select a non-64-bit emulator executable for snapshot-related tasks. */
    public boolean requiresAndroidBug34233Workaround() {
        if (sdkToolsVersion == null) {
            return true;
        }
        final VersionNumber sdk = new VersionNumber(sdkToolsVersion);
        return sdk.isNewerThan(new VersionNumber("20.0.3")) && sdk.isOlderThan(new VersionNumber("22.6"));
    }

}
