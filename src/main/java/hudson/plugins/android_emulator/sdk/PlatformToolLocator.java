package hudson.plugins.android_emulator.sdk;
import hudson.plugins.android_emulator.util.Utils;
import hudson.plugins.android_emulator.SdkInstallationException;
import java.io.File;

public class PlatformToolLocator implements ToolLocator {
    private static final String BUILD_TOOLS_PATH = File.separator + "build-tools" + File.separator;

    public String findInSdk(AndroidSdk androidSdk, Tool tool) throws SdkInstallationException {
        if(tool==Tool.AAPT) {
            File buildToolsDir =new File(androidSdk.getSdkRoot() + BUILD_TOOLS_PATH);

            if(buildToolsDir.exists()) {
                String[] subDirs = buildToolsDir.list();
                return getFirstInstalledBuildToolsDir(subDirs);
            }
        }
        return File.separator + "platform-tools" + File.separator;

    }

    private String getFirstInstalledBuildToolsDir(String [] buildToolsDirs) throws SdkInstallationException {
        if(buildToolsDirs.length==0) { 
            throw new SdkInstallationException("Please install at least one set of build-tools.");
        }

        return BUILD_TOOLS_PATH + buildToolsDirs[0] + File.separator;

    }
}
