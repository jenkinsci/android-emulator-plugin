package hudson.plugins.android_emulator.sdk.cli;

import hudson.plugins.android_emulator.sdk.Tool;

/**
 * Represents a SDK command-line-interface command by a given
 * {@code Tool} and by it's arguments.
 */
public class SdkCliCommand {
    private Tool tool;
    private String args;

    public SdkCliCommand(final Tool tool, final String args) {
        this.tool = tool;
        this.args = args;
    }

    public Tool getTool() {
        return tool;
    }

    public String getArgs() {
        return args;
    }

    public boolean isNoopCmd() {
        return (tool == null);
    }

    public static SdkCliCommand createNoopCommand() {
        return new SdkCliCommand(null, "");
    }
}
