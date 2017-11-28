package hudson.plugins.android_emulator;

import java.io.IOException;

public class BuildNodeUnavailableException extends IOException {

    public BuildNodeUnavailableException() {
        super(Messages.NODE_UNAVAILABLE_EXCEPTION());
    }

    private static final long serialVersionUID = 1L;

}