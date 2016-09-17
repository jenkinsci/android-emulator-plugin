package hudson.plugins.android_emulator;

public abstract class AndroidEmulatorException extends Exception {

    protected AndroidEmulatorException(String message) {
        super(message);
    }

    protected AndroidEmulatorException(String message, Throwable cause) {
        super(message, cause);
    }

    private static final long serialVersionUID = 1L;

}

