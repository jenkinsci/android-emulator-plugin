package hudson.plugins.android_emulator.sdk;

public class PlatformToolLocator implements ToolLocator {
    @Override
    public String findInSdk(final boolean useLegacySdkStructure) {
        return ToolLocator.PLATFORM_TOOLS_DIR;
    }
}
