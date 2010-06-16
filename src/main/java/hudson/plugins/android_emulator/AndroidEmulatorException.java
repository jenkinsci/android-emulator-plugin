package hudson.plugins.android_emulator;

abstract class AndroidEmulatorException extends Exception {

    protected AndroidEmulatorException(String message) {
        super(message);
    }

    protected AndroidEmulatorException(String message, Throwable cause) {
        super(message, cause);
    }

    private static final long serialVersionUID = 1L;

}

final class EmulatorDiscoveryException extends AndroidEmulatorException {

    EmulatorDiscoveryException(String message) {
        super(message);
    }

    private static final long serialVersionUID = 1L;

}

final class EmulatorCreationException extends AndroidEmulatorException {

    EmulatorCreationException(String message) {
        super(message);
    }

    EmulatorCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    private static final long serialVersionUID = 1L;

}