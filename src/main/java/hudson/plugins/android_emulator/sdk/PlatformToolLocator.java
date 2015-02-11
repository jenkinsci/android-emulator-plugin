package hudson.plugins.android_emulator.sdk;

public class PlatformToolLocator implements ToolLocator {
    public String findInSdk(AndroidSdk androidSdk, Tool tool) {
        return "/platform-tools/";
    }
}
