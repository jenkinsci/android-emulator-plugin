package hudson.plugins.android_emulator;

public final class SdkInstallationException extends AndroidEmulatorException {

    SdkInstallationException(String message) {
        super(message);
    }

    SdkInstallationException(String message, Throwable cause) {
        super(message, cause);
    }

    private static final long serialVersionUID = 1L;

}