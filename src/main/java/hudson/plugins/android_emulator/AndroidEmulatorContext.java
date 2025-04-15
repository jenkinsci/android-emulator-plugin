package hudson.plugins.android_emulator;

import java.io.IOException;
import java.io.PrintStream;

import org.jvnet.hudson.plugins.port_allocator.PortAllocationManager;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.sdk.cli.SdkCliCommand;
import hudson.plugins.android_emulator.util.Utils;
import hudson.util.ArgumentListBuilder;
import hudson.util.NullStream;

public class AndroidEmulatorContext {
    /** Interval during which an emulator command should complete. */
    public static final int EMULATOR_COMMAND_TIMEOUT_MS = 60 * 1000;

	private int adbPort, userPort, adbServerPort, emulatorCallbackPort;
	private String serial;

	private PortAllocationManager portAllocator;
	private Proc emulatorProcess;

	private AndroidSdk sdk;

	private AbstractBuild<?, ?> build;
	private BuildListener listener;
	private Launcher launcher;

	public AndroidEmulatorContext(AbstractBuild<?, ?> build_,
			Launcher launcher_, BuildListener listener_, AndroidSdk sdk_)
			throws InterruptedException, IOException {
		build = build_;
		listener = listener_;
		launcher = launcher_;
		sdk = sdk_;

        // Use the Port Allocator plugin to reserve the ports we need
        final Computer computer = Computer.currentComputer();
        portAllocator = PortAllocationManager.getManager(computer);

        // ADB allows up to 64 local devices, each of which uses two consecutive ports: one for the
        // user telnet interface, and one to communicate with ADB.  These pairs start at port 5554.
        // http://android.googlesource.com/platform/system/core/+/d387acc/adb/adb.h#206
        // http://android.googlesource.com/platform/system/core/+/d387acc/adb/transport_local.cpp#44
        //
        // So long as the ADB server automatically registers itself with any emulators in the
        // standard port range of 5555–5861, then we should avoid using that port range.
        // Otherwise, when we run multiple ADB servers and emulators at the same time, each of the
        // ADB servers will race to register with each emulator, meaning that each build will most
        // likely end up with an emulator that always ends up appearing to be "offline".
        // See http://b.android.com/205197
        final int PORT_RANGE_START = 5554 + (2 * 64);
        final int PORT_RANGE_END = PORT_RANGE_START + (2 * 64);

        // When using the emulator `-port` option, the first port must be even, so here we reserve
        // three consecutive ports, ensuring that we will get an even port followed by an odd
        int[] ports = portAllocator.allocatePortRange(build, PORT_RANGE_START, PORT_RANGE_END, 3, true);

        // Assign the ports the user and ADB interfaces should use, such that the user port is even
        int i = 0;
        if (ports[i] % 2 != 0) {
            i++;
        }
        userPort = ports[i++];
        adbPort = ports[i++];

        // Release the port that was reserved but not used
        portAllocator.free(i == 2 ? ports[2] : ports[0]);

        // Reserve two further ports for the ADB server and the callback socket.
        // Use a separate port range so as not to tie up emulator ports unnecessarily
        final int SERVER_PORT_RANGE_START = PORT_RANGE_END;
        final int SERVER_PORT_RANGE_END = SERVER_PORT_RANGE_START + 64;
        ports = portAllocator.allocatePortRange(build, SERVER_PORT_RANGE_START,
                SERVER_PORT_RANGE_END, 2, false);
        adbServerPort = ports[0];
        emulatorCallbackPort = ports[1];

        // Set the emulator qualifier based on the telnet port
        serial = String.format("emulator-%d", userPort);
    }

    public void cleanUp() {
        // Free up the TCP ports that we reserved
        portAllocator.free(adbPort);
        portAllocator.free(userPort);
        portAllocator.free(adbServerPort);
        portAllocator.free(emulatorCallbackPort);
    }

	public int adbPort() {
		return adbPort;
	}
	public int userPort() {
		return userPort;
	}
	public int adbServerPort() {
		return adbServerPort;
	}
    public int getEmulatorCallbackPort() { return emulatorCallbackPort; }

	public String serial() {
		return serial;
	}

	public BuildListener listener() {
		return listener;
	}

	public Launcher launcher() {
		return launcher;
	}

	public AndroidSdk sdk() {
		return sdk;
	}
	public PrintStream logger() {
		return listener.getLogger();
	}

	public Proc process() {
		return emulatorProcess;
	}
	public void setProcess(Proc process) {
		emulatorProcess = process;
	}

	/**
	 * Sets up a standard {@link ProcStarter} for the current context. 
	 * 
	 * @param command What command to run
	 * @param env Additional environment variables to set
	 * @return A ready ProcStarter
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public ProcStarter getProcStarter(final ArgumentListBuilder command, final EnvVars env)
			throws IOException, InterruptedException {
		final EnvVars buildEnvironment = build.getEnvironment(TaskListener.NULL);
		buildEnvironment.put(Constants.ENV_VAR_ANDROID_ADB_SERVER_PORT, Integer.toString(adbServerPort));
        if (sdk.hasKnownRoot()) {
            buildEnvironment.putIfAbsent(Constants.ENV_VAR_ANDROID_SDK_ROOT, sdk.getSdkRoot());
        }
        if (sdk.hasKnownHome()) {
            String sdkHome = sdk.getSdkHome();
            buildEnvironment.putIfAbsent(Constants.ENV_VAR_ANDROID_SDK_HOME, sdkHome);
            buildEnvironment.putIfAbsent("ANDROID_EMULATOR_HOME", sdkHome + "/.android/");
            buildEnvironment.putIfAbsent("ANDROID_AVD_HOME", sdkHome + "/.android/avd/");
        }
		if (launcher.isUnix()) {
			buildEnvironment.put(Constants.ENV_VAR_LD_LIBRARY_PATH, String.format("%s/tools/lib", sdk.getSdkRoot()));
		}
		if (env != null) {
			buildEnvironment.putAll(env);
		}

		final ProcStarter procStarter = launcher.launch().stdout(new NullStream()).stderr(logger());
		procStarter.envs(buildEnvironment);
		if (command != null) {
			procStarter.cmds(command);
		}
		return procStarter;
	}

	/**
	 * 
	 * Sets up a standard {@link ProcStarter} for the current adb environment,
	 * ready to execute the given command.
	 * 
	 * @param command What command to run
	 * @return A ready ProcStarter
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public ProcStarter getProcStarter(ArgumentListBuilder command)
			throws IOException, InterruptedException {
		return getProcStarter(command, null);
	}

        /**
         * Generates a ready-to-use ArgumentListBuilder for one of the Android SDK tools, based on the current context.
         *
         * @param sdkCmd The Android tool and any extra arguments for the command to run.
         * @return Arguments including the full path to the SDK and any extra Windows stuff required.
         */
	public ArgumentListBuilder getToolCommand(final SdkCliCommand sdkCmd) {
		return Utils.getToolCommand(sdk, launcher.isUnix(), sdkCmd);
	}

        /**
         * Generates a ready-to-use ProcStarter for one of the Android SDK tools, based on the current context.
         *
         * @param sdkCmd The Android tool and any extra arguments for the command to run.
         * @param env Additional environment variables to set
         * @return A ready ProcStarter
         *
         * @throws IOException
         * @throws InterruptedException
         */
        public ProcStarter getToolProcStarter(final SdkCliCommand sdkCmd, final EnvVars env)
                throws IOException, InterruptedException {
            return getProcStarter(Utils.getToolCommand(sdk, launcher.isUnix(), sdkCmd), env);
        }

        /**
         * Generates a ready-to-use ProcStarter for one of the Android SDK tools, based on the current context.
         *
         * @param sdkCmd The Android tool and any extra arguments for the command to run.
         * @return A ready ProcStarter
	 * @throws IOException
	 * @throws InterruptedException
         */
	public ProcStarter getToolProcStarter(final SdkCliCommand sdkCmd)
			throws IOException, InterruptedException {
		return getProcStarter(Utils.getToolCommand(sdk, launcher.isUnix(), sdkCmd));
	}

	/**
	 * Sends a user command to the running emulator via its telnet interface.<br>
	 * Execution will be cancelled if it takes longer than
	 * {@link #EMULATOR_COMMAND_TIMEOUT_MS}.
	 * 
	 * @param command The command to execute on the emulator's telnet interface.
	 * @return Whether sending the command succeeded.
	 */
	public boolean sendCommand(final String command) {
		return sendCommand(command, EMULATOR_COMMAND_TIMEOUT_MS);
	}
	
	/**
	 * Sends a user command to the running emulator via its telnet interface.<br>
	 * Execution will be cancelled if it takes longer than timeout ms.
	 * 
	 * @param command The command to execute on the emulator's telnet interface.
	 * @param timeout The command's timeout, in ms.
	 * @return Whether sending the command succeeded.
	 */
	public boolean sendCommand(final String command, int timeout) {
		return Utils.sendEmulatorCommand(launcher, logger(), userPort, command, timeout);
	}
}
