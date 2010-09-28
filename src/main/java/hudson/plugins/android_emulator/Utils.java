package hudson.plugins.android_emulator;

import hudson.Launcher;
import hudson.Util;
import hudson.util.ArgumentListBuilder;

public class Utils {


    /**
     * Retrieves the path to the Android SDK tools directory, based on the given SDK root path.
     *
     * @param androidHome  The path to the Android SDK root, may be empty or <code>null</code>.
     * @return  The path to the general Android SDK tools directory.
     */
    public static String getAndroidToolsDirectory(final String androidHome) {
        final String androidToolsDir;

        // If no home was provided, we'll assume that everything is on the PATH
        if (androidHome == null) {
            androidToolsDir = "";
        } else {
            androidToolsDir = androidHome +"/tools/";
        }

        return androidToolsDir;
    }

    /**
     * Generates a ready-to-use ArgumentListBuilder for one of the Android SDK tools.
     *
     * @param launcher The launcher for the remote node.
     * @param androidHome The Android SDK root.
     * @param tool The Android tool to run.
     * @param args Any extra arguments for the command.
     * @return Arguments including the full path to the SDK and any extra Windows stuff required.
     */
    public static ArgumentListBuilder getToolCommand(Launcher launcher, String androidHome,
            Tool tool, String args) {
        final String executable = tool.getExecutable(launcher.isUnix());
        return getToolCommand(androidHome, executable, args);
    }

    /**
     * Generates a ready-to-use ArgumentListBuilder for one of the Android SDK tools.
     *
     * @param androidHome The Android SDK root.
     * @param executable The executable to run.
     * @param args Any extra arguments for the command.
     * @return Arguments including the full path to the SDK and any extra Windows stuff required.
     */
    public static ArgumentListBuilder getToolCommand(String androidHome, String executable, String args) {
        // Figure out where the tools are that we need
        final String androidToolsDir = getAndroidToolsDirectory(androidHome);

        // Build tool command
        ArgumentListBuilder builder = new ArgumentListBuilder(androidToolsDir + executable);
        if (args != null) {
            builder.add(Util.tokenize(args));
        }

        return builder;
    }

}
