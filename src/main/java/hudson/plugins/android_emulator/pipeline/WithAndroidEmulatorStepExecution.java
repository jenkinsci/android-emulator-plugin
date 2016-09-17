package hudson.plugins.android_emulator.pipeline;

import com.google.inject.Inject;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.android_emulator.Messages;
import hudson.plugins.android_emulator.util.Utils;
import hudson.util.ArgumentListBuilder;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.EnvironmentExpander;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import static hudson.plugins.android_emulator.AndroidEmulator.log;

public class WithAndroidEmulatorStepExecution extends AbstractSynchronousNonBlockingStepExecution {

    private static final long serialVersionUID = 1L;

    // TODO: Make configurable, or depend on whether emulator is new / wiped
    private static final int BOOT_TIMEOUT_MS = 120 * 1000;

    private static final int ADB_TIMEOUT_MS = 5000;

    @Inject(optional = true)
    private transient WithAndroidEmulatorStep step;

    @StepContextParameter
    private transient Launcher launcher;

    @StepContextParameter
    private transient TaskListener listener;

    @StepContextParameter
    private transient EnvVars env;

    @StepContextParameter
    private transient Run run;

    private EmulatorState emuState;

    @Override
    protected Object run() throws Exception {
        emuState = step.getEmulatorState();

        // Wait for emulator to have booted fully
        boolean booted = waitForBootCompletion();

        // If booting times-out, clean up and give up
        if (!booted) {
            log(listener.getLogger(), Messages.BOOT_COMPLETION_TIMED_OUT(BOOT_TIMEOUT_MS / 1000));
            cleanUp(launcher, env, emuState);
            throw new AbortException("Failed to boot…");
        }

        // TODO: Stuff from freestyle
        // - start streaming logcat output to a temporary file (maybe)
        // - attempt to unlock screen
        // - initialise snapshots (if configured)

        // Execute block with environment variables for the ADB server and emulator exported
        getContext().newBodyInvoker()
                .withContext(EnvironmentExpander.merge(getContext().get(EnvironmentExpander.class),
                        new Expander(emuState)))
                .withCallback(new Callback(emuState))
                .start();

        return null;
    }

    /** @return {@code true} if the emulator booted within {@link #BOOT_TIMEOUT_MS}. */
    private boolean waitForBootCompletion() {
        final long start = System.currentTimeMillis();
        final int sleep = BOOT_TIMEOUT_MS / (int) (Math.sqrt(BOOT_TIMEOUT_MS / 1000) * 2);

        final String prop = "init.svc.bootanim";
        final String expectedAnswer = "stopped";
        final String cmd = String.format("adb -s %s wait-for-device shell getprop %s", emuState.getSerial(), prop);
        ArgumentListBuilder bootCheckCmd = new ArgumentListBuilder(Util.tokenize(cmd));

        try {
            while (System.currentTimeMillis() < start + BOOT_TIMEOUT_MS) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream(16);

                // Run "getprop", timing-out in case adb hangs
                int retVal = getProcStarter(bootCheckCmd)
                        .stdout(stream)
                        .start().joinWithTimeout(ADB_TIMEOUT_MS, TimeUnit.MILLISECONDS, listener);
                if (retVal == 0) {
                    // If boot is complete, our work here is done
                    String result = stream.toString().trim();
                    log(listener.getLogger(), Messages.EMULATOR_STATE_REPORT(result));
                    if (result.equals(expectedAnswer)) {
                        return true;
                    }
                }

                // Wait for a bit before asking the emulator again
                Thread.sleep(sleep);
            }
        } catch (InterruptedException ex) {
            log(listener.getLogger(), Messages.INTERRUPTED_DURING_BOOT_COMPLETION());
        } catch (IOException ex) {
            log(listener.getLogger(), Messages.COULD_NOT_CHECK_BOOT_COMPLETION());
            ex.printStackTrace(listener.getLogger());
        }

        // Emulator did not boot within the allotted time
        return false;
    }

    @Override
    public void stop(@Nonnull Throwable cause) throws Exception {
        log(listener.getLogger(), "Shutting down emulator...", cause);
        // TODO: This seems to duplicate work from the BodyInvoker Callback?
        cleanUp(launcher, env, emuState);
    }

    private static void cleanUp(Launcher launcher, EnvVars env, EmulatorState emuState)
            throws IOException, InterruptedException {
        final PrintStream logger = launcher.getListener().getLogger();

        // Kill the emulator
        Utils.sendEmulatorCommand(launcher, logger, emuState.getTelnetPort(), "kill", ADB_TIMEOUT_MS);

        // Kill the associated ADB server
        getProcStarter(launcher, env, emuState).cmdAsSingleString("adb kill-server")
                .stdout(logger).stderr(logger)
                .start().joinWithTimeout(ADB_TIMEOUT_MS, TimeUnit.MILLISECONDS, launcher.getListener());
    }

    private Launcher.ProcStarter getProcStarter() throws IOException, InterruptedException {
        return getProcStarter(launcher, env, emuState);
    }

    private Launcher.ProcStarter getProcStarter(ArgumentListBuilder command)
            throws IOException, InterruptedException {
        return getProcStarter().cmds(command);
    }

    private static Launcher.ProcStarter getProcStarter(Launcher launcher, EnvVars env, EmulatorState emuState) {
        env.put("ANDROID_ADB_SERVER_PORT", Integer.toString(emuState.getAdbServerPort()));
        PrintStream logger = launcher.getListener().getLogger();
        return launcher.launch().stdout(logger).stderr(logger).envs(env);
    }

    private static class Expander extends EnvironmentExpander {

        private static final long serialVersionUID = 1L;

        private final EmulatorState emuState;

        private Expander(EmulatorState emuState) {
            this.emuState = emuState;
        }

        @Override
        public void expand(@Nonnull EnvVars env) throws IOException, InterruptedException {
            // TODO: Export the other documented variables, based on the emulator config
            env.put("ANDROID_SERIAL", emuState.getSerial());
            env.put("ANDROID_ADB_SERVER_PORT", Integer.toString(emuState.getAdbServerPort()));
        }
    }

    private static class Callback extends BodyExecutionCallback.TailCall {

        private static final long serialVersionUID = 1L;

        private final EmulatorState emuState;

        Callback(EmulatorState emulatorState) {
            this.emuState = emulatorState;
        }

        @Override
        protected void finished(StepContext context) throws Exception {
            // Execution is done; kill the emulator off
            // TODO: make this configurable, depending on doAfterBoot/doOnly?
            cleanUp(context.get(Launcher.class), context.get(EnvVars.class), emuState);
        }

    }

}
