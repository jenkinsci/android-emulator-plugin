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

	private int adbPort, userPort, adbServerPort;
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
		final int PORT_RANGE_START = 5554;
		final int PORT_RANGE_END = 9999; // Make sure the port is four digits, as there are tools that rely on this
		int[] ports = portAllocator.allocatePortRange(build, PORT_RANGE_START, PORT_RANGE_END, 3, true);
		userPort = ports[0];
		adbPort = ports[1];
		adbServerPort = ports[2];

		serial = String.format("localhost:%d", adbPort);
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
	public void setAdbServerPort(int adbServerPort) {
		this.adbServerPort = adbServerPort;
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
