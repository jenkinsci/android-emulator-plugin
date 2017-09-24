package hudson.plugins.android_emulator.sdk.cli;

import static org.junit.Assert.*;

import org.junit.Test;

import hudson.plugins.android_emulator.sdk.Tool;
import hudson.plugins.android_emulator.sdk.cli.SdkCliCommand;
import hudson.plugins.android_emulator.sdk.cli.SdkCliCommandFactory;

public class SdkCommandsTest {

    @Test
    public void testInstallAndUpdateCommand() {
        assertUpgradeParamsToAllToolVersions("platform-tools", "platform-tool");
        assertUpgradeParamsToAllToolVersions("platform-tools", "platform-tools");

        assertUpgradeParamsToAllToolVersions("tools", "tool");
        assertUpgradeParamsToAllToolVersions("tools", "tools");

        assertUpgradeParamsToAllToolVersions("add-ons;addon-google_apis-google-17", "addon-google_apis-google-17");
        assertUpgradeParamsToAllToolVersions("add-ons;addon-google_apis-google-17", "add-ons;addon-google_apis-google-17");

        assertUpgradeParamsToAllToolVersions("add-ons;addon-google_gdk-google-19", "addon-google_gdk-google-19");
        assertUpgradeParamsToAllToolVersions("add-ons;addon-google_gdk-google-19", "add-ons;addon-google_gdk-google-19");

        assertUpgradeParamsToAllToolVersions("build-tools;26.0.1", "build-tools-26.0.1");
        assertUpgradeParamsToAllToolVersions("build-tools;26.0.1", "build-tools;26.0.1");

        assertUpgradeParamsToAllToolVersions("platforms;android-22", "android-22");
        assertUpgradeParamsToAllToolVersions("platforms;android-22", "platforms;android-22");

        assertUpgradeParamsToAllToolVersions("extras;android;m2repository", "extra-android-m2repository");
        assertUpgradeParamsToAllToolVersions("extras;android;m2repository", "extras;android;m2repository");

        assertUpgradeParamsToAllToolVersions("extras;google;auto", "extra-google-auto");
        assertUpgradeParamsToAllToolVersions("extras;google;auto", "extras;google;auto");

        assertUpgradeParamsToAllToolVersions("extras;google;m2repository", "extra-google-m2repository");
        assertUpgradeParamsToAllToolVersions("extras;google;m2repository", "extras;google;m2repository");

        assertUpgradeParamsToAllToolVersions("system-images;android-24;default;x86_64", "sys-img-x86_64-android-24");
        assertUpgradeParamsToAllToolVersions("system-images;android-24;default;x86_64", "system-images;android-24;default;x86_64");

        assertUpgradeParamsToAllToolVersions(
                "tools extras;android;m2repository extras;google;m2repository",
                "tool,extra-android-m2repository,extra-google-m2repository");

        assertUpgradeParamsToAllToolVersions(
                "platform-tools tools extras;android;m2repository extras;google;m2repository",
                "platform-tool,tool,extra-android-m2repository,extra-google-m2repository");


    }

    private void assertUpgradeParamsToAllToolVersions(final String expected, final String input) {
        final SdkCliCommand installCmdV25_3 = SdkCliCommandFactory.getCommandsForSdk("25.3").getSdkInstallAndUpdateCommand("", input);
        final SdkCliCommand installCmdV25 = SdkCliCommandFactory.getCommandsForSdk("25").getSdkInstallAndUpdateCommand("", input);
        final SdkCliCommand installCmdV17 = SdkCliCommandFactory.getCommandsForSdk("17").getSdkInstallAndUpdateCommand("", input);
        final SdkCliCommand installCmdV04 = SdkCliCommandFactory.getCommandsForSdk("4").getSdkInstallAndUpdateCommand("", input);

        assertEquals(Tool.SDKMANAGER, installCmdV25_3.getTool());
        assertEquals(Tool.ANDROID_LEGACY, installCmdV25.getTool());
        assertEquals(Tool.ANDROID_LEGACY, installCmdV17.getTool());
        assertEquals(Tool.ANDROID_LEGACY, installCmdV04.getTool());

        assertEquals("--include_obsolete " + expected, installCmdV25_3.getArgs());
        assertEquals("update sdk -u -a  -t " + input, installCmdV25.getArgs());
        assertEquals("update sdk -u -a  -t " + input, installCmdV17.getArgs());
        assertEquals("update sdk -u -o  -t " + input, installCmdV04.getArgs());
    }

    @Test
    public void testListInstalledComponentsCommand() {
        final SdkCliCommand listSdkComponentsCmdV25_3 = SdkCliCommandFactory.getCommandsForSdk("25.3").getListSdkComponentsCommand();
        final SdkCliCommand listSdkComponentsCmdV25 = SdkCliCommandFactory.getCommandsForSdk("25").getListSdkComponentsCommand();
        final SdkCliCommand listSdkComponentsCmdV17 = SdkCliCommandFactory.getCommandsForSdk("17").getListSdkComponentsCommand();
        final SdkCliCommand listSdkComponentsCmdV04 = SdkCliCommandFactory.getCommandsForSdk("4").getListSdkComponentsCommand();

        assertEquals(Tool.SDKMANAGER, listSdkComponentsCmdV25_3.getTool());
        assertEquals(Tool.ANDROID_LEGACY, listSdkComponentsCmdV25.getTool());
        assertEquals(Tool.ANDROID_LEGACY, listSdkComponentsCmdV17.getTool());
        assertEquals(Tool.ANDROID_LEGACY, listSdkComponentsCmdV04.getTool());

        assertEquals("--list --verbose", listSdkComponentsCmdV25_3.getArgs());
        assertEquals("list sdk --extended", listSdkComponentsCmdV25.getArgs());
        assertEquals("list sdk --extended", listSdkComponentsCmdV17.getArgs());
        assertEquals("list sdk --extended", listSdkComponentsCmdV04.getArgs());
    }

    @Test
    public void testListExistingTargetsCommand() {
        final SdkCliCommand listExistingTargetsCmdV25_3 = SdkCliCommandFactory.getCommandsForSdk("25.3").getListExistingTargetsCommand();
        final SdkCliCommand listExistingTargetsCmdV25 = SdkCliCommandFactory.getCommandsForSdk("25").getListExistingTargetsCommand();
        final SdkCliCommand listExistingTargetsCmdV17 = SdkCliCommandFactory.getCommandsForSdk("17").getListExistingTargetsCommand();
        final SdkCliCommand listExistingTargetsCmdV04 = SdkCliCommandFactory.getCommandsForSdk("4").getListExistingTargetsCommand();

        assertEquals(Tool.AVDMANAGER, listExistingTargetsCmdV25_3.getTool());
        assertEquals(Tool.ANDROID_LEGACY, listExistingTargetsCmdV25.getTool());
        assertEquals(Tool.ANDROID_LEGACY, listExistingTargetsCmdV17.getTool());
        assertEquals(Tool.ANDROID_LEGACY, listExistingTargetsCmdV04.getTool());

        assertEquals("list target", listExistingTargetsCmdV25_3.getArgs());
        assertEquals("list target", listExistingTargetsCmdV25.getArgs());
        assertEquals("list target", listExistingTargetsCmdV17.getArgs());
        assertEquals("list target", listExistingTargetsCmdV04.getArgs());
    }

    @Test
    public void testListSystemImagesCommand() {
        final SdkCliCommand listSystemImagesCmdV25_3 = SdkCliCommandFactory.getCommandsForSdk("25.3").getListSystemImagesCommand();
        final SdkCliCommand listSystemImagesCmdV25 = SdkCliCommandFactory.getCommandsForSdk("25").getListSystemImagesCommand();
        final SdkCliCommand listSystemImagesCmdV17 = SdkCliCommandFactory.getCommandsForSdk("17").getListSystemImagesCommand();
        final SdkCliCommand listSystemImagesCmdV04 = SdkCliCommandFactory.getCommandsForSdk("4").getListSystemImagesCommand();

        assertEquals(Tool.SDKMANAGER, listSystemImagesCmdV25_3.getTool());
        assertEquals(Tool.ANDROID_LEGACY, listSystemImagesCmdV25.getTool());
        assertEquals(Tool.ANDROID_LEGACY, listSystemImagesCmdV17.getTool());
        assertEquals(Tool.ANDROID_LEGACY, listSystemImagesCmdV04.getTool());

        assertEquals("--list --verbose", listSystemImagesCmdV25_3.getArgs());
        assertEquals("list target", listSystemImagesCmdV25.getArgs());
        assertEquals("list target", listSystemImagesCmdV17.getArgs());
        assertEquals("list target", listSystemImagesCmdV04.getArgs());
    }

    @Test
    public void testIsImageForPlatformAndABIInstalledParser() {
        assertFalse(SdkCliCommandFactory.getCommandsForSdk("25.3")
                .isImageForPlatformAndABIInstalled(SdkCommandsTestData.LIST_SDK_COMPONENTS_SDKMANAGER, "android-26", "x86_64"));
        assertTrue(SdkCliCommandFactory.getCommandsForSdk("25.3")
                .isImageForPlatformAndABIInstalled(SdkCommandsTestData.LIST_SDK_COMPONENTS_SDKMANAGER, "android-26", "google_apis/x86"));
        assertTrue(SdkCliCommandFactory.getCommandsForSdk("25.3")
                .isImageForPlatformAndABIInstalled(SdkCommandsTestData.LIST_SDK_COMPONENTS_SDKMANAGER, "android-24", "x86_64"));
        assertTrue(SdkCliCommandFactory.getCommandsForSdk("26")
                .isImageForPlatformAndABIInstalled(SdkCommandsTestData.LIST_SDK_COMPONENTS_SDKMANAGER, "android-24", "google_apis/x86_64"));
        assertFalse(SdkCliCommandFactory.getCommandsForSdk("26")
                .isImageForPlatformAndABIInstalled(SdkCommandsTestData.LIST_SDK_COMPONENTS_SDKMANAGER, "android-24", "armeabi-v7a"));
        assertFalse(SdkCliCommandFactory.getCommandsForSdk("25.3")
                .isImageForPlatformAndABIInstalled(SdkCommandsTestData.LIST_SDK_COMPONENTS_SDKMANAGER, "android-23", "x86_64"));
        assertFalse(SdkCliCommandFactory.getCommandsForSdk("25.3")
                .isImageForPlatformAndABIInstalled(SdkCommandsTestData.LIST_SDK_COMPONENTS_SDKMANAGER, "android-23", "armeabi-v7a"));
        assertFalse(SdkCliCommandFactory.getCommandsForSdk("23")
                .isImageForPlatformAndABIInstalled(SdkCommandsTestData.LIST_TARGETS_LEGACY_OUTPUT, "android-23", "x86_64"));
        assertTrue(SdkCliCommandFactory.getCommandsForSdk("25")
                .isImageForPlatformAndABIInstalled(SdkCommandsTestData.LIST_TARGETS_LEGACY_OUTPUT, "android-24", "default/x86_64"));
        assertTrue(SdkCliCommandFactory.getCommandsForSdk("23")
                .isImageForPlatformAndABIInstalled(SdkCommandsTestData.LIST_TARGETS_LEGACY_OUTPUT, "android-24", "x86_64"));
        assertFalse(SdkCliCommandFactory.getCommandsForSdk("23")
                .isImageForPlatformAndABIInstalled("", "android-24", "x86_64"));
    }

    @Test
    public void testCreateAvdCommand() {
        // no snapshot, no sdcard
        final SdkCliCommand createAvdBasicCmdV25_3 = SdkCliCommandFactory.getCommandsForSdk("25.3")
                .getCreatedAvdCommand("test25", false, null, "dummy", "9",
                        null, "system-images;android-24;default;x86_64");
        final SdkCliCommand createAvdBasicCmdV25 = SdkCliCommandFactory.getCommandsForSdk("25")
                .getCreatedAvdCommand("test25", false, null, "dummy", "9",
                        "android-23", "system-images;android-24;default;x86_64");
        final SdkCliCommand createAvdBasicCmdV17 = SdkCliCommandFactory.getCommandsForSdk("17")
                .getCreatedAvdCommand("test17", false, null, "768x1024", null,
                        "android-23", null);
        final SdkCliCommand createAvdBasicCmdV04 = SdkCliCommandFactory.getCommandsForSdk("4")
                .getCreatedAvdCommand("test04", false, null, "1080x1920", "7",
                        "android-23", null);

        assertEquals(Tool.AVDMANAGER, createAvdBasicCmdV25_3.getTool());
        assertEquals(Tool.ANDROID_LEGACY, createAvdBasicCmdV25.getTool());
        assertEquals(Tool.ANDROID_LEGACY, createAvdBasicCmdV17.getTool());
        assertEquals(Tool.ANDROID_LEGACY, createAvdBasicCmdV04.getTool());

        assertEquals("create avd -f -d 9 -n test25 -k system-images;android-24;default;x86_64", createAvdBasicCmdV25_3.getArgs());
        assertEquals("create avd -f -s dummy -n test25 -t android-23", createAvdBasicCmdV25.getArgs());
        assertEquals("create avd -f -s 768x1024 -n test17 -t android-23", createAvdBasicCmdV17.getArgs());
        assertEquals("create avd -f -s 1080x1920 -n test04 -t android-23", createAvdBasicCmdV04.getArgs());

        final SdkCliCommand createAvdWithSnapshotCmdV25_3 = SdkCliCommandFactory.getCommandsForSdk("25.3")
                .getCreatedAvdCommand("test25", true, null, null, "4",
                        null, "system-images;android-24;default;x86_64");
        final SdkCliCommand createAvdWithSnapshotCmdV25 = SdkCliCommandFactory.getCommandsForSdk("25")
                .getCreatedAvdCommand("test25", true, null, null, "4",
                        "android-23", "system-images;android-24;default;x86_64");
        final SdkCliCommand createAvdWithSnapshotCmdV17 = SdkCliCommandFactory.getCommandsForSdk("17")
                .getCreatedAvdCommand("test17", true, null, "1x1", null,
                        "android-23", null);
        final SdkCliCommand createAvdWithSnapshotCmdV04 = SdkCliCommandFactory.getCommandsForSdk("4")
                .getCreatedAvdCommand("test04", true, null, "test", null,
                        "android-23", null);

        assertEquals(Tool.AVDMANAGER, createAvdWithSnapshotCmdV25_3.getTool());
        assertEquals(Tool.ANDROID_LEGACY, createAvdWithSnapshotCmdV25.getTool());
        assertEquals(Tool.ANDROID_LEGACY, createAvdWithSnapshotCmdV17.getTool());
        assertEquals(Tool.ANDROID_LEGACY, createAvdWithSnapshotCmdV04.getTool());

        assertEquals("create avd -f -a -d 4 -n test25 -k system-images;android-24;default;x86_64", createAvdWithSnapshotCmdV25_3.getArgs());
        assertEquals("create avd -f -a -s null -n test25 -t android-23", createAvdWithSnapshotCmdV25.getArgs());
        assertEquals("create avd -f -a -s 1x1 -n test17 -t android-23", createAvdWithSnapshotCmdV17.getArgs());
        assertEquals("create avd -f -a -s test -n test04 -t android-23", createAvdWithSnapshotCmdV04.getArgs());

        final SdkCliCommand createAvdWithSdCardCmdV25_3 = SdkCliCommandFactory.getCommandsForSdk("25.3")
                .getCreatedAvdCommand("test25", false, "100M", null, "4",
                        null, "system-images;android-24;default;x86_64");
        final SdkCliCommand createAvdWithSdCardCmdV25 = SdkCliCommandFactory.getCommandsForSdk("25")
                .getCreatedAvdCommand("test25", false, "100M", null, "4",
                        "android-23", "system-images;android-24;default;x86_64");
        final SdkCliCommand createAvdWithSdCardCmdV17 = SdkCliCommandFactory.getCommandsForSdk("17")
                .getCreatedAvdCommand("test17", true, "1G", "1x1", null,
                        "android-23", null);
        final SdkCliCommand createAvdWithSdCardCmdV04 = SdkCliCommandFactory.getCommandsForSdk("4")
                .getCreatedAvdCommand("test04", false, "200M", "test", null,
                        "android-23", null);

        assertEquals(Tool.AVDMANAGER, createAvdWithSdCardCmdV25_3.getTool());
        assertEquals(Tool.ANDROID_LEGACY, createAvdWithSdCardCmdV25.getTool());
        assertEquals(Tool.ANDROID_LEGACY, createAvdWithSdCardCmdV17.getTool());
        assertEquals(Tool.ANDROID_LEGACY, createAvdWithSdCardCmdV04.getTool());

        assertEquals("create avd -f -c 100M -d 4 -n test25 -k system-images;android-24;default;x86_64", createAvdWithSdCardCmdV25_3.getArgs());
        assertEquals("create avd -f -c 100M -s null -n test25 -t android-23", createAvdWithSdCardCmdV25.getArgs());
        assertEquals("create avd -f -a -c 1G -s 1x1 -n test17 -t android-23", createAvdWithSdCardCmdV17.getArgs());
        assertEquals("create avd -f -c 200M -s test -n test04 -t android-23", createAvdWithSdCardCmdV04.getArgs());
    }

    @Test
    public void testAdbInstallPackageCommand() {
        final SdkCliCommand adbInstallPkgCmdV25_3 = SdkCliCommandFactory.getCommandsForSdk("25.3").getAdbInstallPackageCommand("dummyId", "/home/android/test.apk");
        final SdkCliCommand adbInstallPkgCmdV25 = SdkCliCommandFactory.getCommandsForSdk("25").getAdbInstallPackageCommand("dummyId", "/home/android/test.apk");
        final SdkCliCommand adbInstallPkgCmdV17 = SdkCliCommandFactory.getCommandsForSdk("17").getAdbInstallPackageCommand("android-23920", "jenkins.apk");
        final SdkCliCommand adbInstallPkgCmdV04 = SdkCliCommandFactory.getCommandsForSdk("4").getAdbInstallPackageCommand(null, "dummy.id");

        assertEquals(Tool.ADB, adbInstallPkgCmdV25_3.getTool());
        assertEquals(Tool.ADB, adbInstallPkgCmdV25.getTool());
        assertEquals(Tool.ADB, adbInstallPkgCmdV17.getTool());
        assertEquals(Tool.ADB, adbInstallPkgCmdV04.getTool());

        assertEquals("-s dummyId install -r \"/home/android/test.apk\"", adbInstallPkgCmdV25_3.getArgs());
        assertEquals("-s dummyId install -r \"/home/android/test.apk\"", adbInstallPkgCmdV25.getArgs());
        assertEquals("-s android-23920 install -r \"jenkins.apk\"", adbInstallPkgCmdV17.getArgs());
        assertEquals("install -r \"dummy.id\"", adbInstallPkgCmdV04.getArgs());
    }

    @Test
    public void testAdbUninstallPackageCommand() {
        final SdkCliCommand adbUninstallPkgCmdV25_3 = SdkCliCommandFactory.getCommandsForSdk("25.3").getAdbUninstallPackageCommand("dummyId", "org.test.package");
        final SdkCliCommand adbUninstallPkgCmdV25 = SdkCliCommandFactory.getCommandsForSdk("25").getAdbUninstallPackageCommand("dummyId", "org.test.package");
        final SdkCliCommand adbUninstallPkgCmdV17 = SdkCliCommandFactory.getCommandsForSdk("17").getAdbUninstallPackageCommand("android-23920", "test.jenkins");
        final SdkCliCommand adbUninstallPkgCmdV04 = SdkCliCommandFactory.getCommandsForSdk("4").getAdbUninstallPackageCommand("xid", "dummy.id");

        assertEquals(Tool.ADB, adbUninstallPkgCmdV25_3.getTool());
        assertEquals(Tool.ADB, adbUninstallPkgCmdV25.getTool());
        assertEquals(Tool.ADB, adbUninstallPkgCmdV17.getTool());
        assertEquals(Tool.ADB, adbUninstallPkgCmdV04.getTool());

        assertEquals("-s dummyId uninstall org.test.package", adbUninstallPkgCmdV25_3.getArgs());
        assertEquals("-s dummyId uninstall org.test.package", adbUninstallPkgCmdV25.getArgs());
        assertEquals("-s android-23920 uninstall test.jenkins", adbUninstallPkgCmdV17.getArgs());
        assertEquals("-s xid uninstall dummy.id", adbUninstallPkgCmdV04.getArgs());
    }

    @Test
    public void testWithoudDeviceIdentifier() {
        final SdkCliCommand adbInstallPkgCmdV25_3 = SdkCliCommandFactory.getCommandsForSdk("25.3").getAdbInstallPackageCommand(null, "/home/android/test.apk");
        final SdkCliCommand adbInstallPkgCmdV25 = SdkCliCommandFactory.getCommandsForSdk("25").getAdbInstallPackageCommand(null, "/home/android/test.apk");
        final SdkCliCommand adbInstallPkgCmdV17 = SdkCliCommandFactory.getCommandsForSdk("17").getAdbInstallPackageCommand("", "jenkins.apk");
        final SdkCliCommand adbInstallPkgCmdV04 = SdkCliCommandFactory.getCommandsForSdk("4").getAdbInstallPackageCommand(null, "dummy.id");

        assertEquals(Tool.ADB, adbInstallPkgCmdV25_3.getTool());
        assertEquals(Tool.ADB, adbInstallPkgCmdV25.getTool());
        assertEquals(Tool.ADB, adbInstallPkgCmdV17.getTool());
        assertEquals(Tool.ADB, adbInstallPkgCmdV04.getTool());

        assertEquals("install -r \"/home/android/test.apk\"", adbInstallPkgCmdV25_3.getArgs());
        assertEquals("install -r \"/home/android/test.apk\"", adbInstallPkgCmdV25.getArgs());
        assertEquals("install -r \"jenkins.apk\"", adbInstallPkgCmdV17.getArgs());
        assertEquals("install -r \"dummy.id\"", adbInstallPkgCmdV04.getArgs());

        final SdkCliCommand adbUninstallPkgCmdV25_3 = SdkCliCommandFactory.getCommandsForSdk("25.3").getAdbUninstallPackageCommand("", "org.test.package");
        final SdkCliCommand adbUninstallPkgCmdV25 = SdkCliCommandFactory.getCommandsForSdk("25").getAdbUninstallPackageCommand("", "org.test.package");
        final SdkCliCommand adbUninstallPkgCmdV17 = SdkCliCommandFactory.getCommandsForSdk("17").getAdbUninstallPackageCommand(null, "test.jenkins");
        final SdkCliCommand adbUninstallPkgCmdV04 = SdkCliCommandFactory.getCommandsForSdk("4").getAdbUninstallPackageCommand(null, "dummy.id");

        assertEquals(Tool.ADB, adbUninstallPkgCmdV25_3.getTool());
        assertEquals(Tool.ADB, adbUninstallPkgCmdV25.getTool());
        assertEquals(Tool.ADB, adbUninstallPkgCmdV17.getTool());
        assertEquals(Tool.ADB, adbUninstallPkgCmdV04.getTool());

        assertEquals("uninstall org.test.package", adbUninstallPkgCmdV25_3.getArgs());
        assertEquals("uninstall org.test.package", adbUninstallPkgCmdV25.getArgs());
        assertEquals("uninstall test.jenkins", adbUninstallPkgCmdV17.getArgs());
        assertEquals("uninstall dummy.id", adbUninstallPkgCmdV04.getArgs());
    }

    @Test
    public void testCreateSdkCardCommand() {
        final SdkCliCommand createSdCardCmdV25_3 = SdkCliCommandFactory.getCommandsForSdk("25.3").getCreateSdkCardCommand("/dev/null", "32G");
        final SdkCliCommand createSdCardCmdV25 = SdkCliCommandFactory.getCommandsForSdk("25").getCreateSdkCardCommand("/dev/null", "32G");
        final SdkCliCommand createSdCardCmdV17 = SdkCliCommandFactory.getCommandsForSdk("17").getCreateSdkCardCommand(".android/avd/test.img", "16M");
        final SdkCliCommand createSdCardCmdV04 = SdkCliCommandFactory.getCommandsForSdk("4").getCreateSdkCardCommand(null, "32768");

        assertEquals(Tool.MKSDCARD, createSdCardCmdV25_3.getTool());
        assertEquals(Tool.MKSDCARD, createSdCardCmdV25.getTool());
        assertEquals(Tool.MKSDCARD, createSdCardCmdV17.getTool());
        assertEquals(Tool.MKSDCARD, createSdCardCmdV04.getTool());

        assertEquals("32G /dev/null", createSdCardCmdV25_3.getArgs());
        assertEquals("32G /dev/null", createSdCardCmdV25.getArgs());
        assertEquals("16M .android/avd/test.img", createSdCardCmdV17.getArgs());
        assertEquals("32768 null", createSdCardCmdV04.getArgs());
    }

    @Test
    public void testEmulatorListSnapshotsCommand() {
        final SdkCliCommand emulatorListSnapshotsCmdV25_3 = SdkCliCommandFactory.getCommandsForSdk("25.3").getEmulatorListSnapshotsCommand("avdtest", true);
        final SdkCliCommand emulatorListSnapshotsCmdV25 = SdkCliCommandFactory.getCommandsForSdk("25").getEmulatorListSnapshotsCommand("avdtest", true);
        final SdkCliCommand emulatorListSnapshotsCmdV17 = SdkCliCommandFactory.getCommandsForSdk("17").getEmulatorListSnapshotsCommand("dummy", false);
        final SdkCliCommand emulatorListSnapshotsCmdV04 = SdkCliCommandFactory.getCommandsForSdk("4").getEmulatorListSnapshotsCommand("test", true);

        assertEquals(Tool.EMULATOR_ARM, emulatorListSnapshotsCmdV25_3.getTool());
        assertEquals(Tool.EMULATOR_ARM, emulatorListSnapshotsCmdV25.getTool());
        assertEquals(Tool.EMULATOR, emulatorListSnapshotsCmdV17.getTool());
        assertEquals(Tool.EMULATOR_ARM, emulatorListSnapshotsCmdV04.getTool());

        assertEquals("-snapshot-list -no-window -avd avdtest", emulatorListSnapshotsCmdV25_3.getArgs());
        assertEquals("-snapshot-list -no-window -avd avdtest", emulatorListSnapshotsCmdV25.getArgs());
        assertEquals("-snapshot-list -no-window -avd dummy", emulatorListSnapshotsCmdV17.getArgs());
        assertEquals("-snapshot-list -no-window -avd test", emulatorListSnapshotsCmdV04.getArgs());
    }
    @Test
    public void testAdbStartServerCommand() {
        final SdkCliCommand adbStartServerCmdV25_3 = SdkCliCommandFactory.getCommandsForSdk("25.3").getAdbStartServerCommand();
        final SdkCliCommand adbStartServerCmdV25 = SdkCliCommandFactory.getCommandsForSdk("25").getAdbStartServerCommand();
        final SdkCliCommand adbStartServerCmdV17 = SdkCliCommandFactory.getCommandsForSdk("17").getAdbStartServerCommand();
        final SdkCliCommand adbStartServerCmdV04 = SdkCliCommandFactory.getCommandsForSdk("4").getAdbStartServerCommand();

        assertEquals(Tool.ADB, adbStartServerCmdV25_3.getTool());
        assertEquals(Tool.ADB, adbStartServerCmdV25.getTool());
        assertEquals(Tool.ADB, adbStartServerCmdV17.getTool());
        assertEquals(Tool.ADB, adbStartServerCmdV04.getTool());

        assertEquals("start-server", adbStartServerCmdV25_3.getArgs());
        assertEquals("start-server", adbStartServerCmdV25.getArgs());
        assertEquals("start-server", adbStartServerCmdV17.getArgs());
        assertEquals("start-server", adbStartServerCmdV04.getArgs());
    }

    @Test
    public void testAdbKillServerCommand() {
        final SdkCliCommand adbKillServerCmdV25_3 = SdkCliCommandFactory.getCommandsForSdk("25.3").getAdbKillServerCommand();
        final SdkCliCommand adbKillServerCmdV25 = SdkCliCommandFactory.getCommandsForSdk("25").getAdbKillServerCommand();
        final SdkCliCommand adbKillServerCmdV17 = SdkCliCommandFactory.getCommandsForSdk("17").getAdbKillServerCommand();
        final SdkCliCommand adbKillServerCmdV04 = SdkCliCommandFactory.getCommandsForSdk("4").getAdbKillServerCommand();

        assertEquals(Tool.ADB, adbKillServerCmdV25_3.getTool());
        assertEquals(Tool.ADB, adbKillServerCmdV25.getTool());
        assertEquals(Tool.ADB, adbKillServerCmdV17.getTool());
        assertEquals(Tool.ADB, adbKillServerCmdV04.getTool());

        assertEquals("kill-server", adbKillServerCmdV25_3.getArgs());
        assertEquals("kill-server", adbKillServerCmdV25.getArgs());
        assertEquals("kill-server", adbKillServerCmdV17.getArgs());
        assertEquals("kill-server", adbKillServerCmdV04.getArgs());
    }

    @Test
    public void testUpdateProjectCommand() {
        final SdkCliCommand updateProjectCmdV25_3 = SdkCliCommandFactory.getCommandsForSdk("25.3").getUpdateProjectCommand("proj1");
        final SdkCliCommand updateProjectCmdV25 = SdkCliCommandFactory.getCommandsForSdk("25").getUpdateProjectCommand("proj1");
        final SdkCliCommand updateProjectCmdV17 = SdkCliCommandFactory.getCommandsForSdk("17").getUpdateProjectCommand("proj2");
        final SdkCliCommand updateProjectCmdV04 = SdkCliCommandFactory.getCommandsForSdk("4").getUpdateProjectCommand(".");

        assertEquals(null, updateProjectCmdV25_3.getTool());
        assertEquals(Tool.ANDROID_LEGACY, updateProjectCmdV25.getTool());
        assertEquals(Tool.ANDROID_LEGACY, updateProjectCmdV17.getTool());
        assertEquals(Tool.ANDROID_LEGACY, updateProjectCmdV04.getTool());

        assertEquals("", updateProjectCmdV25_3.getArgs());
        assertEquals("update project -p proj1", updateProjectCmdV25.getArgs());
        assertEquals("update project -p proj2", updateProjectCmdV17.getArgs());
        assertEquals("update project -p .", updateProjectCmdV04.getArgs());

        assertTrue(updateProjectCmdV25_3.isNoopCmd());
        assertFalse(updateProjectCmdV25.isNoopCmd());
        assertFalse(updateProjectCmdV17.isNoopCmd());
        assertFalse(updateProjectCmdV04.isNoopCmd());
    }

    @Test
    public void testUpdateTestProjectCommand() {
        final SdkCliCommand updateTestProjectCmdV25_3 = SdkCliCommandFactory.getCommandsForSdk("25.3").getUpdateTestProjectCommand("testProj1", null);
        final SdkCliCommand updateTestProjectCmdV25 = SdkCliCommandFactory.getCommandsForSdk("25").getUpdateTestProjectCommand("testProj1", "com.test.class");
        final SdkCliCommand updateTestProjectCmdV17 = SdkCliCommandFactory.getCommandsForSdk("17").getUpdateTestProjectCommand("testProj2", "com.test.class");
        final SdkCliCommand updateTestProjectCmdV04 = SdkCliCommandFactory.getCommandsForSdk("4").getUpdateTestProjectCommand(".", "org.comp.proj.test.testclass");

        assertEquals(null, updateTestProjectCmdV25_3.getTool());
        assertEquals(Tool.ANDROID_LEGACY, updateTestProjectCmdV25.getTool());
        assertEquals(Tool.ANDROID_LEGACY, updateTestProjectCmdV17.getTool());
        assertEquals(Tool.ANDROID_LEGACY, updateTestProjectCmdV04.getTool());

        assertEquals("", updateTestProjectCmdV25_3.getArgs());
        assertEquals("update test-project -p testProj1 -m com.test.class", updateTestProjectCmdV25.getArgs());
        assertEquals("update test-project -p testProj2 -m com.test.class", updateTestProjectCmdV17.getArgs());
        assertEquals("update test-project -p . -m org.comp.proj.test.testclass", updateTestProjectCmdV04.getArgs());

        assertTrue(updateTestProjectCmdV25_3.isNoopCmd());
        assertFalse(updateTestProjectCmdV25.isNoopCmd());
        assertFalse(updateTestProjectCmdV17.isNoopCmd());
        assertFalse(updateTestProjectCmdV04.isNoopCmd());
    }

    @Test
    public void testUpdateLibProjectCommand() {
        final SdkCliCommand updateLibraryProjectCmdV25_3 = SdkCliCommandFactory.getCommandsForSdk("25.3").getUpdateLibProjectCommand("libProj1");
        final SdkCliCommand updateLibraryProjectCmdV25 = SdkCliCommandFactory.getCommandsForSdk("25").getUpdateLibProjectCommand("libProj1");
        final SdkCliCommand updateLibraryProjectCmdV17 = SdkCliCommandFactory.getCommandsForSdk("17").getUpdateLibProjectCommand("libProj2");
        final SdkCliCommand updateLibraryProjectCmdV04 = SdkCliCommandFactory.getCommandsForSdk("4").getUpdateLibProjectCommand(".");

        assertEquals(null, updateLibraryProjectCmdV25_3.getTool());
        assertEquals(Tool.ANDROID_LEGACY, updateLibraryProjectCmdV25.getTool());
        assertEquals(Tool.ANDROID_LEGACY, updateLibraryProjectCmdV17.getTool());
        assertEquals(Tool.ANDROID_LEGACY, updateLibraryProjectCmdV04.getTool());

        assertEquals("", updateLibraryProjectCmdV25_3.getArgs());
        assertEquals("update lib-project -p libProj1", updateLibraryProjectCmdV25.getArgs());
        assertEquals("update lib-project -p libProj2", updateLibraryProjectCmdV17.getArgs());
        assertEquals("update lib-project -p .", updateLibraryProjectCmdV04.getArgs());

        assertTrue(updateLibraryProjectCmdV25_3.isNoopCmd());
        assertFalse(updateLibraryProjectCmdV25.isNoopCmd());
        assertFalse(updateLibraryProjectCmdV17.isNoopCmd());
        assertFalse(updateLibraryProjectCmdV04.isNoopCmd());
    }
}
