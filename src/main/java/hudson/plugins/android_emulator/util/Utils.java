package hudson.plugins.android_emulator.util;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.Util;
import hudson.Launcher.ProcStarter;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.plugins.android_emulator.Constants;
import hudson.plugins.android_emulator.Messages;
import hudson.plugins.android_emulator.AndroidEmulator.DescriptorImpl;
import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.sdk.Tool;
import hudson.remoting.Callable;
import hudson.util.ArgumentListBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public class Utils {

    /**
     * Retrieves the configured Android SDK root directory.
     *
     * @return The configured Android SDK root, if any. May include un-expanded variables.
     */
    public static String getConfiguredAndroidHome() {
        return Hudson.getInstance().getDescriptorByType(DescriptorImpl.class).androidHome;
    }

    /**
     * Gets a combined set of environment variables for the current computer and build.
     *
     * @param build The build for which we should retrieve environment variables.
     * @param listener The listener used to get the environment variables.
     * @return Environment variables for the current computer, with the build variables taking precedence.
     */
    public static EnvVars getEnvironment(AbstractBuild<?, ?> build, BuildListener listener) {
        final EnvVars envVars = new EnvVars();
        try {
            // Get environment of the local computer
            EnvVars localVars = Computer.currentComputer().getEnvironment();
            envVars.putAll(localVars);

            // Add variables specific to this build
            envVars.putAll(build.getEnvironment(listener));
        } catch (InterruptedException e) {
            // Ignore
        } catch (IOException e) {
            // Ignore
        }

        return envVars;
    }

    /**
     * Tries to validate the given Android SDK root directory; otherwise tries to
     * locate a copy of the SDK by checking for common environment variables.
     *
     * @param launcher The launcher for the remote node.
     * @param envVars Environment variables for the build.
     * @param androidHome The (variable-expanded) SDK root given in global config.
     * @return Either a discovered SDK path or, if all else fails, the given androidHome value.
     */
    public static String discoverAndroidHome(Launcher launcher, Node node,
            final EnvVars envVars, final String androidHome) {
        final String autoInstallDir = getSdkInstallDirectory(node).getRemote();

        Callable<String, InterruptedException> task = new Callable<String, InterruptedException>() {
            public String call() throws InterruptedException {
                // Verify existence of provided value
                if (validateHomeDir(androidHome)) {
                    return androidHome;
                }

                // Check for common environment variables
                String[] keys = { "ANDROID_SDK_ROOT", "ANDROID_SDK_HOME",
                                  "ANDROID_HOME", "ANDROID_SDK" };

                // Resolve each variable to its directory name
                List<String> potentialSdkDirs = new ArrayList<String>();
                for (String key : keys) {
                    potentialSdkDirs.add(envVars.get(key));
                }

                // Also add the auto-installed SDK directory to the list of candidates
                potentialSdkDirs.add(autoInstallDir);

                // Check each directory to see if it's a valid Android SDK
                for (String home : potentialSdkDirs) {
                    if (validateHomeDir(home)) {
                        return home;
                    }
                }

                // Give up
                return null;
            }

            private boolean validateHomeDir(String dir) {
                if (Util.fixEmptyAndTrim(dir) == null) {
                    return false;
                }
                return !Utils.validateAndroidHome(new File(dir), false).isFatal();
            }

            private static final long serialVersionUID = 1L;
        };

        String result = androidHome;
        try {
            result = launcher.getChannel().call(task);
        } catch (InterruptedException e) {
            // Ignore; will return default value
        } catch (IOException e) {
            // Ignore; will return default value
        }
        return result;
    }

    /**
     * Determines the properties of the SDK installed on the build machine.
     *
     * @param launcher The launcher for the remote node.
     * @param androidHome The SDK root directory specified in the job/system configuration.
     * @return AndroidSdk object representing the properties of the installed SDK.
     */
    public static AndroidSdk getAndroidSdk(Launcher launcher, final String androidHome) {
        final boolean isUnix = launcher.isUnix();

        Callable<AndroidSdk, IOException> task = new Callable<AndroidSdk, IOException>() {
            public AndroidSdk call() throws IOException {
                String sdkRoot = androidHome;
                if (androidHome == null) {
                    // If no SDK root was specified, attempt to detect it from PATH
                    sdkRoot = getSdkRootFromPath(isUnix);

                    // If still nothing was found, then we cannot continue
                    if (sdkRoot == null) {
                        return null;
                    }
                } else {
                    // Validate given SDK root
                    ValidationResult result = Utils.validateAndroidHome(new File(sdkRoot), false);
                    if (result.isFatal()) {
                        return null;
                    }
                }

                // Create SDK instance with what we know so far
                AndroidSdk sdk = new AndroidSdk(sdkRoot);

                // Determine whether SDK has platform tools installed
                File toolsDirectory = new File(sdkRoot, "platform-tools");
                sdk.setUsesPlatformTools(toolsDirectory.isDirectory());

                // Determine SDK tools version
                File toolsPropFile = new File(sdkRoot, "tools/source.properties");
                Map<String, String> toolsProperties = Utils.parseConfigFile(toolsPropFile);
                String revisionStr = Util.fixEmptyAndTrim(toolsProperties.get("Pkg.Revision"));
                if (revisionStr != null) {
                    int version = Integer.parseInt(revisionStr);
                    sdk.setSdkToolsVersion(version);
                }

                return sdk;
            }
            private static final long serialVersionUID = 1L;
        };

        try {
            return launcher.getChannel().call(task);
        } catch (IOException e) {
            // Ignore
        } catch (InterruptedException e) {
            // Ignore
        }

        return null;
    }

    /**
     * Validates whether the given directory looks like a valid Android SDK directory.
     *
     * @param sdkRoot The directory to validate.
     * @param fromWebConfig Whether we are being called from the web config and should be more lax.
     * @return Whether the SDK looks valid or not (or a warning if the SDK install is incomplete).
     */
    public static ValidationResult validateAndroidHome(File sdkRoot, boolean fromWebConfig) {
        // This can be used to check the existence of a file on the server, so needs to be protected
        if (fromWebConfig && !Hudson.getInstance().hasPermission(Hudson.ADMINISTER)) {
            return ValidationResult.ok();
        }

        // Check the utter basics
        if (fromWebConfig && (sdkRoot == null || sdkRoot.getPath().equals(""))) {
            return ValidationResult.ok();
        }
        if (!sdkRoot.isDirectory()) {
            if (fromWebConfig && sdkRoot.getPath().matches(".*("+ Constants.REGEX_VARIABLE +").*")) {
                return ValidationResult.ok();
            }
            return ValidationResult.error(Messages.INVALID_DIRECTORY());
        }

        // We'll be using items from the tools and platforms directories.
        // Ignore that "platform-tools" may also be required for newer SDKs,
        // as we'll check for the presence of the individual tools in a moment
        final String[] sdkDirectories = { "tools", "platforms" };
        for (String dirName : sdkDirectories) {
            File dir = new File(sdkRoot, dirName);
            if (!dir.exists() || !dir.isDirectory()) {
                return ValidationResult.error(Messages.INVALID_SDK_DIRECTORY());
            }
        }

        // Search the possible tool directories to ensure the tools exist
        int toolsFound = 0;
        int expectedToolCount = Tool.values().length;
        if (!new File(sdkRoot, "platform-tools").exists()) {
            // aapt doesn't exist in "tools" until SDK Tools r9
            expectedToolCount--;
        }
        final String[] toolDirectories = { "tools", "platform-tools" };
        for (String dir : toolDirectories) {
            File toolsDir = new File(sdkRoot, dir);
            if (!toolsDir.isDirectory()) {
                continue;
            }
            for (String executable : Tool.getAllExecutableVariants()) {
                File toolPath = new File(toolsDir, executable);
                if (toolPath.exists() && toolPath.isFile()) {
                    toolsFound++;
                }
            }
        }
        if (toolsFound < expectedToolCount) {
            return ValidationResult.errorWithMarkup(Messages.REQUIRED_SDK_TOOLS_NOT_FOUND());
        }

        // Give the user a nice warning (not error) if they've not downloaded any platforms yet
        File platformsDir = new File(sdkRoot, "platforms");
        if (platformsDir.list().length == 0) {
            return ValidationResult.warning(Messages.SDK_PLATFORMS_EMPTY());
        }

        return ValidationResult.ok();
    }

    /**
     * Detects the root directory of an SDK installation based on the Android tools on the PATH.
     *
     * @param isUnix Whether the system where this command should run is sane.
     * @return The root directory of an Android SDK, or {@code null} if none could be determined.
     */
    private static String getSdkRootFromPath(boolean isUnix) {
        // Get list of required tools when working from PATH
        Tool[] tools = { Tool.ADB, Tool.EMULATOR };

        // Get list of directories from the PATH environment variable
        List<String> paths = Arrays.asList(System.getenv("PATH").split(File.pathSeparator));

        // Examine each directory to see whether it contains Android SDK Tools
        for (String path : paths) {
            File toolsDirectory = new File(path);
            if (isSdkToolsDirectory(tools, toolsDirectory, isUnix)) {
                // Return the parent path (i.e. the SDK root)
                return toolsDirectory.getParent();
            }
        }

        return null;
    }

    private static boolean isSdkToolsDirectory(Tool[] tools, File toolsDir, boolean isUnix) {
        int toolCount = 0;
        if (toolsDir.isDirectory()) {
            for (Tool tool : tools) {
                String executable = tool.getExecutable(isUnix);
                if (new File(toolsDir, executable).exists()) {
                    toolCount++;
                }
            }
        }

        // If all the tools were found in this directory, we have a winner
        return (toolCount == tools.length);
    }

    /**
     * Retrieves the path at which the Android SDK should be installed on the current node.
     *
     * @return Path within the tools folder where the SDK should live.
     */
    public static final FilePath getSdkInstallDirectory(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("Node is null");
        }

        // Get the root of the node installation
        FilePath root = node.getRootPath();
        if (root == null) {
            throw new IllegalArgumentException("Node " + node.getDisplayName() + " seems to be offline");
        }
        return root.child("tools").child("android-sdk");
    }

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
            if (tool.isPlatformTool() && androidSdk.usesPlatformTools()) {
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
     * Runs an Android tool on the remote build node and waits for completion before returning.
     *
     * @param launcher The launcher for the remote node.
     * @param stdout The stream to which standard output should be redirected.
     * @param stderr The stream to which standard error should be redirected.
     * @param androidSdk The Android SDK to use.
     * @param tool The Android tool to run.
     * @param args Any extra arguments for the command.
     * @param workingDirectory The directory to run the tool from, or {@code null} if irrelevant
     * @throws IOException If execution of the tool fails.
     * @throws InterruptedException If execution of the tool is interrupted.
     */
    public static void runAndroidTool(Launcher launcher, OutputStream stdout, OutputStream stderr,
            AndroidSdk androidSdk, Tool tool, String args, FilePath workingDirectory)
                throws IOException, InterruptedException {
        ArgumentListBuilder cmd = Utils.getToolCommand(androidSdk, launcher.isUnix(), tool, args);
        ProcStarter procStarter = launcher.launch().stdout(stdout).stderr(stderr).cmds(cmd);
        if (workingDirectory != null) {
            procStarter = procStarter.pwd(workingDirectory);
        }
        procStarter.join();
    }

    /**
     * Parses the contents of a properties file into a map.
     *
     * @param configFile The file to read.
     * @return The key-value pairs contained in the file, ignoring any comments or blank lines.
     * @throws IOException If the file could not be read.
     */
    public static Map<String,String> parseConfigFile(File configFile) throws IOException {
        FileReader fileReader = new FileReader(configFile);
        BufferedReader reader = new BufferedReader(fileReader);

        String line;
        Map<String,String> values = new HashMap<String,String>();
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0 || line.charAt(0) == '#') {
                continue;
            }
            String[] parts = line.split("=", 2);
            values.put(parts[0], parts[1]);
        }

        return values;
    }


    /**
     * Expands the variable in the given string to its value in the environment variables available
     * to this build.  The Jenkins-specific build variables for this build are then substituted.
     *
     * @param build  The build from which to get the build-specific and environment variables.
     * @param listener  The listener used to get the environment variables.
     * @param token  The token which may or may not contain variables in the format <tt>${foo}</tt>.
     * @return  The given token, with applicable variable expansions done.
     */
    public static String expandVariables(AbstractBuild<?,?> build, BuildListener listener, String token) {
        EnvVars envVars;
        Map<String, String> buildVars;

        try {
            EnvVars localVars = Computer.currentComputer().getEnvironment();
            envVars = new EnvVars(localVars);
            envVars.putAll(build.getEnvironment(listener));
            buildVars = build.getBuildVariables();
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            return null;
        }

        return expandVariables(envVars, buildVars, token);
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
        if (buildVars == null) {
            buildVars = new HashMap<String,String>(0);
        }

        String result = Util.fixEmptyAndTrim(token);
        if (result != null) {
            result = Util.replaceMacro(Util.replaceMacro(result, envVars), buildVars);
        }
        return result;
    }

    /**
     * Attempts to kill the given process, timing-out after {@code timeoutMs}.
     *
     * @param process The process to kill.
     * @param timeoutMs How long to wait for before cancelling the attempt to kill the process.
     * @return {@code true} if the process was killed successfully.
     */
    public static boolean killProcess(final Proc process, final int timeoutMs) {
        Boolean result = null;
        FutureTask<Boolean> task = null;
        try {
            // Attempt to kill the process; remoting will be handled by the process object
            task = new FutureTask<Boolean>(new java.util.concurrent.Callable<Boolean>() {
                public Boolean call() throws Exception {
                    process.kill();
                    return true;
                }
            });

            // Execute the task asynchronously and wait for a result or timeout
            Executors.newSingleThreadExecutor().execute(task);
            result = task.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // Ignore
        } finally {
            if (task != null && !task.isDone()) {
                task.cancel(true);
            }
        }

        return Boolean.TRUE.equals(result);
    }

}
