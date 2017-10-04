package hudson.plugins.android_emulator.sdk;

public interface ToolLocator {
    public static final String BUILD_TOOLS_DIR = "build-tools";
    public static final String EMULATOR_DIR = "emulator";
    public static final String PLATFORM_TOOLS_DIR = "platform-tools";
    public static final String PLATFORMS_DIR = "platforms";
    public static final String TOOLS_DIR = "tools";
    public static final String TOOLS_BIN_DIR = "tools/bin";

    public static final String[] SDK_DIRECTORIES_LEGACY = {
            TOOLS_DIR, PLATFORM_TOOLS_DIR
    };

    public static final String[] SDK_DIRECTORIES = {
            EMULATOR_DIR, PLATFORM_TOOLS_DIR, TOOLS_DIR, TOOLS_BIN_DIR
    };

    String findInSdk(final boolean useLegacySdkStructure);
}
