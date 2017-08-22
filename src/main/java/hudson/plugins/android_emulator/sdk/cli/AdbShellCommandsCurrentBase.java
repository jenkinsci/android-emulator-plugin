package hudson.plugins.android_emulator.sdk.cli;

import hudson.plugins.android_emulator.constants.AndroidKeyEvent;
import hudson.plugins.android_emulator.sdk.Tool;

/**
 * This class holds the implementations for all used commands via {@code adb shell}
 * on devices running the latest API. As some calls have never changed in history,
 * it is most likely that this class is used as base for the other version implementations.
 */
public class AdbShellCommandsCurrentBase implements AdbShellCommands {

    @Override
    public SdkCliCommand getListProcessesCommand(final String deviceSerial) {
        return getAdbShellCommand(deviceSerial, "ps");
    }

    // Other tools use the "bootanim" variant, which supposedly signifies the system has booted a bit further;
    // though this doesn't appear to be available on Android 1.5, while it should work fine on Android 1.6+
    @Override
    public SdkCliCommand getWaitForDeviceStartupCommand(final String deviceSerial) {
        return getAdbShellCommand(deviceSerial, true, "getprop init.svc.bootanim");
    }

    @Override
    public String getWaitForDeviceStartupExpectedAnswer() {
        return "stopped";
    }

    @Override
    public SdkCliCommand getClearMainLogCommand(final String deviceSerial) {
        return getAdbShellCommand(deviceSerial, "logcat -c");
    }

    @Override
    public SdkCliCommand getSetLogCatFormatToTimeCommand(String deviceSerial) {
        return getAdbShellCommand(deviceSerial, "logcat -v time");
    }

    @Override
    public SdkCliCommand getLogMessageCommand(final String deviceSerial, final String logMessage) {
        final String logCommand = String.format("log -p v -t Jenkins '%s'", logMessage);
        return getAdbShellCommand(deviceSerial, logCommand);
    }

    @Override
    public SdkCliCommand getSendKeyEventCommand(final String deviceSerial, final AndroidKeyEvent keyEvent) {
        final String inputKeyEventCommand = String.format("input keyevent %d", keyEvent.getKeyCode());
        return getAdbShellCommand(deviceSerial, inputKeyEventCommand);
    }

    @Override
    public SdkCliCommand getSendBackKeyEventCommand(final String deviceSerial) {
        return getSendKeyEventCommand(deviceSerial, AndroidKeyEvent.KEYCODE_BACK);
    }

    @Override
    public SdkCliCommand getDismissKeyguardCommand(final String deviceSerial) {
        // Android 6.0 introduced a command to dismiss the keyguard on unsecured devices
        return getAdbShellCommand(deviceSerial, "wm dismiss-keyguard");
    }

    @Override
    public SdkCliCommand getMonkeyInputCommand(final String deviceSerial,
            final long seedValue, final int throttleMs,
            final String extraArgs, final int eventCount) {
        final String command = String.format("monkey -v -v -s %d --throttle %d %s %d", seedValue,
                throttleMs, extraArgs, eventCount);
        return getAdbShellCommand(deviceSerial, command);
    }

    /**
     * Generic method to generate and 'adb shell' command to run on the given device.
     *
     * @param deviceSerial device to run adb command on (add via '-s' option)
     * @param waitForDevice if true the 'wait-for-device' directive is added as adb parameter
     * @param command the command to run on the device
     * @return {@code SdkCliCommand} object which holds the ADB-Tool and the generated command
     */
    protected SdkCliCommand getAdbShellCommand(final String deviceSerial, final boolean waitForDevice, final String command) {
        final String deviceSerialArgs;
        if (deviceSerial != null && !deviceSerial.isEmpty()) {
            deviceSerialArgs = "-s " + deviceSerial + " ";
        } else {
            deviceSerialArgs = "";
        }
        final String waitForDeviceStr = (waitForDevice) ? "wait-for-device " : "";

        final String shellCommand = String.format("%s%sshell %s", deviceSerialArgs, waitForDeviceStr, command);
        return new SdkCliCommand(Tool.ADB, shellCommand);
    }

    /**
     * Generic method to generate and 'adb shell' command to run on the given device.
     * Wrapper for {@code getAdbShellCommand} with assuming waitForDevice is false.
     *
     * @param deviceSerial device to run adb command on (add via '-s' option)
     * @param command the command to run on the device
     * @return {@code SdkCliCommand} object which holds the ADB-Tool and the generated command
     */
    protected SdkCliCommand getAdbShellCommand(final String deviceSerial, final String command) {
        return getAdbShellCommand(deviceSerial, false, command);
    }
}
