package hudson.plugins.android_emulator;

public class EmulatorPortConfig {

	private boolean useRandAdbServerPort;
	private int adbServerPort;
	private boolean manageAdbServer;

	/**
	 * Creates new port settings for the android emulator
	 * 
	 * @param useRandomAdbServerPort If a random port should be used for adb server
	 * @param adbServerPortIfFixed The port number if the port is fixed
	 * @param manageAdbServer If the adb server should still be managed, although a fixed port should be used
	 */
	public EmulatorPortConfig(boolean useRandomAdbServerPort,
			int adbServerPortIfFixed, boolean manageAdbServer) {
		this.useRandAdbServerPort = useRandomAdbServerPort;
		this.adbServerPort = adbServerPortIfFixed;
		this.manageAdbServer = manageAdbServer;
	}

	public boolean useRandomAdbServerPort() {
		return useRandAdbServerPort;
	}

	public int adbServerPort() {
		return adbServerPort;
	}

	public boolean manageAdbServer() {
		return manageAdbServer;
	}
}
