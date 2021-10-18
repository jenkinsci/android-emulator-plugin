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

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.plugins.android_emulator.Constants;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import jenkins.plugin.android.emulator.Messages;
import jenkins.plugin.android.emulator.tools.AndroidSDKInstaller.Channel;
import net.sf.json.JSONObject;

/**
 * Information about JDK installation.
 *
 * @author Nikolas Falco
 * @since 4.0
 */
@SuppressWarnings("serial")
public class AndroidSDKInstallation extends ToolInstallation implements EnvironmentSpecific<AndroidSDKInstallation>, NodeSpecific<AndroidSDKInstallation> {
    private Platform platform;

    @DataBoundConstructor
    public AndroidSDKInstallation(String name, String home, List<? extends ToolProperty<?>> properties, Platform platform) {
        super(name, home, properties);
        this.platform = platform;
    }

    @Override
    public AndroidSDKInstallation forEnvironment(EnvVars environment) {
        return new AndroidSDKInstallation(getName(), environment.expand(getHome()), getProperties().toList(), platform);
    }

    @Override
    public AndroidSDKInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new AndroidSDKInstallation(getName(), translateFor(node, log), getProperties().toList(), Platform.of(node));
    }

    /**
     * Gets a locator for CLI executables installed by this tool.
     *
     * @param launcher a way to start processes
     * @return a locator for CLI executables for this tool
     * @throws IOException if something goes wrong
     */
    public ToolLocator getToolLocator() throws IOException {
        return new ToolLocator(getPlatform(), getHome());
    }

    @Override
    public void buildEnvVars(EnvVars env) {
        env.put(Constants.ENV_VAR_ANDROID_SDK_ROOT, getHome());
        env.put(Constants.ENV_VAR_PATH_SDK_TOOLS, getBin());
        super.buildEnvVars(env);
    }

    /**
     * Calculate the tools bin folder based on current Node platform. We can't
     * use {@link Computer#currentComputer()} because it's always null in case of
     * pipeline.
     *
     * @return path of the bin folder for the installation tool in the current
     *         Node.
     */
    private String getBin() {
        Platform currentPlatform = null;
        try {
            currentPlatform = getPlatform();
        } catch (DetectionFailedException e) {
            throw new RuntimeException(e);  // NOSONAR
        }

        String bin = getHome();
        if (!StringUtils.isBlank(currentPlatform.binFolder)) {
            switch (currentPlatform) {
            case WINDOWS:
                bin += "\\tools\\" + currentPlatform.binFolder;
                break;
            case LINUX:
            case OSX:
            default:
                bin += "/tools/" + currentPlatform.binFolder;
            }
        }

        return bin;
    }

    @Nonnull
    private Platform getPlatform() throws DetectionFailedException {
        Platform currentPlatform = platform;

        // missed to call method forNode?
        if (currentPlatform == null) {
            Computer computer = Computer.currentComputer();
            if (computer != null) {
                Node node = computer.getNode();
                if (node == null) {
                    throw new DetectionFailedException(Messages.nodeNotAvailable());
                }

                currentPlatform = Platform.of(node);
            } else {
                // pipeline or MasterToSlave use case
                currentPlatform = Platform.current();
            }

            platform = currentPlatform;
        }

        return currentPlatform;
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<AndroidSDKInstallation> {
        public DescriptorImpl() {
            // load installations at Jenkins startup
            load();
        }

        @Override
        public String getDisplayName() {
            return Messages.AndroidSDKInstallation_displayName();
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            return Collections.singletonList(new AndroidSDKInstaller(null, Channel.STABLE));
        }

        /*
         * (non-Javadoc)
         * @see hudson.tools.Descriptor#configure(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject)
         */
        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws hudson.model.Descriptor.FormException {
            boolean result = super.configure(req, json);
            /*
             * Invoked when the global configuration page is submitted. If
             * installation are modified programmatically than it's a developer
             * task perform the call to save method on this descriptor.
             */
            save();
            return result;
        }

    }

}
