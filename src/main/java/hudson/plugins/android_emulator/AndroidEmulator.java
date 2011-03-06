package hudson.plugins.android_emulator;


import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.Util;
import hudson.Launcher.ProcStarter;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.ArgumentListBuilder;
import hudson.util.ForkOutputStream;
import hudson.util.FormValidation;
import hudson.util.NullStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONObject;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.jvnet.hudson.plugins.port_allocator.PortAllocationManager;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

public class AndroidEmulator extends BuildWrapper implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Duration by which the emulator should start being available via adb. */
    private static final int ADB_CONNECT_TIMEOUT_MS = 60 * 1000;

    /** Duration by which emulator booting should normally complete. */
    private static final int BOOT_COMPLETE_TIMEOUT_MS = 120 * 1000;

    private DescriptorImpl descriptor;

    private final String avdName;
    private final String osVersion;
    private final String screenDensity;
    private final String screenResolution;
    private final String deviceLocale;
    private final String sdCardSize;
    private final boolean wipeData;
    private final boolean showWindow;
    private final String commandLineOptions;
    private final int startupDelay;
    private final HardwareProperty[] hardwareProperties;

    @DataBoundConstructor
    @SuppressWarnings("hiding")
    public AndroidEmulator(String avdName, String osVersion, String screenDensity,
            String screenResolution, String deviceLocale, String sdCardSize,
            HardwareProperty[] hardwareProperties, boolean wipeData,
            boolean showWindow, String commandLineOptions, int startupDelay) {
        this.avdName = avdName;
        this.osVersion = osVersion;
        this.screenDensity = screenDensity;
        this.screenResolution = screenResolution;
        this.deviceLocale = deviceLocale;
        this.sdCardSize = sdCardSize;
        this.hardwareProperties = hardwareProperties;
        this.wipeData = wipeData;
        this.showWindow = showWindow;
        this.commandLineOptions = commandLineOptions;
        this.startupDelay = Math.abs(startupDelay);
    }

    public boolean getUseNamedEmulator() {
        return avdName != null;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getAvdName() {
        return avdName;
    }

    public String getScreenDensity() {
        return screenDensity;
    }

    public String getScreenResolution() {
        return screenResolution;
    }

    public String getDeviceLocale() {
        return deviceLocale;
    }

    public String getSdCardSize() {
        return sdCardSize;
    }

    public boolean shouldWipeData() {
        return wipeData;
    }

    public boolean shouldShowWindow() {
        return showWindow;
    }

    public String getCommandLineOptions() {
        return commandLineOptions;
    }

    public int getStartupDelay() {
        return startupDelay;
    }

    public HardwareProperty[] getHardwareProperties() {
        return hardwareProperties;
    }

    @Override
    @SuppressWarnings({"hiding", "unchecked"})
    public Environment setUp(AbstractBuild build, final Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {
        final PrintStream logger = listener.getLogger();
        if (descriptor == null) {
            descriptor = Hudson.getInstance().getDescriptorByType(DescriptorImpl.class);
        }

        // Substitute environment and build variables into config
        final EnvVars localVars = Computer.currentComputer().getEnvironment();
        final EnvVars envVars = new EnvVars(localVars);
        envVars.putAll(build.getEnvironment(listener));
        final Map<String, String> buildVars = build.getBuildVariables();

        // Device properties
        String avdName = Utils.expandVariables(envVars, buildVars, this.avdName);
        String osVersion = Utils.expandVariables(envVars, buildVars, this.osVersion);
        String screenDensity = Utils.expandVariables(envVars, buildVars, this.screenDensity);
        String screenResolution = Utils.expandVariables(envVars, buildVars, this.screenResolution);
        String deviceLocale = Utils.expandVariables(envVars, buildVars, this.deviceLocale);
        String sdCardSize = Utils.expandVariables(envVars, buildVars, this.sdCardSize);

        // Expand macros within hardware property values
        final int propCount = hardwareProperties == null ? 0 : hardwareProperties.length;
        HardwareProperty[] expandedProperties = new HardwareProperty[propCount];
        for (int i = 0; i < propCount; i++) {
            HardwareProperty prop = hardwareProperties[i];
            String expandedValue = Utils.expandVariables(envVars, buildVars, prop.value);
            expandedProperties[i] = new HardwareProperty(prop.key, expandedValue);
        }

        // Emulator properties
        String commandLineOptions = Utils.expandVariables(envVars, buildVars, this.commandLineOptions);

        // SDK location
        String androidHome = Utils.expandVariables(envVars, buildVars, descriptor.androidHome);
        androidHome = discoverAndroidHome(launcher, localVars, androidHome);

        // Despite the nice inline checks and warnings when the user is editing the config,
        // these are not binding, so the user may have saved invalid configuration.
        // Here we check whether or not it's worth proceeding based on the saved values.
        String configError = isConfigValid(avdName, osVersion, screenDensity, screenResolution,
                deviceLocale, sdCardSize);
        if (configError != null) {
            log(logger, Messages.ERROR_MISCONFIGURED(configError));
            build.setResult(Result.NOT_BUILT);
            return null;
        }

        // Confirm that tools are available on PATH
        AndroidSdk androidSdk = getAndroidSdk(launcher, androidHome);
        if (androidSdk == null) {
            log(logger, Messages.SDK_TOOLS_NOT_FOUND());
            build.setResult(Result.NOT_BUILT);
            return null;
        }

        // Ok, everything looks good.. let's go
        String displayHome = androidSdk.hasKnownRoot() ? androidSdk.getSdkRoot() : Messages.USING_PATH();
        log(logger, Messages.USING_SDK(displayHome));
        EmulatorConfig emuConfig = EmulatorConfig.create(avdName, osVersion, screenDensity,
                screenResolution, deviceLocale, sdCardSize, wipeData, showWindow, commandLineOptions);

        return doSetUp(build, launcher, listener, androidSdk, emuConfig, expandedProperties);
    }

    @SuppressWarnings("hiding")
    private Environment doSetUp(final AbstractBuild<?, ?> build, final Launcher launcher,
            final BuildListener listener, final AndroidSdk androidSdk,
            final EmulatorConfig emuConfig, final HardwareProperty[] hardwareProperties)
                throws IOException, InterruptedException {
        final PrintStream logger = listener.getLogger();
        final boolean isUnix = launcher.isUnix();

        // First ensure that emulator exists
        final Computer computer = Computer.currentComputer();
        final boolean emulatorAlreadyExists;
        try {
            Callable<Boolean, AndroidEmulatorException> task = emuConfig.getEmulatorCreationTask(androidSdk, isUnix, listener);
            emulatorAlreadyExists = launcher.getChannel().call(task);
        } catch (EmulatorDiscoveryException ex) {
            log(logger, Messages.CANNOT_START_EMULATOR(ex.getMessage()));
            build.setResult(Result.FAILURE);
            return null;
        } catch (AndroidEmulatorException ex) {
            log(logger, Messages.COULD_NOT_CREATE_EMULATOR(ex.getMessage()));
            build.setResult(Result.NOT_BUILT);
            return null;
        }

        // Update emulator configuration
        if (!emuConfig.isNamedEmulator() && hardwareProperties.length != 0) {
            Callable<Void, IOException> task = emuConfig.getEmulatorConfigTask(hardwareProperties, isUnix, listener);
            launcher.getChannel().call(task);
        }

        // Delay start up by the configured amount of time
        final int delaySecs = getStartupDelay();
        if (delaySecs > 0) {
            log(logger, Messages.DELAYING_START_UP(delaySecs));
            Thread.sleep(delaySecs * 1000);
        }

        // Use the Port Allocator plugin to reserve the two ports we need
        final PortAllocationManager portAllocator = PortAllocationManager.getManager(computer);
        final int userPort = portAllocator.allocateRandom(build, 0);
        final int adbPort = portAllocator.allocateRandom(build, 0);

        // Compile complete command for starting emulator
        final String avdArgs = emuConfig.getCommandArguments();
        String emulatorArgs = String.format("-ports %s,%s %s", userPort, adbPort, avdArgs);
        ArgumentListBuilder emulatorCmd = Utils.getToolCommand(androidSdk, isUnix, Tool.EMULATOR, emulatorArgs);

        // Prepare to capture and log emulator standard output
        ByteArrayOutputStream emulatorOutput = new ByteArrayOutputStream();
        ForkOutputStream emulatorLogger = new ForkOutputStream(logger, emulatorOutput);

        // Start emulator process
        log(logger, Messages.STARTING_EMULATOR());
        if (emulatorAlreadyExists && emuConfig.shouldWipeData()) {
            log(logger, Messages.ERASING_EXISTING_EMULATOR_DATA());
        }
        final long bootTime = System.currentTimeMillis();
        final EnvVars buildEnvironment = build.getEnvironment(TaskListener.NULL);
        final ProcStarter procStarter = launcher.launch().stdout(emulatorLogger).stderr(logger);
        final Proc emulatorProcess = procStarter.envs(buildEnvironment).cmds(emulatorCmd).start();

        // Give the emulator process a chance to initialise
        Thread.sleep(5 * 1000);

        // Check whether a failure was reported on stdout
        if (emulatorOutput.toString().contains("image is used by another emulator")) {
            log(logger, Messages.EMULATOR_ALREADY_IN_USE(emuConfig.getAvdName()));
            return null;
        }

        // Wait for TCP socket to become available
        boolean socket = waitForSocket(launcher, adbPort, ADB_CONNECT_TIMEOUT_MS);
        if (!socket || !emulatorProcess.isAlive()) {
            log(logger, Messages.EMULATOR_DID_NOT_START());
            build.setResult(Result.NOT_BUILT);
            cleanUp(logger, launcher, androidSdk, portAllocator, emulatorProcess, adbPort, userPort);
            return null;
        }

        // Notify adb of our existence
        final String adbConnectArgs = "connect localhost:"+ adbPort;
        ArgumentListBuilder adbConnectCmd = Utils.getToolCommand(androidSdk, isUnix, Tool.ADB, adbConnectArgs);
        int result = procStarter.cmds(adbConnectCmd).stdout(new NullStream()).start().join();
        if (result != 0) { // adb currently only ever returns 0!
            log(logger, Messages.CANNOT_CONNECT_TO_EMULATOR());
            build.setResult(Result.NOT_BUILT);
            cleanUp(logger, launcher, androidSdk, portAllocator, emulatorProcess, adbPort, userPort);
            return null;
        }

        // Start dumping logs to disk
        final File artifactsDir = build.getArtifactsDir();
        final FilePath logcatFile = build.getWorkspace().createTempFile("logcat_", ".log");
        final OutputStream logcatStream = logcatFile.write();
        final String logcatArgs = "-s localhost:"+ adbPort +" logcat -v time";
        ArgumentListBuilder logcatCmd = Utils.getToolCommand(androidSdk, isUnix, Tool.ADB, logcatArgs);
        final Proc logWriter = procStarter.cmds(logcatCmd).stdout(logcatStream).stderr(new NullStream()).start();

        // Monitor device for boot completion signal
        log(logger, Messages.WAITING_FOR_BOOT_COMPLETION());
        int bootTimeout = BOOT_COMPLETE_TIMEOUT_MS;
        if (!emulatorAlreadyExists || emuConfig.shouldWipeData()) {
            bootTimeout *= 4;
        }
        boolean bootSucceeded = waitForBootCompletion(logger, launcher, androidSdk, emulatorProcess, adbPort, bootTimeout);
        if (!bootSucceeded) {
            if ((System.currentTimeMillis() - bootTime) < bootTimeout) {
                log(logger, Messages.EMULATOR_STOPPED_DURING_BOOT());
            } else {
                log(logger, Messages.BOOT_COMPLETION_TIMED_OUT(bootTimeout / 1000));
            }
            build.setResult(Result.NOT_BUILT);
            cleanUp(logger, launcher, androidSdk, portAllocator, emulatorProcess,
                    adbPort, userPort, logWriter, logcatFile, logcatStream, artifactsDir);
            return null;
        }

        // Unlock emulator by pressing the Menu key once, if required.
        // Upon first boot (and when the data is wiped) the emulator is already unlocked
        if (emulatorAlreadyExists && !wipeData) {
            // Even if the emulator has started, we generally need to wait longer before the lock
            // screen is up and ready to accept key presses.
            // The delay here is a function of boot time, i.e. relative to the slowness of the host
            final long bootDuration = System.currentTimeMillis() - bootTime;
            Thread.sleep(bootDuration / 4);

            log(logger, Messages.UNLOCKING_SCREEN());
            final String keyEventArgs = String.format("-s localhost:%d shell input keyevent %%d", adbPort);
            final String menuArgs = String.format(keyEventArgs, 82);
            ArgumentListBuilder menuCmd = Utils.getToolCommand(androidSdk, isUnix, Tool.ADB, menuArgs);
            procStarter.cmds(menuCmd).start().join();

            // If a named emulator already existed, it may not have been booted yet, so the screen
            // wouldn't be locked.  In this case, after pressing Menu, we press Back to compensate
            if (emuConfig.isNamedEmulator()) {
                final String backArgs = String.format(keyEventArgs, 4);
                ArgumentListBuilder backCmd = Utils.getToolCommand(androidSdk, isUnix, Tool.ADB, backArgs);
                procStarter.cmds(backCmd).start().join();
            }
        }

        // Done!
        final long bootCompleteTime = System.currentTimeMillis();
        log(logger, Messages.EMULATOR_IS_READY((bootCompleteTime - bootTime) / 1000));

        // Return wrapped environment
        return new Environment() {
            @Override
            public void buildEnvVars(Map<String, String> env) {
                env.put("ANDROID_AVD_DEVICE", "localhost:"+ adbPort);
                env.put("ANDROID_AVD_ADB_PORT", Integer.toString(adbPort));
                env.put("ANDROID_AVD_USER_PORT", Integer.toString(userPort));
                env.put("ANDROID_AVD_NAME", emuConfig.getAvdName());
                if (!emuConfig.isNamedEmulator()) {
                    env.put("ANDROID_AVD_OS", emuConfig.getOsVersion().toString());
                    env.put("ANDROID_AVD_DENSITY", emuConfig.getScreenDensity().toString());
                    env.put("ANDROID_AVD_RESOLUTION", emuConfig.getScreenResolution().toString());
                    env.put("ANDROID_AVD_SKIN", emuConfig.getScreenResolution().getSkinName());
                    env.put("ANDROID_AVD_LOCALE", emuConfig.getDeviceLocale());
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean tearDown(AbstractBuild build, BuildListener listener)
                    throws IOException, InterruptedException {
                cleanUp(logger, launcher, androidSdk, portAllocator, emulatorProcess,
                        adbPort, userPort, logWriter, logcatFile, logcatStream, artifactsDir);

                return true;
            }
        };
    }

    /** Helper method for writing to the build log in a consistent manner. */
    synchronized static void log(final PrintStream logger, final String message) {
        log(logger, message, false);
    }

    /** Helper method for writing to the build log in a consistent manner. */
    synchronized static void log(final PrintStream logger, String message, boolean indent) {
        if (indent) {
            message = '\t' + message.replace("\n", "\n\t");
        } else {
            logger.print("[android] ");
        }
        logger.println(message);
    }

    /**
     * Called when this wrapper needs to exit, so we need to clean up some processes etc.
     *
     * @param logger The build logger.
     * @param launcher The launcher for the remote node.
     * @param androidSdk The Android SDK being used.
     * @param portAllocator The port allocator used.
     * @param emulatorProcess The Android emulator process.
     * @param adbPort The ADB port used by the emulator.
     * @param userPort The user port used by the emulator.
     */
    private void cleanUp(PrintStream logger, Launcher launcher, AndroidSdk androidSdk,
            PortAllocationManager portAllocator, Proc emulatorProcess, int adbPort, int userPort)
                throws IOException, InterruptedException {
        cleanUp(logger, launcher, androidSdk, portAllocator, emulatorProcess, adbPort, userPort, null, null, null, null);
    }

    /**
     * Called when this wrapper needs to exit, so we need to clean up some processes etc.
     *
     * @param logger The build logger.
     * @param launcher The launcher for the remote node.
     * @param androidSdk The Android SDK being used.
     * @param portAllocator The port allocator used.
     * @param emulatorProcess The Android emulator process.
     * @param adbPort The ADB port used by the emulator.
     * @param userPort The user port used by the emulator.
     * @param logcatProcess The adb logcat process.
     * @param logcatFile The file the logcat output is being written to.
     * @param logcatStream The stream the logcat output is being written to.
     * @param artifactsDir The directory where build artifacts should go.
     */
    private void cleanUp(PrintStream logger, Launcher launcher, AndroidSdk androidSdk,
            PortAllocationManager portAllocator, Proc emulatorProcess, int adbPort,
            int userPort, Proc logcatProcess, FilePath logcatFile,
            OutputStream logcatStream, File artifactsDir)
                throws IOException, InterruptedException {
        // FIXME: Sometimes on Windows neither the emulator.exe nor the adb.exe processes die.
        //        Launcher.kill(EnvVars) does not appear to help either.
        //        This is (a) inconsistent; (b) very annoying.

        // Disconnect emulator from adb
        final String args = "disconnect localhost:"+ adbPort;
        ArgumentListBuilder adbDisconnectCmd = Utils.getToolCommand(androidSdk, launcher.isUnix(), Tool.ADB, args);
        final ProcStarter procStarter = launcher.launch().stderr(logger);
        procStarter.cmds(adbDisconnectCmd).stdout(new NullStream()).start().join();

        // Stop emulator process
        log(logger, Messages.STOPPING_EMULATOR());
        boolean killed = sendEmulatorCommand(launcher, logger, userPort, "kill");

        // Ensure the process is dead
        if (!killed && emulatorProcess.isAlive()) {
            emulatorProcess.kill();
        }

        // Clean up logging process
        if (logcatProcess != null) {
            logcatStream.close();
            if (killed && logcatProcess.isAlive()) {
                // This should have stopped when the emulator was,
                // but if not attempt to kill the process manually
                logcatProcess.kill();
            }

            // Archive the logs
            if (logcatFile.length() != 0) {
                log(logger, Messages.ARCHIVING_LOG());
                logcatFile.copyTo(new FilePath(artifactsDir).child("logcat.txt"));
            }
            logcatFile.delete();
        }

        // Free up the TCP ports
        portAllocator.free(adbPort);
        portAllocator.free(userPort);
    }

    /**
     * Validates this instance's configuration.
     *
     * @return A human-readable error message, or <code>null</code> if the config is valid.
     */
    @SuppressWarnings("hiding")
    private String isConfigValid(String avdName, String osVersion, String screenDensity,
            String screenResolution, String deviceLocale, String sdCardSize) {
        if (getUseNamedEmulator()) {
            ValidationResult result = descriptor.doCheckAvdName(avdName, false);
            if (result.isFatal()) {
                return result.getMessage();
            }
        } else {
            ValidationResult result = descriptor.doCheckOsVersion(osVersion, false);
            if (result.isFatal()) {
                return result.getMessage();
            }
            result = descriptor.doCheckScreenDensity(screenDensity, false);
            if (result.isFatal()) {
                return result.getMessage();
            }
            result = descriptor.doCheckScreenResolution(screenResolution, null, false);
            if (result.isFatal()) {
                return result.getMessage();
            }
            result = descriptor.doCheckDeviceLocale(deviceLocale, false);
            if (result.isFatal()) {
                return result.getMessage();
            }
            result = descriptor.doCheckSdCardSize(sdCardSize, false);
            if (result.isFatal()) {
                return result.getMessage();
            }
        }

        return null;
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
    private String discoverAndroidHome(final Launcher launcher, final EnvVars envVars,
            final String androidHome) {
        Callable<String, InterruptedException> task = new Callable<String, InterruptedException>() {
            public String call() throws InterruptedException {
                // Verify existence of provided value
                if (validateHomeDir(androidHome)) {
                    return androidHome;
                }

                // Check for common environment variables
                String[] keys = { "ANDROID_SDK_ROOT", "ANDROID_SDK_HOME",
                                  "ANDROID_HOME", "ANDROID_SDK" };
                for (String key : keys) {
                    String home = envVars.get(key);
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
                return !descriptor.doCheckAndroidHome(new File(dir), false).isFatal();
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
    private AndroidSdk getAndroidSdk(Launcher launcher, final String androidHome) {
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
                }

                // Create SDK instance with what we know so far
                AndroidSdk sdk = new AndroidSdk(sdkRoot);

                // Determine whether SDK has platform tools installed
                File toolsDirectory = new File(sdkRoot, "platform-tools");
                sdk.setUsesPlatformTools(toolsDirectory.isDirectory());

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
     * Detects the root directory of an SDK installation based on the Android tools on the PATH.
     *
     * @param isUnix Whether the system where this command should run is sane.
     * @return The root directory of an Android SDK, or {@code null} if none could be determined.
     */
    private String getSdkRootFromPath(boolean isUnix) {
        // Get list of required tools when working from PATH
        Tool[] tools = { Tool.ADB, Tool.EMULATOR };

        // Examine each directory specified by the PATH environment variable.
        int toolCount = 0;
        String[] paths = System.getenv("PATH").split(File.pathSeparator);
        for (String path : paths) {
            File toolsDirectory = new File(path);
            if (toolsDirectory.isDirectory()) {
                for (Tool tool : tools) {
                    String executable = tool.getExecutable(isUnix);
                    if (new File(toolsDirectory, executable).exists()) {
                        toolCount++;
                    }
                }
            }

            // If all the tools were found in this directory, we have a winner
            if (toolCount == tools.length) {
                // Return the parent path (i.e. the SDK root)
                return toolsDirectory.getParent();
            }
        }

        return null;
    }

    /**
     * Waits for a socket on the remote machine's localhost to become available, or times out.
     *
     * @param launcher The launcher for the remote node.
     * @param port The port to try and connect to.
     * @param timeout How long to keep trying (in milliseconds) before giving up.
     * @return <code>true</code> if the socket was available, <code>false</code> if we timed-out.
     */
    private boolean waitForSocket(Launcher launcher, int port, int timeout) {
        try {
            LocalPortOpenTask task = new LocalPortOpenTask(port, timeout);
            return launcher.getChannel().call(task);
        } catch (InterruptedException ex) {
            // Ignore
        } catch (IOException e) {
            // Ignore
        }

        return false;
    }

    /**
     * Checks whether the emulator running on the given port has finished booting yet, or times out.
     *
     * @param logger The build logger.
     * @param launcher The launcher for the remote node.
     * @param androidSdk The Android SDK being used.
     * @param emulatorProcess The Android emulator process.
     * @param port The emulator's ADB port.
     * @param timeout How long to keep trying (in milliseconds) before giving up.
     * @return <code>true</code> if the emulator has booted, <code>false</code> if we timed-out.
     */
    private boolean waitForBootCompletion(final PrintStream logger, final Launcher launcher,
            final AndroidSdk androidSdk, final Proc emulatorProcess, final int port, final int timeout) {
        long start = System.currentTimeMillis();
        int sleep = timeout / (int) Math.sqrt(timeout / 1000);

        final String serialNo = "localhost:"+ port;
        final String args = "-s "+ serialNo +" shell getprop dev.bootcomplete";
        ArgumentListBuilder cmd = Utils.getToolCommand(androidSdk, launcher.isUnix(), Tool.ADB, args);

        try {
            while (System.currentTimeMillis() < start + timeout && emulatorProcess.isAlive()) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream(4);

                // Run "getprop"
                launcher.launch().cmds(cmd).stdout(stream).start().join();

                // Check output
                String result = stream.toString().trim();
                if (result.equals("1")) {
                    return true;
                }

                // Otherwise continue...
                Thread.sleep(sleep);
            }
        } catch (InterruptedException ex) {
            log(logger, Messages.INTERRUPTED_DURING_BOOT_COMPLETION());
        } catch (IOException ex) {
            log(logger, Messages.COULD_NOT_CHECK_BOOT_COMPLETION());
            ex.printStackTrace(logger);
        }

        return false;
    }

    /**
     * Sends a user command to the running emulator via its telnet interface.
     *
     * @param logger The build logger.
     * @param launcher The launcher for the remote node.
     * @param port The emulator's telnet port.
     * @param command The command to execute on the emulator's telnet interface.
     * @return Whether sending the command succeeded.
     */
    private boolean sendEmulatorCommand(final Launcher launcher, final PrintStream logger,
            final int port, final String command) {
        Callable<Boolean, IOException> task = new Callable<Boolean, IOException>() {
            @SuppressWarnings("null")
            public Boolean call() throws IOException {
                Socket socket = null;
                BufferedReader in = null;
                PrintWriter out = null;
                try {
                    socket = new Socket("127.0.0.1", port);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    if (in.readLine() == null) {
                        return false;
                    }

                    out.write(command);
                    out.write("\r\n");
                } finally {
                    try {
                        out.close();
                        in.close();
                        socket.close();
                    } catch (Exception e) {
                        // Buh
                    }
                }

                return true;
            }

            private static final long serialVersionUID = 1L;
        };

        boolean result = false;
        try {
            result = launcher.getChannel().call(task);
        } catch (Exception e) {
            log(logger, String.format("Failed to execute emulator command '%s': %s", command, e));
        }

        return result;
    }

    @Extension(ordinal=-100) // Negative ordinal makes us execute after other wrappers (i.e. Xvnc)
    public static final class DescriptorImpl extends BuildWrapperDescriptor implements Serializable {

        private static final long serialVersionUID = 1L;

        // From hudson.Util.VARIABLE
        private static final String VARIABLE_REGEX = "\\$([A-Za-z0-9_]+|\\{[A-Za-z0-9_]+\\}|\\$)";

        /**
         * The Android SDK home directory.  Can include variables, e.g. <tt>${ANDROID_HOME}</tt>.
         * <p>If <code>null</code>, we will just assume the required commands are on the PATH.</p>
         */
        public String androidHome;

        public DescriptorImpl() {
            super(AndroidEmulator.class);
            load();
        }

        @Override
        public String getDisplayName() {
            return Messages.JOB_DESCRIPTION();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            req.bindParameters(this, "android-emulator.");
            save();
            return true;
        }

        @Override
        public BuildWrapper newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            String avdName = null;
            String osVersion = null;
            String screenDensity = null;
            String screenResolution = null;
            String deviceLocale = null;
            String sdCardSize = null;
            List<HardwareProperty> hardware = new ArrayList<HardwareProperty>();
            boolean wipeData = false;
            boolean showWindow = true;
            String commandLineOptions = null;
            int startupDelay = 0;

            JSONObject emulatorData = formData.getJSONObject("useNamed");
            String useNamedValue = emulatorData.getString("value");
            if (Boolean.parseBoolean(useNamedValue)) {
                avdName = Util.fixEmptyAndTrim(emulatorData.getString("avdName"));
            } else {
                osVersion = Util.fixEmptyAndTrim(emulatorData.getString("osVersion"));
                screenDensity = Util.fixEmptyAndTrim(emulatorData.getString("screenDensity"));
                screenResolution = Util.fixEmptyAndTrim(emulatorData.getString("screenResolution"));
                deviceLocale = Util.fixEmptyAndTrim(emulatorData.getString("deviceLocale"));
                sdCardSize = Util.fixEmptyAndTrim(emulatorData.getString("sdCardSize"));
                if (sdCardSize != null) {
                    sdCardSize = sdCardSize.toUpperCase().replaceAll("[ B]", "");
                }
                hardware = req.bindJSONToList(HardwareProperty.class, emulatorData.get("hardwareProperties"));
            }
            wipeData = formData.getBoolean("wipeData");
            showWindow = formData.getBoolean("showWindow");
            commandLineOptions = formData.getString("commandLineOptions");
            try {
                startupDelay = Integer.parseInt(formData.getString("startupDelay"));
            } catch (NumberFormatException e) {}

            return new AndroidEmulator(avdName, osVersion, screenDensity, screenResolution,
                    deviceLocale, sdCardSize, hardware.toArray(new HardwareProperty[0]),
                    wipeData, showWindow, commandLineOptions, startupDelay);
        }

        @Override
        public String getHelpFile() {
            return "/plugin/android-emulator/help-buildConfig.html";
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        /** Used in config.jelly: Lists the OS versions available. */
        public AndroidPlatform[] getAndroidVersions() {
            return AndroidPlatform.PRESETS;
        }

        /** Used in config.jelly: Lists the screen densities available. */
        public ScreenDensity[] getDeviceDensities() {
            return ScreenDensity.PRESETS;
        }

        /** Used in config.jelly: Lists the screen resolutions available. */
        public ScreenResolution[] getDeviceResolutions() {
            return ScreenResolution.PRESETS;
        }

        /** Used in config.jelly: Lists the locales available. */
        public String[] getEmulatorLocales() {
            return Constants.EMULATOR_LOCALES;
        }

        /** Used in config.jelly: Lists common hardware properties that can be set. */
        public String[] getHardwareProperties() {
            return Constants.HARDWARE_PROPERTIES;
        }

        public FormValidation doCheckAvdName(@QueryParameter String value) {
            return doCheckAvdName(value, true).getFormValidation();
        }

        private ValidationResult doCheckAvdName(String avdName, boolean allowVariables) {
            if (avdName == null || avdName.equals("")) {
                return ValidationResult.error(Messages.AVD_NAME_REQUIRED());
            }
            String regex = Constants.REGEX_AVD_NAME;
            if (allowVariables) {
                regex = "(("+ Constants.REGEX_AVD_NAME +")*("+ VARIABLE_REGEX +")*)+";
            }
            if (!avdName.matches(regex)) {
                return ValidationResult.error(Messages.INVALID_AVD_NAME());
            }

            return ValidationResult.ok();

        }

        public FormValidation doCheckOsVersion(@QueryParameter String value) {
            return doCheckOsVersion(value, true).getFormValidation();
        }

        private ValidationResult doCheckOsVersion(String osVersion, boolean allowVariables) {
            if (osVersion == null || osVersion.equals("")) {
                return ValidationResult.error(Messages.OS_VERSION_REQUIRED());
            }
            if (!allowVariables && osVersion.matches(VARIABLE_REGEX)) {
                return ValidationResult.error(Messages.INVALID_OS_VERSION());
            }

            return ValidationResult.ok();
        }

        public FormValidation doCheckScreenDensity(@QueryParameter String value) {
            return doCheckScreenDensity(value, true).getFormValidation();
        }

        private ValidationResult doCheckScreenDensity(String density, boolean allowVariables) {
            if (density == null || density.equals("")) {
                return ValidationResult.error(Messages.SCREEN_DENSITY_REQUIRED());
            }
            String regex = Constants.REGEX_SCREEN_DENSITY;
            if (allowVariables) {
                regex += "|"+ VARIABLE_REGEX;
            }
            if (!density.matches(regex)) {
                return ValidationResult.error(Messages.SCREEN_DENSITY_NOT_NUMERIC());
            }

            return ValidationResult.ok();
        }

        public FormValidation doCheckScreenResolution(@QueryParameter String value,
                @QueryParameter String density) {
            return doCheckScreenResolution(value, density, true).getFormValidation();
        }

        private ValidationResult doCheckScreenResolution(String resolution, String density,
                boolean allowVariables) {
            if (resolution == null || resolution.equals("")) {
                return ValidationResult.error(Messages.SCREEN_RESOLUTION_REQUIRED());
            }
            String regex = Constants.REGEX_SCREEN_RESOLUTION_FULL;
            if (allowVariables) {
                regex += "|"+ VARIABLE_REGEX;
            }
            if (!resolution.matches(regex)) {
                return ValidationResult.error(Messages.INVALID_RESOLUTION_FORMAT());
            }

            // Check for shenanigans
            ScreenResolution resolutionValue = ScreenResolution.valueOf(resolution);
            ScreenDensity densityValue = ScreenDensity.valueOf(density);
            if (resolutionValue != null && densityValue != null
                    && !resolutionValue.isCustomResolution() && !densityValue.isCustomDensity()) {
                boolean densityFound = false;
                for (ScreenDensity okDensity : resolutionValue.getApplicableDensities()) {
                    if (okDensity.equals(densityValue)) {
                        densityFound = true;
                        break;
                    }
                }
                if (!densityFound) {
                    return ValidationResult.warning(Messages.SUSPECT_RESOLUTION(resolution, densityValue));
                }
            }

            return ValidationResult.ok();
        }

        public FormValidation doCheckDeviceLocale(@QueryParameter String value) {
            return doCheckDeviceLocale(value, true).getFormValidation();
        }

        private ValidationResult doCheckDeviceLocale(String locale, boolean allowVariables) {
            if (locale == null || locale.equals("")) {
                return ValidationResult.warning(Messages.DEFAULT_LOCALE_WARNING(Constants.DEFAULT_LOCALE));
            }
            String regex = Constants.REGEX_LOCALE;
            if (allowVariables) {
                regex += "|"+ VARIABLE_REGEX;
            }
            if (!locale.matches(regex)) {
                return ValidationResult.error(Messages.LOCALE_FORMAT_WARNING());
            }

            return ValidationResult.ok();
        }

        public FormValidation doCheckSdCardSize(@QueryParameter String value) {
            return doCheckSdCardSize(value, true).getFormValidation();
        }

        private ValidationResult doCheckSdCardSize(String sdCardSize, boolean allowVariables) {
            if (sdCardSize == null || sdCardSize.equals("")) {
                // No value, no SD card is created
                return ValidationResult.ok();
            }
            String regex = Constants.REGEX_SD_CARD_SIZE;
            if (allowVariables) {
                regex += "|"+ VARIABLE_REGEX;
            }
            if (!sdCardSize.matches(regex)) {
                return ValidationResult.error(Messages.INVALID_SD_CARD_SIZE());
            }

            // Validate size of SD card: New AVD requires at least 9MB
            Matcher matcher = Pattern.compile(Constants.REGEX_SD_CARD_SIZE).matcher(sdCardSize);
            if (matcher.matches()) {
                long bytes = Long.parseLong(matcher.group(1));
                if (matcher.group(2).toUpperCase().equals("M")) {
                    // Convert to KB
                    bytes *= 1024;
                }
                bytes *= 1024L;
                if (bytes < (9 * 1024 * 1024)) {
                    return ValidationResult.error(Messages.SD_CARD_SIZE_TOO_SMALL());
                }
            }

            return ValidationResult.ok();
        }

        public FormValidation doCheckAndroidHome(@QueryParameter File value) {
            return doCheckAndroidHome(value, true).getFormValidation();
        }

        private ValidationResult doCheckAndroidHome(File sdkRoot, boolean fromWebConfig) {
            // This can be used to check the existence of a file on the server, so needs to be protected
            if (fromWebConfig && !Hudson.getInstance().hasPermission(Hudson.ADMINISTER)) {
                return ValidationResult.ok();
            }

            // Check the utter basics
            if (fromWebConfig && (sdkRoot == null || sdkRoot.getPath().equals(""))) {
                return ValidationResult.ok();
            }
            if (!sdkRoot.isDirectory()) {
                if (fromWebConfig && sdkRoot.getPath().matches(".*("+ VARIABLE_REGEX +").*")) {
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
            if (toolsFound < Tool.values().length) {
                return ValidationResult.errorWithMarkup(Messages.REQUIRED_SDK_TOOLS_NOT_FOUND());
            }

            // Give the user a nice warning (not error) if they've not downloaded any platforms yet
            File platformsDir = new File(sdkRoot, "platforms");
            if (platformsDir.list().length == 0) {
                return ValidationResult.warning(Messages.SDK_PLATFORMS_EMPTY());
            }

            return ValidationResult.ok();
        }
    }

    /** Task that will block until it can either connect to a port on localhost, or it times-out. */
    private static final class LocalPortOpenTask implements Callable<Boolean, InterruptedException> {

        private static final long serialVersionUID = 1L;

        private final int port;
        private final int timeout;

        /**
         * @param port The local TCP port to attempt to connect to.
         * @param timeout How long to keep trying (in milliseconds) before giving up.
         */
        @SuppressWarnings("hiding")
        public LocalPortOpenTask(int port, int timeout) {
            this.port = port;
            this.timeout = timeout;
        }

        public Boolean call() throws InterruptedException {
            final long start = System.currentTimeMillis();

            while (System.currentTimeMillis() < start + timeout) {
                try {
                    Socket socket = new Socket("127.0.0.1", port);
                    socket.getOutputStream();
                    socket.close();
                    return true;
                } catch (IOException ex) {
                    // Ignore
                }

                Thread.sleep(1000);
            }

            return false;
        }
    }

    @ExportedBean
    public static final class HardwareProperty implements Serializable {

        private static final long serialVersionUID = 1L;

        @Exported
        public final String key;

        @Exported
        public final String value;

        @DataBoundConstructor
        @SuppressWarnings("hiding")
        public HardwareProperty(String key, String value) {
            this.key = key;
            this.value = value;
        }

    }

}
