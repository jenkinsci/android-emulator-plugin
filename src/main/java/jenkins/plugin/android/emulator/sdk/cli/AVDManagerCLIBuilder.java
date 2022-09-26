/*
 * The MIT License
 *
 * Copyright (c) 2020, Nikolas Falco
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.plugin.android.emulator.sdk.cli;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import hudson.util.ArgumentListBuilder;
import jenkins.plugin.android.emulator.sdk.cli.CLICommand.OutputParser;
import jenkins.plugin.android.emulator.sdk.cli.Targets.TargetType;

/**
 * Build a command line argument for avdmanager command.
 * 
 * @author Nikolas Falco
 */
public class AVDManagerCLIBuilder {

    static class ListTargetParser implements OutputParser<List<Targets>> {

        @Override
        public List<Targets> parse(InputStream input) throws IOException {
            List<Targets> targets = new ArrayList<>();

            boolean context = false; // indicates when the useful text starting
                                     // for parsing
            Targets target = null;
            for (String line : IOUtils.readLines(input, "UTF-8")) { // NOSONAR
                line = Util.fixEmptyAndTrim(line);
                if (StringUtils.isBlank(line)) {
                    continue;
                }

                String lcLine = line.toLowerCase();
                if (!context || isHeader(lcLine)) {
                    context |= lcLine.startsWith("available android targets");
                    continue;
                }

                String key = lcLine.split(":")[0];
                String value = Util.fixEmptyAndTrim(line.split(":")[1]);
                if (value != null) {
                    switch (key) {
                    case "id":
                        target = new Targets();
                        targets.add(target);
                        int idx = value.indexOf('"');
                        target.setId(value.substring(idx + 1, value.lastIndexOf('"')));
                        break;
                    case "name":
                        if (target != null) {
                            target.setName(value);
                        }
                        break;
                    case "type":
                        if (target != null) {
                            target.setType(TargetType.valueOf(value.toLowerCase()));
                        }
                        break;
                    case "api level":
                        if (target != null) {
                            target.setAPILevel(Integer.parseInt(value));
                        }
                        break;
                    case "revision":
                        if (target != null) {
                            target.setRevision(Integer.parseInt(value));
                        }
                        break;
                    default:
                        break;
                    }
                }
            }
            return targets;
        }

        private boolean isHeader(String lcLine) {
            return lcLine.startsWith("-") || lcLine.contains("loading local repository");
        }

    }

    static class ListAVDParser implements OutputParser<List<AVDevice>> {

        @Override
        public List<AVDevice> parse(InputStream input) throws IOException {
            List<AVDevice> devices = new ArrayList<>();

            boolean context = false; // indicates when the useful text starting
            // for parsing
            AVDevice device = null;
            for (String line : IOUtils.readLines(input, "UTF-8")) { // NOSONAR
                line = Util.fixEmptyAndTrim(line);
                if (StringUtils.isBlank(line)) {
                    continue;
                }

                String lcLine = line.toLowerCase();
                if (!context || isHeader(lcLine) || lcLine.contains("android virtual devices could not be loaded")) {
                    context |= lcLine.startsWith("available android virtual devices");
                    continue;
                }

                String key = getKey(lcLine);
                String value = getValue(line);
                if (value != null) {
                    switch (key) {
                    case "name":
                        device = new AVDevice();
                        device.setName(value);
                        devices.add(device);
                        break;
                    case "path":
                        if (device != null) {
                            device.setPath(value);
                        }
                        break;
                    case "target":
                        if (device != null) {
                            device.setTarget(value);
                        }
                        break;
                    case "based on":
                        if (device != null) {
                            device.setAndroidOS(value);
                        }
                        break;
                    case "tag/abi":
                        if (device != null) {
                            device.setAndroidOS(value);
                        }
                        break;
                    case "sdcard":
                        if (device != null) {
                            device.setSDCard(value);
                        }
                        break;
                    case "error":
                        if (device != null) {
                            device.setError(value);
                        }
                        break;
                    default:
                        break;
                    }
                }
            }
            return devices;
        }

        private String getValue(String line) {
            String[] split = line.split(":");
            if (split.length > 1) {
                return Util.fixEmptyAndTrim(split[1]);
            } else {
                return null;
            }
        }

        private String getKey(String lcLine) {
            return lcLine.split(":")[0];
        }

        private boolean isHeader(String lcLine) {
            return lcLine.startsWith("-") || lcLine.contains("loading local repository");
        }

    }

    private static final String ARG_SILENT = "--silent";
    private static final String ARG_VERBOSE = "--verbose";
    private static final String ARG_CLEAR_CACHE = "--clear-cache";
    private static final String[] ARG_LIST_TARGET = new String[] { "list", "target" };
    private static final String[] ARG_LIST_AVD = new String[] { "list", "avd" };
    private static final String[] ARG_CREATE = new String[] { "create", "avd" };
    private static final String[] ARG_DELETE = new String[] { "delete", "avd" };
    private static final String ARG_NAME = "--name";
    private static final String ARG_PACKAGE = "--package";
    private static final String ARG_FORCE = "--force";
    private static final String ARG_DEVICE = "--device";
    private static final String ARG_ABI = "--abi";
    private static final String ARG_SDCARD = "--sdcard";

    private final FilePath executable;
    private boolean verbose;
    private boolean silent;
    private int sdcard = -1;
    private String packagePath;
    private String abi;
    private String device;

    private AVDManagerCLIBuilder(@CheckForNull FilePath executable) {
        if (executable == null) {
            throw new IllegalArgumentException("Invalid empty or null executable");
        }
        this.executable = executable;
    }

    public static AVDManagerCLIBuilder with(@Nullable FilePath executable) {
        return new AVDManagerCLIBuilder(executable);
    }

    public AVDManagerCLIBuilder abi(String abi) {
        this.abi = abi;
        return this;
    }

    public AVDManagerCLIBuilder device(String device) {
        this.device = device;
        return this;
    }

    public AVDManagerCLIBuilder packagePath(String packagePath) {
        this.packagePath = packagePath;
        return this;
    }

    public AVDManagerCLIBuilder verbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    public AVDManagerCLIBuilder silent(boolean silent) {
        this.silent = silent;
        return this;
    }

    /**
     * Prepare the CLI command of avdmanager to create new device.
     * 
     * @return the command line to execute.
     */
    public CLICommand<Void> create(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Device name is required");
        }

        ArgumentListBuilder arguments = new ArgumentListBuilder();

        addGlobalOptions(arguments);

        // action
        arguments.add(ARG_CREATE);

        // action options
        arguments.add(ARG_NAME, name);

        if (packagePath != null) {
            arguments.add(ARG_PACKAGE, packagePath);
        }
        if (device != null) {
            arguments.add(ARG_DEVICE, device);
        }
        if (abi != null) {
            arguments.add(ARG_ABI, abi);
        }
        if (sdcard != -1) {
            arguments.add(ARG_SDCARD, String.valueOf(sdcard));
        }
        arguments.add(ARG_FORCE);

        EnvVars env = new EnvVars();
        additionalEnv(env);

        return new CLICommand<Void>(executable, arguments, env) //
                // FIXME hardware profiles??
                .withInput("\r\n");
    }

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private void additionalEnv(EnvVars env) {
        // fix a bug in windows script where calculates wrong the
        // SDK root because raising up two parent instead of one
        env.put("AVDMANAGER_OPTS", "-Dcom.android.sdkmanager.toolsdir=" + executable.getParent().getRemote());
    }

    public CLICommand<List<Targets>> listTargets() {
        ArgumentListBuilder arguments = new ArgumentListBuilder();

        addGlobalOptions(arguments);

        // action
        arguments.add(ARG_LIST_TARGET);

        EnvVars env = new EnvVars();
        additionalEnv(env);

        return new CLICommand<List<Targets>>(executable, arguments, new EnvVars()) //
                .withParser(new ListTargetParser());
    }

    public CLICommand<List<AVDevice>> listAVD() {
        ArgumentListBuilder arguments = new ArgumentListBuilder();

        addGlobalOptions(arguments);

        // action
        arguments.add(ARG_LIST_AVD);

        EnvVars env = new EnvVars();
        additionalEnv(env);

        return new CLICommand<List<AVDevice>>(executable, arguments, new EnvVars()) //
                .withParser(new ListAVDParser());
    }

    private void addGlobalOptions(ArgumentListBuilder arguments) {
        if (verbose) {
            arguments.add(ARG_VERBOSE);
        } else if (silent) {
            arguments.add(ARG_SILENT);
        }

        arguments.add(ARG_CLEAR_CACHE);
    }

    public CLICommand<Void> deleteAVD(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Device name is required");
        }

        ArgumentListBuilder arguments = new ArgumentListBuilder();

        addGlobalOptions(arguments);

        // action
        arguments.add(ARG_DELETE);

        // action options
        arguments.add(ARG_NAME, name);

        EnvVars env = new EnvVars();
        additionalEnv(env);

        return new CLICommand<>(executable, arguments, env);
    }

    public CLICommand<Void> arguments(String[] args) {
        ArgumentListBuilder arguments = new ArgumentListBuilder();
        addGlobalOptions(arguments);
        arguments.add(args);

        EnvVars env = new EnvVars();
        additionalEnv(env);

        return new CLICommand<Void>(executable, arguments, env).withInput("\r\n");
    }

}