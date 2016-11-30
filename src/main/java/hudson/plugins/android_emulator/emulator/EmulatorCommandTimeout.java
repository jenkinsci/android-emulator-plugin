package hudson.plugins.android_emulator.emulator;

import javax.annotation.Nonnull;

class EmulatorCommandTimeout extends Thread {

    private final long timeoutMS;
    private final TimeoutConsumer timeoutConsumer;
    private boolean doRun = true;
    private boolean timeoutReached;

    EmulatorCommandTimeout(final long timeoutMS, final @Nonnull TimeoutConsumer timeoutConsumer) {
        this.timeoutConsumer = timeoutConsumer;
        this.timeoutMS = timeoutMS;
        this.setName("EmulatorCommandTimeout");
    }

    @Override
    public void run() {
        long startedAt = now();
        while (doRun) {
            checkForTimeout(startedAt);
            try {
                sleep(10);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void checkForTimeout(long startedAt) {
        synchronized (this) {
            timeoutReached = (doRun && now() - startedAt >= timeoutMS);
        }
        if (timeoutReached) {
            setTimedOut();
        }
    }

    private void setTimedOut() {
        synchronized (this) {
            doRun = false;
            timeoutConsumer.onTimedOut(this);
        }
    }

    void finish() {
        synchronized (this) {
            doRun = false;
        }
    }

    private long now() {
        return System.currentTimeMillis();
    }

    boolean isTimeoutReached() {
        return this.timeoutReached;
    }
}
