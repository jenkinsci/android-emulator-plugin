package hudson.plugins.android_emulator.sdk;

import java.io.Serializable;

public class AndroidSdk implements Serializable {

    private static final long serialVersionUID = 1L;

    /** First version in which snapshots were supported. */
    private static final int SDK_TOOLS_SNAPSHOTS = 9;

    /** First version in which we can automatically install individual SDK components. */
    private static final int SDK_AUTO_INSTALL = 14;

    /** First version in which we can programmatically install system images. */
    private static final int SDK_INSTALL_SYSTEM_IMAGE = 17;

    private final String sdkHome;
    private boolean usesPlatformTools;
    private int sdkToolsVersion;

    public AndroidSdk(String home) {
        this.sdkHome = home;
    }

    public boolean hasKnownRoot() {
        return this.sdkHome != null;
    }

    public String getSdkRoot() {
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
        return sdkToolsVersion >= SDK_INSTALL_SYSTEM_IMAGE;
    }

}
