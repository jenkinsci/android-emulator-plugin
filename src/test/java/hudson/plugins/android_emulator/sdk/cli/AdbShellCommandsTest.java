package hudson.plugins.android_emulator.sdk.cli;

import static org.junit.Assert.*;

import org.junit.Test;

import hudson.plugins.android_emulator.constants.AndroidKeyEvent;
import hudson.plugins.android_emulator.sdk.Tool;
import hudson.plugins.android_emulator.sdk.cli.SdkCliCommand;
import hudson.plugins.android_emulator.sdk.cli.SdkCliCommandFactory;

public class AdbShellCommandsTest {

    @Test
    public void testAdbShellListProcesses() {
        assertAdbShellCommand("-s dummyId shell ps",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(25).getListProcessesCommand("dummyId"));
        assertAdbShellCommand("-s android-23920 shell ps",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(22).getListProcessesCommand("android-23920"));
        assertAdbShellCommand("-s xid shell ps",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel( 3).getListProcessesCommand("xid"));
    }

    @Test
    public void testAdbWaitForDeviceStartCommand() {
        assertAdbShellCommand("-s dummyId wait-for-device shell getprop init.svc.bootanim",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(25).getWaitForDeviceStartupCommand("dummyId"));
        assertAdbShellCommand("-s android-23920 wait-for-device shell getprop init.svc.bootanim",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(22).getWaitForDeviceStartupCommand("android-23920"));
        assertAdbShellCommand("wait-for-device shell getprop init.svc.bootanim",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(4).getWaitForDeviceStartupCommand(null));
        assertAdbShellCommand("-s android-xxx wait-for-device shell getprop init.svc.bootanim",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(4).getWaitForDeviceStartupCommand("android-xxx"));
        assertAdbShellCommand("-s xid wait-for-device shell getprop dev.bootcomplete",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(3).getWaitForDeviceStartupCommand("xid"));
    }

    @Test
    public void testAdbWaitForDeviceStartExpectedAnswer() {
        assertEquals("stopped", SdkCliCommandFactory.getAdbShellCommandForAPILevel(25).getWaitForDeviceStartupExpectedAnswer());
        assertEquals("stopped", SdkCliCommandFactory.getAdbShellCommandForAPILevel(22).getWaitForDeviceStartupExpectedAnswer());
        assertEquals("stopped", SdkCliCommandFactory.getAdbShellCommandForAPILevel(4).getWaitForDeviceStartupExpectedAnswer());
        assertEquals("1", SdkCliCommandFactory.getAdbShellCommandForAPILevel(3).getWaitForDeviceStartupExpectedAnswer());
    }

    @Test
    public void testAdbClearMainLogCommand() {
        assertAdbShellCommand("-s dummyId shell logcat -c",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(25).getClearMainLogCommand("dummyId"));
        assertAdbShellCommand("-s android-23920 shell logcat -c",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(22).getClearMainLogCommand("android-23920"));
        assertAdbShellCommand("-s xid shell logcat -c",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel( 3).getClearMainLogCommand("xid"));
    }

    @Test
    public void testSetLogCatFormatToTimeCommand() {
        assertAdbShellCommand("-s dummyId shell logcat -v time",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(25).getSetLogCatFormatToTimeCommand("dummyId"));
        assertAdbShellCommand("-s android-23920 shell logcat -v time",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(22).getSetLogCatFormatToTimeCommand("android-23920"));
        assertAdbShellCommand("-s xid shell logcat -v time",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel( 3).getSetLogCatFormatToTimeCommand("xid"));
    }

    @Test
    public void testAdbLogMessage() {
        assertAdbShellCommand("-s dummyId shell log -p v -t Jenkins 'I'm a testcase!'",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(25).getLogMessageCommand("dummyId", "I'm a testcase!"));
        assertAdbShellCommand("-s android-23920 shell log -p v -t Jenkins 'Hello'",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(22).getLogMessageCommand("android-23920", "Hello"));
        assertAdbShellCommand("-s xid shell log -p v -t Jenkins 'dummy.msg'",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel( 3).getLogMessageCommand("xid", "dummy.msg"));
    }

    @Test
    public void testAdbSendKeyEventCommand() {
        assertAdbShellCommand("-s dummyId shell input keyevent 3",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(25).getSendKeyEventCommand("dummyId", AndroidKeyEvent.KEYCODE_HOME));
        assertAdbShellCommand("-s android-23920 shell input keyevent 4",
                        SdkCliCommandFactory.getAdbShellCommandForAPILevel(22).getSendKeyEventCommand("android-23920", AndroidKeyEvent.KEYCODE_BACK));
        assertAdbShellCommand("-s xid shell input keyevent 0",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(3).getSendKeyEventCommand("xid", AndroidKeyEvent.KEYCODE_UNKNOWN));
    }

    @Test
    public void testAdbSendBackKeyEventCommand() {
        assertAdbShellCommand("-s dummyId shell input keyevent 4",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(25).getSendBackKeyEventCommand("dummyId"));
        assertAdbShellCommand("-s android-23920 shell input keyevent 4",
                        SdkCliCommandFactory.getAdbShellCommandForAPILevel(22).getSendBackKeyEventCommand("android-23920"));
        assertAdbShellCommand("-s xid shell input keyevent 4",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(3).getSendBackKeyEventCommand("xid"));
    }

    @Test
    public void testAdbDismissKeyguardCommand() {
        assertAdbShellCommand("-s dummyId shell wm dismiss-keyguard",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(25).getDismissKeyguardCommand("dummyId"));
        assertAdbShellCommand("shell wm dismiss-keyguard",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(23).getDismissKeyguardCommand(null));
        assertAdbShellCommand("-s android-23920 shell input keyevent 82",
                        SdkCliCommandFactory.getAdbShellCommandForAPILevel(22).getDismissKeyguardCommand("android-23920"));
        assertAdbShellCommand("shell input keyevent 82",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(3).getDismissKeyguardCommand(null));
    }

    @Test
    public void testAdbMonkeyCommand() {
        assertAdbShellCommand("-s dummyId shell monkey -v -v -s 28640 --throttle 0 --dbg-no-events --ignore-crashes 1",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(25).getMonkeyInputCommand("dummyId", 28640, 0, "--dbg-no-events --ignore-crashes", 1));
        assertAdbShellCommand("-s android-23920 shell monkey -v -v -s 0 --throttle 100 -p test -c main 999",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(22).getMonkeyInputCommand("android-23920", 0, 100, "-p test -c main", 999));
        assertAdbShellCommand("-s xid shell monkey -v -v -s 73 --throttle 333 --monitor-native-crashes 33",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel( 3).getMonkeyInputCommand("xid", 73, 333, "--monitor-native-crashes", 33));
    }

    @Test
    public void testWithoudDeviceIdentifier() {
        assertAdbShellCommand("shell ps", SdkCliCommandFactory.getAdbShellCommandForAPILevel(25).getListProcessesCommand(null));
        assertAdbShellCommand("shell ps", SdkCliCommandFactory.getAdbShellCommandForAPILevel(22).getListProcessesCommand(""));
        assertAdbShellCommand("wait-for-device shell getprop dev.bootcomplete",
                SdkCliCommandFactory.getAdbShellCommandForAPILevel(3).getWaitForDeviceStartupCommand(null));
    }

    private void assertAdbShellCommand(final String expectedCommand, final SdkCliCommand actualCommand) {
        assertEquals(Tool.ADB, actualCommand.getTool());
        assertEquals(expectedCommand, actualCommand.getArgs());
    }
}
