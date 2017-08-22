package hudson.plugins.android_emulator.sdk.cli;

import hudson.plugins.android_emulator.sdk.AndroidSdk;

/**
 * This helper class is used to retrieve the correct implementation
 * of the {@code adb shell} and the Android SDK tools commands for
 * the given device API level respective for the SDK version.
 */
public final class SdkCliCommandFactory {

    // empty private constructor to avoid instantiation and sub-classing
    private SdkCliCommandFactory() {}

    /**
     * Retrieve the correct {@code AdbShellCommands} for the given API-Level of the
     * device where the commands should run.
     *
     * @param deviceAPILevel Android API-Level for the target device
     * @return an object of an class implementing the {@code AdbShellCommands} interface to retrieve
     * the correct {@code adb shell} commands for the given API-level
     */
    public static AdbShellCommands getAdbShellCommandForAPILevel(final int deviceAPILevel) {
        if (deviceAPILevel < 4) {
            return new AdbShellCommand00To03();
        } else if (deviceAPILevel < 23) {
            return new AdbShellCommand04To22();
        } else {
            return new AdbShellCommandsCurrentBase();
        }
    }

    /**
     * Retrieve the correct {@code SdkCommands} for the given Android SDK.
     *
     * @param androidSdk SDK tools to extract the current version and retrieve the correct commands
     * @return an object of an class implementing the {@code SdkToolsCommands} interface to retrieve
     * the correct tools commands for the given SDK
     */
    public static SdkToolsCommands getCommandsForSdk(final AndroidSdk androidSdk) {
        final int sdkMajorVersion = (androidSdk != null) ? androidSdk.getSdkToolsMajorVersion() : Integer.MAX_VALUE;
        return SdkCliCommandFactory.getCommandsForSdk(sdkMajorVersion);
    }

    /**
     * Retrieve the correct {@code SdkCommands} for the given SDK Tools major version.
     *
     * @param sdkMajorVersion SDK tools version, to retrieve the correct commands
     * @return an object of an class implementing the {@code SdkToolsCommands} interface to retrieve
     * the correct tools commands for the given SDK version
     */
    public static SdkToolsCommands getCommandsForSdk(final int sdkMajorVersion) {
        if (sdkMajorVersion < 17) {
            return new SdkToolsCommands00To16();
        } else if (sdkMajorVersion < 25) {
            return new SdkToolsCommands17To24();
        } else {
            return new SdkToolsCommandsCurrentBase();
        }
    }
}
