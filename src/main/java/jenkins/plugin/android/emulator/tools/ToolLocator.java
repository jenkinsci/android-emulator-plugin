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
package jenkins.plugin.android.emulator.tools;

import java.io.File;
import java.io.IOException;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import hudson.FilePath;
import hudson.Launcher;
import hudson.plugins.android_emulator.sdk.Tool;
import hudson.remoting.VirtualChannel;
import jenkins.security.MasterToSlaveCallable;

public class ToolLocator {
    private final class LookupExecuteCallable extends MasterToSlaveCallable<String, IOException> {

        private static final long serialVersionUID = -6703610106678288597L;

        private final Tool tool;

        public LookupExecuteCallable(Tool tool) {
            this.tool = tool;
        }

        @Override
        public String call() throws IOException {
            Platform currentPlatform = platform;
            File toolHome = new File(home, tool.toolLocator.findInSdk(false));
            if (!toolHome.exists()) {
                toolHome = new File(home, tool.toolLocator.findInSdk(true));
            }
            File cmd = new File(toolHome, tool.getExecutable(currentPlatform != Platform.WINDOWS));
            if (cmd.exists()) {
                return cmd.getPath();
            }
            return null;
        }
    }

    private final Platform platform;
    private final String home;

    public ToolLocator(@Nonnull Platform platform, @CheckForNull String home) {
        this.platform = platform;
        this.home = home;
    }

    /**
     * Gets the executable path of SDKManager on the given target system.
     *
     * @param launcher a way to start processes
     * @return the sdkmanager executable in the system is exists, {@code null}
     *         otherwise.
     * @throws InterruptedException if the step is interrupted
     * @throws IOException if something goes wrong
     */
    public FilePath getSDKManager(final Launcher launcher) throws InterruptedException, IOException {
        return getToolLocation(launcher, Tool.SDKMANAGER);
    }

    /**
     * Gets the executable path of AVDManager on the given target system.
     *
     * @param launcher a way to start processes
     * @return the avdmanager executable in the system is exists, {@code null}
     *         otherwise.
     * @throws InterruptedException if the step is interrupted
     * @throws IOException if something goes wrong
     */
    public FilePath getAVDManager(final Launcher launcher) throws InterruptedException, IOException {
        return getToolLocation(launcher, Tool.AVDMANAGER);
    }

    /**
     * Gets the executable path of ADB on the given target system.
     *
     * @param launcher a way to start processes
     * @return the adb executable in the system is exists, {@code null}
     *         otherwise.
     * @throws InterruptedException if the step is interrupted
     * @throws IOException if something goes wrong
     */
    public FilePath getADB(final Launcher launcher) throws InterruptedException, IOException {
        return getToolLocation(launcher, Tool.ADB);
    }

    /**
     * Gets the executable path of emulator on the given target system.
     *
     * @param launcher a way to start processes
     * @return the emulator executable in the system is exists, {@code null}
     *         otherwise.
     * @throws InterruptedException if the step is interrupted
     * @throws IOException if something goes wrong
     */
    public FilePath getEmulator(Launcher launcher) throws InterruptedException, IOException {
        return getToolLocation(launcher, Tool.EMULATOR);
    }

    private FilePath getToolLocation(final Launcher launcher, Tool tool) throws IOException, InterruptedException {
        // DO NOT REMOVE this callable otherwise paths constructed by File
        // and similar API will be based on the master node O.S.
        final VirtualChannel channel = launcher.getChannel();
        if (channel == null) {
            throw new IOException("Unable to get a channel for the launcher");
        }
        return new FilePath(channel, channel.call(new LookupExecuteCallable(tool)));
    }
}
