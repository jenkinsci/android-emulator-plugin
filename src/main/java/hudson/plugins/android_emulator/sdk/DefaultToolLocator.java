package hudson.plugins.android_emulator.sdk;

public class DefaultToolLocator implements ToolLocator {
    public String findInSdk(AndroidSdk androidSdk, Tool tool ) {
        return "/tools/";
    }
}
