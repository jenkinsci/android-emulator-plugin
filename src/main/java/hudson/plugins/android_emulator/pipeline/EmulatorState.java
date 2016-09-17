package hudson.plugins.android_emulator.pipeline;

import java.io.Serializable;

public class EmulatorState implements Serializable {

    private final int adbServerPort;
    private final int telnetPort;
    private final int adbPort;

    // TODO: API level of emulator (for boot detection)
    // TODO: Other emulator attributes, that we need to export to the environment
    // TODO: Android SDK object/dir?

    public EmulatorState(int adbServerPort, int telnetPort, int adbPort) {
        this.adbServerPort = adbServerPort;
        this.telnetPort = telnetPort;
        this.adbPort = adbPort;
    }

    public int getAdbServerPort() {
        return adbServerPort;
    }

    public int getTelnetPort() {
        return telnetPort;
    }

    public int getAdbPort() {
        return adbPort;
    }

    public String getSerial() {
        return String.format("emulator-%d", telnetPort);
    }

}
