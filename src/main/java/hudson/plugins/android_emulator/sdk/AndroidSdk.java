package hudson.plugins.android_emulator.sdk;

import hudson.Util;
import hudson.plugins.android_emulator.util.Utils;

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
    private boolean usesPlatformTools;
    private int sdkToolsVersion;
    private String sdkToolsRevision;

    public AndroidSdk(String root, String home) throws IOException {
        this.sdkRoot = root;
        this.sdkHome = home;
        if (hasKnownRoot()) {
            determineVersion();
        }
    }

    private void determineVersion() throws IOException {
        // Determine whether SDK has platform tools installed
        File toolsDirectory = new File(sdkRoot, "platform-tools");
        setUsesPlatformTools(toolsDirectory.isDirectory());

        // Determine SDK tools version
        File toolsPropFile = new File(sdkRoot, "tools/source.properties");
        Map<String, String> toolsProperties;
        toolsProperties = Utils.parseConfigFile(toolsPropFile);
        String revisionStr = Util.fixEmptyAndTrim(toolsProperties.get("Pkg.Revision"));
        if (revisionStr != null) {
            setSdkToolsVersion(Utils.parseRevisionString(revisionStr));
        }
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

    public boolean usesPlatformTools() {
        return this.usesPlatformTools;
    }

    public void setUsesPlatformTools(boolean usesPlatformTools) {
        this.usesPlatformTools = usesPlatformTools;
    }

    public void setSdkToolsVersion(int version) {
        this.sdkToolsVersion = version;
    }

    /** @return The major version number of the SDK tools being used. */
    public int getSdkToolsVersion() {
        return this.sdkToolsVersion;
    }

    public boolean supportsSnapshots() {
        return sdkToolsVersion >= SDK_TOOLS_SNAPSHOTS;
    }

    public boolean supportsComponentInstallation() {
        return sdkToolsVersion >= SDK_AUTO_INSTALL;
    }

    public boolean supportsSystemImageInstallation() {
        return sdkToolsVersion >= SDK_SYSTEM_IMAGE_INSTALL;
    }

    public boolean supportsSystemImageNewFormat() {
        return sdkToolsVersion >= SDK_SYSTEM_IMAGE_NEW_FORMAT;
    }

}
