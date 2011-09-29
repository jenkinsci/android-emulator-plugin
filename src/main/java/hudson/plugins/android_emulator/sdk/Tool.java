package hudson.plugins.android_emulator.sdk;

public enum Tool {
    AAPT("aapt", ".exe", true),
    ADB("adb", ".exe", true),
    ANDROID("android", ".bat"),
    EMULATOR("emulator", ".exe"),
    MKSDCARD("mksdcard", ".exe");

    final String executable;
    final String windowsExtension;
    final boolean isPlatformTool;

    Tool(String executable, String windowsExtension) {
        this(executable, windowsExtension, false);
    }

    Tool(String executable, String windowsExtension, boolean isPlatformTool) {
        this.executable = executable;
        this.windowsExtension = windowsExtension;
        this.isPlatformTool = isPlatformTool;
    }

    public boolean isPlatformTool() {
        return isPlatformTool;
    }

    public String getExecutable(boolean isUnix) {
        if (isUnix) {
            return executable;
        }
        return executable + windowsExtension;
    }

    public static String[] getAllExecutableVariants() {
        final Tool[] tools = values();
        String[] executables = new String[tools.length * 2];
        for (int i = 0, n = tools.length; i < n; i++) {
            executables[i*2] = tools[i].getExecutable(true);
            executables[i*2+1] = tools[i].getExecutable(false);
        }

        return executables;
    }

}