package hudson.plugins.android_emulator.monkey;

import hudson.plugins.android_emulator.Messages;

public enum BuildOutcome {

    UNSTABLE(Messages.BUILD_RESULT_UNSTABLE()),
    FAILURE(Messages.BUILD_RESULT_FAILURE()),
    IGNORE(Messages.BUILD_RESULT_IGNORE());

    private final String displayName;

    BuildOutcome(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

}
