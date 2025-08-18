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
import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.sdk.cli.SdkCliCommand;
import hudson.plugins.android_emulator.sdk.cli.SdkCliCommandFactory;
import hudson.plugins.android_emulator.sdk.cli.SdkToolsCommands;
import hudson.plugins.android_emulator.util.Utils;
import hudson.plugins.android_emulator.util.ConfigFileUtils;
import hudson.remoting.VirtualChannel;
import hudson.tasks.Builder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.tools.ant.DirectoryScanner;
import org.jvnet.localizer.Localizable;
import org.kohsuke.stapler.DataBoundConstructor;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jenkins.MasterToSlaveFileCallable;

public class UpdateProjectBuilder extends AbstractBuilder {

    @DataBoundConstructor
    public UpdateProjectBuilder() {
        // No configuration required
    }

    private static final class Project implements Serializable {

        private final String path;

        private final ProjectType type;

        public Project(String path, ProjectType type) {
            this.path = path;
            this.type = type;
        }

        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj);
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }

        private static final long serialVersionUID = 1l;

    }

    private enum ProjectType {
        APP(Messages._PROJECT_TYPE_APP()), //
        LIBRARY(Messages._PROJECT_TYPE_LIBRARY()), //
        TEST(Messages._PROJECT_TYPE_TEST());

        private final Localizable name;

        private ProjectType(Localizable name) {
            this.name = name;
        }
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        final PrintStream logger = listener.getLogger();

        // Ensure we have an SDK, and export ANDROID_HOME
        AndroidSdk androidSdk = getAndroidSdk(build, launcher, listener);
        if (androidSdk == null) {
            return false;
        }

        // As the whole functionality of handling projects was dropped (moved to Android Studio)
        // with SDK Tools r25.3, upgrading is needed on users side.
        if (androidSdk.isAndroidCmdDeprecated()) {
            return true;
        }

        // Gather list of projects, determined by reading Android project files in the workspace
        log(logger, Messages.FINDING_PROJECTS());
        final FilePath buildWorkspace = build.getWorkspace();
        if (buildWorkspace == null) {
            throw new BuildNodeUnavailableException();
        }
        final List<Project> projects = buildWorkspace.act(new ProjectFinder(listener));
        if (projects == null || projects.isEmpty()) {
            // No projects found. Odd, but that's ok
            log(logger, Messages.NO_PROJECTS_FOUND_TO_UPDATE());
            return true;
        }
        log(logger, Messages.FOUND_PROJECTS_TO_UPDATE(projects.size()));

        // Calling "update project" doesn't work unless the target platform is installed
        new ProjectPrerequisitesInstaller().perform(build, launcher, listener);

        // Run the appropriate command for each project found
        final String workspace = getWorkspacePath(buildWorkspace);
        for (Project project : projects) {
            log(logger, "");

            final String projectPath = ".";
            String testMainClass = "";

            FilePath dir = new FilePath(new File(project.path));

            if (project.type == ProjectType.TEST) {
                // Find the "nearest" app project
                int minDistance = Integer.MAX_VALUE;
                Project appProject = null;
                for (Project p : projects) {
                    if (p.type != ProjectType.APP) {
                        continue;
                    }
                    int distance = Utils.getRelativePathDistance(project.path, p.path);
                    if (distance < minDistance) {
                        appProject = p;
                        minDistance = distance;
                    }
                }

                // We should have found something, but otherwise just log it and move on
                if (appProject == null) {
                    log(logger, Messages.FOUND_TEST_PROJECT_WITHOUT_APP(project.path));
                    continue;
                }

                // Determine relative path to the app project from this test project
                testMainClass = Utils.getRelativePath(project.path, appProject.path);
            }

            // Run the project update command
            String shortPath;
            if (workspace.equals(project.path)) {
                shortPath = ".";
            } else {
                shortPath = project.path.substring(workspace.length() + 1);
            }
            log(logger, Messages.CREATING_BUILD_FILES(project.type.name.toString(), shortPath));

            final SdkToolsCommands sdkCommands = SdkCliCommandFactory.getCommandsForSdk(androidSdk);
            final SdkCliCommand updateProjectCmd;

            switch (project.type) {
            case LIBRARY:
                updateProjectCmd = sdkCommands.getUpdateLibProjectCommand(projectPath);
                break;
            case TEST:
                updateProjectCmd = sdkCommands.getUpdateTestProjectCommand(projectPath, testMainClass);
                break;
            case APP:
            default:
                updateProjectCmd = sdkCommands.getUpdateProjectCommand(projectPath);
                break;
            }

            if (!updateProjectCmd.isNoopCmd()) {
                Utils.runAndroidTool(launcher, logger, logger, androidSdk, updateProjectCmd, dir);
            }
        }

        // Done!
        return true;
    }

    /** Determines the canonical path to the current build's workspace. */
    private static String getWorkspacePath(FilePath workspace) throws IOException,
            InterruptedException {
        return workspace.act(new MasterToSlaveFileCallable<String>() {
            private static final long serialVersionUID = 1L;

            public String invoke(File f, VirtualChannel channel) throws IOException {
                return f.getCanonicalPath();
            }
        });
    }

    /** FileCallable to determine Android target projects specified in a given directory. */
    private static final class ProjectFinder extends MasterToSlaveFileCallable<List<Project>> {

        private final BuildListener listener;
        private transient PrintStream logger;

        ProjectFinder(BuildListener listener) {
            this.listener = listener;
        }

        public List<Project> invoke(File workspace, VirtualChannel channel)
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
            Collection<Project> projects = new HashSet<Project>();
            String[] files = scanner.getIncludedFiles();
            if (files != null) {
                for (String filename : files) {
                    Project p = getProjectFromProjectFile(logger, new File(workspace, filename));
                    if (p != null) {
                        projects.add(p);
                    }
                }
            }

            return new ArrayList<Project>(projects);
        }

        /** Determines the type of an Android project from its directory. */
        private static Project getProjectFromProjectFile(PrintStream logger, File projectFile) {
            String dir;
            ProjectType type;

            try {
                dir = projectFile.getParentFile().getCanonicalPath();

                Map<String, String> config = ConfigFileUtils.parseConfigFile(projectFile);
                boolean isLibrary = Boolean.valueOf(config.get("android.library"));
                if (isLibrary) {
                    type = ProjectType.LIBRARY;
                } else if (isTestProject(logger, new File(dir))) {
                    type = ProjectType.TEST;
                } else {
                    type = ProjectType.APP;
                }
            } catch (IOException e) {
                log(logger, Messages.FAILED_TO_DETERMINE_PROJECT_TYPE(projectFile), e);
                e.printStackTrace();
                return null;
            }

            return new Project(dir, type);
        }

        /** Determines whether the given directory contains an Android test project. */
        @SuppressFBWarnings("DM_DEFAULT_ENCODING")
        private static boolean isTestProject(PrintStream logger, File projectDir) {
            File manifest = new File(projectDir, "AndroidManifest.xml");
            try {
                // Best indication that this is a test project is the <instrumentation> manifest tag
                XPath xPath = XPathFactory.newInstance().newXPath();
                InputSource source = new InputSource(new FileReader(manifest));
                NodeList result = (NodeList) xPath.evaluate("//instrumentation", source,
                        XPathConstants.NODESET);
                return result.getLength() > 0;
            } catch (XPathExpressionException e) {
                // Not sure this could ever happen...
                log(logger, Messages.MANIFEST_XPATH_FAILURE(manifest), e);
            } catch (IOException e) {
                log(logger, Messages.FAILED_TO_READ_MANIFEST(manifest));
            }

            // Failed to read file
            return false;
        }

        private static final long serialVersionUID = 1L;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Builder> implements Serializable {

        private static final long serialVersionUID = 1L;

        public DescriptorImpl() {
            super(UpdateProjectBuilder.class);
        }

        @Override
        public String getHelpFile() {
            return Functions.getResourcePath() + "/plugin/android-emulator/help-createBuildFiles.html";
        }

        @Override
        public String getDisplayName() {
            return Messages.CREATE_PROJECT_BUILD_FILES();
        }

    }

}
