package jenkins.plugin.android.emulator.sdk.cli;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import org.apache.commons.lang.StringUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.ProxyConfiguration;
import hudson.Util;
import hudson.plugins.android_emulator.Constants;
import hudson.util.ArgumentListBuilder;
import hudson.util.Secret;

/**
 * Build a command line argument for emulator command.
 * 
 * @see <a href="https://developer.android.com/studio/run/emulator-commandline">Start the emulator from the command line</a>
 * @author Nikolas Falco
 */
public class EmulatorCLIBuilder {
    private static final String ARG_NO_BOOT_ANIM = "-no-boot-anim";
    private static final String ARG_NO_AUDIO = "-no-audio";
    private static final String ARG_WIPE_DATA = "-wipe-data";
    private static final String ARG_PROP = "-prop";
    private static final String ARG_MEMORY = "-memory";
    private static final String ARG_CAMERA_BACK = "-camera-back";
    private static final String ARG_CAMERA_FRONT = "-camera-front";
    private static final String ARG_NO_SNAPSHOT = "-no-snapshot";
    private static final String ARG_NO_SNAPSHOT_LOAD = "-no-snapshot-load";
    private static final String ARG_NO_SNAPSHOT_SAVE = "-no-snapshot-save";
    private static final String ARG_PORTS = "-ports";
    private static final String ARG_ACCEL = "-accel";
    private static final String ARG_PROXY = "-http-proxy";
    private static final String ARG_NO_WINDOW = "-no-window";

    public enum SNAPSHOT {
        NONE, PERSIST, NOT_PERSIST;
    }

    public enum CAMERA {
        NONE, EMULATED;
    }

    public static EmulatorCLIBuilder with(@Nullable FilePath executable) {
        return new EmulatorCLIBuilder(executable);
    }

    private FilePath executable;
    private String dataDir;
    private SNAPSHOT mode = SNAPSHOT.NONE;
    private CAMERA cameraBack = CAMERA.NONE;
    private CAMERA cameraFront = CAMERA.NONE;
    private int memory = -1;
    private boolean wipe = true;
    private ProxyConfiguration proxy;
    private String avdName = Constants.SNAPSHOT_NAME;
    private String locale;
    private int reportConsolePort = -1;
    private int reportConsoleTimeout;

    private EmulatorCLIBuilder(@CheckForNull FilePath executable) {
        if (executable == null) {
            throw new IllegalArgumentException("Invalid empty or null executable");
        }
        this.executable = executable;
    }

    public EmulatorCLIBuilder reportConsolePort(int port) {
        this.reportConsolePort = port;
        return this;
    }

    public EmulatorCLIBuilder reportConsoleTimeout(int timeout) {
        this.reportConsoleTimeout = timeout;
        return this;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2")
    public EmulatorCLIBuilder proxy(ProxyConfiguration proxy) {
        this.proxy = proxy;
        return this;
    }

    public EmulatorCLIBuilder dataDir(String dataDir) {
        this.dataDir = dataDir;
        return this;
    }

    public EmulatorCLIBuilder quickBoot(SNAPSHOT mode) {
        this.mode = mode;
        return this;
    }

    public EmulatorCLIBuilder cameraBack(CAMERA mode) {
        this.cameraBack = mode;
        return this;
    }

    public EmulatorCLIBuilder cameraFront(CAMERA mode) {
        this.cameraFront = mode;
        return this;
    }

    public EmulatorCLIBuilder memory(int memory) {
        this.memory = memory;
        return this;
    }

    public EmulatorCLIBuilder wipe(boolean wipe) {
        this.wipe = wipe;
        return this;
    }

    public EmulatorCLIBuilder avdName(String avdName) {
        this.avdName = avdName;
        return this;
    }

    public EmulatorCLIBuilder locale(String locale) {
        this.locale = Util.fixEmptyAndTrim(locale);
        return this;
    }

    public CLICommand<Void> build(int consolePort) {
        return build(consolePort, consolePort + 1);
    }

    public CLICommand<Void> build(int consolePort, int adbPort) {
        if (consolePort < 5554) {
            throw new IllegalArgumentException("Emulator port must be greater or equals than 5554");
        }
        EnvVars env = new EnvVars();
        ArgumentListBuilder arguments = new ArgumentListBuilder();

        if (avdName == null) {
            avdName = Constants.SNAPSHOT_NAME;
        }
        arguments.add("-avd", avdName);

        if (dataDir != null) {
            arguments.add("-datadir");
            arguments.addQuoted(dataDir);
        }

        // Quick Boot params
        switch (mode) {
        case NOT_PERSIST:
            arguments.add(ARG_NO_SNAPSHOT_SAVE);
            break;
        case PERSIST:
            arguments.add(ARG_NO_SNAPSHOT_LOAD);
            break;
        case NONE:
            arguments.add(ARG_NO_SNAPSHOT);
            break;
        default:
        }

        // Device Hardware params
        switch (cameraFront) {
        case EMULATED:
            arguments.add(ARG_CAMERA_FRONT, "emulated");
            break;
        case NONE:
            arguments.add(ARG_CAMERA_FRONT, "none");
            break;
        default:
        }

        switch (cameraBack) {
        case EMULATED:
            arguments.add(ARG_CAMERA_BACK, "emulated");
            break;
        case NONE:
            arguments.add(ARG_CAMERA_BACK, "none");
            break;
        default:
        }

        arguments.add(ARG_NO_AUDIO);
        env.put(Constants.ENV_VAR_QEMU_AUDIO_DRV, "none");

        // Disk Images and Memory params
        if (memory != -1) {
            arguments.add(ARG_MEMORY, String.valueOf(memory));
        }

        if (wipe) {
            arguments.add(ARG_WIPE_DATA);
        }

        if (locale != null) {
            Locale l = Locale.forLanguageTag(locale);
            arguments.add(ARG_PROP, "persist.sys.language=" + l.getLanguage());
            arguments.add(ARG_PROP, "persist.sys.country=" + l.getCountry());
        }

        // Network params
        arguments.add(ARG_PORTS, consolePort + "," + adbPort);

        buildProxyArguments(arguments);

        // System params
        arguments.add(ARG_ACCEL, "auto");
        arguments.add(ARG_NO_WINDOW);

        // UI params
        arguments.add(ARG_NO_BOOT_ANIM);

        if (reportConsolePort > 0) {
            arguments.add("-report-console", "tcp:" + reportConsolePort + ",max=" + reportConsoleTimeout);
        }

        return new CLICommand<>(executable, arguments, env);
    }

    private void buildProxyArguments(ArgumentListBuilder arguments) {
        if (proxy == null) {
            return;
        }

        String userInfo = Util.fixEmptyAndTrim(proxy.getUserName());
        // append password only if userName is defined
        if (userInfo != null && StringUtils.isNotBlank(proxy.getEncryptedPassword())) {
            Secret secret = Secret.decrypt(proxy.getEncryptedPassword());
            if (secret != null) {
                userInfo += ":" + Util.fixEmptyAndTrim(secret.getPlainText());
            }
        }

        arguments.add(ARG_PROXY);
        try {
            String proxyURL = new URI("http", userInfo, proxy.name, proxy.port, null, null, null).toString();
            if (userInfo != null) {
                arguments.addMasked(proxyURL);
            } else {
                arguments.add(proxyURL);
            }
        } catch (URISyntaxException e) {
            if (userInfo != null) {
                arguments.addMasked(userInfo + "@" + proxy.name + ":" + proxy.port);
            } else {
                arguments.add(proxy.name + ":" + proxy.port);
            }
        }
    }

    public CLICommand<Void> arguments(String[] args) {
        ArgumentListBuilder arguments = new ArgumentListBuilder();
        buildProxyArguments(arguments);
        arguments.add(args);

        return new CLICommand<>(executable, arguments, new EnvVars());
    }
}