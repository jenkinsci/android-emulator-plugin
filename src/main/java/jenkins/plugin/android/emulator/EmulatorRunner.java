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
package jenkins.plugin.android.emulator;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.Nullable;

import org.apache.commons.lang3.StringUtils;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.model.TaskListener;
import hudson.plugins.android_emulator.Constants;
import hudson.plugins.android_emulator.Messages;
import hudson.plugins.android_emulator.ReceiveEmulatorPortTask;
import jenkins.model.Jenkins;
import jenkins.plugin.android.emulator.sdk.cli.ADBCLIBuilder;
import jenkins.plugin.android.emulator.sdk.cli.AVDManagerCLIBuilder;
import jenkins.plugin.android.emulator.sdk.cli.AVDevice;
import jenkins.plugin.android.emulator.sdk.cli.EmulatorCLIBuilder;
import jenkins.plugin.android.emulator.sdk.cli.EmulatorCLIBuilder.SNAPSHOT;
import jenkins.plugin.android.emulator.sdk.cli.SDKManagerCLIBuilder;
import jenkins.plugin.android.emulator.sdk.cli.SDKPackages;
import jenkins.plugin.android.emulator.tools.AndroidSDKInstaller.Channel;
import jenkins.plugin.android.emulator.tools.ToolLocator;

public class EmulatorRunner {

    private final EmulatorConfig config;
    private final ToolLocator locator;

    public EmulatorRunner(@NonNull EmulatorConfig config, @NonNull ToolLocator locator) {
        this.config = config;
        this.locator = locator;
    }

    public void run(@NonNull FilePath workspace,
                    @NonNull TaskListener listener,
                    @Nullable EnvVars env) throws IOException, InterruptedException {
        Launcher launcher = workspace.createLauncher(listener);
        if (env == null) {
            env = new EnvVars();
        }

        ProxyConfiguration proxy = Jenkins.get().proxy;

        FilePath avdManager = locator.getAVDManager(launcher);
        if (avdManager == null) {
            throw new AbortException(jenkins.plugin.android.emulator.Messages.noExecutableFound(avdManager));
        }
        FilePath sdkManager = locator.getSDKManager(launcher);
        if (sdkManager == null) {
            throw new AbortException(jenkins.plugin.android.emulator.Messages.noExecutableFound(sdkManager));
        }
        FilePath adb = locator.getADB(launcher);
        if (adb == null) {
            throw new AbortException(jenkins.plugin.android.emulator.Messages.noExecutableFound(adb));
        }
        FilePath emulator = locator.getEmulator(launcher);
        if (emulator == null) {
            throw new AbortException(jenkins.plugin.android.emulator.Messages.noExecutableFound(emulator));
        }

        String avdHome = env.get(AndroidSDKConstants.ENV_ANDROID_AVD_HOME);
        String sdkRoot = env.get(Constants.ENV_VAR_ANDROID_SDK_ROOT); // FIXME required!

        // read installed components
        SDKPackages packages = SDKManagerCLIBuilder.with(sdkManager) //
            .channel(Channel.STABLE) // FIXME get that one configured in the installation tool
            .sdkRoot(sdkRoot) //
            .proxy(proxy) //
            .list() //
            .withEnv(env) //
            .execute();
        listener.getLogger().println("SDK Manager is reading installed components");

        // gather required components
        Set<String> components = getComponents();
        packages.getInstalled().forEach(p -> components.remove(p.getId()));
        if (!components.isEmpty()) {
            SDKManagerCLIBuilder.with(sdkManager) //
                    .channel(Channel.STABLE) // FIXME get that one configured in the installation tool
                    .sdkRoot(sdkRoot) //
                    .proxy(proxy) //
                    .install(components) //
                    .withEnv(env) //
                    .execute();
            listener.getLogger().println("SDK Manager is installing " + StringUtils.join(components, ' '));
        }

        // check if there are running device
        List<AVDevice> devices = AVDManagerCLIBuilder.with(avdManager) //
                .silent(true) //
                .listAVD() //
                .withEnv(env) //
                .execute();

        if (devices.stream().anyMatch(d -> config.getAVDName().equals(d.getName()))) {
            listener.getLogger().println("Android Virtual Device " + config.getAVDName() + " already exist, removing...");

            AVDManagerCLIBuilder.with(avdManager) //
                    .silent(true) //
                    .deleteAVD(config.getAVDName()) //
                    .withEnv(env) //
                    .execute();
        }

        // create new device
        listener.getLogger().println("AVD Manager is creating a new device named " + config.getAVDName() + " using sysimage "
                + getSystemComponent());

        AVDManagerCLIBuilder.with(avdManager) //
                .silent(true) //
                .packagePath(getSystemComponent()) //
                .create(config.getAVDName()) //
                .withEnv(env) //
                .execute();

        // create AVD descriptor file
        writeConfigFile(new FilePath(avdManager.getChannel(), avdHome));

        // start ADB service
        ADBCLIBuilder.with(adb) //
                .maxEmulators(1) // FIXME set equals to the number of node executors
                .port(config.getADBServerPort()) //
                .start() //
                .withEnv(env) //
                .execute();

        // start emulator
        EmulatorCLIBuilder.with(emulator) //
                .avdName(config.getAVDName()) //
                .dataDir(avdHome) //
                .locale(config.getLocale()) //
                .reportConsoleTimeout(config.getADBConnectionTimeout()) //
                .reportConsolePort(config.getReportPort()) // FIXME
                .proxy(proxy) //
                .quickBoot(SNAPSHOT.NOT_PERSIST)
                .build(5554) // FIXME calculate the free using the executor number, in case of multiple emulator for this executor than store into a map <Node, port> pay attention on Node that could not be saved into an aware map.
                .withEnv(env) //
                .executeAsync(listener);

        Integer port = workspace.act(new ReceiveEmulatorPortTask(config.getReportPort(), config.getADBConnectionTimeout()));
        if (port <= 0) {
            throw new IOException(Messages.EMULATOR_DID_NOT_START()); // FIXME
        }
    }

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private void writeConfigFile(FilePath avdHome) throws IOException, InterruptedException {
        FilePath advPath = avdHome.child(config.getAVDName() + ".avd");
        FilePath advConfig = avdHome.child(config.getAVDName() + ".ini");

        String remoteAVDPath = advPath.getRemote();
        String remoteAndroidHome = avdHome.getParent().getRemote();

        advConfig.touch(new Date().getTime());
        String content = "avd.ini.encoding=UTF-8\n" + 
                "path=" + remoteAVDPath + "\n" +
                "path.rel=" + remoteAVDPath.substring(remoteAndroidHome.length() + 1) + "\n" +
                "target=" + config.getOSVersion();
        advConfig.write(content, "UTF-8");
    }

    private Set<String> getComponents() {
        Set<String> components = new LinkedHashSet<>();
        components.add(buildComponent("platforms", config.getOSVersion()));
        components.add(getSystemComponent());
        return components;
    }

    private String getSystemComponent() {
        return buildComponent("system-images", config.getOSVersion(), "default", config.getTargetABI());
    }

    private String buildComponent(String...parts) {
        return StringUtils.join(parts, ';');
    }

}
