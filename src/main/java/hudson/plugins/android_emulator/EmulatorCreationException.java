package hudson.plugins.android_emulator;

public class EmulatorCreationException extends AndroidEmulatorException {

    public EmulatorCreationException(String message) {
        super(message);
    }

    public EmulatorCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    private static final long serialVersionUID = 1L;

}
