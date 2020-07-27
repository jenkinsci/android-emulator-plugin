package hudson.plugins.android_emulator.sdk;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public interface ToolLocator {
    String BUILD_TOOLS_DIR = "build-tools";
    String EMULATOR_DIR = "emulator";
    String PLATFORM_TOOLS_DIR = "platform-tools";
    String PLATFORMS_DIR = "platforms";
    String TOOLS_DIR = "tools";
    String TOOLS_BIN_DIR = "tools/bin";

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
