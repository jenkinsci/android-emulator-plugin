package hudson.plugins.android_emulator;

import java.io.IOException;
import java.io.PrintStream;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import hudson.util.NullStream;

import org.jvnet.hudson.plugins.port_allocator.PortAllocationManager;

import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.sdk.Tool;
import hudson.plugins.android_emulator.util.Utils;

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

		final Computer computer = Computer.currentComputer();

		// Use the Port Allocator plugin to reserve the ports we need
		portAllocator = PortAllocationManager.getManager(computer);
        // According to https://developer.android.com/tools/help/adb.html,
        // It locates emulator/device instances by scanning odd-numbered ports in the range 5555 to 5585,
        // the range used by emulators/devices. Ports are allocated in pairs console/adb
		final int PORT_RANGE_START = 5554;
		final int PORT_RANGE_END = 5585; // Make sure the port is four digits, as there are tools that rely on this
        // Allocate 4 ports so that we start on an even every time
		int[] ports = portAllocator.allocatePortRange(build, PORT_RANGE_START, PORT_RANGE_END, 2, true);
		userPort = ports[0];
		adbPort = ports[1];
		adbServerPort = 5037; // This is the standard according to the android docs
        emulatorCallbackPort = portAllocator.allocateRandom(build, 49152);
        // This is a best guess. adb get-serialno will return the actual value
		serial = String.format("emulator-%d", userPort);
	}

	public void cleanUp() {
		// Free up the TCP ports
		portAllocator.free(adbPort);
		portAllocator.free(userPort);
		portAllocator.free(adbServerPort);
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

    public String connectString() {
        return "localhost:" + userPort;
    }

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
	 * @return A ready ProcStarter
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public ProcStarter getProcStarter() throws IOException, InterruptedException {
		final EnvVars buildEnvironment = build.getEnvironment(TaskListener.NULL);
		buildEnvironment.put("ANDROID_ADB_SERVER_PORT", Integer.toString(adbServerPort));
		if (sdk.hasKnownHome()) {
			buildEnvironment.put("ANDROID_SDK_HOME", sdk.getSdkHome());
		}
		if (launcher.isUnix()) {
			buildEnvironment.put("LD_LIBRARY_PATH", String.format("%s/tools/lib", sdk.getSdkRoot()));
		}
		return launcher.launch().stdout(new NullStream()).stderr(logger()).envs(buildEnvironment);
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
		return getProcStarter().cmds(command);
	}

        /**
         * Generates a ready-to-use ArgumentListBuilder for one of the Android SDK tools, based on the current context.
         *
         * @param tool The Android tool to run.
         * @param args Any extra arguments for the command.
         * @return Arguments including the full path to the SDK and any extra Windows stuff required.
         */
	public ArgumentListBuilder getToolCommand(Tool tool, String args) {
		return Utils.getToolCommand(sdk, launcher.isUnix(), tool, args);
	}

        /**
         * Generates a ready-to-use ProcStarter for one of the Android SDK tools, based on the current context.
         *
         * @param tool The Android tool to run.
         * @param args Any extra arguments for the command.
         * @return A ready ProcStarter
	 * @throws IOException
	 * @throws InterruptedException
         */
	public ProcStarter getToolProcStarter(Tool tool, String args)
			throws IOException, InterruptedException {
		return getProcStarter(Utils.getToolCommand(sdk, launcher.isUnix(), tool, args));
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
