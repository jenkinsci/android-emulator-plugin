package hudson.plugins.android_emulator.sdk;
import hudson.plugins.android_emulator.SdkInstallationException;

public interface ToolLocator {
    String findInSdk(AndroidSdk androidSdk, Tool tool)throws SdkInstallationException ;
}
