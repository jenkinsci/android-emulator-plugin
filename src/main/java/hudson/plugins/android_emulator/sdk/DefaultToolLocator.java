package hudson.plugins.android_emulator.sdk;

public class DefaultToolLocator implements ToolLocator {
    @Override
    public String findInSdk(final boolean useLegacySdkStructure) {
        return ToolLocator.TOOLS_DIR;
    }
}
