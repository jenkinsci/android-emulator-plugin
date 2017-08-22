package hudson.plugins.android_emulator.sdk.cli;

import hudson.plugins.android_emulator.constants.AndroidKeyEvent;

/**
 * Commands which are run via 'adb shell' command on a specified device
 */
public interface AdbShellCommands {
    SdkCliCommand getListProcessesCommand(final String deviceSerial);

    SdkCliCommand getWaitForDeviceStartupCommand(final String deviceSerial);
    String getWaitForDeviceStartupExpectedAnswer();

    SdkCliCommand getClearMainLogCommand(final String deviceSerial);

    SdkCliCommand getSetLogCatFormatToTimeCommand(final String deviceSerial);
    SdkCliCommand getLogMessageCommand(final String deviceSerial, final String logMessage);

    SdkCliCommand getSendKeyEventCommand(final String deviceSerial, final AndroidKeyEvent keyEvent);
    SdkCliCommand getSendBackKeyEventCommand(final String deviceSerial);

    SdkCliCommand getDismissKeyguardCommand(final String deviceSerial);

    SdkCliCommand getMonkeyInputCommand(final String deviceSerial,
            final long seedValue, final int throttleMs,
            final String extraArgs, final int eventCount);
}
