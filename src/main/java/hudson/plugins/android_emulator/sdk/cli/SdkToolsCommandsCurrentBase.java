package hudson.plugins.android_emulator.sdk.cli;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import hudson.plugins.android_emulator.sdk.Tool;

/**
 * This class holds the implementations for all used commands in the latest
 * SDK version. As some calls have never changed in history, it is most likely
 * that this class is used as base for the other version implementations.
 */
public class SdkToolsCommandsCurrentBase implements SdkToolsCommands {

    @Override
    public SdkCliCommand getSdkInstallAndUpdateCommand(final String proxySettings, final String list) {
        final String components[] = StringUtils.split(list, ",");
        for (int idx = 0; idx < components.length; idx++) {
            components[idx] = components[idx].replaceAll("^tool$", "tools");
            components[idx] = components[idx].replaceAll("^platform-tool$", "platform-tools");
            components[idx] = components[idx].replaceAll("^addon-$", "addons-");
            components[idx] = components[idx].replaceAll("^extra-google-", "extras;google;");
            components[idx] = components[idx].replaceAll("^extra-android-", "extras;android;");
            components[idx] = components[idx].replaceAll("^addon-", "add-ons;addon-");
            components[idx] = components[idx].replaceAll("^build-tools-", "build-tools;");
            components[idx] = components[idx].replaceAll("^android-", "platforms;android-");
            components[idx] = components[idx].replaceAll("^sys-img-(.*)-(android-[0-9]*)", "system-images;$2;default;$1");
        }

        final String upgradeArgs = String.format("--include_obsolete %s", StringUtils.join(components, " "));
        return new SdkCliCommand(Tool.SDKMANAGER, upgradeArgs);
    }

    @Override
    public SdkCliCommand getListSdkComponentsCommand() {
        return new SdkCliCommand(Tool.SDKMANAGER, "--list --verbose");
    }
    @Override
    public SdkCliCommand getListExistingTargetsCommand() {
        // Preferably we'd use the "--compact" flag here, but it wasn't added until r12,
        // nor does it give any information about which system images are installed...
        return new SdkCliCommand(Tool.AVDMANAGER, "list target");
    }

    @Override
    public SdkCliCommand getListSystemImagesCommand() {
        return getListSdkComponentsCommand();
    }

    @Override
    public boolean isImageForPlatformAndABIInstalled(final String listSystemImagesOutput,
            final String platform, final String abi) {
        // split ABI into abi or tag/abi
        final String abiSplit[] = StringUtils.split(abi, '/');
        final boolean containsTag = (abiSplit.length > 1);
        final String abiString = (containsTag) ? abiSplit[1] : abiSplit[0];
        final String tagString = (containsTag) ? abiSplit[0] : "default";

        final String installedPkg = listSystemImagesOutput.replaceAll("(?is)Available Packages:.*", "");
        final String systemImageName = "system-images;" + platform + ";" + tagString + ";" + abiString;
        Pattern pattern = Pattern.compile("^" + systemImageName, Pattern.MULTILINE);
        return pattern.matcher(installedPkg).find();
    }

    @Override
    public SdkCliCommand getCreatedAvdCommand(final String avdName, final boolean createSnapshot,
            final String sdCardSize, final String screenResolutionSkinName, final String deviceDefinition) {

        // Build up basic arguments to `android` command
        final StringBuilder args = new StringBuilder(100);
        args.append("create avd ");

        // Overwrite any existing files
        args.append("-f");

        // Initialise snapshot support, regardless of whether we will actually use it
        if (createSnapshot) {
            args.append(" -a");
        }

        if (sdCardSize != null) {
            args.append(" -c ");
            args.append(sdCardSize);
        }

        // A device definition needs to be set, if not there would be an prompt of the avdmanager command
        args.append(" -d ");
        args.append(deviceDefinition);

        args.append(" -n ");
        args.append(avdName);

        return new SdkCliCommand(Tool.AVDMANAGER, args.toString());
    }

    @Override
    public SdkCliCommand getAdbInstallPackageCommand(final String deviceIdentifier, final String packageFileName) {
        final String adbInstallArgs = String.format("%sinstall -r \"%s\"", getAdbDeviceSerialArg(deviceIdentifier), packageFileName);
        return new SdkCliCommand(Tool.ADB, adbInstallArgs);
    }

    @Override
    public SdkCliCommand getAdbUninstallPackageCommand(final String deviceIdentifier, final String packageId) {
        final String adbArgs = String.format("%suninstall %s", getAdbDeviceSerialArg(deviceIdentifier), packageId);
        return new SdkCliCommand(Tool.ADB, adbArgs);
    }

    /**
     * Retrieve the device serial argument for adb, dependent if deviceSerial is set or not
     *
     * @param deviceSerial device to run adb command on
     * @return '-s deviceSerial' or empty string
     */
    private String getAdbDeviceSerialArg(final String deviceSerial) {
        return (deviceSerial != null && !deviceSerial.isEmpty()) ? "-s " + deviceSerial + " " : "";
    }

    @Override
    public SdkCliCommand getCreateSdkCardCommand(final String absolutePathToSdCard, final String requestedSdCardSize) {
        // Build command: mksdcard 32M /home/foo/.android/avd/whatever.avd/sdcard.img
        final String mksdcardArgs = String.format("%s %s", requestedSdCardSize, absolutePathToSdCard);
        return new SdkCliCommand(Tool.MKSDCARD, mksdcardArgs);
    }

    @Override
    public SdkCliCommand getEmulatorListSnapshotsCommand(final String avdName, final boolean useArmEmulator) {
        final String emulatorListSnapshotArgs = String.format("-snapshot-list -no-window -avd %s", avdName);
        final Tool executable = useArmEmulator ? Tool.EMULATOR_ARM : Tool.EMULATOR;
        return new SdkCliCommand(executable, emulatorListSnapshotArgs);
    }

    @Override
    public SdkCliCommand getAdbStartServerCommand() {
        return new SdkCliCommand(Tool.ADB, "start-server");
    }

    @Override
    public SdkCliCommand getAdbKillServerCommand() {
        return new SdkCliCommand(Tool.ADB, "kill-server");
    }

    @Override
    public SdkCliCommand getUpdateProjectCommand(final String projectPath) {
        return SdkCliCommand.createNoopCommand();
    }

    @Override
    public SdkCliCommand getUpdateTestProjectCommand(final String projectPath, final String testMainClass) {
        return SdkCliCommand.createNoopCommand();
    }

    @Override
    public SdkCliCommand getUpdateLibProjectCommand(final String projectPath) {
        return SdkCliCommand.createNoopCommand();
    }
}
