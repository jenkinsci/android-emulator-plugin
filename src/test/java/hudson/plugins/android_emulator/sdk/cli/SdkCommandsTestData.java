package hudson.plugins.android_emulator.sdk.cli;

public interface SdkCommandsTestData {
    final String LIST_TARGETS_LEGACY_OUTPUT = "Available Android targets:\n" + 
            "----------\n" + 
            "id: 1 or \"android-23\"\n" + 
            "     Name: Android 6.0\n" + 
            "     Type: Platform\n" + 
            "     API level: 23\n" + 
            "     Revision: 3\n" + 
            "     Skins: HVGA, QVGA, WQVGA400, WQVGA432, WSVGA, WVGA800 (default), WVGA854, WXGA720, WXGA800, WXGA800-7in\n" + 
            " Tag/ABIs : no ABIs.\n" + 
            "----------\n" + 
            "id: 2 or \"android-24\"\n" + 
            "     Name: Android 7.0\n" + 
            "     Type: Platform\n" + 
            "     API level: 24\n" + 
            "     Revision: 2\n" + 
            "     Skins: HVGA, QVGA, WQVGA400, WQVGA432, WSVGA, WVGA800 (default), WVGA854, WXGA720, WXGA800, WXGA800-7in\n" + 
            " Tag/ABIs : default/x86_64";
    final String LIST_SDK_COMPONENTS_SDKMANAGER = 
            "Info: Parsing /opt/android-sdk/add-ons/addon-google_apis-google-23/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/add-ons/addon-google_apis-google-24/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/build-tools/25.0.0/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/build-tools/25.0.1/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/build-tools/25.0.3/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/build-tools/26.0.0/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/build-tools/26.0.1/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/cmake/3.6.3155560/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/cmake/3.6.4111459/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/emulator/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/extras/android/gapid/3/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/extras/android/m2repository/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/extras/google/m2repository/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/extras/m2repository/com/android/support/constraint/constraint-layout-solver/1.0.2/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/extras/m2repository/com/android/support/constraint/constraint-layout/1.0.2/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/lldb/2.0/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/lldb/2.3/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/lldb/3.0/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/ndk-bundle/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/patcher/v4/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/platform-tools/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/platforms/android-23/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/platforms/android-24/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/platforms/android-25/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/platforms/android-26/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/sources/android-23/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/sources/android-24/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/system-images/android-24/default/x86_64/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/system-images/android-24/google_apis/x86_64/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/system-images/android-26/google_apis/x86/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/tools/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/add-ons/addon-google_apis-google-23/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/add-ons/addon-google_apis-google-24/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/build-tools/25.0.0/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/build-tools/25.0.1/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/build-tools/25.0.3/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/build-tools/26.0.0/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/build-tools/26.0.1/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/cmake/3.6.3155560/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/cmake/3.6.4111459/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/emulator/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/extras/android/gapid/3/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/extras/android/m2repository/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/extras/google/m2repository/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/extras/m2repository/com/android/support/constraint/constraint-layout-solver/1.0.2/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/extras/m2repository/com/android/support/constraint/constraint-layout/1.0.2/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/lldb/2.0/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/lldb/2.3/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/lldb/3.0/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/ndk-bundle/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/patcher/v4/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/platform-tools/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/platforms/android-23/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/platforms/android-24/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/platforms/android-25/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/platforms/android-26/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/sources/android-23/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/sources/android-24/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/system-images/android-24/default/x86_64/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/system-images/android-24/google_apis/x86_64/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/system-images/android-26/google_apis/x86/package.xml\n" + 
            "Info: Parsing /opt/android-sdk/tools/package.xml\n" + 
            "Installed packages:\n" + 
            "--------------------------------------\n" + 
            "add-ons;addon-google_apis-google-23\n" + 
            "    Description:        Google APIs\n" + 
            "    Version:            1\n" + 
            "    Installed Location: /opt/android-sdk/add-ons/addon-google_apis-google-23\n" + 
            "\n" + 
            "add-ons;addon-google_apis-google-24\n" + 
            "    Description:        Google APIs\n" + 
            "    Version:            1\n" + 
            "    Installed Location: /opt/android-sdk/add-ons/addon-google_apis-google-24\n" + 
            "\n" + 
            "build-tools;25.0.0\n" + 
            "    Description:        Android SDK Build-Tools 25\n" + 
            "    Version:            25.0.0\n" + 
            "    Installed Location: /opt/android-sdk/build-tools/25.0.0\n" + 
            "\n" + 
            "build-tools;25.0.1\n" + 
            "    Description:        Android SDK Build-Tools 25.0.1\n" + 
            "    Version:            25.0.1\n" + 
            "    Installed Location: /opt/android-sdk/build-tools/25.0.1\n" + 
            "\n" + 
            "build-tools;25.0.3\n" + 
            "    Description:        Android SDK Build-Tools 25.0.3\n" + 
            "    Version:            25.0.3\n" + 
            "    Installed Location: /opt/android-sdk/build-tools/25.0.3\n" + 
            "\n" + 
            "build-tools;26.0.0\n" + 
            "    Description:        Android SDK Build-Tools 26\n" + 
            "    Version:            26.0.0\n" + 
            "    Installed Location: /opt/android-sdk/build-tools/26.0.0\n" + 
            "\n" + 
            "build-tools;26.0.1\n" + 
            "    Description:        Android SDK Build-Tools 26.0.1\n" + 
            "    Version:            26.0.1\n" + 
            "    Installed Location: /opt/android-sdk/build-tools/26.0.1\n" + 
            "\n" + 
            "cmake;3.6.3155560\n" + 
            "    Description:        CMake 3.6.3155560\n" + 
            "    Version:            3.6.3155560\n" + 
            "    Installed Location: /opt/android-sdk/cmake/3.6.3155560\n" + 
            "\n" + 
            "cmake;3.6.4111459\n" + 
            "    Description:        CMake 3.6.4111459\n" + 
            "    Version:            3.6.4111459\n" + 
            "    Installed Location: /opt/android-sdk/cmake/3.6.4111459\n" + 
            "\n" + 
            "emulator\n" + 
            "    Description:        Android Emulator\n" + 
            "    Version:            26.1.3\n" + 
            "    Installed Location: /opt/android-sdk/emulator\n" + 
            "\n" + 
            "extras;android;gapid;3\n" + 
            "    Description:        GPU Debugging tools\n" + 
            "    Version:            3.1.0\n" + 
            "    Installed Location: /opt/android-sdk/extras/android/gapid/3\n" + 
            "\n" + 
            "extras;android;m2repository\n" + 
            "    Description:        Android Support Repository\n" + 
            "    Version:            47.0.0\n" + 
            "    Installed Location: /opt/android-sdk/extras/android/m2repository\n" + 
            "\n" + 
            "extras;google;m2repository\n" + 
            "    Description:        Google Repository\n" + 
            "    Version:            57\n" + 
            "    Installed Location: /opt/android-sdk/extras/google/m2repository\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.2\n" + 
            "    Description:        Solver for ConstraintLayout 1.0.2\n" + 
            "    Version:            1\n" + 
            "    Installed Location: /opt/android-sdk/extras/m2repository/com/android/support/constraint/constraint-layout-solver/1.0.2\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.2\n" + 
            "    Description:        ConstraintLayout for Android 1.0.2\n" + 
            "    Version:            1\n" + 
            "    Installed Location: /opt/android-sdk/extras/m2repository/com/android/support/constraint/constraint-layout/1.0.2\n" + 
            "\n" + 
            "lldb;2.0\n" + 
            "    Description:        LLDB 2.0\n" + 
            "    Version:            2.0.2558144\n" + 
            "    Installed Location: /opt/android-sdk/lldb/2.0\n" + 
            "\n" + 
            "lldb;2.3\n" + 
            "    Description:        LLDB 2.3\n" + 
            "    Version:            2.3.3614996\n" + 
            "    Installed Location: /opt/android-sdk/lldb/2.3\n" + 
            "\n" + 
            "lldb;3.0\n" + 
            "    Description:        LLDB 3.0\n" + 
            "    Version:            3.0.3970975\n" + 
            "    Installed Location: /opt/android-sdk/lldb/3.0\n" + 
            "\n" + 
            "ndk-bundle\n" + 
            "    Description:        NDK\n" + 
            "    Version:            15.1.4119039\n" + 
            "    Installed Location: /opt/android-sdk/ndk-bundle\n" + 
            "\n" + 
            "patcher;v4\n" + 
            "    Description:        SDK Patch Applier v4\n" + 
            "    Version:            1\n" + 
            "    Installed Location: /opt/android-sdk/patcher/v4\n" + 
            "\n" + 
            "platform-tools\n" + 
            "    Description:        Android SDK Platform-Tools\n" + 
            "    Version:            26.0.0\n" + 
            "    Installed Location: /opt/android-sdk/platform-tools\n" + 
            "\n" + 
            "platforms;android-23\n" + 
            "    Description:        Android SDK Platform 23\n" + 
            "    Version:            3\n" + 
            "    Installed Location: /opt/android-sdk/platforms/android-23\n" + 
            "\n" + 
            "platforms;android-24\n" + 
            "    Description:        Android SDK Platform 24\n" + 
            "    Version:            2\n" + 
            "    Installed Location: /opt/android-sdk/platforms/android-24\n" + 
            "\n" + 
            "platforms;android-25\n" + 
            "    Description:        Android SDK Platform 25\n" + 
            "    Version:            3\n" + 
            "    Installed Location: /opt/android-sdk/platforms/android-25\n" + 
            "\n" + 
            "platforms;android-26\n" + 
            "    Description:        Android SDK Platform 26\n" + 
            "    Version:            2\n" + 
            "    Installed Location: /opt/android-sdk/platforms/android-26\n" + 
            "\n" + 
            "sources;android-23\n" + 
            "    Description:        Sources for Android 23\n" + 
            "    Version:            1\n" + 
            "    Installed Location: /opt/android-sdk/sources/android-23\n" + 
            "\n" + 
            "sources;android-24\n" + 
            "    Description:        Sources for Android 24\n" + 
            "    Version:            1\n" + 
            "    Installed Location: /opt/android-sdk/sources/android-24\n" + 
            "\n" + 
            "system-images;android-24;default;x86_64\n" + 
            "    Description:        Intel x86 Atom_64 System Image\n" + 
            "    Version:            8\n" + 
            "    Installed Location: /opt/android-sdk/system-images/android-24/default/x86_64\n" + 
            "\n" + 
            "system-images;android-24;google_apis;x86_64\n" + 
            "    Description:        Google APIs Intel x86 Atom_64 System Image\n" + 
            "    Version:            16\n" + 
            "    Installed Location: /opt/android-sdk/system-images/android-24/google_apis/x86_64\n" + 
            "\n" + 
            "system-images;android-26;google_apis;x86\n" + 
            "    Description:        Google APIs Intel x86 Atom System Image\n" + 
            "    Version:            5\n" + 
            "    Installed Location: /opt/android-sdk/system-images/android-26/google_apis/x86\n" + 
            "\n" + 
            "tools\n" + 
            "    Description:        Android SDK Tools\n" + 
            "    Version:            26.0.2\n" + 
            "    Installed Location: /opt/android-sdk/tools\n" + 
            "\n" + 
            "Available Packages:\n" + 
            "--------------------------------------\n" + 
            "add-ons;addon-google_apis-google-15\n" + 
            "    Description:        Google APIs\n" + 
            "    Version:            3\n" + 
            "\n" + 
            "add-ons;addon-google_apis-google-16\n" + 
            "    Description:        Google APIs\n" + 
            "    Version:            4\n" + 
            "\n" + 
            "add-ons;addon-google_apis-google-17\n" + 
            "    Description:        Google APIs\n" + 
            "    Version:            4\n" + 
            "\n" + 
            "add-ons;addon-google_apis-google-18\n" + 
            "    Description:        Google APIs\n" + 
            "    Version:            4\n" + 
            "\n" + 
            "add-ons;addon-google_apis-google-19\n" + 
            "    Description:        Google APIs\n" + 
            "    Version:            20\n" + 
            "\n" + 
            "add-ons;addon-google_apis-google-21\n" + 
            "    Description:        Google APIs\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "add-ons;addon-google_apis-google-22\n" + 
            "    Description:        Google APIs\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "add-ons;addon-google_apis-google-23\n" + 
            "    Description:        Google APIs\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "add-ons;addon-google_apis-google-24\n" + 
            "    Description:        Google APIs\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "add-ons;addon-google_gdk-google-19\n" + 
            "    Description:        Glass Development Kit Preview\n" + 
            "    Version:            11\n" + 
            "\n" + 
            "build-tools;19.1.0\n" + 
            "    Description:        Android SDK Build-Tools 19.1\n" + 
            "    Version:            19.1.0\n" + 
            "\n" + 
            "build-tools;20.0.0\n" + 
            "    Description:        Android SDK Build-Tools 20\n" + 
            "    Version:            20.0.0\n" + 
            "\n" + 
            "build-tools;21.1.2\n" + 
            "    Description:        Android SDK Build-Tools 21.1.2\n" + 
            "    Version:            21.1.2\n" + 
            "\n" + 
            "build-tools;22.0.1\n" + 
            "    Description:        Android SDK Build-Tools 22.0.1\n" + 
            "    Version:            22.0.1\n" + 
            "\n" + 
            "build-tools;23.0.1\n" + 
            "    Description:        Android SDK Build-Tools 23.0.1\n" + 
            "    Version:            23.0.1\n" + 
            "\n" + 
            "build-tools;23.0.2\n" + 
            "    Description:        Android SDK Build-Tools 23.0.2\n" + 
            "    Version:            23.0.2\n" + 
            "\n" + 
            "build-tools;23.0.3\n" + 
            "    Description:        Android SDK Build-Tools 23.0.3\n" + 
            "    Version:            23.0.3\n" + 
            "\n" + 
            "build-tools;24.0.0\n" + 
            "    Description:        Android SDK Build-Tools 24\n" + 
            "    Version:            24.0.0\n" + 
            "\n" + 
            "build-tools;24.0.1\n" + 
            "    Description:        Android SDK Build-Tools 24.0.1\n" + 
            "    Version:            24.0.1\n" + 
            "\n" + 
            "build-tools;24.0.2\n" + 
            "    Description:        Android SDK Build-Tools 24.0.2\n" + 
            "    Version:            24.0.2\n" + 
            "\n" + 
            "build-tools;24.0.3\n" + 
            "    Description:        Android SDK Build-Tools 24.0.3\n" + 
            "    Version:            24.0.3\n" + 
            "\n" + 
            "build-tools;25.0.0\n" + 
            "    Description:        Android SDK Build-Tools 25\n" + 
            "    Version:            25.0.0\n" + 
            "\n" + 
            "build-tools;25.0.1\n" + 
            "    Description:        Android SDK Build-Tools 25.0.1\n" + 
            "    Version:            25.0.1\n" + 
            "\n" + 
            "build-tools;25.0.2\n" + 
            "    Description:        Android SDK Build-Tools 25.0.2\n" + 
            "    Version:            25.0.2\n" + 
            "\n" + 
            "build-tools;25.0.3\n" + 
            "    Description:        Android SDK Build-Tools 25.0.3\n" + 
            "    Version:            25.0.3\n" + 
            "\n" + 
            "build-tools;26.0.0\n" + 
            "    Description:        Android SDK Build-Tools 26\n" + 
            "    Version:            26.0.0\n" + 
            "\n" + 
            "build-tools;26.0.1\n" + 
            "    Description:        Android SDK Build-Tools 26.0.1\n" + 
            "    Version:            26.0.1\n" + 
            "\n" + 
            "build-tools;26.0.2\n" +
            "    Description:        Android SDK Build-Tools 26.0.2\n" +
            "    Version:            26.0.2\n" +
            "\n" +
            "build-tools;26.0.3\n" +
            "    Description:        Android SDK Build-Tools 26.0.3\n" +
            "    Version:            26.0.3\n" +
            "\n" +
            "build-tools;27.0.0\n" +
            "    Description:        Android SDK Build-Tools 27\n" +
            "    Version:            27.0.0\n" +
            "\n" +
            "build-tools;27.0.1\n" +
            "    Description:        Android SDK Build-Tools 27.0.1\n" +
            "    Version:            27.0.1\n" +
            "\n" +
            "build-tools;27.0.2\n" +
            "    Description:        Android SDK Build-Tools 27.0.2\n" +
            "    Version:            27.0.2\n" +
            "\n" +
            "build-tools;27.0.3\n" +
            "    Description:        Android SDK Build-Tools 27.0.3\n" +
            "    Version:            27.0.3\n" +
            "\n" +
            "build-tools;28.0.0-rc1\n" +
            "    Description:        Android SDK Build-Tools 28-rc1\n" +
            "    Version:            28.0.0 rc1\n" +
            "\n" +
            "cmake;3.6.4111459\n" + 
            "    Description:        CMake 3.6.4111459\n" + 
            "    Version:            3.6.4111459\n" + 
            "\n" + 
            "docs\n" + 
            "    Description:        Documentation for Android SDK\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "emulator\n" + 
            "    Description:        Android Emulator\n" + 
            "    Version:            26.1.3\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "        tools Revision 25.3\n" + 
            "\n" + 
            "extras;android;gapid;1\n" + 
            "    Description:        GPU Debugging tools\n" + 
            "    Version:            1.0.3\n" + 
            "\n" + 
            "extras;android;gapid;3\n" + 
            "    Description:        GPU Debugging tools\n" + 
            "    Version:            3.1.0\n" + 
            "\n" + 
            "extras;android;m2repository\n" + 
            "    Description:        Android Support Repository\n" + 
            "    Version:            47.0.0\n" + 
            "\n" + 
            "extras;google;auto\n" + 
            "    Description:        Android Auto Desktop Head Unit emulator\n" + 
            "    Version:            1.1\n" + 
            "\n" + 
            "extras;google;google_play_services\n" + 
            "    Description:        Google Play services\n" + 
            "    Version:            43\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "extras;google;instantapps\n" + 
            "    Description:        Instant Apps Development SDK\n" + 
            "    Version:            1.0.0\n" + 
            "\n" + 
            "extras;google;m2repository\n" + 
            "    Description:        Google Repository\n" + 
            "    Version:            57\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "extras;google;market_apk_expansion\n" + 
            "    Description:        Google Play APK Expansion library\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "extras;google;market_licensing\n" + 
            "    Description:        Google Play Licensing Library\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "extras;google;play_billing\n" + 
            "    Description:        Google Play Billing Library\n" + 
            "    Version:            5\n" + 
            "\n" + 
            "extras;google;simulators\n" + 
            "    Description:        Android Auto API Simulators\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "extras;google;webdriver\n" + 
            "    Description:        Google Web Driver\n" + 
            "    Version:            2\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0\n" + 
            "    Description:        Solver for ConstraintLayout 1.0.0\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-alpha2\n" + 
            "    Description:        com.android.support.constraint:constraint-layout-solver:1.0.0-alpha2\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-alpha3\n" + 
            "    Description:        com.android.support.constraint:constraint-layout-solver:1.0.0-alpha3\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-alpha4\n" + 
            "    Description:        com.android.support.constraint:constraint-layout-solver:1.0.0-alpha4\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-alpha5\n" + 
            "    Description:        Solver for ConstraintLayout 1.0.0-alpha5\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-alpha6\n" + 
            "    Description:        Solver for ConstraintLayout 1.0.0-alpha6\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-alpha7\n" + 
            "    Description:        Solver for ConstraintLayout 1.0.0-alpha7\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-alpha8\n" + 
            "    Description:        Solver for ConstraintLayout 1.0.0-alpha8\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-alpha9\n" + 
            "    Description:        Solver for ConstraintLayout 1.0.0-alpha9\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-beta1\n" + 
            "    Description:        Solver for ConstraintLayout 1.0.0-beta1\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-beta2\n" + 
            "    Description:        Solver for ConstraintLayout 1.0.0-beta2\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-beta3\n" + 
            "    Description:        Solver for ConstraintLayout 1.0.0-beta3\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-beta4\n" + 
            "    Description:        Solver for ConstraintLayout 1.0.0-beta4\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-beta5\n" + 
            "    Description:        Solver for ConstraintLayout 1.0.0-beta5\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.1\n" + 
            "    Description:        Solver for ConstraintLayout 1.0.1\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.2\n" + 
            "    Description:        Solver for ConstraintLayout 1.0.2\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.0\n" + 
            "    Description:        ConstraintLayout for Android 1.0.0\n" + 
            "    Version:            1\n" + 
            "    Dependencies:\n" + 
            "        extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.0-alpha2\n" + 
            "    Description:        com.android.support.constraint:constraint-layout:1.0.0-alpha2\n" + 
            "    Version:            1\n" + 
            "    Dependencies:\n" + 
            "        extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-alpha2\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.0-alpha3\n" + 
            "    Description:        com.android.support.constraint:constraint-layout:1.0.0-alpha3\n" + 
            "    Version:            1\n" + 
            "    Dependencies:\n" + 
            "        extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-alpha3\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.0-alpha4\n" + 
            "    Description:        com.android.support.constraint:constraint-layout:1.0.0-alpha4\n" + 
            "    Version:            1\n" + 
            "    Dependencies:\n" + 
            "        extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-alpha4\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.0-alpha5\n" + 
            "    Description:        ConstraintLayout for Android 1.0.0-alpha5\n" + 
            "    Version:            1\n" + 
            "    Dependencies:\n" + 
            "        extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-alpha5\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.0-alpha6\n" + 
            "    Description:        ConstraintLayout for Android 1.0.0-alpha6\n" + 
            "    Version:            1\n" + 
            "    Dependencies:\n" + 
            "        extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-alpha6\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.0-alpha7\n" + 
            "    Description:        ConstraintLayout for Android 1.0.0-alpha7\n" + 
            "    Version:            1\n" + 
            "    Dependencies:\n" + 
            "        extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-alpha7\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.0-alpha8\n" + 
            "    Description:        ConstraintLayout for Android 1.0.0-alpha8\n" + 
            "    Version:            1\n" + 
            "    Dependencies:\n" + 
            "        extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-alpha8\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.0-alpha9\n" + 
            "    Description:        ConstraintLayout for Android 1.0.0-alpha9\n" + 
            "    Version:            1\n" + 
            "    Dependencies:\n" + 
            "        extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-alpha9\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.0-beta1\n" + 
            "    Description:        ConstraintLayout for Android 1.0.0-beta1\n" + 
            "    Version:            1\n" + 
            "    Dependencies:\n" + 
            "        extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-beta1\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.0-beta2\n" + 
            "    Description:        ConstraintLayout for Android 1.0.0-beta2\n" + 
            "    Version:            1\n" + 
            "    Dependencies:\n" + 
            "        extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-beta2\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.0-beta3\n" + 
            "    Description:        ConstraintLayout for Android 1.0.0-beta3\n" + 
            "    Version:            1\n" + 
            "    Dependencies:\n" + 
            "        extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-beta3\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.0-beta4\n" + 
            "    Description:        ConstraintLayout for Android 1.0.0-beta4\n" + 
            "    Version:            1\n" + 
            "    Dependencies:\n" + 
            "        extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-beta4\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.0-beta5\n" + 
            "    Description:        ConstraintLayout for Android 1.0.0-beta5\n" + 
            "    Version:            1\n" + 
            "    Dependencies:\n" + 
            "        extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.0-beta5\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.1\n" + 
            "    Description:        ConstraintLayout for Android 1.0.1\n" + 
            "    Version:            1\n" + 
            "    Dependencies:\n" + 
            "        extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.1\n" + 
            "\n" + 
            "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.2\n" + 
            "    Description:        ConstraintLayout for Android 1.0.2\n" + 
            "    Version:            1\n" + 
            "    Dependencies:\n" + 
            "        extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.2\n" + 
            "\n" + 
            "lldb;2.0\n" + 
            "    Description:        LLDB 2.0\n" + 
            "    Version:            2.0.2558144\n" + 
            "\n" + 
            "lldb;2.1\n" + 
            "    Description:        LLDB 2.1\n" + 
            "    Version:            2.1.2852477\n" + 
            "\n" + 
            "lldb;2.2\n" + 
            "    Description:        LLDB 2.2\n" + 
            "    Version:            2.2.3271982\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "lldb;2.3\n" + 
            "    Description:        LLDB 2.3\n" + 
            "    Version:            2.3.3614996\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "ndk-bundle\n" + 
            "    Description:        NDK\n" + 
            "    Version:            15.1.4119039\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "patcher;v4\n" + 
            "    Description:        SDK Patch Applier v4\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "platform-tools\n" + 
            "    Description:        Android SDK Platform-Tools\n" + 
            "    Version:            26.0.0\n" + 
            "\n" + 
            "platforms;android-10\n" + 
            "    Description:        Android SDK Platform 10\n" + 
            "    Version:            2\n" + 
            "\n" + 
            "platforms;android-11\n" + 
            "    Description:        Android SDK Platform 11\n" + 
            "    Version:            2\n" + 
            "\n" + 
            "platforms;android-12\n" + 
            "    Description:        Android SDK Platform 12\n" + 
            "    Version:            3\n" + 
            "\n" + 
            "platforms;android-13\n" + 
            "    Description:        Android SDK Platform 13\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "platforms;android-14\n" + 
            "    Description:        Android SDK Platform 14\n" + 
            "    Version:            4\n" + 
            "\n" + 
            "platforms;android-15\n" + 
            "    Description:        Android SDK Platform 15\n" + 
            "    Version:            5\n" + 
            "\n" + 
            "platforms;android-16\n" + 
            "    Description:        Android SDK Platform 16\n" + 
            "    Version:            5\n" + 
            "\n" + 
            "platforms;android-17\n" + 
            "    Description:        Android SDK Platform 17\n" + 
            "    Version:            3\n" + 
            "\n" + 
            "platforms;android-18\n" + 
            "    Description:        Android SDK Platform 18\n" + 
            "    Version:            3\n" + 
            "\n" + 
            "platforms;android-19\n" + 
            "    Description:        Android SDK Platform 19\n" + 
            "    Version:            4\n" + 
            "\n" + 
            "platforms;android-20\n" + 
            "    Description:        Android SDK Platform 20\n" + 
            "    Version:            2\n" + 
            "\n" + 
            "platforms;android-21\n" + 
            "    Description:        Android SDK Platform 21\n" + 
            "    Version:            2\n" + 
            "\n" + 
            "platforms;android-22\n" + 
            "    Description:        Android SDK Platform 22\n" + 
            "    Version:            2\n" + 
            "\n" + 
            "platforms;android-23\n" + 
            "    Description:        Android SDK Platform 23\n" + 
            "    Version:            3\n" + 
            "\n" + 
            "platforms;android-24\n" + 
            "    Description:        Android SDK Platform 24\n" + 
            "    Version:            2\n" + 
            "\n" + 
            "platforms;android-25\n" + 
            "    Description:        Android SDK Platform 25\n" + 
            "    Version:            3\n" + 
            "\n" + 
            "platforms;android-26\n" + 
            "    Description:        Android SDK Platform 26\n" + 
            "    Version:            2\n" + 
            "\n" + 
            "platforms;android-7\n" + 
            "    Description:        Android SDK Platform 7\n" + 
            "    Version:            3\n" + 
            "\n" + 
            "platforms;android-8\n" + 
            "    Description:        Android SDK Platform 8\n" + 
            "    Version:            3\n" + 
            "\n" + 
            "platforms;android-9\n" + 
            "    Description:        Android SDK Platform 9\n" + 
            "    Version:            2\n" + 
            "\n" + 
            "sources;android-15\n" + 
            "    Description:        Sources for Android 15\n" + 
            "    Version:            2\n" + 
            "\n" + 
            "sources;android-16\n" + 
            "    Description:        Sources for Android 16\n" + 
            "    Version:            2\n" + 
            "\n" + 
            "sources;android-17\n" + 
            "    Description:        Sources for Android 17\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "sources;android-18\n" + 
            "    Description:        Sources for Android 18\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "sources;android-19\n" + 
            "    Description:        Sources for Android 19\n" + 
            "    Version:            2\n" + 
            "\n" + 
            "sources;android-20\n" + 
            "    Description:        Sources for Android 20\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "sources;android-21\n" + 
            "    Description:        Sources for Android 21\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "sources;android-22\n" + 
            "    Description:        Sources for Android 22\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "sources;android-23\n" + 
            "    Description:        Sources for Android 23\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "sources;android-24\n" + 
            "    Description:        Sources for Android 24\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "sources;android-25\n" + 
            "    Description:        Sources for Android 25\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "system-images;android-10;default;armeabi-v7a\n" + 
            "    Description:        ARM EABI v7a System Image\n" + 
            "    Version:            4\n" + 
            "\n" + 
            "system-images;android-10;default;x86\n" + 
            "    Description:        Intel x86 Atom System Image\n" + 
            "    Version:            4\n" + 
            "\n" + 
            "system-images;android-10;google_apis;armeabi-v7a\n" + 
            "    Description:        Google APIs ARM EABI v7a System Image\n" + 
            "    Version:            5\n" + 
            "\n" + 
            "system-images;android-10;google_apis;x86\n" + 
            "    Description:        Google APIs Intel x86 Atom System Image\n" + 
            "    Version:            5\n" + 
            "\n" + 
            "system-images;android-14;default;armeabi-v7a\n" + 
            "    Description:        ARM EABI v7a System Image\n" + 
            "    Version:            2\n" + 
            "\n" + 
            "system-images;android-15;default;armeabi-v7a\n" + 
            "    Description:        ARM EABI v7a System Image\n" + 
            "    Version:            4\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-15;default;mips\n" + 
            "    Description:        MIPS System Image\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "system-images;android-15;default;x86\n" + 
            "    Description:        Intel x86 Atom System Image\n" + 
            "    Version:            4\n" + 
            "\n" + 
            "system-images;android-15;google_apis;armeabi-v7a\n" + 
            "    Description:        Google APIs ARM EABI v7a System Image\n" + 
            "    Version:            5\n" + 
            "\n" + 
            "system-images;android-15;google_apis;x86\n" + 
            "    Description:        Google APIs Intel x86 Atom System Image\n" + 
            "    Version:            5\n" + 
            "\n" + 
            "system-images;android-16;default;armeabi-v7a\n" + 
            "    Description:        ARM EABI v7a System Image\n" + 
            "    Version:            4\n" + 
            "\n" + 
            "system-images;android-16;default;mips\n" + 
            "    Description:        MIPS System Image\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "system-images;android-16;default;x86\n" + 
            "    Description:        Intel x86 Atom System Image\n" + 
            "    Version:            5\n" + 
            "\n" + 
            "system-images;android-16;google_apis;armeabi-v7a\n" + 
            "    Description:        Google APIs ARM EABI v7a System Image\n" + 
            "    Version:            5\n" + 
            "\n" + 
            "system-images;android-16;google_apis;x86\n" + 
            "    Description:        Google APIs Intel x86 Atom System Image\n" + 
            "    Version:            5\n" + 
            "\n" + 
            "system-images;android-17;default;armeabi-v7a\n" + 
            "    Description:        ARM EABI v7a System Image\n" + 
            "    Version:            5\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-17;default;mips\n" + 
            "    Description:        MIPS System Image\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "system-images;android-17;default;x86\n" + 
            "    Description:        Intel x86 Atom System Image\n" + 
            "    Version:            3\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-17;google_apis;armeabi-v7a\n" + 
            "    Description:        Google APIs ARM EABI v7a System Image\n" + 
            "    Version:            5\n" + 
            "\n" + 
            "system-images;android-17;google_apis;x86\n" + 
            "    Description:        Google APIs Intel x86 Atom System Image\n" + 
            "    Version:            5\n" + 
            "\n" + 
            "system-images;android-18;default;armeabi-v7a\n" + 
            "    Description:        ARM EABI v7a System Image\n" + 
            "    Version:            4\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-18;default;x86\n" + 
            "    Description:        Intel x86 Atom System Image\n" + 
            "    Version:            3\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-18;google_apis;armeabi-v7a\n" + 
            "    Description:        Google APIs ARM EABI v7a System Image\n" + 
            "    Version:            5\n" + 
            "\n" + 
            "system-images;android-18;google_apis;x86\n" + 
            "    Description:        Google APIs Intel x86 Atom System Image\n" + 
            "    Version:            5\n" + 
            "\n" + 
            "system-images;android-19;default;armeabi-v7a\n" + 
            "    Description:        ARM EABI v7a System Image\n" + 
            "    Version:            5\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-19;default;x86\n" + 
            "    Description:        Intel x86 Atom System Image\n" + 
            "    Version:            6\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-19;google_apis;armeabi-v7a\n" + 
            "    Description:        Google APIs ARM EABI v7a System Image\n" + 
            "    Version:            30\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-19;google_apis;x86\n" + 
            "    Description:        Google APIs Intel x86 Atom System Image\n" + 
            "    Version:            30\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-21;android-tv;armeabi-v7a\n" + 
            "    Description:        Android TV ARM EABI v7a System Image\n" + 
            "    Version:            3\n" + 
            "\n" + 
            "system-images;android-21;android-tv;x86\n" + 
            "    Description:        Android TV Intel x86 Atom System Image\n" + 
            "    Version:            3\n" + 
            "\n" + 
            "system-images;android-21;default;armeabi-v7a\n" + 
            "    Description:        ARM EABI v7a System Image\n" + 
            "    Version:            4\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-21;default;x86\n" + 
            "    Description:        Intel x86 Atom System Image\n" + 
            "    Version:            5\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-21;default;x86_64\n" + 
            "    Description:        Intel x86 Atom_64 System Image\n" + 
            "    Version:            5\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-21;google_apis;armeabi-v7a\n" + 
            "    Description:        Google APIs ARM EABI v7a System Image\n" + 
            "    Version:            22\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-21;google_apis;x86\n" + 
            "    Description:        Google APIs Intel x86 Atom System Image\n" + 
            "    Version:            22\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-21;google_apis;x86_64\n" + 
            "    Description:        Google APIs Intel x86 Atom_64 System Image\n" + 
            "    Version:            22\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-22;android-tv;armeabi-v7a\n" + 
            "    Description:        Android TV ARM EABI v7a System Image\n" + 
            "    Version:            1\n" + 
            "\n" + 
            "system-images;android-22;android-tv;x86\n" + 
            "    Description:        Android TV Intel x86 Atom System Image\n" + 
            "    Version:            3\n" + 
            "\n" + 
            "system-images;android-22;default;armeabi-v7a\n" + 
            "    Description:        ARM EABI v7a System Image\n" + 
            "    Version:            2\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-22;default;x86\n" + 
            "    Description:        Intel x86 Atom System Image\n" + 
            "    Version:            6\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-22;default;x86_64\n" + 
            "    Description:        Intel x86 Atom_64 System Image\n" + 
            "    Version:            6\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-22;google_apis;armeabi-v7a\n" + 
            "    Description:        Google APIs ARM EABI v7a System Image\n" + 
            "    Version:            16\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-22;google_apis;x86\n" + 
            "    Description:        Google APIs Intel x86 Atom System Image\n" + 
            "    Version:            16\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-22;google_apis;x86_64\n" + 
            "    Description:        Google APIs Intel x86 Atom_64 System Image\n" + 
            "    Version:            16\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-23;android-tv;armeabi-v7a\n" + 
            "    Description:        Android TV ARM EABI v7a System Image\n" + 
            "    Version:            11\n" + 
            "\n" + 
            "system-images;android-23;android-tv;x86\n" + 
            "    Description:        Android TV Intel x86 Atom System Image\n" + 
            "    Version:            11\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-23;android-wear;armeabi-v7a\n" + 
            "    Description:        Android Wear ARM EABI v7a System Image\n" + 
            "    Version:            6\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-23;android-wear;x86\n" + 
            "    Description:        Android Wear Intel x86 Atom System Image\n" + 
            "    Version:            6\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-23;default;x86\n" + 
            "    Description:        Intel x86 Atom System Image\n" + 
            "    Version:            10\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-23;default;x86_64\n" + 
            "    Description:        Intel x86 Atom_64 System Image\n" + 
            "    Version:            10\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-23;google_apis;armeabi-v7a\n" + 
            "    Description:        Google APIs ARM EABI v7a System Image\n" + 
            "    Version:            23\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-23;google_apis;x86\n" + 
            "    Description:        Google APIs Intel x86 Atom System Image\n" + 
            "    Version:            23\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-23;google_apis;x86_64\n" + 
            "    Description:        Google APIs Intel x86 Atom_64 System Image\n" + 
            "    Version:            23\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-24;android-tv;x86\n" + 
            "    Description:        Android TV Intel x86 Atom System Image\n" + 
            "    Version:            12\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-24;default;arm64-v8a\n" + 
            "    Description:        ARM 64 v8a System Image\n" + 
            "    Version:            7\n" + 
            "\n" + 
            "system-images;android-24;default;armeabi-v7a\n" + 
            "    Description:        ARM EABI v7a System Image\n" + 
            "    Version:            7\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-24;default;x86\n" + 
            "    Description:        Intel x86 Atom System Image\n" + 
            "    Version:            8\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-24;default;x86_64\n" + 
            "    Description:        Intel x86 Atom_64 System Image\n" + 
            "    Version:            8\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-24;google_apis;arm64-v8a\n" + 
            "    Description:        Google APIs ARM 64 v8a System Image\n" + 
            "    Version:            16\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-24;google_apis;armeabi-v7a\n" + 
            "    Description:        Google APIs ARM EABI v7a System Image\n" + 
            "    Version:            16\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-24;google_apis;x86\n" + 
            "    Description:        Google APIs Intel x86 Atom System Image\n" + 
            "    Version:            16\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-24;google_apis;x86_64\n" + 
            "    Description:        Google APIs Intel x86 Atom_64 System Image\n" + 
            "    Version:            16\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-24;google_apis_playstore;x86\n" + 
            "    Description:        Google Play Intel x86 Atom System Image\n" + 
            "    Version:            13\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-25;android-tv;x86\n" + 
            "    Description:        Android TV Intel x86 Atom System Image\n" + 
            "    Version:            6\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-25;android-wear;armeabi-v7a\n" + 
            "    Description:        Android Wear ARM EABI v7a System Image\n" + 
            "    Version:            3\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-25;android-wear;x86\n" + 
            "    Description:        Android Wear Intel x86 Atom System Image\n" + 
            "    Version:            3\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-25;google_apis;arm64-v8a\n" + 
            "    Description:        Google APIs ARM 64 v8a System Image\n" + 
            "    Version:            8\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-25;google_apis;armeabi-v7a\n" + 
            "    Description:        Google APIs ARM EABI v7a System Image\n" + 
            "    Version:            8\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-25;google_apis;x86\n" + 
            "    Description:        Google APIs Intel x86 Atom System Image\n" + 
            "    Version:            8\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-25;google_apis;x86_64\n" + 
            "    Description:        Google APIs Intel x86 Atom_64 System Image\n" + 
            "    Version:            8\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-26;android-tv;x86\n" + 
            "    Description:        Android TV Intel x86 Atom System Image\n" + 
            "    Version:            4\n" + 
            "    Dependencies:\n" + 
            "        emulator Revision 26.1.3\n" + 
            "\n" + 
            "system-images;android-26;android-wear;x86\n" + 
            "    Description:        Android Wear Intel x86 Atom System Image\n" + 
            "    Version:            1\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "\n" + 
            "system-images;android-26;google_apis;x86\n" + 
            "    Description:        Google APIs Intel x86 Atom System Image\n" + 
            "    Version:            5\n" + 
            "    Dependencies:\n" + 
            "        emulator Revision 26.1.3\n" + 
            "\n" + 
            "system-images;android-26;google_apis_playstore;x86\n" + 
            "    Description:        Google Play Intel x86 Atom System Image\n" + 
            "    Version:            5\n" + 
            "    Dependencies:\n" + 
            "        emulator Revision 26.1.3\n" + 
            "\n" + 
            "tools\n" + 
            "    Description:        Android SDK Tools\n" + 
            "    Version:            26.0.2\n" + 
            "    Dependencies:\n" + 
            "        patcher;v4\n" + 
            "        emulator\n" + 
            "        platform-tools Revision 20\n" + 
            "\n" + 
            "done";
}
