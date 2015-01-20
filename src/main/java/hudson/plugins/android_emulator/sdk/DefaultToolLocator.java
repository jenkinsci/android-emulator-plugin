package hudson.plugins.android_emulator.sdk;

import java.io.File;

public class DefaultToolLocator implements ToolLocator {
    public String findInSdk(AndroidSdk androidSdk, Tool tool) {
        return "/tools/";
    }
}
