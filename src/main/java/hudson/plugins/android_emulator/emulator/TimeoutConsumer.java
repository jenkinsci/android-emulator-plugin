package hudson.plugins.android_emulator.emulator;

import javax.annotation.Nonnull;

public interface TimeoutConsumer {
    void onTimedOut(@Nonnull EmulatorCommandTimeout emulatorCommandTimeout);
}
