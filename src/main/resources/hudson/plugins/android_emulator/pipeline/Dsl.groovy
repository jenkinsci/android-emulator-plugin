package hudson.plugins.android_emulator.pipeline

import org.jenkinsci.plugins.workflow.cps.CpsScript;

class Dsl implements Serializable {

    private final CpsScript script

    public Dsl(CpsScript script) {
        this.script = script
    }

    public EmulatorDescriptor forPlatform(String platformId) {
        EmulatorConfig config = new EmulatorConfig(platformId)
        return withConfig(config)
    }

    public EmulatorDescriptor withConfig(EmulatorConfig config) {
        return new EmulatorDescriptor(this, config)
    }

    // TODO: This class should be immutable, so it could be reused to boot multiple emulators with the same config
    public static class EmulatorDescriptor implements Serializable {

        private final Dsl dsl;
        private EmulatorConfig config;

        private EmulatorDescriptor(Dsl dsl, EmulatorConfig config) {
            this.dsl = dsl;
            this.config = config;
        }

        public Emulator boot() {
            Emulator emu = new Emulator(dsl, this)
            emu.boot();
            return emu;
        }

        public <V> V doOnly(Closure<V> body) {
            Emulator emu = new Emulator(dsl, this)
            EmulatorState context = emu.boot()
            dsl.script.withAndroidEmulator(context) {
                body(emu)
            }
            // TODO: Explicit, separate shutdown step in finally block?
        }

    }

    public static class Emulator implements Serializable {

        private final Dsl dsl
        private final EmulatorDescriptor descriptor

        private EmulatorState emuState

        private Emulator(Dsl dsl, EmulatorDescriptor descriptor) {
            this.dsl = dsl
            this.descriptor = descriptor
        }

        public void doStuff() {
            dsl.script.echo "Telnet port: ${emuState.telnetPort}"
        }

        /** Boots this emulator and returns immediately. i.e. The emulator will not be ready for use yet. */
        public EmulatorState boot() {
            // You only boot once
            if (emuState != null) {
                return emuState
            }

            // TODO: Locate Android SDK we want to use
            // TODO: Download and install Android SDK if necessary

            // TODO: Create emulator if necessary
            // TODO: Download and install images required for this emulator if necessary

            // Allocate ports
            // TODO: Actually allocate free ports, somehow
            int adbServerPort = 9090
            int telnetPort = 6000;
            int adbPort = 6001;
            emuState = new EmulatorState(adbServerPort, telnetPort, adbPort)

            // Start ADB server
            dsl.script.withEnv(["ANDROID_ADB_SERVER_PORT=${adbServerPort}"]) {
                dsl.script.timeout(10) {
                    // TODO: Cross-platform
                    // TODO: Need to know which Android SDK we're using
                    dsl.script.sh 'adb start-server'
                }
            }

            // Put emulator configuration together
            String platform = descriptor.config.platform.getTargetName()
            // TODO: Make this less horrible, e.g. pass in the descriptor.config directly
            hudson.plugins.android_emulator.EmulatorConfig emuConfig =
                    hudson.plugins.android_emulator.EmulatorConfig.create(null, platform, "hdpi",
                            "480x800", "en_GB", null, false, true, false, null, "x86", null, null, null);
            def args = emuConfig.getCommandArguments(null, true, true, telnetPort, adbPort, 8000, 30);

            // Boot emulator in the background
            // TODO: Cross-platform
            // TODO: Need to know which Android SDK we're using
            // TODO: Need to detect if starting the emulator fails (exit code? Windows?)
            dsl.script.withEnv(["ANDROID_ADB_SERVER_PORT=${adbServerPort}"]) {
                dsl.script.sh "emulator ${args} &"
            }

            // Return the execution info
            return emuState
        }

        /** Blocks until this emulator is ready for use, and then executes the given closure. */
        public <V> V doAfterBoot(Closure<V> body) {
            // Wait for emulator to be ready
            waitUntilBooted()

            // Run closure, with ourselves as an argument
            body.call(this)
        }

        /** Blocks until this emulator is ready for use. */
        // TODO: Probably could split out WithAndroidEmulatorStepExecution#waitForBootCompletion into its own step?
        public void waitUntilBooted() {
            // Boot, if we haven't done so already
            boot()

            // TODO: Wait for emulator to be ready
            def counter = 1
            dsl.script.waitUntil {
                dsl.script.echo("Waiting to boot: ${counter}")
                return counter++ > 2
            }
        }

        /** Shuts down this emulator. */
        public void shutDown() {
            dsl.script.echo("Killing emulator!")
        }

    }

}
