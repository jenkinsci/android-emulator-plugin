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

  private final String sdkRoot;
  private final String sdkHome;
  private boolean usesPlatformTools;
  private int sdkToolsVersion;
  private int sdkToolsMajorVersion;

  public AndroidSdk(String root, String home) {
    this.sdkRoot = root;
    this.sdkHome = home;
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

  public int getSdkToolsVersion() {
    return this.sdkToolsVersion;
  }

  public int getSdkToolsMajorVersion() {
    return this.sdkToolsMajorVersion;
  }

  public boolean supportsSnapshots() {
    return sdkToolsMajorVersion >= SDK_TOOLS_SNAPSHOTS;
  }

  public boolean supportsComponentInstallation() {
    return sdkToolsMajorVersion >= SDK_AUTO_INSTALL;
  }

  public boolean supportsSystemImageInstallation() {
    return sdkToolsMajorVersion >= SDK_INSTALL_SYSTEM_IMAGE;
  }

}
