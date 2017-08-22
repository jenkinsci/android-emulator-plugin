package hudson.plugins.android_emulator.sdk;

public interface ToolLocator {
    public final String EMULATOR_DIR = "/emulator/";
    public final String PLATFORM_TOOLS_DIR = "/platform-tools/";
    public final String TOOLS_DIR = "/tools/";
    public final String TOOLS_BIN_DIR = "/tools/bin/";

    String findInSdk(AndroidSdk androidSdk);
}
