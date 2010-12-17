package hudson.plugins.android_emulator;

import hudson.Util;
import hudson.util.ArgumentListBuilder;

public class Utils {

    /**
     * Generates a ready-to-use ArgumentListBuilder for one of the Android SDK tools.
     *
     * @param androidSdk The Android SDK to use.
     * @param isUnix Whether the system where this command should run is sane.
     * @param tool The Android tool to run.
     * @param args Any extra arguments for the command.
     * @return Arguments including the full path to the SDK and any extra Windows stuff required.
     */
    public static ArgumentListBuilder getToolCommand(AndroidSdk androidSdk, boolean isUnix, Tool tool, String args) {
        // Determine the path to the desired tool
        String androidToolsDir;
        if (androidSdk.hasKnownRoot()) {
            if (tool.isPlatformTool && androidSdk.usesPlatformTools()) {
                androidToolsDir = androidSdk.getSdkRoot() +"/platform-tools/";
            } else {
                androidToolsDir = androidSdk.getSdkRoot() +"/tools/";
            }
        } else {
            // If SDK root is unknown, we'll assume that the tool is on the PATH
            androidToolsDir = "";
        }

        // Build tool command
        final String executable = tool.getExecutable(isUnix);
        ArgumentListBuilder builder = new ArgumentListBuilder(androidToolsDir + executable);
        if (args != null) {
            builder.add(Util.tokenize(args));
        }

        return builder;
    }

}
