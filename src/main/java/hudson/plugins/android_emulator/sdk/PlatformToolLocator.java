package hudson.plugins.android_emulator.sdk;
import hudson.plugins.android_emulator.util.Utils;
import hudson.plugins.android_emulator.SdkInstallationException;
import java.io.File;

public class PlatformToolLocator implements ToolLocator {
    public String findInSdk(AndroidSdk androidSdk, Tool tool) throws SdkInstallationException {
        if(tool==Tool.AAPT) {
            String buildToolsDirectory ="/build-tools/";
            File buildToolsDir =new File(androidSdk.getSdkRoot() + buildToolsDirectory);

            if(buildToolsDir.exists()) {
                return getFirstInstalledBuildToolsDir(buildToolsDir);
            }
        }
        return "/platform-tools/";
    }

    private String getFirstInstalledBuildToolsDir(File buildToolsDir) throws SdkInstallationException {
        String[] subDirs = buildToolsDir.list();
        if(subDirs.length==0) { 
            throw new SdkInstallationException("Please install at least one set of build-tools.");
        }

        return buildToolsDir +"/"+subDirs[0]+"/";

    }
}
