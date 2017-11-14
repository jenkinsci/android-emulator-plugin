package hudson.plugins.android_emulator.sdk;

public class SdkToolLocator implements ToolLocator {
    @Override
    public String findInSdk(final boolean useLegacySdkStructure) {
        return ToolLocator.TOOLS_BIN_DIR;
    }
}
