package hudson.plugins.android_emulator.sdk.cli;

public interface SdkCommandsTestData {
    String LIST_TARGETS_LEGACY_OUTPUT = """
            Available Android targets:
            ----------
            id: 1 or "android-23"
                 Name: Android 6.0
                 Type: Platform
                 API level: 23
                 Revision: 3
                 Skins: HVGA, QVGA, WQVGA400, WQVGA432, WSVGA, WVGA800 (default), WVGA854, WXGA720, WXGA800, WXGA800-7in
             Tag/ABIs : no ABIs.
            ----------
            id: 2 or "android-24"
                 Name: Android 7.0
                 Type: Platform
                 API level: 24
                 Revision: 2
                 Skins: HVGA, QVGA, WQVGA400, WQVGA432, WSVGA, WVGA800 (default), WVGA854, WXGA720, WXGA800, WXGA800-7in
             Tag/ABIs : default/x86_64""";
}
