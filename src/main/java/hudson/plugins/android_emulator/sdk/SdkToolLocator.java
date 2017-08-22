package hudson.plugins.android_emulator.sdk;

public class SdkToolLocator implements ToolLocator {
    @Override
    public String findInSdk(AndroidSdk androidSdk) {
        return ToolLocator.TOOLS_BIN_DIR;
    }
}
