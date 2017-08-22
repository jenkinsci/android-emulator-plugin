package hudson.plugins.android_emulator.sdk;

public class DefaultToolLocator implements ToolLocator {
    @Override
    public String findInSdk(AndroidSdk androidSdk) {
        return ToolLocator.TOOLS_DIR;
    }
}
