package hudson.plugins.android_emulator;


import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.Proc;
import hudson.Util;
import hudson.matrix.Combination;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.sdk.Tool;
import hudson.plugins.android_emulator.util.Utils;
import hudson.plugins.android_emulator.util.ValidationResult;
import hudson.remoting.Callable;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.ArgumentListBuilder;
import hudson.util.ForkOutputStream;
import hudson.util.FormValidation;
import hudson.util.NullStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONObject;

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
    private static final int BOOT_COMPLETE_TIMEOUT_MS = 180 * 1000;

    /** Interval during which killing a process should complete. */
    private static final int KILL_PROCESS_TIMEOUT_MS = 10 * 1000;

    private DescriptorImpl descriptor;

    // Config properties: AVD name
    @Exported public final String avdName;

    // Custom emulator properties
    @Exported public final String osVersion;
    @Exported public final String screenDensity;
    @Exported public final String screenResolution;
    @Exported public final String deviceLocale;
    @Exported public final String targetAbi;
    @Exported public final String sdCardSize;
    @Exported public final HardwareProperty[] hardwareProperties;

    // Common properties
    @Exported public final boolean wipeData;
    @Exported public final boolean showWindow;
    @Exported public final boolean useSnapshots;

    // Advanced properties
    @Exported public final boolean deleteAfterBuild;
    @Exported public final int startupDelay;
    @Exported public final String commandLineOptions;
    @Exported public final String executable;


    @DataBoundConstructor
    public AndroidEmulator(String avdName, String osVersion, String screenDensity,
            String screenResolution, String deviceLocale, String sdCardSize,
            HardwareProperty[] hardwareProperties, boolean wipeData, boolean showWindow,
            boolean useSnapshots, boolean deleteAfterBuild, int startupDelay,
            String commandLineOptions, String targetAbi, String executable) {
        this.avdName = avdName;
        this.osVersion = osVersion;
        this.screenDensity = screenDensity;
        this.screenResolution = screenResolution;
        this.deviceLocale = deviceLocale;
        this.sdCardSize = sdCardSize;
        this.hardwareProperties = hardwareProperties;
        this.wipeData = wipeData;
        this.showWindow = showWindow;
        this.useSnapshots = useSnapshots;
        this.deleteAfterBuild = deleteAfterBuild;
        this.executable = executable;
        this.startupDelay = Math.abs(startupDelay);
        this.commandLineOptions = commandLineOptions;
        this.targetAbi = targetAbi;
    }

    public boolean getUseNamedEmulator() {
        return avdName != null;
    }

    /**
     * A hash representing the variables that are used to determine which emulator configuration
     * should be started to fulfil the job configuration.
     *
     * @param node The Node on which the emulator would be run.
     * @return A hash representing the emulator configuration for this instance.
     */
    public String getConfigHash(Node node) {
        return getConfigHash(node, null);
    }

    /**
     * A hash representing the variables that are used to determine which emulator configuration
     * should be started to fulfil the job configuration.
     *
     * @param node The Node on which the emulator would be run.
     * @param combination The matrix combination values used to expand emulator config variables.
     * @return A hash representing the emulator configuration for this instance.
     */
    public String getConfigHash(Node node, Combination combination) {
        EnvVars envVars;
        try {
            envVars = node.toComputer().getEnvironment();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        // Expand variables using the node's environment and the matrix properties, if any
        String avdName = Utils.expandVariables(envVars, combination, this.avdName);
        String osVersion = Utils.expandVariables(envVars, combination, this.osVersion);
        String screenDensity = Utils.expandVariables(envVars, combination, this.screenDensity);
        String screenResolution = Utils.expandVariables(envVars, combination, this.screenResolution);
        String deviceLocale = Utils.expandVariables(envVars, combination, this.deviceLocale);
        String targetAbi = Utils.expandVariables(envVars, combination, this.targetAbi);

        return EmulatorConfig.getAvdName(avdName, osVersion, screenDensity, screenResolution,
                deviceLocale, targetAbi);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Environment setUp(AbstractBuild build, final Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {
        final PrintStream logger = listener.getLogger();
        if (descriptor == null) {
            descriptor = Hudson.getInstance().getDescriptorByType(DescriptorImpl.class);
        }

        // Substitute environment and build variables into config
        final EnvVars envVars = Utils.getEnvironment(build, listener);
        final Map<String, String> buildVars = build.getBuildVariables();

        // Device properties
        String avdName = Utils.expandVariables(envVars, buildVars, this.avdName);
        String osVersion = Utils.expandVariables(envVars, buildVars, this.osVersion);
        String screenDensity = Utils.expandVariables(envVars, buildVars, this.screenDensity);
        String screenResolution = Utils.expandVariables(envVars, buildVars, this.screenResolution);
        String deviceLocale = Utils.expandVariables(envVars, buildVars, this.deviceLocale);
        String sdCardSize = Utils.expandVariables(envVars, buildVars, this.sdCardSize);
        if (sdCardSize != null) {
            sdCardSize = sdCardSize.toUpperCase().replaceAll("[ B]", "");
        }
        String targetAbi = Utils.expandVariables(envVars, buildVars, this.targetAbi);

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
        Node node = Computer.currentComputer().getNode();
        String androidHome = Utils.expandVariables(envVars, buildVars, descriptor.androidHome);
        androidHome = Utils.discoverAndroidHome(launcher, node, envVars, androidHome);

        // Despite the nice inline checks and warnings when the user is editing the config,
        // these are not binding, so the user may have saved invalid configuration.
        // Here we check whether or not it's worth proceeding based on the saved values.
        // As config variables aren't yet expanded, this check can't catch all possible errors.
        String configError = isConfigValid(avdName, osVersion, screenDensity, screenResolution,
                deviceLocale, sdCardSize);
        if (configError != null) {
            log(logger, Messages.ERROR_MISCONFIGURED(configError));
            build.setResult(Result.NOT_BUILT);
            return null;
        }

        // Build emulator config, ensuring that variables expand to valid SDK values
        EmulatorConfig emuConfig;
        final String androidSdkHome = (envVars != null && descriptor.shouldKeepInWorkspace ? envVars.get("WORKSPACE") : null);
        try {
            emuConfig = EmulatorConfig.create(avdName, osVersion, screenDensity,
                screenResolution, deviceLocale, sdCardSize, wipeData, showWindow, useSnapshots,
                commandLineOptions, targetAbi, androidSdkHome, executable);
        } catch (IllegalArgumentException e) {
            log(logger, Messages.EMULATOR_CONFIGURATION_BAD(e.getLocalizedMessage()));
            build.setResult(Result.NOT_BUILT);
            return null;
        }

        // Confirm that the required SDK tools are available
        AndroidSdk androidSdk = Utils.getAndroidSdk(launcher, androidHome, androidSdkHome);
        if (androidSdk == null) {
            if (!descriptor.shouldInstallSdk) {
                // Couldn't find an SDK, don't want to install it, give up
                log(logger, Messages.SDK_TOOLS_NOT_FOUND());
                build.setResult(Result.NOT_BUILT);
                return null;
            }

            // Ok, let's download and install the SDK
            log(logger, Messages.INSTALLING_SDK());
            try {
                androidSdk = SdkInstaller.install(launcher, listener, androidSdkHome);
            } catch (SdkInstallationException e) {
                log(logger, Messages.SDK_INSTALLATION_FAILED(), e);
                build.setResult(Result.NOT_BUILT);
                return null;
            }
        } else if (descriptor.shouldKeepInWorkspace) {
            SdkInstaller.optOutOfSdkStatistics(launcher, listener, androidSdkHome);
        }

        // Install the required SDK components for the desired platform, if necessary
        if (descriptor.shouldInstallSdk) {
            SdkInstaller.installDependencies(logger, launcher, androidSdk, emuConfig);
        }

        // Ok, everything looks good.. let's go
        String displayHome = androidSdk.hasKnownRoot() ? androidSdk.getSdkRoot() : Messages.USING_PATH();
        log(logger, Messages.USING_SDK(displayHome));

        return doSetUp(build, launcher, listener, androidSdk, emuConfig, expandedProperties);
    }

    private Environment doSetUp(final AbstractBuild<?, ?> build, final Launcher launcher,
            final BuildListener listener, final AndroidSdk androidSdk,
            final EmulatorConfig emuConfig, final HardwareProperty[] hardwareProperties)
                throws IOException, InterruptedException {
        final PrintStream logger = listener.getLogger();

        // First ensure that emulator exists
        final boolean emulatorAlreadyExists;
        try {
            Callable<Boolean, AndroidEmulatorException> task = emuConfig.getEmulatorCreationTask(androidSdk, listener);
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

        // Update emulator configuration with desired hardware properties
        if (!emuConfig.isNamedEmulator() && hardwareProperties.length != 0) {
            Callable<Void, IOException> task = emuConfig.getEmulatorConfigTask(hardwareProperties, listener);
            launcher.getChannel().call(task);
        }

        // Delay start up by the configured amount of time
        final int delaySecs = startupDelay;
        if (delaySecs > 0) {
            log(logger, Messages.DELAYING_START_UP(delaySecs));
            Thread.sleep(delaySecs * 1000);
        }

        EmulatorPortConfig portConfig = new EmulatorPortConfig(descriptor.useRandomPortForAdbServer,
                                                                    descriptor.adbServerPort,
                                                                    descriptor.manageAdbServer);
        final AndroidEmulatorContext emu = new AndroidEmulatorContext(build, launcher, listener, androidSdk,
                                                                        portConfig);

        // We manually start the adb-server so that later commands will not have to start it,
        // allowing them to complete faster.
        Proc adbStart = emu.getToolProcStarter(Tool.ADB, "start-server").stdout(logger).start();

        // Determine whether we need to create the first snapshot
        final SnapshotState snapshotState;
        if (useSnapshots && androidSdk.supportsSnapshots()) {
            boolean hasSnapshot = emuConfig.hasExistingSnapshot(launcher, androidSdk);
            if (hasSnapshot) {
                // Boot from the existing "jenkins" snapshot
                snapshotState = SnapshotState.BOOT;
            } else {
                // Create an initial "jenkins" snapshot...
                snapshotState = SnapshotState.INITIALISE;
                // ..with a clean start
                emuConfig.setShouldWipeData();
            }
        } else {
            // If snapshots are disabled or not supported, there's nothing to do
            snapshotState = SnapshotState.NONE;
        }

        // Compile complete command for starting emulator
        final String emulatorArgs = emuConfig.getCommandArguments(snapshotState,
                androidSdk.supportsSnapshots(), emu.userPort(), emu.adbPort());

        // Start emulator process
        if (snapshotState == SnapshotState.BOOT) {
            log(logger, Messages.STARTING_EMULATOR_FROM_SNAPSHOT());
        } else if (snapshotState == SnapshotState.INITIALISE) {
            log(logger, Messages.STARTING_EMULATOR_SNAPSHOT_INIT());
        } else {
            log(logger, Messages.STARTING_EMULATOR());
        }
        if (emulatorAlreadyExists && emuConfig.shouldWipeData()) {
            log(logger, Messages.ERASING_EXISTING_EMULATOR_DATA());
        }
        final long bootTime = System.currentTimeMillis();

        // Prepare to capture and log emulator standard output
        ByteArrayOutputStream emulatorOutput = new ByteArrayOutputStream();
        ForkOutputStream emulatorLogger = new ForkOutputStream(logger, emulatorOutput);

        Proc emulatorProcess = null;
        if (!descriptor.shouldKeepInWorkspace) {
            emulatorProcess = emu.getToolProcStarter(emuConfig.getExecutable(), emulatorArgs).stdout(emulatorLogger).start();
        } else {
            emulatorProcess = emu.getToolProcStarter(emuConfig.getExecutable(), emulatorArgs).pwd(build.getWorkspace()).stdout(emulatorLogger).start();
        }
        emu.setProcess(emulatorProcess);

        // Give the emulator process a chance to initialise
        Thread.sleep(5 * 1000);
        adbStart.joinWithTimeout(5L, TimeUnit.SECONDS, listener);

        // Check whether a failure was reported on stdout
        if (emulatorOutput.toString().contains("image is used by another emulator")) {
            log(logger, Messages.EMULATOR_ALREADY_IN_USE(emuConfig.getAvdName()));
            return null;
        }

        // Wait for TCP socket to become available
        boolean socket = waitForSocket(launcher, emu.adbPort(), ADB_CONNECT_TIMEOUT_MS);
        if (!socket) {
            log(logger, Messages.EMULATOR_DID_NOT_START());
            build.setResult(Result.NOT_BUILT);
            cleanUp(emuConfig, emu);
            return null;
        }

        // As of SDK Tools r12, "emulator" is no longer the main process; it just starts a certain
        // child process depending on the AVD architecture.  Therefore on Windows, checking the
        // status of this original process will not work, as it ends after it has started the child.
        //
        // With the adb socket open we know the correct process is running, so we set this flag to
        // indicate that any methods wanting to check the "emulator" process state should ignore it.
        boolean ignoreProcess = !launcher.isUnix() && androidSdk.getSdkToolsVersion() >= 12;

        // Notify adb of our existence
        int result = emu.getToolProcStarter(Tool.ADB, "connect " + emu.serial()).join();
        if (result != 0) { // adb currently only ever returns 0!
            log(logger, Messages.CANNOT_CONNECT_TO_EMULATOR());
            build.setResult(Result.NOT_BUILT);
            cleanUp(emuConfig, emu);
            return null;
        }

        // Monitor device for boot completion signal
        log(logger, Messages.WAITING_FOR_BOOT_COMPLETION());
        int bootTimeout = BOOT_COMPLETE_TIMEOUT_MS;
        if (!emulatorAlreadyExists || emuConfig.shouldWipeData() || snapshotState == SnapshotState.INITIALISE) {
            bootTimeout *= 4;
        }
        boolean bootSucceeded = waitForBootCompletion(ignoreProcess, bootTimeout, emu);
        if (!bootSucceeded) {
            if ((System.currentTimeMillis() - bootTime) < bootTimeout) {
                log(logger, Messages.EMULATOR_STOPPED_DURING_BOOT());
            } else {
                log(logger, Messages.BOOT_COMPLETION_TIMED_OUT(bootTimeout / 1000));
            }
            build.setResult(Result.NOT_BUILT);
            cleanUp(emuConfig, emu);
            return null;
        }

        // Start dumping logcat to temporary file
        final File artifactsDir = build.getArtifactsDir();
        final FilePath logcatFile = build.getWorkspace().createTextTempFile("logcat_", ".log", "", false);
        final OutputStream logcatStream = logcatFile.write();
        final String logcatArgs = String.format("-s %s logcat -v time", emu.serial());
        final Proc logWriter = emu.getToolProcStarter(Tool.ADB, logcatArgs).stdout(logcatStream).stderr(new NullStream()).start();

        // Unlock emulator by pressing the Menu key once, if required.
        // Upon first boot (and when the data is wiped) the emulator is already unlocked
        final long bootDuration = System.currentTimeMillis() - bootTime;
        if (emulatorAlreadyExists && !wipeData && snapshotState != SnapshotState.BOOT) {
            // Even if the emulator has started, we generally need to wait longer before the lock
            // screen is up and ready to accept key presses.
            // The delay here is a function of boot time, i.e. relative to the slowness of the host
            Thread.sleep(bootDuration / 4);

            // Make sure we're still connected
            connectEmulator(emu);

            log(logger, Messages.UNLOCKING_SCREEN());
            final String keyEventArgs = String.format("-s %s shell input keyevent %%d", emu.serial());
            final String menuArgs = String.format(keyEventArgs, 82);
            ArgumentListBuilder menuCmd = emu.getToolCommand(Tool.ADB, menuArgs);
            emu.getProcStarter(menuCmd).join();

            // If a named emulator already existed, it may not have been booted yet, so the screen
            // wouldn't be locked.  Similarly, an non-named emulator may have already booted the
            // first time without us knowing.  In both cases, we press Back after Menu to compensate
            final String backArgs = String.format(keyEventArgs, 4);
            ArgumentListBuilder backCmd = emu.getToolCommand(Tool.ADB, backArgs);
            emu.getProcStarter(backCmd).join();
        }

        // Initialise snapshot image, if required
        if (snapshotState == SnapshotState.INITIALISE) {
            // In order to create a clean initial snapshot, give the system some more time to settle
            log(logger, Messages.WAITING_INITIAL_SNAPSHOT());
            Thread.sleep((long) (bootDuration * 0.8));

            // Make sure we're still connected
            connectEmulator(emu);

            // Clear main log before creating snapshot
            final String clearArgs = String.format("-s %s logcat -c", emu.serial());
            ArgumentListBuilder adbCmd = emu.getToolCommand(Tool.ADB, clearArgs);
            emu.getProcStarter(adbCmd).join();
            final String msg = Messages.LOG_CREATING_SNAPSHOT();
            final String logArgs = String.format("-s %s shell log -p v -t Jenkins '%s'", emu.serial(), msg);
            adbCmd = emu.getToolCommand(Tool.ADB, logArgs);
            emu.getProcStarter(adbCmd).join();

            // Pause execution of the emulator
            boolean stopped = emu.sendCommand("avd stop");
            if (stopped) {
                // Attempt snapshot generation
                log(logger, Messages.EMULATOR_PAUSED_SNAPSHOT());
                int creationTimeout = AndroidEmulatorContext.EMULATOR_COMMAND_TIMEOUT_MS * 4;
                boolean success = emu.sendCommand("avd snapshot save "+ Constants.SNAPSHOT_NAME, creationTimeout);
                if (!success) {
                    log(logger, Messages.SNAPSHOT_CREATION_FAILED());
                }

                // Restart emulator execution
                boolean restarted = emu.sendCommand("avd start");
                if (!restarted) {
                    log(logger, Messages.EMULATOR_RESUME_FAILED());
                    cleanUp(emuConfig, emu, logWriter, logcatFile, logcatStream, artifactsDir);
                }
            } else {
                log(logger, Messages.SNAPSHOT_CREATION_FAILED());
            }
        }

        // Make sure we're still connected
        connectEmulator(emu);

        // Done!
        final long bootCompleteTime = System.currentTimeMillis();
        log(logger, Messages.EMULATOR_IS_READY((bootCompleteTime - bootTime) / 1000));

        // Return wrapped environment
        return new Environment() {
            @Override
            public void buildEnvVars(Map<String, String> env) {
                env.put("ANDROID_SERIAL", emu.serial());
                env.put("ANDROID_AVD_DEVICE", emu.serial());
                env.put("ANDROID_AVD_ADB_PORT", Integer.toString(emu.adbPort()));
                env.put("ANDROID_AVD_USER_PORT", Integer.toString(emu.userPort()));
                env.put("ANDROID_AVD_NAME", emuConfig.getAvdName());
                env.put("ANDROID_ADB_SERVER_PORT", Integer.toString(emu.adbServerPort()));
                env.put("ANDROID_TMP_LOGCAT_FILE", logcatFile.getRemote());
                if (!emuConfig.isNamedEmulator()) {
                    env.put("ANDROID_AVD_OS", emuConfig.getOsVersion().toString());
                    env.put("ANDROID_AVD_DENSITY", emuConfig.getScreenDensity().toString());
                    env.put("ANDROID_AVD_RESOLUTION", emuConfig.getScreenResolution().toString());
                    env.put("ANDROID_AVD_SKIN", emuConfig.getScreenResolution().getSkinName());
                    env.put("ANDROID_AVD_LOCALE", emuConfig.getDeviceLocale());
                }
                if (androidSdk.hasKnownRoot()) {
                    env.put("JENKINS_ANDROID_HOME", androidSdk.getSdkRoot());
                    env.put("ANDROID_HOME", androidSdk.getSdkRoot());
                }
            }

            @Override
            @SuppressWarnings("rawtypes")
            public boolean tearDown(AbstractBuild build, BuildListener listener)
                    throws IOException, InterruptedException {
                cleanUp(emuConfig, emu, logWriter, logcatFile, logcatStream, artifactsDir);

                return true;
            }
        };
    }

    private static void connectEmulator(AndroidEmulatorContext emu)
            throws IOException, InterruptedException {
        ArgumentListBuilder adbConnectCmd = emu.getToolCommand(Tool.ADB, "connect " + emu.serial());
        emu.getProcStarter(adbConnectCmd).start().joinWithTimeout(5L, TimeUnit.SECONDS, emu.launcher().getListener());
    }

    private static void disconnectEmulator(AndroidEmulatorContext emu)
            throws IOException, InterruptedException {
        final String args = "disconnect "+ emu.serial();
        ArgumentListBuilder adbDisconnectCmd = emu.getToolCommand(Tool.ADB, args);
        emu.getProcStarter(adbDisconnectCmd).start().joinWithTimeout(5L, TimeUnit.SECONDS, emu.launcher().getListener());
    }


    /** Helper method for writing to the build log in a consistent manner. */
    public synchronized static void log(final PrintStream logger, final String message) {
        log(logger, message, false);
    }

    /** Helper method for writing to the build log in a consistent manner. */
    public synchronized static void log(final PrintStream logger, final String message, final Throwable t) {
        log(logger, message, false);
        StringWriter s = new StringWriter();
        t.printStackTrace(new PrintWriter(s));
        log(logger, s.toString(), false);
    }

    /** Helper method for writing to the build log in a consistent manner. */
    synchronized static void log(final PrintStream logger, String message, boolean indent) {
        if (indent) {
            message = '\t' + message.replace("\n", "\n\t");
        } else if (message.length() > 0) {
            logger.print("[android] ");
        }
        logger.println(message);
    }

    /**
     * Called when this wrapper needs to exit, so we need to clean up some processes etc.
     * @param emulatorConfig The emulator being run.
     * @param emu The emulator context
     */
    private void cleanUp(EmulatorConfig emulatorConfig, AndroidEmulatorContext emu) throws IOException, InterruptedException {
        cleanUp(emulatorConfig, emu, null, null, null, null);
    }

    /**
     * Called when this wrapper needs to exit, so we need to clean up some processes etc.
     * @param emulatorConfig The emulator being run.
     * @param emu The emulator context
     * @param logcatProcess The adb logcat process.
     * @param logcatFile The file the logcat output is being written to.
     * @param logcatStream The stream the logcat output is being written to.
     * @param artifactsDir The directory where build artifacts should go.
     */
    private void cleanUp(EmulatorConfig emulatorConfig, AndroidEmulatorContext emu, Proc logcatProcess,
            FilePath logcatFile, OutputStream logcatStream, File artifactsDir) throws IOException, InterruptedException {
        // FIXME: Sometimes on Windows neither the emulator.exe nor the adb.exe processes die.
        //        Launcher.kill(EnvVars) does not appear to help either.
        //        This is (a) inconsistent; (b) very annoying.

        // Disconnect emulator from adb
        disconnectEmulator(emu);

        // Stop emulator process
        log(emu.logger(), Messages.STOPPING_EMULATOR());
        boolean killed = emu.sendCommand("kill");

        // Ensure the process is dead
        if (!killed && emu.process().isAlive()) {
            // Give up trying to kill it after a few seconds, in case it's deadlocked
            killed = Utils.killProcess(emu.process(), KILL_PROCESS_TIMEOUT_MS);
            if (!killed) {
                log(emu.logger(), Messages.EMULATOR_SHUTDOWN_FAILED());
            }
        }

        // Clean up logging process
        if (logcatProcess != null) {
            if (logcatProcess.isAlive()) {
                // This should have stopped when the emulator was,
                // but if not attempt to kill the process manually.
                // First, give it a final chance to finish cleanly.
                Thread.sleep(3 * 1000);
                if (logcatProcess.isAlive()) {
                    Utils.killProcess(logcatProcess, KILL_PROCESS_TIMEOUT_MS);
                }
            }
            try {
                logcatStream.close();
            } catch (Exception ignore) {}

            // Archive the logs
            if (logcatFile.length() != 0) {
                log(emu.logger(), Messages.ARCHIVING_LOG());
                logcatFile.copyTo(new FilePath(artifactsDir).child("logcat.txt"));
            }
            logcatFile.delete();
        }

        if (descriptor.useRandomPortForAdbServer || descriptor.manageAdbServer) {
            ArgumentListBuilder adbKillCmd = emu.getToolCommand(Tool.ADB, "kill-server");
            emu.getProcStarter(adbKillCmd).join();
        }

        emu.cleanUp();

        // Delete the emulator, if required
        if (deleteAfterBuild) {
            try {
                Callable<Boolean, Exception> deletionTask = emulatorConfig.getEmulatorDeletionTask(
                        emu.launcher().getListener());
                emu.launcher().getChannel().call(deletionTask);
            } catch (Exception ex) {
                log(emu.logger(), Messages.FAILED_TO_DELETE_AVD(ex.getLocalizedMessage()));
            }
        }
    }

    /**
     * Validates this instance's configuration.
     *
     * @return A human-readable error message, or <code>null</code> if the config is valid.
     */
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
            result = descriptor.doCheckScreenResolution(screenResolution, null, null, false);
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
     * @param ignoreProcess Whether to bypass checking that the process is alive (e.g. on Windows).
     * @param timeout How long to keep trying (in milliseconds) before giving up.
     * @param emu The emulator context
     * @return <code>true</code> if the emulator has booted, <code>false</code> if we timed-out.
     */
    private boolean waitForBootCompletion(final boolean ignoreProcess,
            final int timeout, AndroidEmulatorContext emu) {
        long start = System.currentTimeMillis();
        int sleep = timeout / (int) (Math.sqrt(timeout / 1000) * 2);

        final String args = String.format("-s %s shell getprop dev.bootcomplete", emu.serial());
        ArgumentListBuilder bootCheckCmd = emu.getToolCommand(Tool.ADB, args);

        try {
            final long adbTimeout = timeout / 8;
            int iterations = 0;
            while (System.currentTimeMillis() < start + timeout && (ignoreProcess || emu.process().isAlive())) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream(4);

                // Run "getprop", timing-out in case adb hangs
                Proc proc = emu.getProcStarter(bootCheckCmd).stdout(stream).start();
                int retVal = proc.joinWithTimeout(adbTimeout, TimeUnit.MILLISECONDS, emu.launcher().getListener());
                if (retVal == 0) {
                    // If boot is complete, our work here is done
                    String result = stream.toString().trim();
                    if (result.equals("1")) {
                        return true;
                    }
                }

                // Otherwise continue...

                /* Ensure the emulator is connected to adb, in case it had crashed.
                 * We also disconnect it every 3 tries, in case it's stuck in an offline state.
                 */
                if (++iterations % 3 == 0) {
                    try {
                        disconnectEmulator(emu);
                    } catch (Exception ex) { }
                }
                connectEmulator(emu);

                Thread.sleep(sleep);
            }
        } catch (InterruptedException ex) {
            log(emu.logger(), Messages.INTERRUPTED_DURING_BOOT_COMPLETION());
        } catch (IOException ex) {
            log(emu.logger(), Messages.COULD_NOT_CHECK_BOOT_COMPLETION());
            ex.printStackTrace(emu.logger());
        }

        return false;
    }

    @Extension(ordinal=-100) // Negative ordinal makes us execute after other wrappers (i.e. Xvnc)
    public static final class DescriptorImpl extends BuildWrapperDescriptor implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * The Android SDK home directory.  Can include variables, e.g. <tt>${ANDROID_HOME}</tt>.
         * <p>If <code>null</code>, we will just assume the required commands are on the PATH.</p>
         */
        public String androidHome;

        /** Whether the SDK should be automatically installed where it's not found. */
        public boolean shouldInstallSdk = true;
        /** Whether the emulators should be kept in the workspace. */
        public boolean shouldKeepInWorkspace = false;

        /** Whether a random port should be used for the adb daemon */
        public boolean useRandomPortForAdbServer = true;
        /** The port of adb daemon, if no random port is used */
        public int adbServerPort = 5037;
        /** Whether the adb daemon should still be managed by this plugin, even if the adb daemon port is fixed */
        public boolean manageAdbServer = false;

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
            androidHome = json.optString("androidHome");
            shouldInstallSdk = json.optBoolean("shouldInstallSdk", true);
            shouldKeepInWorkspace = json.optBoolean("shouldKeepInWorkspace", false);
            useRandomPortForAdbServer = json.getJSONObject("useRandomPortForAdbServer").getBoolean("value");
            adbServerPort = json.optInt("adbServerPort", 5037);
            if (adbServerPort < 0 || adbServerPort > 65535) {
                adbServerPort = 5037;
            }
            manageAdbServer = json.optBoolean("manageAdbServer", false);
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
            String targetAbi = null;
            List<HardwareProperty> hardware = new ArrayList<HardwareProperty>();
            boolean wipeData = false;
            boolean showWindow = true;
            boolean useSnapshots = true;
            boolean deleteAfterBuild = false;
            int startupDelay = 0;
            String commandLineOptions = null;
            String executable = null;

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
                hardware = req.bindJSONToList(HardwareProperty.class, emulatorData.get("hardwareProperties"));
                targetAbi = Util.fixEmptyAndTrim(emulatorData.getString("targetAbi"));
            }
            wipeData = formData.getBoolean("wipeData");
            showWindow = formData.getBoolean("showWindow");
            useSnapshots = formData.getBoolean("useSnapshots");
            deleteAfterBuild = formData.getBoolean("deleteAfterBuild");
            commandLineOptions = formData.getString("commandLineOptions");
            executable = formData.getString("executable");

            try {
                startupDelay = Integer.parseInt(formData.getString("startupDelay"));
            } catch (NumberFormatException e) {}

            return new AndroidEmulator(avdName, osVersion, screenDensity, screenResolution,
                    deviceLocale, sdCardSize, hardware.toArray(new HardwareProperty[0]), wipeData,
                    showWindow, useSnapshots, deleteAfterBuild, startupDelay, commandLineOptions,
                    targetAbi, executable);
        }

        @Override
        public String getHelpFile() {
            return Functions.getResourcePath() + "/plugin/android-emulator/help-buildConfig.html";
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

        /** Used in config.jelly: Lists common abis that can be set. */
        public String[] getTargetAbis() {
            return Constants.TARGET_ABIS;
        }

        public Tool[] getExecutables() {
            return Tool.EMULATORS;
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
                regex = "(("+ Constants.REGEX_AVD_NAME +")*("+ Constants.REGEX_VARIABLE +")*)+";
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
            if (!allowVariables && osVersion.matches(Constants.REGEX_VARIABLE)) {
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
                regex += "|"+ Constants.REGEX_VARIABLE;
            }
            if (!density.matches(regex)) {
                return ValidationResult.error(Messages.SCREEN_DENSITY_NOT_NUMERIC());
            }

            return ValidationResult.ok();
        }

        public FormValidation doCheckScreenResolution(@QueryParameter String value,
                @QueryParameter String density, @QueryParameter String osVersion) {
            return doCheckScreenResolution(value, density, osVersion, true).getFormValidation();
        }

        private ValidationResult doCheckScreenResolution(String resolution, String density,
                String osVersion, boolean allowVariables) {
            if (resolution == null || resolution.equals("")) {
                return ValidationResult.error(Messages.SCREEN_RESOLUTION_REQUIRED());
            }
            String regex = Constants.REGEX_SCREEN_RESOLUTION_FULL;
            if (allowVariables) {
                regex += "|"+ Constants.REGEX_VARIABLE;
            }
            if (!resolution.matches(regex)) {
                return ValidationResult.warning(Messages.INVALID_RESOLUTION_FORMAT());
            }

            // Warn about inconsistent WXGA skin names between Android 3.x and 4.x
            AndroidPlatform platform = AndroidPlatform.valueOf(osVersion);
            if (platform != null) {
                int sdkLevel = platform.getSdkLevel();
                if (sdkLevel >= 11 && platform.getSdkLevel() <= 13) {
                    if (resolution.equals("WXGA720") || resolution.equals("WXGA800")) {
                        String msg = Messages.SUSPECT_RESOLUTION_ANDROID_3(platform);
                        return ValidationResult.warning(msg);
                    }
                } else if (sdkLevel >= 14 && resolution.equals("WXGA")) {
                    String msg = Messages.SUSPECT_RESOLUTION_ANDROID_4(platform);
                    return ValidationResult.warning(msg);
                }
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
                regex += "|"+ Constants.REGEX_VARIABLE;
            }
            if (!locale.matches(regex)) {
                return ValidationResult.error(Messages.LOCALE_FORMAT_WARNING());
            }

            return ValidationResult.ok();
        }

        public FormValidation doCheckTargetAbi(@QueryParameter String value) {
            return checkTargetAbi(value, true).getFormValidation();
        }

        private ValidationResult checkTargetAbi(String value, boolean allowVariables) {
            if (value == null || "".equals(value.trim())) {
                return ValidationResult.ok();
            }

            if (allowVariables && value.matches(Constants.REGEX_VARIABLE)) {
                return ValidationResult.ok();
            }

            for (String s : Constants.TARGET_ABIS) {
                if (s.equals(value)) {
                    return ValidationResult.ok();
                }
            }
            return ValidationResult.error(Messages.INVALID_TARGET_ABI());
        }

        public FormValidation doCheckExecutable(@QueryParameter String value) {
            if (value == null || "".equals(value.trim())) {
                return ValidationResult.ok().getFormValidation();
            }
            for (Tool t : Tool.EMULATORS) {
                if (t.toString().equals(value)) {
                    return ValidationResult.ok().getFormValidation();
                }
            }
            return ValidationResult.error(Messages.INVALID_EXECUTABLE()).getFormValidation();
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
                regex += "|"+ Constants.REGEX_VARIABLE;
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
            return Utils.validateAndroidHome(value, true).getFormValidation();
        }

        public FormValidation doCheckAdbServerPort(@QueryParameter String value) {
            try {
                int port = Integer.parseInt(value);
                if (port < 0 || port > 65535) {
                    return ValidationResult.error("Invalid port number. Needs to be between 0 and 65535. Otherwise default port is used.").getFormValidation();
                }
            } catch (NumberFormatException e) {
                return ValidationResult.error("Please insert a number between 0 and 65535. Otherwise default port is used.").getFormValidation();
            }

            return ValidationResult.ok().getFormValidation();
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
                } catch (IOException ignore) {}

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
        public HardwareProperty(String key, String value) {
            this.key = key;
            this.value = value;
        }

    }

}
