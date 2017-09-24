package hudson.plugins.android_emulator.sdk;

public class EmulatorToolLocator implements ToolLocator {
    @Override
    public String findInSdk(AndroidSdk androidSdk) {
        if (!androidSdk.useLegacySdkStructure()) {
            return ToolLocator.EMULATOR_DIR;
        } else {
            return ToolLocator.TOOLS_DIR;
        }
    }
}
