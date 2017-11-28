package hudson.plugins.android_emulator.builder;

import static hudson.plugins.android_emulator.AndroidEmulator.log;
import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.AbstractBuild;
import hudson.plugins.android_emulator.BuildNodeUnavailableException;
import hudson.plugins.android_emulator.Messages;
import hudson.plugins.android_emulator.SdkInstaller;
import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.util.ConfigFileUtils;
import hudson.remoting.VirtualChannel;
import hudson.tasks.Builder;
import jenkins.MasterToSlaveFileCallable;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

import org.apache.tools.ant.DirectoryScanner;
import org.kohsuke.stapler.DataBoundConstructor;

public class ProjectPrerequisitesInstaller extends AbstractBuilder {

    @DataBoundConstructor
    public ProjectPrerequisitesInstaller() {
        // Nowt to do
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        final PrintStream logger = listener.getLogger();

        // Gather list of platforms specified by Android project files in the workspace
        log(logger, Messages.FINDING_PROJECTS());
        final FilePath workspace = build.getWorkspace();
        if (workspace == null) {
            throw new BuildNodeUnavailableException();
        }
        final Collection<String> platforms = workspace.act(new ProjectPlatformFinder(listener));
        if (platforms == null || platforms.isEmpty()) {
            // Nothing to install, but that's ok
            log(logger, Messages.NO_PROJECTS_FOUND_FOR_PREREQUISITES());
            return true;
        }

        // Ensure we have an SDK
        AndroidSdk androidSdk = getAndroidSdk(build, launcher, listener);
        if (androidSdk == null) {
            return false;
        }

        // Install platform(s)
        log(logger, Messages.ENSURING_PLATFORMS_INSTALLED(platforms));
        for (String platform : platforms) {
            SdkInstaller.installPlatform(logger, launcher, androidSdk, platform, null, true);
        }

        // Done!
        return true;
    }

    /** FileCallable to determine Android target projects specified in a given directory. */
    private static final class ProjectPlatformFinder extends MasterToSlaveFileCallable<Collection<String>> {

        private final BuildListener listener;
        private transient PrintStream logger;

        ProjectPlatformFinder(BuildListener listener) {
            this.listener = listener;
        }

        public Collection<String> invoke(File workspace, VirtualChannel channel)
                throws IOException, InterruptedException {
            if (logger == null) {
                logger = listener.getLogger();
            }

            // Find the appropriate file: project.properties or default.properties
            final String[] filePatterns = { "**/default.properties", "**/project.properties" };
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(workspace);
            scanner.setIncludes(filePatterns);
            scanner.scan();

            // Extract platform from each config file
            Collection<String> platforms = new HashSet<String>();
            String[] files = scanner.getIncludedFiles();
            if (files != null) {
                for (String filename : files) {
                    String platform = getPlatformFromProjectFile(logger, new File(workspace, filename));
                    if (platform != null) {
                        log(logger, Messages.PROJECT_HAS_PLATFORM(filename, platform));
                        platforms.add(platform);
                    }
                }
            }

            return platforms;
        }

        private static String getPlatformFromProjectFile(PrintStream logger, File f) {
            String platform = null;
            try {
                // Read configured target platform from file
                platform = ConfigFileUtils.parseConfigFile(f).get("target");
                if (platform != null) {
                    platform = platform.trim();
                }
            } catch (IOException e) {
                log(logger, Messages.READING_PROJECT_FILE_FAILED(), e);
                e.printStackTrace();
            }
            return platform;
        }

        private static final long serialVersionUID = 1L;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Builder> implements Serializable {

        private static final long serialVersionUID = 1L;

        public DescriptorImpl() {
            super(ProjectPrerequisitesInstaller.class);
        }

        @Override
        public String getHelpFile() {
            return Functions.getResourcePath() + "/plugin/android-emulator/help-installPrerequisites.html";
        }

        @Override
        public String getDisplayName() {
            return Messages.INSTALL_PREREQUISITES();
        }

    }

}
