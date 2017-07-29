package hudson.plugins.android_emulator.sdk;

public class EmulatorToolLocator implements ToolLocator {
    public String findInSdk(AndroidSdk androidSdk, Tool tool) {
        if (androidSdk.supportsEmulatorV2()) {
            return "/emulator/";
        } else {
            return "/tools/";
        }
    }
}
