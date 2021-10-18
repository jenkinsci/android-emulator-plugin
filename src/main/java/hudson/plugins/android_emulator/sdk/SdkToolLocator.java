package hudson.plugins.android_emulator.sdk;

public class SdkToolLocator implements ToolLocator {
    @Override
    public String findInSdk(final boolean useLegacySdkStructure) {
        if (!useLegacySdkStructure) {
            return ToolLocator.CMD_TOOLS_BIN_DIR;
        }
        return ToolLocator.TOOLS_BIN_DIR;
    }
}
