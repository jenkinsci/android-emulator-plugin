package hudson.plugins.android_emulator.sdk.cli;

import hudson.plugins.android_emulator.constants.AndroidKeyEvent;

/**
 * Extends {@code AdbShellCommandsCurrentBase} and simply overwrites the commands
 * which differ for devices running on API-level 4 to 22.
 */
public class AdbShellCommand04To22 extends AdbShellCommandsCurrentBase implements AdbShellCommands {

    @Override
    public SdkCliCommand getDismissKeyguardCommand(String deviceSerial) {
        return getSendKeyEventCommand(deviceSerial, AndroidKeyEvent.KEYCODE_MENU);
    }
}
