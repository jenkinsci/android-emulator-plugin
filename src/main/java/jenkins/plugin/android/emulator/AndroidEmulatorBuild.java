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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.CheckForNull;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.android_emulator.Constants;
import hudson.plugins.android_emulator.ScreenDensity;
import hudson.plugins.android_emulator.ScreenResolution;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import jenkins.plugin.android.emulator.EmulatorConfig.ValidationError;
import jenkins.plugin.android.emulator.sdk.home.DefaultHomeLocator;
import jenkins.plugin.android.emulator.sdk.home.HomeLocator;
import jenkins.plugin.android.emulator.tools.AndroidSDKInstallation;
import jenkins.tasks.SimpleBuildWrapper;

public class AndroidEmulatorBuild extends SimpleBuildWrapper {

    private static class EnvVarsAdapter extends EnvVars {
        private static final long serialVersionUID = 1L;

        private final transient Context context; // NOSONAR

        public EnvVarsAdapter(@NonNull Context context) {
            this.context = context;
        }

        @Override
        public String put(String key, String value) {
            context.env(key, value);
            return null; // old value does not exist, just one binding for key
        }

        @Override
        public void override(String key, String value) {
            put(key, value);
        }
    }

    private final String osVersion;
    private final String screenDensity;
    private final String screenResolution;
    private final String emulatorTool;
    private String deviceLocale;
    private String deviceDefinition;
    private String sdCardSize;
    private String targetABI;
    private HomeLocator homeLocationStrategy;
    private String avdName;
    private List<HardwareProperty> hardwareProperties = new ArrayList<>();

    // advanced options
    private int adbTimeout;

    @DataBoundConstructor
    public AndroidEmulatorBuild(@CheckForNull String emulatorTool, String osVersion, String screenDensity, String screenResolution) {
        this.emulatorTool = Util.fixEmptyAndTrim(emulatorTool);
        this.osVersion = Util.fixEmptyAndTrim(osVersion);
        this.screenDensity = Util.fixEmptyAndTrim(screenDensity);
        this.screenResolution = Util.fixEmptyAndTrim(screenResolution);
        this.homeLocationStrategy = new DefaultHomeLocator();
    }

    @Override
    public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars initialEnvironment) throws IOException, InterruptedException {
        // get specific installation for the node
        AndroidSDKInstallation sdk = AndroidSDKUtil.getAndroidSDK(emulatorTool);
        if (sdk == null) {
            throw new AbortException(Messages.noInstallationFound(emulatorTool));
        }

        Computer computer = workspace.toComputer();
        if (computer == null) {
            throw new AbortException(jenkins.plugin.android.emulator.Messages.nodeNotAvailable());
        }
        Node node = computer.getNode();
        if (node == null) {
            throw new AbortException(jenkins.plugin.android.emulator.Messages.nodeNotAvailable());
        }
        sdk = sdk.forNode(node, listener);
        sdk = sdk.forEnvironment(initialEnvironment);

        EnvVarsAdapter contextEnv = new EnvVarsAdapter(context);
        sdk.buildEnvVars(contextEnv);

        // configure home location
        FilePath homeLocation = homeLocationStrategy.locate(workspace);
        HomeLocator.buildEnvVars(homeLocation, contextEnv);

        // replace variable in user input
        final EnvVars env = initialEnvironment.overrideAll(context.getEnv());
        EmulatorConfig config = new EmulatorConfig();
        config.setOSVersion(Util.replaceMacro(osVersion, env));
        config.setScreenDensity(Util.replaceMacro(screenDensity, env));
        config.setScreenResolution(Util.replaceMacro(screenResolution, env));
        config.setAVDName(Util.replaceMacro(avdName, env));
        config.setLocale(Util.replaceMacro(deviceLocale, env));
        config.setDefinition(Util.replaceMacro(deviceDefinition, env));
        config.setCardSize(Util.replaceMacro(sdCardSize, env));
        config.setTargetABI(Util.replaceMacro(targetABI, env));
        config.setHardware(hardwareProperties.stream() //
                .map(p -> new HardwareProperty(Util.replaceMacro(p.getKey(), env), Util.replaceMacro(p.getValue(), env))) //
                .collect(Collectors.toList()));
        config.setADBConnectionTimeout(adbTimeout * 1000);
        config.setReportPort(55000); // FIXME

        // validate input
        Collection<ValidationError> errors = config.validate();
        if (!errors.isEmpty()) {
            throw new AbortException(StringUtils.join(errors, "\n"));
        }

        EmulatorRunner emulatorRunner = new EmulatorRunner(config, sdk.getToolLocator());
        emulatorRunner.run(workspace, listener, env);
    }

    /**
     * Needed for syntax snippet generator.
     *
     * @return installation name to use by this step.
     */
    public String getEmulatorTool() {
        return emulatorTool;
    }

    /**
     * Needed for syntax snippet generator.
     *
     * @return the Android O.S. version.
     */
    public String getOsVersion() {
        return osVersion;
    }

    /**
     * Needed for syntax snippet generator.
     *
     * @return the screen pixel density (dpi).
     */
    public String getScreenDensity() {
        return screenDensity;
    }

    /**
     * Needed for syntax snippet generator.
     *
     * @return the screen resolution like 480x640.
     */
    public String getScreenResolution() {
        return screenResolution;
    }

    public HomeLocator getHomeLocationStrategy() {
        return homeLocationStrategy;
    }

    @DataBoundSetter
    public void setHomeLocationStrategy(HomeLocator homeLocationStrategy) {
        this.homeLocationStrategy = homeLocationStrategy == null ? new DefaultHomeLocator() : homeLocationStrategy;
    }

    public String getAvdName() {
        return avdName;
    }

    @DataBoundSetter
    public void setAvdName(String avdName) {
        this.avdName = avdName;
    }

    public String getDeviceLocale() {
        return deviceLocale;
    }

    @DataBoundSetter
    public void setDeviceLocale(String deviceLocale) {
        this.deviceLocale = deviceLocale;
    }

    public String getDeviceDefinition() {
        return deviceDefinition;
    }

    @DataBoundSetter
    public void setDeviceDefinition(String deviceDefinition) {
        this.deviceDefinition = deviceDefinition;
    }

    public String getSdCardSize() {
        return sdCardSize;
    }

    @DataBoundSetter
    public void setSdCardSize(String sdCardSize) {
        this.sdCardSize = sdCardSize;
    }

    public String getTargetABI() {
        return targetABI;
    }

    @DataBoundSetter
    public void setTargetABI(String targetABI) {
        this.targetABI = targetABI;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP")
    public List<HardwareProperty> getHardwareProperties() {
        return hardwareProperties;
    }

    @DataBoundSetter
    public void setHardwareProperties(List<HardwareProperty> hardwareProperties) {
        this.hardwareProperties = hardwareProperties;
    }

    public int getAdbTimeout() {
        return adbTimeout == 0 ? AndroidSDKConstants.ADB_CONNECT_TIMEOUT : adbTimeout;
    }

    @DataBoundSetter
    public void setAdbTimeout(int adbTimeout) {
        this.adbTimeout = adbTimeout;
    }

    @Symbol("androidEmulator")
    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.AndroidEmulatorBuild_displayName();
        }

        public FormValidation doCheckOsVersion(@QueryParameter @CheckForNull String osVersion) {
            if (StringUtils.isBlank(osVersion)) {
                return FormValidation.error(Messages.required());
            }
            return FormValidation.ok();
        }

        public ComboBoxModel doFillScreenDensityItems() {
            ComboBoxModel values = new ComboBoxModel();
            for (ScreenDensity density : ScreenDensity.values()) {
                values.add(density.toString());
            }
            return values;
        }

        public FormValidation doCheckScreenDensity(@QueryParameter @CheckForNull String screenDensity) {
            if (StringUtils.isBlank(screenDensity)) {
                return FormValidation.error(Messages.required());
            } else if (ScreenDensity.valueOf(screenDensity) == null) {
                return FormValidation.error(Messages.AndroidEmulatorBuild_wrongDensity());
            }
            return FormValidation.ok();
        }

        public ComboBoxModel doFillScreenResolutionItems() {
            ComboBoxModel values = new ComboBoxModel();
            for (ScreenResolution resolution : ScreenResolution.values()) {
                values.add(resolution.toString());
            }
            return values;
        }

        public FormValidation doCheckScreenResolution(@QueryParameter @CheckForNull String screenResolution) {
            if (StringUtils.isBlank(screenResolution)) {
                return FormValidation.error(Messages.required());
            } else if (ScreenResolution.valueOf(screenResolution) == null) {
                return FormValidation.error(Messages.AndroidEmulatorBuild_wrongDensity());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckDeviceLocale(@QueryParameter @CheckForNull String deviceLocale) {
            if (StringUtils.isBlank(deviceLocale)) {
                return FormValidation.warning(Messages.AndroidEmulatorBuild_defaultLocale(Constants.DEFAULT_LOCALE));
            }

            try {
                Locale locale = Locale.forLanguageTag(deviceLocale);
                if (locale.getISO3Language() != null && locale.getISO3Country() != null) {
                    return FormValidation.ok();
                }
            } catch (MissingResourceException e) {
                return FormValidation.error(e, Messages.AndroidEmulatorBuild_wrongLocale());
            }
            return FormValidation.error(Messages.AndroidEmulatorBuild_wrongLocale());
        }

        public ComboBoxModel doFillDeviceLocaleItems() {
            ComboBoxModel options = new ComboBoxModel();
            for (Locale locale : Locale.getAvailableLocales()) {
                options.add(locale.toLanguageTag());
            }
            return options;
        }

        public FormValidation doCheckSdCardSize(@QueryParameter @CheckForNull String sdCardSize) {
            if (StringUtils.isBlank(sdCardSize)) {
                return FormValidation.ok();
            }

            try {
                int size = Integer.parseInt(sdCardSize);
                if (size < 9) {
                    return FormValidation.error(Messages.AndroidEmulatorBuild_sdCardTooSmall());
                }
            } catch (NumberFormatException e) {
                // maybe it's a variable
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckTargetAbi(@QueryParameter String targetABI) {
            if (StringUtils.isBlank(targetABI)) {
                return FormValidation.error(Messages.required());
            }

//            for (String s : Constants.TARGET_ABIS) {
//                if (s.equals(value) || (value.contains("/") && value.endsWith(s))) {
//                    return ValidationResult.ok();
//                }
//            }
//            return ValidationResult.error(Messages.INVALID_TARGET_ABI());
            return FormValidation.ok();
        }
    }
}
