package hudson.plugins.android_emulator.util;

import static hudson.plugins.android_emulator.AndroidEmulator.log;

import java.io.BufferedReader;
import java.io.File;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.exception.ExceptionUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import hudson.model.TaskListener;
import hudson.plugins.android_emulator.AndroidEmulator.DescriptorImpl;
import hudson.plugins.android_emulator.Constants;
import hudson.plugins.android_emulator.Messages;
import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.sdk.Tool;
import hudson.plugins.android_emulator.sdk.ToolLocator;
import hudson.plugins.android_emulator.sdk.cli.SdkCliCommand;
import hudson.remoting.Callable;
import hudson.remoting.Future;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;
import hudson.util.VersionNumber;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;

public class Utils {

    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());

    /**
     * Retrieves the configured Android SDK root directory.
     *
     * @return The configured Android SDK root, if any. May include un-expanded variables.
     */
    public static String getConfiguredAndroidHome() {
        DescriptorImpl descriptor = Jenkins.get().getDescriptorByType(DescriptorImpl.class);
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
     * Tries to validate the given Android SDK root directory.
     *
     * @param launcher The launcher for the remote node.
     * @param androidSdkRootPreferred The preferred SDK root directory.
     * Normally the (variable-expanded) SDK root directory specified in the job/system configuration.
     * @param androidSdkHome The SDK home directory, i.e. the workspace directory.
     * @return AndroidSdk object representing the properties of the installed SDK, or null if no valid
     * directory is found.
     */
    public static AndroidSdk getAndroidSdk(final Launcher launcher,
            final String androidSdkRootPreferred, final String androidSdkHome) {
        return getAndroidSdk(launcher, null, null, true, androidSdkRootPreferred, androidSdkHome);
    }

    /**
     * Tries to validate the given Android SDK root directory; otherwise tries to
     * locate a copy of the SDK by checking for the auto-install directory and for
     * common environment variables.
     *
     * @param launcher The launcher for the remote node.
     * @param node Current node
     * @param envVars Environment variables for the build.
     * @param androidSdkRootPreferred The preferred SDK root directory.
     * Normally the (variable-expanded) SDK root directory specified in the job/system configuration.
     * @param androidSdkHome The SDK home directory, i.e. the workspace directory.
     * @return AndroidSdk object representing the properties of the installed SDK, or null if no valid
     * directory is found.
     */
    public static AndroidSdk getAndroidSdk(final Launcher launcher, final Node node,
            final EnvVars envVars,
            final String androidSdkRootPreferred, final String androidSdkHome) {
        return getAndroidSdk(launcher, node, envVars, false, androidSdkRootPreferred, androidSdkHome);
    }

    /**
     * Tries to validate the given Android SDK root directory; otherwise tries to
     * locate a copy of the SDK by checking for the auto-install directory and for
     * common environment variables.
     *
     * @param launcher The launcher for the remote node.
     * @param node Current node
     * @param envVars Environment variables for the build.
     * @param checkPreferredOnly just check preferred directory, do not try to determine
     * other SDK roots by evaluating auto-install directory or common environment variables.
     * @param androidSdkRootPreferred The preferred SDK root directory.
     * Normally the (variable-expanded) SDK root directory specified in the job/system configuration.
     * @param androidSdkHome The SDK home directory, i.e. the workspace directory.
     * @return AndroidSdk object representing the properties of the installed SDK, or null if no valid
     * directory is found.
     */
    public static AndroidSdk getAndroidSdk(final Launcher launcher, final Node node,
            final EnvVars envVars, final boolean checkPreferredOnly,
            final String androidSdkRootPreferred, final String androidSdkHome) {

        final TaskListener listener = launcher.getListener();
        final String autoInstallDir = (!checkPreferredOnly) ? getSdkInstallDirectory(node).getRemote() : "";

        Callable<AndroidSdk, IOException> task = new AndroidSDKCallable(envVars, checkPreferredOnly, androidSdkRootPreferred,
                androidSdkHome, autoInstallDir, listener);

        final PrintStream logger = listener.getLogger();
        try {
            VirtualChannel channel = launcher.getChannel();
            if (channel == null) {
                throw new IllegalStateException("Channel is not configured");
            }
            return channel.call(task);
        } catch (IOException e) {
            // Ignore, log only
            log(logger, ExceptionUtils.getFullStackTrace(e));
        } catch (InterruptedException e) {
            // Ignore, log only
            log(logger, ExceptionUtils.getFullStackTrace(e));
        }

        return null;
    }

    /**
     * Check if a root directory contains all the given subDirectories.
     * If a single subDirectory does not exist, false is returned.
     *
     * @param root the root-directory which needs to hold the subDirectories
     * @param subDirectories the names of the subDirectories to check for existence
     * @return true if all subDirectories exist or empty, false otherwise
     */
    public static boolean areAllSubdirectoriesExistant(final File root, final String[] subDirectories) {
        if (subDirectories == null) {
            return true;
        }

        for (final String dirName : subDirectories) {
            final File dir = new File(root, dirName);
            if (!dir.exists() || !dir.isDirectory()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if a root directory contains all the given files.
     * If a single file is not found false is returned.
     *
     * @param root the root-directory which needs to hold the subDirectories
     * @param relativeFilePaths the names of the files to check for existence
     * @return true if all files exist or empty, false otherwise
     */
    public static boolean areAllFilesExistantInDir(final File root, final String[] relativeFilePaths) {
        if (relativeFilePaths == null) {
            return true;
        }

        for (String filePath : relativeFilePaths) {
            File file = new File(root, filePath);
            if (!file.isFile()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Helper method to wrap check for validateAndroidHome to determine if given SDK
     * root directory contains all needed tools. A flag allows to define if the
     * legacy layout is defined as valid or not.
     *
     * @param sdkRoot The directory to validate.
     * @param allowLegacy Whether the legacy SDK Tools layout is considered valid
     * @return Whether the SDK root directory contains all necessary files
     */
    private static boolean validateSDKCheckIfAllNecessaryFilesExists(final File sdkRoot, final boolean allowLegacy) {
        final boolean sdkDirNewLayoutExists =
                areAllFilesExistantInDir(sdkRoot, Tool.getRequiredToolsRelativePaths(true))
                        || areAllFilesExistantInDir(sdkRoot, Tool.getRequiredToolsRelativePaths(false));
        final boolean sdkDirOldLayoutExists = !sdkDirNewLayoutExists && allowLegacy
                && (areAllFilesExistantInDir(sdkRoot, Tool.getRequiredToolsLegacyRelativePaths(true))
                        || areAllFilesExistantInDir(sdkRoot, Tool.getRequiredToolsLegacyRelativePaths(false)));
        final boolean cmdLineToolsLayoutExists = areAllFilesExistantInDir(sdkRoot, Tool.getRequiredCmdLineToolsPaths(true))
                || areAllFilesExistantInDir(sdkRoot, Tool.getRequiredCmdLineToolsPaths(false));

        return (sdkDirNewLayoutExists || sdkDirOldLayoutExists || cmdLineToolsLayoutExists);
    }

    /**
     * Validates whether the given directory looks like a valid Android SDK directory.
     *
     * @param sdkRoot The directory to validate.
     * @param allowLegacy Whether the legacy SDK Tools layout is considered valid
     * @param fromWebConfig Whether we are being called from the web config and should be more lax.
     * @return Whether the SDK looks valid or not (or a warning if the SDK install is incomplete).
     */
    public static ValidationResult validateAndroidHome(final File sdkRoot, final boolean allowLegacy, final boolean fromWebConfig) {

        // This can be used to check the existence of a file on the server, so needs to be protected
        if (fromWebConfig && !Jenkins.get().hasPermission(Hudson.ADMINISTER)) {
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
        if (
                !areAllSubdirectoriesExistant(sdkRoot, ToolLocator.SDK_DIRECTORIES)
                && !(allowLegacy && areAllSubdirectoriesExistant(sdkRoot, ToolLocator.SDK_DIRECTORIES_LEGACY))
                ) {
            return ValidationResult.error(Messages.INVALID_SDK_DIRECTORY());
        }

        // Search the various tool directories to ensure the basic tools exist
        if (!validateSDKCheckIfAllNecessaryFilesExists(sdkRoot, allowLegacy)) {
            return ValidationResult.errorWithMarkup(Messages.REQUIRED_SDK_TOOLS_NOT_FOUND());
        }

        // Give the user a nice warning (not error) if they've not downloaded any platforms yet
        File platformsDir = new File(sdkRoot, ToolLocator.PLATFORMS_DIR);
        final String[] platformsDirList = platformsDir.list();
        if (platformsDirList == null || platformsDirList.length == 0) {
            return ValidationResult.warning(Messages.SDK_PLATFORMS_EMPTY());
        }

        return ValidationResult.ok();
    }

    /**
     * Locates the Android SDK home directory using the same scheme as the Android SDK does.
     *
     * @param androidSdkHome
     * @return A {@link File} representing the directory in which the ".android" subdirectory should go.
     */
    @SuppressFBWarnings(value = "ENV_USE_PROPERTY_INSTEAD_OF_ENV", justification = "Same scheme as the Android SDK does it")
    public static File getAndroidSdkHomeDirectory(String androidSdkHome) {
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
    @SuppressFBWarnings(value = "ENV_USE_PROPERTY_INSTEAD_OF_ENV", justification = "Same scheme as the Android SDK does it")
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
     * Retrieves a list of directories from the PATH-Environment-Variable, which could be an SDK installation.
     * Currently it is only checked, if the path points to an 'tools'-directory.
     *
     * @param envVar the environment variables currently set for the node
     * @return A list of possible root directories of an Android SDK
     */
    private static List<String> getPossibleSdkRootDirectoriesFromPath(final EnvVars envVars) {
        // Get list of directories from the PATH environment variable
        List<String> paths = Arrays.asList(envVars.get(Constants.ENV_VAR_SYSTEM_PATH).split(File.pathSeparator));
        final List<String> possibleSdkRootsFromPath = new ArrayList<String>();

        // Examine each directory to see whether it contains the expected Android tools
        for (String path : paths) {
            if (path.matches(".*[\\\\/]" + ToolLocator.TOOLS_DIR + "[\\\\/]*$")) {
                possibleSdkRootsFromPath.add(path);
            }
        }

        return possibleSdkRootsFromPath;
    }

    /**
     * Retrieves the path at which the Android SDK should be installed on the current node.
     *
     * @param node
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
        final String executable;
        if (androidSdk.hasKnownRoot()) {
            executable = androidSdk.getSdkRoot() + "/" + tool.getPathInSdk(androidSdk, isUnix);
        } else {
            LOGGER.warning("SDK root not found. Assuming command is on the PATH");
            executable = tool.getExecutable(isUnix);
        }

        // Build tool command
        final ArgumentListBuilder builder = new ArgumentListBuilder(executable);
        final String args = sdkCmd.getArgs();
        if (args != null) {
            if (Tool.SDKMANAGER == sdkCmd.getTool() && androidSdk.hasCommandLineTools() && androidSdk.hasKnownRoot()) {
                builder.add("--sdk_root=" + androidSdk.getSdkRoot());
            }
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
        if (androidSdk.hasKnownRoot() && Tool.AVDMANAGER == sdkCmd.getTool() && androidSdk.hasCommandLineTools()) {
            env.put("AVDMANAGER_OPTS", "-Dcom.android.sdkmanager.toolsdir=" + androidSdk.getSdkRoot() + "/tools/bin");
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
     * Expands the variable in the given string to its value in the environment variables available
     * to this build.  The Jenkins-specific build variables for this build are then substituted.
     *
     * @param build  The build from which to get the build-specific and environment variables.
     * @param listener  The listener used to get the environment variables.
     * @param token  The token which may or may not contain variables in the format {@code ${foo}}.
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
     * @param token  The token which may or may not contain variables in the format {@code ${foo}}.
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
        } catch (TimeoutException ex) {
        } catch (InterruptedException ex) {
        } catch (ExecutionException ex) {
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
        VirtualChannel channel = launcher.getChannel();
        if (channel == null) {
            throw new IllegalStateException("Channel not configured");
        }

        Boolean result = null;
        Future<Boolean> future = null;
        try {
            // Execute the task on the remote machine asynchronously, with a timeout
            EmulatorCommandTask task = new EmulatorCommandTask(port, command);
            future = channel.callAsync(task);
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
            // normalize separators first to avoid '//' typos on unix to get converted to UNC paths on windows
            fromPath = new File(normalizePathSeparators(from)).getCanonicalPath();
            toPath = new File(normalizePathSeparators(to)).getCanonicalPath();
        } catch (IOException e1) {
            e1.printStackTrace();
            return null;
        }

        // Nothing to do if the two are equal
        if (fromPath.equals(toPath)) {
            return "";
        }

        // Quote separator, as String.split() takes a regex and
        // File.separator isn't a valid regex character on Windows
        final String separator = Pattern.quote(File.separator);
        // Target directory is somewhere above our directory
        String[] fromParts = fromPath.substring(1).split(separator);
        final int fromLength = getNumberOfNonEmptyEntries(fromParts);
        String[] toParts = toPath.substring(1).split(separator);
        final int toLength = getNumberOfNonEmptyEntries(toParts);

        // Find the number of common path segments
        int commonLength = 0;
        for (int i = 0; i < toLength && i < fromLength; i++) {
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
     * <p>
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

        final String[] parts = relative.split(Pattern.quote(File.separator));
        return getNumberOfNonEmptyEntries(parts);
    }

    /**
     * Reduce multi-slash and multi-backslash to single characters, but keeping
     * double backslash in the beginning to keep UNC paths.
     *
     * @param path the path to normalize
     * @return normalized path without double slash/backslash
     */
    public static String normalizePathSeparators(final String path) {
        return path
                // multi-backslash to double first, then all except on start to single backslash, to avoid loosing UNC paths
                .replaceAll("\\\\\\\\+", "\\\\\\\\").replaceAll("(?<!^)\\\\+", "\\\\")
                // multi-slash to single slash for unix paths
                .replaceAll("/+", "/");
    }

    /**
     * Returns the length of the String-Array omitting empty entries.
     *
     * @param array String array to retrieve length from
     * @return number of non empty String entries
     */
    private static int getNumberOfNonEmptyEntries(final String[] array) {
        int length = 0;
        for (int idx = 0; idx < array.length; idx++) {
            if (!array[idx].isEmpty()) {
                length++;
            }
        }
        return length;
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
     * No RC or beta versions are supported, those versions with an additional suffix will be ignored
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
            final String patternAndVersionRegex = "(" + pattern + "[-;][0-9\\.]+(?:-rc[0-9])?)";
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
     * @param partsToCompare if &gt; 0 then the number of parts for that version number are compared,
     * if &lt;= 0 the complete version number is compared
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

    private static final class AndroidSDKCallable extends MasterToSlaveCallable<AndroidSdk, IOException> {
        private final String androidSdkHome;
        private final String androidSdkRootPreferred;
        private final boolean checkPreferredOnly;
        private final EnvVars envVars;
        private final String autoInstallDir;
        private final TaskListener listener;

        public AndroidSDKCallable(final EnvVars envVars, final boolean checkPreferredOnly,
                                  final String androidSdkRootPreferred, final String androidSdkHome,
                                  final String autoInstallDir, final TaskListener listener) {
            this.envVars = envVars;
            this.androidSdkHome = androidSdkHome;
            this.androidSdkRootPreferred = androidSdkRootPreferred;
            this.checkPreferredOnly = checkPreferredOnly;
            this.autoInstallDir = autoInstallDir;
            this.listener = listener;
        }

        public AndroidSdk call() throws IOException {
            final List<String> potentialSdkDirs = getPotentialSdkDirs();
            final StringBuilder determinationLog = new StringBuilder();

            // Check each directory to see if it's a valid Android SDK
            for (String potentialSdkDir : potentialSdkDirs) {
                if (Util.fixEmptyAndTrim(potentialSdkDir) == null) {
                    continue;
                }

                final ValidationResult result = Utils.validateAndroidHome(new File(potentialSdkDir), true, false);
                if (!result.isFatal()) {
                    // Create SDK instance with what we know so far
                    return new AndroidSdk(potentialSdkDir, androidSdkHome);
                } else {
                    determinationLog.append("['" + potentialSdkDir + "']: " + result.getMessage() + "\n");
                }
            }

            // Give up
            log(listener.getLogger(), Messages.SDK_DETERMINATION_FAILED());
            log(listener.getLogger(), determinationLog.toString(), true);
            return null;
        }

        private List<String> getPotentialSdkDirs() {
            final List<String> potentialSdkDirs = new ArrayList<>();

            // Add global config path first
            potentialSdkDirs.add(androidSdkRootPreferred);

            // do not add any other possible dirs
            if (checkPreferredOnly) {
                return potentialSdkDirs;
            }

            // Add common environment variables
            String[] keys = { Constants.ENV_VAR_ANDROID_SDK_ROOT,
                    Constants.ENV_VAR_ANDROID_SDK_HOME,
                    Constants.ENV_VAR_ANDROID_HOME,
                    Constants.ENV_VAR_ANDROID_SDK };

            // Resolve each variable to its directory name
            for (String key : keys) {
                final String envValue = envVars.get(key);
                if (envValue != null && !envValue.isEmpty()) {
                    potentialSdkDirs.add(envVars.get(key));
                }
            }

            // Also add the auto-installed SDK directory to the list of candidates
            if (Util.fixEmptyAndTrim(autoInstallDir) != null) {
                potentialSdkDirs.add(autoInstallDir);
            }

            // At last, add potential path directories
            potentialSdkDirs.addAll(getPossibleSdkRootDirectoriesFromPath(envVars));

            return potentialSdkDirs;
        }

        private static final long serialVersionUID = 1L;
    };

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
        @SuppressFBWarnings({"DM_DEFAULT_ENCODING", "RV_DONT_JUST_NULL_CHECK_READLINE"})
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

    /**
     * Checks if java.lang.Process is still alive. Native isAlive method
     * exists since Java 8 API.
     *
     * @param process Process to check
     * @return true if process is alive, false if process has exited
     */
    public static boolean isProcessAlive(final Process process) {
        boolean exited = false;
        try {
            process.exitValue();
            exited = true;
        } catch (IllegalThreadStateException ex) {
        }
        return !exited;
    }

}
