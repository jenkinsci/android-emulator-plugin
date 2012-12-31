package hudson.plugins.android_emulator.builder;

import static hudson.plugins.android_emulator.AndroidEmulator.log;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.AbstractBuild;
import hudson.plugins.android_emulator.Messages;
import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.sdk.Tool;
import hudson.plugins.android_emulator.util.Utils;
import hudson.remoting.VirtualChannel;
import hudson.tasks.Builder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;

import org.apache.tools.ant.DirectoryScanner;
import org.kohsuke.stapler.DataBoundConstructor;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class UpdateProjectBuilder extends AbstractBuilder {

    @DataBoundConstructor
    public UpdateProjectBuilder() {
        // Nowt to do
    }

    private static final class Project {

        private final String path;

        private final ProjectType type;

        public Project(String path, ProjectType type) {
            this.path = path;
            this.type = type;
        }
    }

    public static enum ProjectType {
        APP, LIBRARY, TEST
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        final PrintStream logger = listener.getLogger();

        // Gather list of projects, determined by reading Android project files in the workspace
        log(logger, Messages.FINDING_PROJECT_PREREQUISITES()); // TODO
        List<Project> projects = build.getWorkspace().act(new ProjectFinder(listener));
        if (projects == null || projects.isEmpty()) {
            // No projects found. Odd, but that's ok
            log(logger, Messages.NO_PROJECTS_FOUND());
            return true;
        }

        // Ensure we have an SDK
        AndroidSdk androidSdk = getAndroidSdk(build, launcher, listener);
        if (androidSdk == null) {
            return false;
        }

        // TODO: Discover list of projects, library projects and test projects
        // Library projects have "android.library=true"
        // Test projects have an <instrumentation> tag in the manifest...
        // Must run android update in *each* project!

        // 2. For each library project, run 'update lib-project'
        // 3. If there's a test project AND a parent, run 'update test-project'
        // 4. For each non-library, non-test project, run 'update project'

        Collections.sort(projects, new Comparator<Project>() {

            public int compare(Project p1, Project p2) {
                if (p1.type == ProjectType.APP) {
                    return -1;
                }
                return 0;
            }
        });

        boolean haveApp = false;
        for (Project project : projects) {
            if (project.type == ProjectType.APP) {
                haveApp = true;
            }

            if (project.type == ProjectType.LIBRARY) {
                // just do it
            }
        }

        // Done!
        return true;
    }

    /** FileCallable to determine Android target projects specified in a given directory. */
    private static final class ProjectFinder implements FileCallable<List<Project>> {

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
                        log(logger, "Found project: " + p);
                        projects.add(p);
                    }
                }
            }

            return new ArrayList<Project>(projects);
        }

        /**
         *
         * @param logger
         * @param projectFile
         * @return
         */
        private static Project getProjectFromProjectFile(PrintStream logger, File projectFile) {
            String dir = projectFile.getParent();
            ProjectType type;

            try {
                Map<String, String> config = Utils.parseConfigFile(projectFile);
                boolean isLibrary = Boolean.valueOf(config.get("android.library"));
                if (isLibrary) {
                    type = ProjectType.LIBRARY;
                } else if (isTestProject(logger, new File(dir))) {
                    type = ProjectType.TEST;
                } else {
                    type = ProjectType.APP;
                }
            } catch (IOException e) {
                // TODO: new message
                log(logger, Messages.READING_PROJECT_FILE_FAILED(), e);
                e.printStackTrace();
                return null;
            }

            log(logger, String.format("Found project at '%s' of type %s", dir, type));
            return new Project(dir, type);
        }

        private static boolean isTestProject(PrintStream logger, File projectDir) {
            File manifest = new File(projectDir, "AndroidManifest.xml");
            try {
                // Best indication that this is a test project is the <instrumentation> manifest tag
                XPath xPath = XPathFactory.newInstance().newXPath();
                InputSource source = new InputSource(new FileReader(manifest));
                NodeList result = (NodeList) xPath.evaluate("//instrumentation", source,
                        XPathConstants.NODESET);
                return result.getLength() > 0;
            } catch (XPathException e) {
                // TODO: more specific message
                log(logger, Messages.READING_PROJECT_FILE_FAILED(), e);
            } catch (IOException e) {
                // TODO: new message
                log(logger, Messages.READING_PROJECT_FILE_FAILED(), e);
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
            return Functions.getResourcePath() + "/plugin/android-emulator/help-installPrerequisites.html";
        }

        @Override
        public String getDisplayName() {
            return "Run 'android update project'";
        }

    }

}
