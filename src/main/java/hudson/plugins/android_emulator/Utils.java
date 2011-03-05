package hudson.plugins.android_emulator;

import hudson.EnvVars;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.util.ArgumentListBuilder;

import java.io.IOException;
import java.util.Map;

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

    /**
     * Expands the variable in the given string to its value in the environment variables available
     * to this build.  The Jenkins-specific build variables for this build are then substituted.
     *
     * @param envVars  Map of the environment variables.
     * @param buildVars  Map of the build-specific variables.
     * @param token  The token which may or may not contain variables in the format <tt>${foo}</tt>.
     * @return  The given token, with applicable variable expansions done.
     */
    public static String expandVariables(EnvVars envVars, Map<String,String> buildVars,
            String token) {

        String result = Util.fixEmptyAndTrim(token);
        if (result != null) {
            result = Util.replaceMacro(Util.replaceMacro(result, envVars), buildVars);
        }
        return result;
    }

}
