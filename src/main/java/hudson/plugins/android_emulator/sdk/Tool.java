package hudson.plugins.android_emulator.sdk;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("SE_BAD_FIELD")
public enum Tool {
    ADB("adb", ".exe", new PlatformToolLocator()),
    ANDROID_LEGACY("android", ".bat"),
    EMULATOR("emulator", ".exe", new EmulatorToolLocator()),
    EMULATOR_ARM("emulator-arm", ".exe", new EmulatorToolLocator()),
    EMULATOR_MIPS("emulator-mips", ".exe", new EmulatorToolLocator()),
    EMULATOR_X86("emulator-x86", ".exe", new EmulatorToolLocator()),
    EMULATOR64_ARM("emulator64-arm", ".exe", new EmulatorToolLocator()),
    EMULATOR64_MIPS("emulator64-mips", ".exe", new EmulatorToolLocator()),
    EMULATOR64_X86("emulator64-x86", ".exe", new EmulatorToolLocator()),
    AVDMANAGER("avdmanager", ".bat", new SdkToolLocator()),
    SDKMANAGER("sdkmanager", ".bat", new SdkToolLocator()),
    MKSDCARD("mksdcard", ".exe");

    @SuppressFBWarnings("MS_MUTABLE_ARRAY")
    public static Tool[] EMULATORS = new Tool[] { EMULATOR,
           EMULATOR_ARM,   EMULATOR_MIPS,   EMULATOR_X86,
           EMULATOR64_ARM, EMULATOR64_MIPS, EMULATOR64_X86
    };

    private static Tool[] CMD_LINE_TOOLS = new Tool[] {
        AVDMANAGER, SDKMANAGER
    };

    private static Tool[] REQUIRED = new Tool[] {
        ADB, EMULATOR, AVDMANAGER, SDKMANAGER
    };

    private static Tool[] REQUIRED_LEGACY = new Tool[] {
        ADB, ANDROID_LEGACY, EMULATOR
    };

    public final String executable;
    public final String windowsExtension;
    public final ToolLocator toolLocator;

    Tool(String executable, String windowsExtension) {
        this(executable, windowsExtension, new DefaultToolLocator());
    }

    Tool(String executable, String windowsExtension, ToolLocator toolLocator) {
        this.executable = executable;
        this.windowsExtension = windowsExtension;
        this.toolLocator = toolLocator;
    }

    public String getExecutable(boolean isUnix) {
        if (isUnix) {
            return executable;
        }
        return executable + windowsExtension;
    }

    private String getPathInSdk(final boolean legacySdkStructure, final boolean isUnix) {
        return toolLocator.findInSdk(legacySdkStructure) + "/" + getExecutable(isUnix);
    }

    public String getPathInSdk(final AndroidSdk androidSdk, final boolean isUnix) {
        return getPathInSdk(androidSdk.useLegacySdkStructure(), isUnix);
    }

    /**
     * Retrieve a list of relative paths to the SDK root directory
     * for the given tools, either for the legacy or the new structure.
     *
     * @param tools the Tools to get the relative path for
     * @param useLegacy return the paths for the new or the legacy structure
     * @param isUnix if false the windows suffix is appended
     * @return a list of relative paths (including the executable name) expected to exist
     */
    private static String[] getRequiredToolsRelativePaths(final Tool[] tools, final boolean useLegacy, final boolean isUnix) {
        final List<String> paths = new ArrayList<>();
        for (final Tool tool : tools) {
            paths.add(tool.getPathInSdk(useLegacy, isUnix));
        }
        return paths.toArray(new String[0]);
    }

    /**
     * Retrieve a list of relative paths to the SDK root directory
     * for all necessary tools needed for a SDK installations using
     * the new structure (tools/bin, emulator dir).
     *
     * @param isUnix if false the windows suffix is appended
     * @return a list of relative paths (including the executable name) expected to exist
     */
    public static String[] getRequiredToolsRelativePaths(final boolean isUnix) {
        return getRequiredToolsRelativePaths(Tool.REQUIRED, false, isUnix);
    }

    public static String[] getRequiredCmdLineToolsPaths(final boolean isUnix) {
        return getRequiredToolsRelativePaths(Tool.CMD_LINE_TOOLS, false, isUnix);
    }

    /**
     * Retrieve a list of relative paths to the SDK root directory
     * for all necessary tools needed for a SDK installations using
     * the legacy structure (tools/android, ...).
     *
     * @param isUnix if false the windows suffix is appended
     * @return a list of relative paths (including the executable name) expected to exist
     */
    public static String[] getRequiredToolsLegacyRelativePaths(final boolean isUnix) {
        return getRequiredToolsRelativePaths(Tool.REQUIRED_LEGACY, true, isUnix);
    }

    @Override
    public String toString() {
        return executable;
    }
}
