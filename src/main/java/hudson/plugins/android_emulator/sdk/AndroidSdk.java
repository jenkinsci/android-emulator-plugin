package hudson.plugins.android_emulator.sdk;

import java.io.Serializable;

public class AndroidSdk implements Serializable {

    private static final long serialVersionUID = 1L;

    /** First version in which "adb connect" supported emulators. */
    private static final int SDK_TOOLS_EMU_CONNECT = 7;

    /** First version in which snapshots were supported. */
    private static final int SDK_TOOLS_SNAPSHOTS = 9;

    /** First version in which we can automatically install individual SDK components. */
    private static final int SDK_AUTO_INSTALL = 14;

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

    public boolean supportsEmuConnect() {
        return sdkToolsVersion >= SDK_TOOLS_EMU_CONNECT;
    }

    public boolean supportsSnapshots() {
        return sdkToolsVersion >= SDK_TOOLS_SNAPSHOTS;
    }

    public boolean supportsComponentInstallation() {
        return sdkToolsVersion >= SDK_AUTO_INSTALL;
    }

}