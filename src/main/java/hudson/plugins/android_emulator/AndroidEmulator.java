package hudson.plugins.android_emulator;


import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.Proc;
import hudson.Util;
import hudson.matrix.Combination;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Result;
import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.sdk.Tool;
import hudson.plugins.android_emulator.sdk.cli.AdbShellCommands;
import hudson.plugins.android_emulator.sdk.cli.SdkCliCommand;
import hudson.plugins.android_emulator.sdk.cli.SdkCliCommandFactory;
import hudson.plugins.android_emulator.util.Utils;
import hudson.plugins.android_emulator.util.ValidationResult;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.ArgumentListBuilder;
import hudson.util.ForkOutputStream;
import hudson.util.FormValidation;
import hudson.util.NullStream;
import jenkins.model.ArtifactManager;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import edu.umd.cs.findbugs.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AndroidEmulator extends BuildWrapper implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Duration by which the emulator should start being available via adb. */
    private static final int ADB_CONNECT_TIMEOUT= 60;

    /** Duration by which emulator booting should normally complete. */
    private static final int BOOT_COMPLETE_TIMEOUT_MS = 360 * 1000;

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
    @Exported public final String deviceDefinition;
    @Exported public final String sdCardSize;
    @Exported public final String avdNameSuffix;
    @Exported public final HardwareProperty[] hardwareProperties;

    // Common properties
    @Exported public final boolean wipeData;
    @Exported public final boolean showWindow;
    @Exported public final boolean useSnapshots;

    // Advanced properties
    @Exported public final boolean deleteAfterBuild;
    @Exported public final int startupDelay;
    @Exported public final int startupTimeout;
    @Exported public final String commandLineOptions;
    @Exported public final String executable;
    private int adbTimeout;


    @DataBoundConstructor
    public AndroidEmulator(String avdName, String osVersion, String screenDensity,
            String screenResolution, String deviceLocale, String sdCardSize,
            HardwareProperty[] hardwareProperties, boolean wipeData, boolean showWindow,
            boolean useSnapshots, boolean deleteAfterBuild, int startupDelay, int startupTimeout,
            String commandLineOptions, String targetAbi, String deviceDefinition,
            String executable, String avdNameSuffix) {
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
        this.startupTimeout = Math.abs(startupTimeout);
        this.commandLineOptions = commandLineOptions;
        this.targetAbi = targetAbi;
        this.deviceDefinition = deviceDefinition;
        this.avdNameSuffix = avdNameSuffix;
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
            final Computer computer = node.toComputer();
            if (computer == null) {
                throw new BuildNodeUnavailableException();
            }
            envVars = computer.getEnvironment();
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
        String deviceDefinition = Utils.expandVariables(envVars, combination, this.deviceDefinition);
        String avdNameSuffix = Utils.expandVariables(envVars, combination, this.avdNameSuffix);

        return EmulatorConfig.getAvdName(avdName, osVersion, screenDensity, screenResolution,
                deviceLocale, targetAbi, deviceDefinition, avdNameSuffix);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Environment setUp(AbstractBuild build, final Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {
        final PrintStream logger = listener.getLogger();
        if (descriptor == null) {
            descriptor = Jenkins.get().getDescriptorByType(DescriptorImpl.class);
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
        String deviceDefinition = Utils.expandVariables(envVars, buildVars, this.deviceDefinition);
        String avdNameSuffix = Utils.expandVariables(envVars, buildVars, this.avdNameSuffix);

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
        boolean shouldKeepInWorkspace = descriptor.shouldKeepInWorkspace && Util.fixEmptyAndTrim(avdName) == null;
        final String androidSdkHome = (envVars != null && shouldKeepInWorkspace 
                ? envVars.get(Constants.ENV_VAR_JENKINS_WORKSPACE)
                : envVars.containsKey(Constants.ENV_VAR_ANDROID_SDK_HOME) ? envVars.get(Constants.ENV_VAR_ANDROID_SDK_HOME) : System.getProperty("user.home"));
        try {
            emuConfig = EmulatorConfig.create(avdName, osVersion, screenDensity,
                screenResolution, deviceLocale, sdCardSize, wipeData, showWindow, useSnapshots,
                commandLineOptions, targetAbi, deviceDefinition, androidSdkHome, executable, avdNameSuffix);
        } catch (IllegalArgumentException e) {
            log(logger, Messages.EMULATOR_CONFIGURATION_BAD(e.getLocalizedMessage()));
            build.setResult(Result.NOT_BUILT);
            return null;
        }

        // SDK location
        Node node = Computer.currentComputer().getNode();
        String configuredAndroidSdkRoot = Utils.expandVariables(envVars, buildVars, descriptor.androidHome);

        // Confirm that the required SDK tools are available
        AndroidSdk androidSdk = Utils.getAndroidSdk(launcher, node, envVars, configuredAndroidSdkRoot, androidSdkHome);

        final boolean sdkFound = (androidSdk != null);

        if (!sdkFound && !descriptor.shouldInstallSdk) {
            // Couldn't find an SDK, don't want to install it, give up
            log(logger, Messages.SDK_TOOLS_NOT_FOUND());
            build.setResult(Result.NOT_BUILT);
            return null;
        }

        // SDK Tools not found, or does not match expected download version, if we should manage SDK
        if ((!sdkFound || !Constants.isLatestVersion(androidSdk)) && descriptor.shouldInstallSdk) {
            // Ok, let's download and install the SDK Tools
            if (!sdkFound) {
                log(logger, Messages.INSTALLING_SDK());
            } else {
                final String currentVersion = (androidSdk != null) ? androidSdk.getSdkToolsVersion() : "UNKNOWN";
                log(logger, Messages.SDK_INSTALL_UPDATE_TOOLS(currentVersion, "build " + Constants.SDK_TOOLS_DEFAULT_BUILD_ID));
            }

            try {
                androidSdk = SdkInstaller.install(launcher, listener, androidSdkHome);
            } catch (SdkInstallationException e) {
                log(logger, Messages.SDK_INSTALLATION_FAILED(), e);
                build.setResult(Result.NOT_BUILT);
                return null;
            }
        }

        if (descriptor.shouldKeepInWorkspace) {
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

    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    private Environment doSetUp(final AbstractBuild<?, ?> build, final Launcher launcher,
            final BuildListener listener, final AndroidSdk androidSdk,
            final EmulatorConfig emuConfig, final HardwareProperty[] hardwareProperties)
                throws IOException, InterruptedException {
        final PrintStream logger = listener.getLogger();

        // First ensure that emulator exists
        final boolean emulatorAlreadyExists;
        VirtualChannel channel = launcher.getChannel();
        if (channel == null) {
            throw new IllegalStateException("Channel is not configured");
        }
        try {
            Callable<Boolean, AndroidEmulatorException> task = emuConfig.getEmulatorCreationTask(androidSdk, listener);
            emulatorAlreadyExists = channel.call(task);
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
            channel.call(task);
        }

        // Write the auth token file for the emulator
        Callable<Void, IOException> authFileTask = emuConfig.getEmulatorAuthFileTask();
        channel.callAsync(authFileTask);

        // Delay start up by the configured amount of time
        final int delaySecs = startupDelay;
        if (delaySecs > 0) {
            log(logger, Messages.DELAYING_START_UP(delaySecs));
            Thread.sleep(delaySecs * 1000);
        }

        final AndroidEmulatorContext emu = new AndroidEmulatorContext(build, launcher, listener, androidSdk);

        // We manually start the adb-server so that later commands will not have to start it,
        // allowing them to complete faster.
        final SdkCliCommand adbStartCmd = SdkCliCommandFactory.getCommandsForSdk(androidSdk).getAdbStartServerCommand();
        Proc adbStart = emu.getToolProcStarter(adbStartCmd).stdout(logger).stderr(logger).start();
        adbStart.joinWithTimeout(5L, TimeUnit.SECONDS, listener);
        Proc adbStart2 = emu.getToolProcStarter(adbStartCmd).stdout(logger).stderr(logger).start();
        adbStart2.joinWithTimeout(5L, TimeUnit.SECONDS, listener);

        // Show warning about snapshots being enabled, but not supported
        if (useSnapshots && !androidSdk.supportsSnapshots()) {
            log(logger, Messages.SNAPSHOTS_NOT_SUPPORTED());
        }

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
        final String emulatorArgs = emuConfig.getCommandArguments(snapshotState, androidSdk,
                emu.userPort(), emu.adbPort(), emu.getEmulatorCallbackPort(), adbTimeout);

        final EnvVars additionalEnvVars = Utils.getEnvironmentVarsFromEmulatorArgs(emulatorArgs);

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

        final SdkCliCommand cmd = new SdkCliCommand(emuConfig.getExecutable(), emulatorArgs);
        final Proc emulatorProcess = emu.getToolProcStarter(cmd, additionalEnvVars)
                .stdout(emulatorLogger).stderr(logger).start();
        emu.setProcess(emulatorProcess);

        // Give the emulator process a chance to initialise
        Thread.sleep(5 * 1000);

        // Check whether a failure was reported on stdout
        if (emulatorOutput.toString().contains("image is used by another emulator")) {
            log(logger, Messages.EMULATOR_ALREADY_IN_USE(emuConfig.getAvdName()));
            return null;
        }

        // Sitting on the socket appears to break adb. If you try and do this you always end up with device offline.
        // A much better way is to use report-console to tell us what the port is (and hence when its available). So
        // we now do this. adb is also now clever enough to figure out that the emulator is booting and will thus
        // cope without this.

        // Wait for TCP socket to become available
        int socket = waitForSocket(launcher, emu.getEmulatorCallbackPort(), adbTimeout * 1000);
        if (socket < 0) {
            log(logger, Messages.EMULATOR_DID_NOT_START());
            build.setResult(Result.NOT_BUILT);
            cleanUp(emuConfig, emu, androidSdk);
            return null;
        }
        log(logger, Messages.EMULATOR_CONSOLE_REPORT(socket));

        // As of SDK Tools r12, "emulator" is no longer the main process; it just starts a certain
        // child process depending on the AVD architecture.  Therefore on Windows, checking the
        // status of this original process will not work, as it ends after it has started the child.
        //
        // With the adb socket open we know the correct process is running, so we set this flag to
        // indicate that any methods wanting to check the "emulator" process state should ignore it.
        boolean ignoreProcess = !launcher.isUnix() && androidSdk.getSdkToolsMajorVersion() >= 12;

        // Monitor device for boot completion signal
        log(logger, Messages.WAITING_FOR_BOOT_COMPLETION());
        int bootTimeout = BOOT_COMPLETE_TIMEOUT_MS;
        if (startupTimeout > 0) {
            bootTimeout = startupTimeout * 1000;
        }
        else if (!emulatorAlreadyExists || emuConfig.shouldWipeData() || snapshotState == SnapshotState.INITIALISE) {
            bootTimeout *= 2;
        }
        boolean bootSucceeded = waitForBootCompletion(ignoreProcess, bootTimeout, emuConfig, emu);
        if (!bootSucceeded) {
            if ((System.currentTimeMillis() - bootTime) < bootTimeout) {
                log(logger, Messages.EMULATOR_STOPPED_DURING_BOOT());
            } else {
                log(logger, Messages.BOOT_COMPLETION_TIMED_OUT(bootTimeout / 1000));
            }
            build.setResult(Result.NOT_BUILT);
            cleanUp(emuConfig, emu, androidSdk);
            return null;
        }

        final int emulatorAPILevel = (emuConfig.getOsVersion() != null) ? emuConfig.getOsVersion().getSdkLevel() : 0;
        final AdbShellCommands adbShellCmds = SdkCliCommandFactory.getAdbShellCommandForAPILevel(emulatorAPILevel);

        // Start dumping logcat to temporary file
        final FilePath workspace = build.getWorkspace();
        if (workspace == null) {
            throw new BuildNodeUnavailableException();
        }
        final ArtifactManager artifactManager = build.getArtifactManager();
        final FilePath logcatFile = workspace.createTextTempFile("logcat_", ".log", "", false);
        final OutputStream logcatStream = logcatFile.write();
        final SdkCliCommand adbSetLogCatFormatCmd = adbShellCmds.getSetLogCatFormatToTimeCommand(emu.serial());
        final Proc logWriter = emu.getToolProcStarter(adbSetLogCatFormatCmd)
                .stdout(logcatStream).stderr(new NullStream()).start();

        // Unlock emulator by pressing the Menu key once, if required.
        // Upon first boot (and when the data is wiped) the emulator is already unlocked
        final long bootDuration = System.currentTimeMillis() - bootTime;
        if (emulatorAlreadyExists && !wipeData && snapshotState != SnapshotState.BOOT) {
            // Even if the emulator has started, we generally need to wait longer before the lock
            // screen is up and ready to accept key presses.
            // The delay here is a function of boot time, i.e. relative to the slowness of the host
            Thread.sleep(bootDuration / 4);

            log(logger, Messages.UNLOCKING_SCREEN());

            final SdkCliCommand adbUnlockCmd = adbShellCmds.getDismissKeyguardCommand(emu.serial());
            ArgumentListBuilder unlockCmd = emu.getToolCommand(adbUnlockCmd);
            Proc proc = emu.getProcStarter(unlockCmd).start();
            proc.joinWithTimeout(adbTimeout, TimeUnit.MILLISECONDS, emu.launcher().getListener());

            // If a named emulator already existed, it may not have been booted yet, so the screen
            // wouldn't be locked.  Similarly, an non-named emulator may have already booted the
            // first time without us knowing.  In both cases, we press Back after attempting to
            // unlock the screen to compensate
            final SdkCliCommand adbSendBackKeyCmd = adbShellCmds.getSendBackKeyEventCommand(emu.serial());
            ArgumentListBuilder backCmd = emu.getToolCommand(adbSendBackKeyCmd);
            proc = emu.getProcStarter(backCmd).start();
            proc.joinWithTimeout(adbTimeout, TimeUnit.MILLISECONDS, emu.launcher().getListener());
        }

        // Initialise snapshot image, if required
        if (snapshotState == SnapshotState.INITIALISE) {
            // In order to create a clean initial snapshot, give the system some more time to settle
            log(logger, Messages.WAITING_INITIAL_SNAPSHOT());
            Thread.sleep((long) (bootDuration * 0.8));

            // Clear main log before creating snapshot
            final SdkCliCommand adbClearLogCmd = adbShellCmds.getClearMainLogCommand(emu.serial());
            ArgumentListBuilder adbCmd = emu.getToolCommand(adbClearLogCmd);
            emu.getProcStarter(adbCmd).join();

            // Log creation of snapshot
            final String msg = Messages.LOG_CREATING_SNAPSHOT();
            final SdkCliCommand adbLogCmd = adbShellCmds.getLogMessageCommand(emu.serial(), msg);
            adbCmd = emu.getToolCommand(adbLogCmd);
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
                    cleanUp(emuConfig, emu, androidSdk, logWriter, logcatFile, logcatStream, artifactManager, launcher, listener);
                }
            } else {
                log(logger, Messages.SNAPSHOT_CREATION_FAILED());
            }
        }

        // Done!
        final long bootCompleteTime = System.currentTimeMillis();
        log(logger, Messages.EMULATOR_IS_READY((bootCompleteTime - bootTime) / 1000));

        // Return wrapped environment
        return new Environment() {
            @Override
            public void buildEnvVars(Map<String, String> env) {
                env.put(Constants.ENV_VAR_ANDROID_SERIAL, emu.serial());
                env.put(Constants.ENV_VAR_ANDROID_AVD_DEVICE, emu.serial());
                env.put(Constants.ENV_VAR_ANDROID_AVD_ADB_PORT, Integer.toString(emu.adbPort()));
                env.put(Constants.ENV_VAR_ANDROID_AVD_USER_PORT, Integer.toString(emu.userPort()));
                env.put(Constants.ENV_VAR_ANDROID_AVD_NAME, emuConfig.getAvdName());
                env.put(Constants.ENV_VAR_ANDROID_ADB_SERVER_PORT, Integer.toString(emu.adbServerPort()));
                env.put(Constants.ENV_VAR_ANDROID_TMP_LOGCAT_FILE, logcatFile.getRemote());
                if (!emuConfig.isNamedEmulator()) {
                    env.put(Constants.ENV_VAR_ANDROID_AVD_OS, emuConfig.getOsVersion().toString());
                    env.put(Constants.ENV_VAR_ANDROID_AVD_DENSITY, emuConfig.getScreenDensity().toString());
                    env.put(Constants.ENV_VAR_ANDROID_AVD_RESOLUTION, emuConfig.getScreenResolution().toString());
                    env.put(Constants.ENV_VAR_ANDROID_AVD_SKIN, emuConfig.getScreenResolution().getSkinName());
                    env.put(Constants.ENV_VAR_ANDROID_AVD_LOCALE, emuConfig.getDeviceLocale());
                }
                if (androidSdk.hasKnownRoot()) {
                    String sdkRoot = androidSdk.getSdkRoot();
                    env.put(Constants.ENV_VAR_JENKINS_ANDROID_HOME, sdkRoot);
                    env.put(Constants.ENV_VAR_ANDROID_HOME, sdkRoot);
                    env.put(Constants.ENV_VAR_ANDROID_SDK_ROOT, sdkRoot);

                    // Prepend the commonly-used Android tools to the start of the PATH for this build
                    env.put(Constants.ENV_VAR_PATH_SDK_TOOLS, sdkRoot + "/tools/");
                    env.put(Constants.ENV_VAR_PATH_SDK_PLATFORM_TOOLS, sdkRoot + "/platform-tools/");
                    // TODO: Export the newest build-tools folder as well, so aapt and friends can be used
                }
            }

            @Override
            @SuppressWarnings("rawtypes")
            public boolean tearDown(AbstractBuild build, BuildListener listener)
                    throws IOException, InterruptedException {
                cleanUp(emuConfig, emu, androidSdk, logWriter, logcatFile, logcatStream, artifactManager, launcher, listener);
                return true;
            }
        };
    }

    /* Helper method for writing to the build log in a consistent manner. */
    public synchronized static void log(final PrintStream logger, final String message) {
        log(logger, message, false);
    }

    /* Helper method for writing to the build log in a consistent manner. */
    public synchronized static void log(final PrintStream logger, final String message, final Throwable t) {
        log(logger, message, false);
        StringWriter s = new StringWriter();
        t.printStackTrace(new PrintWriter(s));
        log(logger, s.toString(), false);
    }

    /* Helper method for writing to the build log in a consistent manner. */
    public synchronized static void log(final PrintStream logger, String message, boolean indent) {
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
     * @param androidSdk The current android SDK
     */
    private void cleanUp(EmulatorConfig emulatorConfig, AndroidEmulatorContext emu, final AndroidSdk androidSdk) throws IOException, InterruptedException {
        cleanUp(emulatorConfig, emu, androidSdk, null, null, null, null, null, null);
    }

    /**
     * Called when this wrapper needs to exit, so we need to clean up some processes etc.
     * @param emulatorConfig The emulator being run.
     * @param emu The emulator context
     * @param androidSdk The current android SDK
     * @param logcatProcess The adb logcat process.
     * @param logcatFile The file the logcat output is being written to.
     * @param logcatStream The stream the logcat output is being written to.
     * @param artifactManager The artifact manager. Used to archive the logcatFile.
     * @param launcher a launcher used by artifactManager to archive the logcatFile.
     * @param listener a listener used by artifactManager to archive the logcatFile.
     */
    private void cleanUp(EmulatorConfig emulatorConfig, AndroidEmulatorContext emu, AndroidSdk androidSdk,
                         @Nullable Proc logcatProcess, @Nullable FilePath logcatFile, @Nullable OutputStream logcatStream,
                         @Nullable ArtifactManager artifactManager, @Nullable Launcher launcher, @Nullable BuildListener listener)
           throws IOException, InterruptedException {

        // FIXME: Sometimes on Windows neither the emulator.exe nor the adb.exe processes die.
        //        Launcher.kill(EnvVars) does not appear to help either.
        //        This is (a) inconsistent; (b) very annoying.

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
            if (logcatFile.length() != 0 && artifactManager != null && launcher != null && listener != null) {
                log(emu.logger(), Messages.ARCHIVING_LOG());
                final FilePath workspace = logcatFile.getParent();
                final Map<String, String> artifacts = Collections.singletonMap("logcat.txt", logcatFile.getName());
                artifactManager.archive(workspace, launcher, listener, artifacts);
            }
            logcatFile.delete();
        }

        final SdkCliCommand killCmd = SdkCliCommandFactory.getCommandsForSdk(androidSdk).getAdbKillServerCommand();
        ArgumentListBuilder adbKillCmd = emu.getToolCommand(killCmd);
        emu.getProcStarter(adbKillCmd).join();

        emu.cleanUp();

        // Delete the emulator, if required
        if (deleteAfterBuild) {
            try {
                Callable<Boolean, Exception> deletionTask = emulatorConfig.getEmulatorDeletionTask(
                        emu.launcher().getListener());
                VirtualChannel channel = emu.launcher().getChannel();
                if (channel == null) {
                    throw new IllegalStateException("Channel is not configured");
                }
                channel.call(deletionTask);
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
     * Waits for an emulator to tell us which port it is using, or times out.
     *
     * @param launcher The launcher for the remote node.
     * @param port The port to listen on.
     * @param timeout How long to keep waiting (in milliseconds) before giving up.
     * @return The port number of the emulator's telnet interface, or {@code -1} in case of failure.
     */
    private int waitForSocket(Launcher launcher, int port, int timeout) throws InterruptedException {
        try {
            ReceiveEmulatorPortTask task = new ReceiveEmulatorPortTask(port, timeout);
            VirtualChannel channel = launcher.getChannel();
            if (channel == null) {
                throw new IllegalStateException("Channel is not configured");
            }
            return channel.call(task);
        } catch (IOException ignore) {
        }
        return -1;
    }

    /**
     * Checks whether the emulator running on the given port has finished booting yet, or times out.
     * @param ignoreProcess Whether to bypass checking that the process is alive (e.g. on Windows).
     * @param timeout How long to keep trying (in milliseconds) before giving up.
     * @param emu The emulator context
     * @return <code>true</code> if the emulator has booted, <code>false</code> if we timed-out.
     */
    @SuppressFBWarnings({"DM_DEFAULT_ENCODING", "ICAST_IDIV_CAST_TO_DOUBLE"})
    private boolean waitForBootCompletion(final boolean ignoreProcess,
            final int timeout, EmulatorConfig config, AndroidEmulatorContext emu) {
        long start = System.currentTimeMillis();
        int sleep = timeout / (int) (Math.sqrt(timeout / 1000) * 2);

        int apiLevel = 0;
        if (!config.isNamedEmulator()) {
            apiLevel = config.getOsVersion().getSdkLevel();
        }

        final AdbShellCommands adbShellCmds = SdkCliCommandFactory.getAdbShellCommandForAPILevel(apiLevel);
        final SdkCliCommand adbDevicesStartCmd = adbShellCmds.getWaitForDeviceStartupCommand(emu.serial());
        final String expectedAnswer = adbShellCmds.getWaitForDeviceStartupExpectedAnswer();
        ArgumentListBuilder bootCheckCmd = emu.getToolCommand(adbDevicesStartCmd);

        try {
            final long adbTimeout = timeout / 8;
            while (System.currentTimeMillis() < start + timeout && (ignoreProcess || emu.process().isAlive())) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream(16);

                // Run "getprop", timing-out in case adb hangs
                Proc proc = emu.getProcStarter(bootCheckCmd).stdout(stream).start();
                int retVal = proc.joinWithTimeout(adbTimeout, TimeUnit.MILLISECONDS, emu.launcher().getListener());
                if (retVal == 0) {
                    // If boot is complete, our work here is done
                    String result = stream.toString().trim();
                    log(emu.logger(), Messages.EMULATOR_STATE_REPORT(result));
                    if (result.equals(expectedAnswer)) {
                        return true;
                    }
                }

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

    public int getAdbTimeout() {
        return adbTimeout;
    }

    @DataBoundSetter
    public void setAdbTimeout(int adbTimeout) {
        this.adbTimeout = adbTimeout;
    }

    /**
     * Migrate old data.
     *
     * @see <a href=
     *      "https://wiki.jenkins-ci.org/display/JENKINS/Hint+on+retaining+backward+compatibility">
     *      Jenkins wiki entry on the subject</a>
     *
     * @return must be always 'this'
     */
    private Object readResolve() {
        // When these values would be missing in the xml config their values are defaults. Otherwise it equals the value from xml-config.
        if (adbTimeout == 0) {
            adbTimeout = ADB_CONNECT_TIMEOUT;
        }
        return this;
    }

    @Extension(ordinal=-100) // Negative ordinal makes us execute after other wrappers (i.e. Xvnc)
    public static final class DescriptorImpl extends BuildWrapperDescriptor implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * The Android SDK home directory.  Can include variables, e.g. {@code ${ANDROID_HOME}}.
         * <p>If <code>null</code>, we will just assume the required commands are on the PATH.</p>
         */
        public String androidHome;

        /** Whether the SDK should be automatically installed where it's not found. */
        public boolean shouldInstallSdk = true;

        /** Whether the emulators should be kept in the workspace. */
        public boolean shouldKeepInWorkspace = false;

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
            String deviceDefinition = null;
            List<HardwareProperty> hardware = new ArrayList<HardwareProperty>();
            boolean wipeData = false;
            boolean showWindow = true;
            boolean useSnapshots = true;
            boolean deleteAfterBuild = false;
            int startupDelay = 0;
            int startupTimeout = 0;
            int adbTimeout = ADB_CONNECT_TIMEOUT;
            String commandLineOptions = null;
            String executable = null;
            String avdNameSuffix = null;

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
                deviceDefinition = Util.fixEmptyAndTrim(emulatorData.getString("deviceDefinition"));
                avdNameSuffix = Util.fixEmptyAndTrim(emulatorData.getString("avdNameSuffix"));
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
            try {
                startupTimeout = Integer.parseInt(formData.getString("startupTimeout"));
            } catch (NumberFormatException e) {}
            try {
                adbTimeout = Integer.parseInt(formData.getString("adbTimeout"));
            } catch (NumberFormatException e) {}

            AndroidEmulator androidEmulator = new AndroidEmulator(avdName, osVersion, screenDensity, screenResolution,
                    deviceLocale, sdCardSize, hardware.toArray(new HardwareProperty[0]), wipeData,
                    showWindow, useSnapshots, deleteAfterBuild, startupDelay, startupTimeout, commandLineOptions,
                    targetAbi, deviceDefinition, executable, avdNameSuffix);
            androidEmulator.setAdbTimeout(adbTimeout);
            return androidEmulator;
        }

        @Override
        public String getHelpFile() {
            return Functions.getResourcePath() + "/plugin/android-emulator/help-buildConfig.html";
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        /* Used in config.jelly: Lists the OS versions available. */
        public String[] getAndroidVersions() {
            return AndroidPlatform.getAllPossibleVersionNames();
        }

        /* Used in config.jelly: Lists the screen densities available. */
        public ScreenDensity[] getDeviceDensities() {
            return ScreenDensity.values();
        }

        /* Used in config.jelly: Lists the screen resolutions available. */
        public ScreenResolution[] getDeviceResolutions() {
            return ScreenResolution.values();
        }

        /* Used in config.jelly: Lists the locales available. */
        public String[] getEmulatorLocales() {
            return Constants.EMULATOR_LOCALES;
        }

        /* Used in config.jelly: Lists common hardware properties that can be set. */
        public String[] getHardwareProperties() {
            return Constants.HARDWARE_PROPERTIES;
        }

        /* Used in config.jelly: Lists common ABIs that can be set. */
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
                @QueryParameter String screenDensity, @QueryParameter String osVersion) {
            return doCheckScreenResolution(value, screenDensity, osVersion, true).getFormValidation();
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
                return ValidationResult.warning(Messages.JOB_CONFIG_EMPTY_ABI());
            }

            if (allowVariables && value.matches(Constants.REGEX_VARIABLE)) {
                return ValidationResult.ok();
            }

            for (String s : Constants.TARGET_ABIS) {
                if (s.equals(value) || (value.contains("/") && value.endsWith(s))) {
                    return ValidationResult.ok();
                }
            }
            return ValidationResult.error(Messages.INVALID_TARGET_ABI());
        }

        public FormValidation doCheckDeviceDefinition(@QueryParameter String value) {
            return checkDeviceDefinition(value, true).getFormValidation();
        }

        private ValidationResult checkDeviceDefinition(String value, boolean allowVariables) {
            return ValidationResult.ok();
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
            return Utils.validateAndroidHome(value, true, true).getFormValidation();
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
