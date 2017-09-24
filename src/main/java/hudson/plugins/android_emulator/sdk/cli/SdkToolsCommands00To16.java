package hudson.plugins.android_emulator.sdk.cli;

import hudson.plugins.android_emulator.sdk.Tool;

/**
 * Extends {@code SdkToolsCommandsCurrentBase} and simply overwrites the commands
 * which differ for SDK with major version prior 17.
 */
public class SdkToolsCommands00To16 extends SdkToolsCommands17To25_2 implements SdkToolsCommands {

    @Override
    public SdkCliCommand getSdkInstallAndUpdateCommand(final String proxySettings, final String list) {
        final String upgradeArgs = String.format("update sdk -u -o %s -t %s", proxySettings, list);
        return new SdkCliCommand(Tool.ANDROID_LEGACY, upgradeArgs);
    }
}
