package hudson.plugins.android_emulator.sdk.cli;

import java.util.List;

/**
 * Extends {@code SdkToolsCommandsCurrentBase} and simply overwrites the commands
 * which differ for SDK with major version prior 17.
 */
public class SdkToolsCommands00To16 extends SdkToolsCommands17To25_2 implements SdkToolsCommands {

    @Override
    public SdkCliCommand getSdkInstallAndUpdateCommand(final String proxySettings, final List<String> components) {
        final SdkCliCommand cmd = super.getSdkInstallAndUpdateCommand(proxySettings, components);
        return new SdkCliCommand(cmd.getTool(), cmd.getArgs().replaceFirst("^update sdk -u -a", "update sdk -u -o"));
    }
}
