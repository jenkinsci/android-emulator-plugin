package hudson.plugins.android_emulator.util;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.plugins.android_emulator.AndroidEmulator.DescriptorImpl;
import hudson.plugins.android_emulator.Constants;
import hudson.plugins.android_emulator.Messages;
import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.sdk.Tool;
import hudson.plugins.android_emulator.sdk.cli.SdkCliCommand;
import hudson.remoting.Callable;
import hudson.remoting.Future;
import hudson.util.ArgumentListBuilder;
import hudson.util.VersionNumber;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jenkins.security.MasterToSlaveCallable;

import static hudson.plugins.android_emulator.AndroidEmulator.log;

public class Utils {

    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());
    private static final Pattern REVISION = Pattern.compile("(\\d++).*");

    /**
     * Retrieves the configured Android SDK root directory.
     *
     * @return The configured Android SDK root, if any. May include un-expanded variables.
     */
    public static String getConfiguredAndroidHome() {
        DescriptorImpl descriptor = Hudson.getInstance().getDescriptorByType(DescriptorImpl.class);
        if (descriptor != null) {
            return descriptor.androidHome;
        }
        return null;
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

        Callable<String, InterruptedException> task = new MasterToSlaveCallable<String, InterruptedException>() {
            public String call() throws InterruptedException {
                // Verify existence of provided value
                if (validateHomeDir(androidHome)) {
                    return androidHome;
                }

                // Check for common environment variables
                String[] keys = { Constants.ENV_VAR_ANDROID_SDK_ROOT,
                        Constants.ENV_VAR_ANDROID_SDK_HOME,
                        Constants.ENV_VAR_ANDROID_HOME,
                        Constants.ENV_VAR_ANDROID_SDK };

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
     * @param androidSdkRoot The SDK root directory specified in the job/system configuration.
     * @param androidSdkHome The SDK home directory, i.e. the workspace directory.
     * @return AndroidSdk object representing the properties of the installed SDK.
     */
    public static AndroidSdk getAndroidSdk(Launcher launcher, final String androidSdkRoot, final String androidSdkHome) {
        final boolean isUnix = launcher.isUnix();
        final PrintStream logger = launcher.getListener().getLogger();

        Callable<AndroidSdk, IOException> task = new MasterToSlaveCallable<AndroidSdk, IOException>() {
            public AndroidSdk call() throws IOException {
                String sdkRoot = androidSdkRoot;
                if (androidSdkRoot == null) {
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
                        log(logger, "Validate Android Home failed: " + result.getMessage());
                        return null;
                    }
                }

                // Create SDK instance with what we know so far
                return new AndroidSdk(sdkRoot, androidSdkHome);
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

        // Ensure that this at least looks like an SDK directory
        final String[] sdkDirectories = { "tools", "platform-tools" };
        for (String dirName : sdkDirectories) {
            File dir = new File(sdkRoot, dirName);
            if (!dir.exists() || !dir.isDirectory()) {
                return ValidationResult.error(Messages.INVALID_SDK_DIRECTORY());
            }
        }

        // Search the various tool directories to ensure the basic tools exist
        int toolsFound = 0;
        final String[] toolDirectories = { "tools", "platform-tools", "build-tools" };
        for (String dir : toolDirectories) {
            File toolsDir = new File(sdkRoot, dir);
            if (!toolsDir.isDirectory()) {
                continue;
            }
            IOFileFilter filter = new NameFileFilter(Tool.getAllExecutableVariants(Tool.REQUIRED));
            toolsFound += FileUtils.listFiles(toolsDir, filter, TrueFileFilter.INSTANCE).size();
        }
        if (toolsFound < Tool.REQUIRED.length) {
            return ValidationResult.errorWithMarkup(Messages.REQUIRED_SDK_TOOLS_NOT_FOUND());
        }

        // Give the user a nice warning (not error) if they've not downloaded any platforms yet
        File platformsDir = new File(sdkRoot, "platforms");
        if (platformsDir.list() == null || platformsDir.list().length == 0) {
            return ValidationResult.warning(Messages.SDK_PLATFORMS_EMPTY());
        }

        return ValidationResult.ok();
    }

    /**
     * Locates the current user's home directory using the same scheme as the Android SDK does.
     *
     * @return A {@link File} representing the directory in which the ".android" subdirectory should go.
     */
    public static File getHomeDirectory(String androidSdkHome) {
        // From git://android.git.kernel.org/platform/external/qemu.git/android/utils/bufprint.c
        String homeDirPath = System.getenv(Constants.ENV_VAR_ANDROID_SDK_HOME);
        if (homeDirPath == null) {
            if (androidSdkHome != null) {
                homeDirPath = androidSdkHome;
            } else if (!Functions.isWindows()) {
                homeDirPath = System.getenv(Constants.ENV_VAR_SYSTEM_HOME);
                if (homeDirPath == null) {
                    homeDirPath = "/tmp";
                }
            } else {
                // The emulator checks Win32 "CSIDL_PROFILE", which should equal USERPROFILE
                homeDirPath = System.getenv(Constants.ENV_VAR_SYSTEM_USERPROFILE);
                if (homeDirPath == null) {
                    // Otherwise fall back to user.home (which should equal USERPROFILE anyway)
                    homeDirPath = System.getProperty("user.home");
                }
            }
        }

        return new File(homeDirPath);
    }

    /**
     * Locates the current user's home directory using the same scheme as the Android SDK does.
     *
     * @return A {@link File} representing the home directory.
     */
    public static File getHomeDirectory() {
        // From https://android.googlesource.com/platform/external/qemu/android/base/system/System.cpp
        String path = null;
        if (Functions.isWindows()) {
            // The emulator queries for the Win32 "CSIDL_PROFILE" path, which should equal USERPROFILE
            path = System.getenv(Constants.ENV_VAR_SYSTEM_USERPROFILE);

            // Otherwise, fall back to the Windows equivalent of HOME
            if (path == null) {
                String homeDrive = System.getenv(Constants.ENV_VAR_SYSTEM_HOMEDRIVE);
                String homePath = System.getenv(Constants.ENV_VAR_SYSTEM_HOMEPATH);
                if (homeDrive != null && homePath != null) {
                    path = homeDrive + homePath;
                }
            }
        } else {
            path = System.getenv(Constants.ENV_VAR_SYSTEM_HOME);
        }

        // Path may not have been discovered
        if (path == null) {
            return null;
        }
        return new File(path);
    }

    /**
     * Detects the root directory of an SDK installation based on the Android tools on the PATH.
     *
     * @param isUnix Whether the system where this command should run is sane.
     * @return The root directory of an Android SDK, or {@code null} if none could be determined.
     */
    private static String getSdkRootFromPath(boolean isUnix) {
        // List of tools which should be found together in an Android SDK tools directory
        Tool[] tools = { Tool.ANDROID_LEGACY, Tool.EMULATOR };

        // Get list of directories from the PATH environment variable
        List<String> paths = Arrays.asList(System.getenv(Constants.ENV_VAR_SYSTEM_PATH).split(File.pathSeparator));

        // Examine each directory to see whether it contains the expected Android tools
        for (String path : paths) {
            File toolsDir = new File(path);
            if (!toolsDir.exists() || !toolsDir.isDirectory()) {
                continue;
            }

            int toolCount = 0;
            for (Tool tool : tools) {
                String executable = tool.getExecutable(isUnix);
                if (new File(toolsDir, executable).exists()) {
                    toolCount++;
                }
            }

            // If all the tools were found in this directory, we have a winner
            if (toolCount == tools.length) {
                // Return the parent path (i.e. the SDK root)
                return toolsDir.getParent();
            }
        }

        return null;
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
     * Parse the given command-line and return the appropriate environment variables if known
     * options are found.
     *
     * Currently this method is only used to workaround Android Emulator Bug 64356053, where
     * the '-no-audio', '-noaudio', '-audio none' option does not work for the qemu2-emulator.
     * If one of the options is found the environment variable 'QEMU_AUDIO_DRV=none' is set.
     *
     * @param commandLineOptions CLI-parameters to parse
     * @return {@code EnvVars} (Map) of additional environment variables based on given commandLine,
     * empty if no recognized option was found or parameters is {@code null}
     */
    public static EnvVars getEnvironmentVarsFromEmulatorArgs(final String commandLineOptions) {
        final EnvVars env = new EnvVars();

        if (commandLineOptions == null || commandLineOptions.isEmpty()) {
            return env;
        }

        if (commandLineOptions.matches(".*(\\s|^)-no-?audio.*")
                || commandLineOptions.matches(".*(\\s|^)-audio none(\\s|$).*")) {
            env.put(Constants.ENV_VAR_QEMU_AUDIO_DRV, Constants.ENV_VALUE_QEMU_AUDIO_DRV_NONE);
        }
        return env;
    }

    /**
     * Generates a ready-to-use ArgumentListBuilder for one of the Android SDK tools.
     *
     * @param androidSdk The Android SDK to use.
     * @param isUnix Whether the system where this command should run is sane.
     * @param sdkCmd The Android tool and any extra arguments for the command to run.
     * @return Arguments including the full path to the SDK and any extra Windows stuff required.
     */
    public static ArgumentListBuilder getToolCommand(AndroidSdk androidSdk, boolean isUnix, final SdkCliCommand sdkCmd) {
        // Determine the path to the desired tool
        final Tool tool = sdkCmd.getTool();
        String androidToolsDir;
        if (androidSdk.hasKnownRoot()) {
            androidToolsDir = androidSdk.getSdkRoot() + tool.findInSdk(androidSdk);
        } else {
            LOGGER.warning("SDK root not found. Assuming command is on the PATH");
            androidToolsDir = "";
        }

        // Build tool command
        final String executable = tool.getExecutable(isUnix);
        ArgumentListBuilder builder = new ArgumentListBuilder(androidToolsDir + executable);
        final String args = sdkCmd.getArgs();
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
     * @param sdkCmd The Android tool and any extra arguments for the command to run.
     * @param workingDirectory The directory to run the tool from, or {@code null} if irrelevant
     * @throws IOException If execution of the tool fails.
     * @throws InterruptedException If execution of the tool is interrupted.
     */
    public static void runAndroidTool(Launcher launcher, OutputStream stdout, OutputStream stderr,
            AndroidSdk androidSdk, final SdkCliCommand sdkCmd, FilePath workingDirectory)
                throws IOException, InterruptedException {
        runAndroidTool(launcher, new EnvVars(), stdout, stderr, androidSdk, sdkCmd, workingDirectory);
    }

    public static void runAndroidTool(Launcher launcher, EnvVars env, OutputStream stdout, OutputStream stderr,
            AndroidSdk androidSdk, final SdkCliCommand sdkCmd, FilePath workingDirectory)
            throws IOException, InterruptedException {
        runAndroidTool(launcher, env, stdout, stderr, androidSdk, sdkCmd, workingDirectory, 0);
    }

    public static void runAndroidTool(Launcher launcher, EnvVars env, OutputStream stdout, OutputStream stderr,
            AndroidSdk androidSdk, final SdkCliCommand sdkCmd, FilePath workingDirectory, long timeoutMs)
                throws IOException, InterruptedException {

        ArgumentListBuilder cmd = Utils.getToolCommand(androidSdk, launcher.isUnix(), sdkCmd);
        ProcStarter procStarter = launcher.launch().stdout(stdout).stderr(stderr).cmds(cmd);
        if (androidSdk.hasKnownHome()) {
            // Copy the old one, so we don't mutate the argument.
            env = new EnvVars((env == null ? new EnvVars() : env));
            env.put(Constants.ENV_VAR_ANDROID_SDK_HOME, androidSdk.getSdkHome());
        }

        if (env != null) {
            procStarter = procStarter.envs(env);
        }

        if (workingDirectory != null) {
            procStarter.pwd(workingDirectory);
        }

        // Start the process and wait for it to end (or time out)
        Proc proc = procStarter.start();
        if (timeoutMs > 0) {
            proc.joinWithTimeout(timeoutMs / 1000, TimeUnit.SECONDS, launcher.getListener());
        } else {
            proc.join();
        }
    }

    /**
     * Parses the contents of a properties file into a map.
     *
     * @param configFile The file to read.
     * @return The key-value pairs contained in the file, ignoring any comments or blank lines.
     * @throws IOException If the file could not be read.
     */
    public static Map<String, String> parseConfigFile(File configFile) throws IOException {
        FileReader fileReader = new FileReader(configFile);
        BufferedReader reader = new BufferedReader(fileReader);
        Properties properties = new Properties();
        properties.load(reader);
        reader.close();

        final Map<String, String> values = new HashMap<String, String>();
        for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
            values.put((String) entry.getKey(), (String) entry.getValue());
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
     * Expands the variable in the given string to its value in the variables available to this build.
     * The Jenkins-specific build variables take precedence over environment variables.
     *
     * @param envVars  Map of the environment variables.
     * @param buildVars  Map of the build-specific variables.
     * @param token  The token which may or may not contain variables in the format <tt>${foo}</tt>.
     * @return  The given token, with applicable variable expansions done.
     */
    public static String expandVariables(EnvVars envVars, Map<String,String> buildVars,
            String token) {
        final Map<String,String> vars = new HashMap<String,String>(envVars);
        if (buildVars != null) {
            // Build-specific variables, if any, take priority over environment variables
            vars.putAll(buildVars);
        }

        String result = Util.fixEmptyAndTrim(token);
        if (result != null) {
            result = Util.replaceMacro(result, vars);
        }
        return Util.fixEmptyAndTrim(result);
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

    /**
     * Sends a user command to the running emulator via its telnet interface.<br>
     * Execution will be cancelled if it takes longer than {@code timeoutMs}.
     *
     * @param logger The build logger.
     * @param launcher The launcher for the remote node.
     * @param port The emulator's telnet port.
     * @param command The command to execute on the emulator's telnet interface.
     * @param timeoutMs How long to wait (in ms) for the command to complete before cancelling it.
     * @return Whether sending the command succeeded.
     */
    public static boolean sendEmulatorCommand(final Launcher launcher, final PrintStream logger,
            final int port, final String command, int timeoutMs) {
        Boolean result = null;
        Future<Boolean> future = null;
        try {
            // Execute the task on the remote machine asynchronously, with a timeout
            EmulatorCommandTask task = new EmulatorCommandTask(port, command);
            future = launcher.getChannel().callAsync(task);
            result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            // Slave communication failed
            log(logger, Messages.SENDING_COMMAND_FAILED(command, e));
            e.printStackTrace(logger);
        } catch (InterruptedException e) {
            // Ignore; the caller should handle shutdown
        } catch (ExecutionException e) {
            // Exception thrown while trying to execute command
            if (command.equals("kill") && e.getCause() instanceof SocketException) {
                // This is expected: sending "kill" causes the emulator process to kill itself
                result = true;
            } else {
                // Otherwise, it was some generic failure
                log(logger, Messages.SENDING_COMMAND_FAILED(command, e));
                e.printStackTrace(logger);
            }
        } catch (TimeoutException e) {
            // Command execution timed-out
            log(logger, Messages.SENDING_COMMAND_TIMED_OUT(command));
        } finally {
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
        }

        return Boolean.TRUE.equals(result);
    }

    /**
     * Determines the relative path required to get from one path to another.
     *
     * @param from Path to go from.
     * @param to Path to reach.
     * @return The relative path between the two, or {@code null} for invalid input.
     */
    public static String getRelativePath(String from, String to) {
        // Check for bad input
        if (from == null || to == null) {
            return null;
        }

        String fromPath, toPath;
        try {
            fromPath = new File(from).getCanonicalPath();
            toPath = new File(to).getCanonicalPath();
        } catch (IOException e1) {
            e1.printStackTrace();
            return null;
        }

        // Nothing to do if the two are equal
        if (fromPath.equals(toPath)) {
            return "";
        }
        // Target directory is a subdirectory
        if (toPath.startsWith(fromPath)) {
            int fromLength = fromPath.length();
            int index = fromLength == 1 ? 1 : fromLength + 1;
            return toPath.substring(index) + File.separatorChar;
        }

        // Quote separator, as String.split() takes a regex and
        // File.separator isn't a valid regex character on Windows
        final String separator = Pattern.quote(File.separator);
        // Target directory is somewhere above our directory
        String[] fromParts = fromPath.substring(1).split(separator);
        final int fromLength = fromParts.length;
        String[] toParts = toPath.substring(1).split(separator);
        final int toLength = toParts.length;

        // Find the number of common path segments
        int commonLength = 0;
        for (int i = 0; i < toLength; i++) {
            if (fromParts[i].length() == 0) {
                continue;
            }
            if (!fromParts[i].equals(toParts[i])) {
                break;
            }
            commonLength++;
        }

        // Determine how many directories up we need to go
        int diff = fromLength - commonLength;
        StringBuilder rel = new StringBuilder();
        for (int i = 0; i < diff; i++) {
            rel.append("..");
            rel.append(File.separatorChar);
        }

        // Add on the remaining path segments to the target
        for (int i = commonLength; i < toLength; i++) {
            rel.append(toParts[i]);
            rel.append(File.separatorChar);
        }

        return rel.toString();
    }

    /**
     * Determines the number of steps required to get between two paths.
     * <p/>
     * e.g. To get from "/foo/bar/baz" to "/foo/blah" requires making three steps:
     * <ul>
     * <li>"/foo/bar"</li>
     * <li>"/foo"</li>
     * <li>"/foo/blah"</li>
     * </ul>
     *
     * @param from Path to go from.
     * @param to Path to reach.
     * @return The relative distance between the two, or {@code -1} for invalid input.
     */
    public static int getRelativePathDistance(String from, String to) {
        final String relative = getRelativePath(from, to);
        if (relative == null) {
            return -1;
        }

        final String[] parts = relative.split("/");
        final int length = parts.length;
        if (length == 1 && parts[0].isEmpty()) {
            return 0;
        }
        return parts.length;
    }

    /**
     * Checks whether the version number string represented by the first parameter is older then
     * the version number string represented by the second parameter. For comparison the utility class
     * {@code VersionNumber} is used.
     *
     * @param strVersion the version number to check if older then {@code strVersionToCompare}
     * @param strVersionToCompare the version number where {@code strVersion} is compared to
     * @return {@code true} if {@code VersionNumber} representation of {@code strVersion} is older then
     * {@code VersionNumber} representation of {@code strVersionToCompare}
     */
    public static boolean isVersionOlderThan(final String strVersion, final String strVersionToCompare) {
        final VersionNumber version = new VersionNumber(Util.fixNull(strVersion));
        final VersionNumber versiontoCompare = new VersionNumber(Util.fixNull(strVersionToCompare));
        return version.isOlderThan(versiontoCompare);
    }

    /**
     * Looks up the input for the given pattern with an attached version number. The pattern
     * with the highest version found is returned. The delimiter between pattern and version
     * may be ';' or '-'.
     *
     * @param multiLine multi-line input string to look up pattern + version
     * @param pattern the pattern to look for
     * @return The pattern found with the highest version number, or null if pattern is not found
     */
    public static String getPatternWithHighestSuffixedVersionNumberInMultiLineInput(final String multiLine, final String pattern) {
        String result = null;
        String currentMaxVersion = "0";

        final String lines[] = multiLine.split("(\r\n|\r|\n)");
        for (int pos = 0; pos < lines.length; pos++) {
            final String line = lines[pos];
            final String patternAndVersionRegex = "(" + pattern + "[-;][0-9\\.]+)";
            final Matcher m = Pattern.compile(patternAndVersionRegex).matcher(line);
            if (m.find()) {
                final String patternAndVersion = m.group(0);
                final String lineVersion = patternAndVersion.replaceAll("^(.*?)([0-9\\.]*)$", "$2");
                if (isVersionOlderThan(currentMaxVersion, lineVersion)) {
                    result = patternAndVersion;
                    currentMaxVersion = lineVersion;
                }
            }
        }
        return result;
    }

    /**
     * Compares one given string representing a version number ({@code"[:digit:]+(\.[:digit:]+)*"})
     * to another one and checks for equality. Additionally the number of parts to compare can
     * be given, this allows comparing only eg: the major and minor numbers. The version number
     * "1.0.0" would match "1.0.1" if partsToCompare would be 2.
     *
     * @param strVersionA version number to compare against {@code strVersionB}
     * @param strVersionB version number to compare against {@code strVersionA}
     * @param partsToCompare if > 0 then the number of parts for that version number are compared,
     * if <= 0 the complete version number is compared
     * @return {@code true} if the versions number (or if requested parts of the version numbers) are identical,
     * {@code false} otherwise
     */
    public static boolean equalsVersion(final String strVersionA, final String strVersionB, final int partsToCompare) {
        String versionA = Util.fixNull(strVersionA);
        String versionB = Util.fixNull(strVersionB);

        if (partsToCompare <= 0) {
            return (versionA.equals(versionB));
        }

        final String[] splitA = versionA.split("\\.");
        final String[] splitB = versionB.split("\\.");

        for (int idx = 0; idx < partsToCompare; idx++) {
            final String a = (idx < splitA.length) ? splitA[idx] : "";
            final String b = (idx < splitB.length) ? splitB[idx] : "";
            if (!a.equals(b)) {
                return false;
            }
        }
        return true;
    }

    /** Task that will execute a command on the given emulator's console port, then quit. */
    private static final class EmulatorCommandTask extends MasterToSlaveCallable<Boolean, IOException> {

        private final int port;
        private final String command;

        @SuppressWarnings("hiding")
        EmulatorCommandTask(int port, String command) {
            this.port = port;
            this.command = command;
        }

        @SuppressWarnings("null")
        public Boolean call() throws IOException {
            Socket socket = null;
            BufferedReader in = null;
            PrintWriter out = null;
            try {
                // Connect to the emulator's console port
                socket = new Socket("127.0.0.1", port);
                out = new PrintWriter(socket.getOutputStream());
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // If we didn't get a banner response, give up
                if (in.readLine() == null) {
                    return false;
                }

                // Send command, then exit the console
                out.write(command);
                out.write("\r\n");
                out.flush();
                out.write("quit\r\n");
                out.flush();

                // Wait for the commands to return a response
                while (in.readLine() != null) {
                    // Ignore
                }
            } finally {
                try {
                    out.close();
                } catch (Exception ignore) {}
                try {
                    in.close();
                } catch (Exception ignore) {}
                try {
                    socket.close();
                } catch (Exception ignore) {}
            }

            return true;
        }

        private static final long serialVersionUID = 1L;
    }

}
