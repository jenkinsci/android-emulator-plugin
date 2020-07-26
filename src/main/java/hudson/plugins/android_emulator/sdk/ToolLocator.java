package hudson.plugins.android_emulator.sdk;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public interface ToolLocator {
    public static final String BUILD_TOOLS_DIR = "build-tools";
    public static final String EMULATOR_DIR = "emulator";
    public static final String PLATFORM_TOOLS_DIR = "platform-tools";
    public static final String PLATFORMS_DIR = "platforms";
    public static final String TOOLS_DIR = "tools";
    public static final String TOOLS_BIN_DIR = "tools/bin";

    @SuppressFBWarnings("MS_MUTABLE_ARRAY")
    public static final String[] SDK_DIRECTORIES_LEGACY = {
            TOOLS_DIR
    };

    @SuppressFBWarnings("MS_MUTABLE_ARRAY")
    public static final String[] SDK_DIRECTORIES = {
            EMULATOR_DIR, PLATFORM_TOOLS_DIR, TOOLS_DIR, TOOLS_BIN_DIR
    };

    String findInSdk(final boolean useLegacySdkStructure);
}
