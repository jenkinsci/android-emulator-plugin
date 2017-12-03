package hudson.plugins.android_emulator.sdk.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import hudson.plugins.android_emulator.sdk.Tool;

/**
 * Extends {@code SdkToolsCommandsCurrentBase} and simply overwrites the commands
 * which differ for SDK Tools version 17 to 25.2.
 */
public class SdkToolsCommands17To25_2 extends SdkToolsCommandsCurrentBase implements SdkToolsCommands {

    @Override
    public SdkCliCommand getSdkInstallAndUpdateCommand(final String proxySettings, final List<String> components) {
        final List<String> complist = new ArrayList<String>(components);
        complist.remove("emulator");

        final String upgradeArgs = String.format("update sdk -u -a %s -t %s", proxySettings, StringUtils.join(complist, ','));
        return new SdkCliCommand(Tool.ANDROID_LEGACY, upgradeArgs);
    }

    @Override
    public SdkCliCommand getListSdkComponentsCommand() {
        return new SdkCliCommand(Tool.ANDROID_LEGACY, "list sdk --extended");
    }

    @Override
    public SdkCliCommand getListExistingTargetsCommand() {
        return new SdkCliCommand(Tool.ANDROID_LEGACY, "list target");
    }

    @Override
    public SdkCliCommand getListSystemImagesCommand() {
        return getListExistingTargetsCommand();
    }

    @Override
    public boolean isImageForPlatformAndABIInstalled(final String listSystemImagesOutput,
            final String platform, final String abi) {
        // Check whether the desired ABI is included in the output
        Pattern regex = Pattern.compile(String.format("\"%s\".+?%s", platform, abi), Pattern.DOTALL);
        Matcher matcher = regex.matcher(listSystemImagesOutput);
        if (!matcher.find() || matcher.group(0).contains("---")) {
            // We did not find the desired ABI within the section for the given platform
            return false;
        } else {
            return true;
        }
    }

    @Override
    public SdkCliCommand getCreatedAvdCommand(final String avdName, final boolean supportsSnapshots,
            final String sdCardSize, final String screenResolutionSkinName, final String deviceDefinition,
            final String androidTarget, final String systemImagePackagePath, final String tag) {

        // Build up basic arguments to `android` command
        final StringBuilder args = new StringBuilder(100);
        args.append("create avd ");

        // Overwrite any existing files
        args.append("-f");

        // Initialise snapshot support, regardless of whether we will actually use it
        if (supportsSnapshots) {
            args.append(" -a");
        }

        if (sdCardSize != null) {
            args.append(" -c ");
            args.append(sdCardSize);
        }

        // screen resolution not supported at creation time in Android Emulator 2.0
        // will be added as skin on emulator start
        args.append(" -s ");
        args.append(screenResolutionSkinName);

        args.append(" -n ");
        args.append(avdName);

        args.append(" -t ");
        args.append(androidTarget);

        if (tag != null && !tag.isEmpty()) {
            args.append(" --tag ");
            args.append(tag);
        }

        return new SdkCliCommand(Tool.ANDROID_LEGACY, args.toString());
    }

    @Override
    public SdkCliCommand getUpdateProjectCommand(final String projectPath) {
        return getGenericUpdateProjectCommand("project", projectPath, null);
    }

    @Override
    public SdkCliCommand getUpdateTestProjectCommand(final String projectPath, final String testMainClass) {
        return getGenericUpdateProjectCommand("test-project", projectPath, testMainClass);
    }

    @Override
    public SdkCliCommand getUpdateLibProjectCommand(final String projectPath) {
        return getGenericUpdateProjectCommand("lib-project", projectPath, null);
    }

    private SdkCliCommand getGenericUpdateProjectCommand(final String projectType,
            final String projectPath, final String testMainClass) {
        String mainClassArg = "";
        if (testMainClass != null) {
            mainClassArg = String.format(" -m %s", testMainClass);
        }
        final String updateProjectArgs =
                String.format("update %s -p %s%s", projectType, projectPath, mainClassArg);
        return new SdkCliCommand(Tool.ANDROID_LEGACY, updateProjectArgs);
    }
}
