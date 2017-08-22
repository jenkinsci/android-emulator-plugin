package hudson.plugins.android_emulator.sdk.cli;

/**
 * Extends {@code AdbShellCommands04To22} and simply overwrites the commands
 * which differ for devices running on API-level 3 and below.
 */
public class AdbShellCommand00To03 extends AdbShellCommand04To22 implements AdbShellCommands {

    @Override
    public SdkCliCommand getWaitForDeviceStartupCommand(final String deviceSerial) {
        return getAdbShellCommand(deviceSerial, true, "getprop dev.bootcomplete");
    }

    @Override
    public String getWaitForDeviceStartupExpectedAnswer() {
        return "1";
    }
}
