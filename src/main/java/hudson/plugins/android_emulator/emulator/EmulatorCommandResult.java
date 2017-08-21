package hudson.plugins.android_emulator.emulator;

import java.io.Serializable;

public class EmulatorCommandResult implements Serializable {
    public boolean success = false;
    public String output = "";
    public boolean timedOut;

    public EmulatorCommandResult setSuccess(boolean value) {
        this.success = value;
        return this;
    }

    EmulatorCommandResult appendOutput(String value) {
        synchronized (this) {
            this.output = this.output + value;
        }
        return this;
    }
}
