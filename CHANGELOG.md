## Version history

### Version 652.v185536c05086 (November 12, 2024)

-👷 Changes for plugin developers
- fix(maven): Revert incrementals. (#222) @gounthar

### Version 651.v471170b_2a_734 (November 12, 2024)

- 🚀 New features and improvements
    - fix(maven): Incrementals. (#221) @gounthar
- 🐛 Bug fixes
    - fix(maven): Incrementals. (#221) @gounthar
    - Update Android SDK command line tools to version 12.0 (#219) @nfalco79
- 👷 Changes for plugin developers
    - fix(maven): Incrementals. (#221) @gounthar
- ✍ Other changes
    - chore(maven): Move the BOM to the latest baseline. (#220) @gounthar
    - chore(dependencies): From weekly to LTS (#218) @gounthar
    - Update Jenkins Security Scan action (#213) @strangelookingnerd
    - Enable Jenkins Security Scan (#208) @strangelookingnerd
    - Fix 'escape-by-default' XML PI (#207) @daniel-beck
    - Updates pom.xml to depend on commons-lang3-api and exclude commons-lang3 where it is brought in transitively. (#180) @gounthar
    - Plugin modernization (#179) @gounthar
    - Test with Java 21, dependencies clean-up and modernization. (#173) @gounthar
    - chore(build): Tests on JDK21. (#169) @gounthar

### Version 592.vb_b_6d427f1923 (July 21, 2023)

- 🐛 Bug fixes
    - Increase core baseline (#153) @basil
- 👻 Maintenance
    - Remove usages of Prototype (#152) @basil
    - Refresh plugin for July 2023 (#151) @basil
- ✍ Other changes
    - fix(cd): tag should match ${scmTag}, in the best case (#159) @gounthar
    - fix(cd): Fully automatic versioning (#158) @gounthar
    - fix(releases): Setting up automated plugin release (#157) @gounthar
    - feature(release): Setting up automated plugin release (#156) @gounthar
    - Create release-drafter.yml (#155) @gounthar
    - fix(documentation): Fixes misprints (#154) @gounthar
    - chore(deps): bump plugin from 4.64 to 4.66 (#144) @dependabot
    - chore(deps): bump plugin from 4.63 to 4.64 (#143) @dependabot
    - chore(deps): bump plugin from 4.58 to 4.59 (#133) @dependabot
    - Bump bom-2.361.x from 1742.vb_70478c1b_25f to 1750.v0071fa_4c4a_e3 (#110) @dependabot
    - Bump plugin from 4.51 to 4.53 (#109) @dependabot
    - Bump bom-2.346.x from 1723.vcb_9fee52c9fc to 1742.vb_70478c1b_25f (#108) @dependabot
    - Bump bom-2.346.x from 1706.vc166d5f429f8 to 1723.vcb_9fee52c9fc (#106) @dependabot
    - [SECURITY] Fix Temporary File Information Disclosure Vulnerability (#103) @JLLeitschuh
    - Bump plugin from 4.50 to 4.51 (#105) @dependabot
    - Bump bom-2.346.x from 1678.vc1feb_6a_3c0f1 to 1706.vc166d5f429f8 (#104) @dependabot
    - Bump plugin from 4.49 to 4.50 (#102) @dependabot
    - Bump bom-2.346.x from 1670.v7f165fc7a_079 to 1678.vc1feb_6a_3c0f1 (#101) @dependabot
    - Bump spotbugs-maven-plugin from 4.7.2.2 to 4.7.3.0 (#100) @dependabot
    - Bump spotbugs-maven-plugin from 4.7.2.1 to 4.7.2.2 (#99) @dependabot
    - Bump bom-2.346.x from 1654.vcb_69d035fa_20 to 1670.v7f165fc7a_079 (#98) @dependabot
    - Bump bom-2.346.x from 1643.v1cffef51df73 to 1654.vcb_69d035fa_20 (#97) @dependabot
    - Bump plugin from 4.48 to 4.49 (#96) @dependabot
    - Bump apk-parser from 2.2.1 to 2.6.10 (#90) @dependabot
    - Bump assertj-core from 3.15.0 to 3.23.1 (#89) @dependabot
    - Bump spotbugs-maven-plugin from 4.7.2.0 to 4.7.2.1 (#92) @dependabot
    - chore(gitpod) Move to gitpod. (#95) @gounthar
    - Bump bom-2.346.x from 1607.va_c1576527071 to 1643.v1cffef51df73 (#93) @dependabot
    - Bump plugin from 4.47 to 4.48 (#91) @dependabot
    - Use most recent parent pom (#94) @gounthar
    - Automate dependency updates (#88) @gounthar
    - Use https: for scm URL, not git: (#87) @gounthar
    - Require 2.346.3 as minimum Jenkins version (#86) @gounthar
    - Replaced deprecated Util.copy with built-in java function (#84) @StefanSpieker
    - Tables to Divs migration in order to be compatible with new jenkins v… (#83) @sorobon

### Version 3.1.1 (August 2, 2020)

- Bugfixes
    - JENKINS-36174 Fix default value on plugin update
    - Fix minor UI issue and alignement

### Version 3.1 (August 2, 2020)

- Bugfixes
    - [JENKINS-36174](https://issues.jenkins-ci.org/browse/JENKINS-36174) Make ADB connection timeout configurable
- Improvements
    - [JENKINS-61782](https://issues.jenkins-ci.org/browse/JENKINS-61782) Latest Android SDK command line tools package is not supporte
    - [JENKINS-51197](https://issues.jenkins-ci.org/browse/JENKINS-51197) Auto-Installation does not work if latest build-tools are an RC version

### Version 3.0 (December 4, 2017)

Many thanks to [Michael Musenbrock](https://github.com/redeamer) for
doing most of the heavy lifting on this release.

-   Added support for Android Emulator 2.0
    ([JENKINS-40178](https://issues.jenkins-ci.org/browse/JENKINS-40178),
    [JENKINS-43215](https://issues.jenkins-ci.org/browse/JENKINS-43215),
    [JENKINS-44490](https://issues.jenkins-ci.org/browse/JENKINS-44490))

    -   The QEMU2 engine will be used automatically, and should be
        faster and more stable

    -   Older SDK Tools will be automatically upgraded to a modern
        version as appropriate

    -   Fixed creation of non-default ABI images with SDK Tools \< 25.3
        ([JENKINS-32737](https://issues.jenkins-ci.org/browse/JENKINS-32737))

    -   Thanks to Michael Musenbrock

-   Fixed to select the configured emulator executable when listing
    snapshots
    ([JENKINS-34678](https://issues.jenkins-ci.org/browse/JENKINS-34678))

    -   Thanks to [Karol Wrótniak](https://github.com/koral--)

-   Updated to non-deprecated artifact archiving mechanism
    ([JENKINS-26941](https://issues.jenkins-ci.org/browse/JENKINS-26941))

    -   Thanks to [Tarek Belkahia](https://github.com/tokou)

-   Added configuration-time check for application ID in the APK
    uninstall step ([PR
    \#53](https://github.com/jenkinsci/android-emulator-plugin/pull/53))

    -   Thanks to [Sung Kim](https://github.com/hunkim)

-   Fixed Findbugs warnings, reduced other warnings, and removed
    deprecated code usages
    ([JENKINS-45456](https://issues.jenkins-ci.org/browse/JENKINS-45456))

    -   Thanks to Michael Musenbrock

-   Added a Jenkinsfile for [ci.jenkins.io](https://ci.jenkins.io/)

-   Updated minimum Jenkins version to 2.32

### Version 2.15 (May 23, 2016)

-   Ensure that newer emulators aren't left running when a build
    completes
    ([JENKINS-35004](https://issues.jenkins-ci.org/browse/JENKINS-35004))
    -   This is required as SDK Tools 25.1.6 introduced a breaking
        change to the emulator console interface

### Version 2.14.1 (April 20, 2016)

-   Fix crash when using named emulators
    ([JENKINS-34152](https://issues.jenkins-ci.org/browse/JENKINS-34152))
-   Updated names and inline help for build steps that create project
    build files or install prerequisites, to mention that these only
    work for the deprecated Ant build system

### Version 2.14 (April 8, 2016)

-   Fixed severe reliability issues when multiple emulators were running
    at the same time
-   Improved emulator startup detection to be more reliable
    -   Thanks to Andy Piper
-   Prevented emulators from using the new QEMU2 engine, which is
    missing required features (e.g. Android bugs
    [\#202762](http://b.android.com/202762),
    [\#202853](http://b.android.com/202853),
    [\#205202](http://b.android.com/205202),
    [\#205204](http://b.android.com/205204),
    [\#205272](http://b.android.com/205272))
-   Ensured that the screen density is configured when creating an
    emulator
-   Added the ability to use the dedicated screen unlock command on
    Android 6+
    ([JENKINS-30849](https://issues.jenkins-ci.org/browse/JENKINS-30849))
-   Implemented [master-agent access
    control](https://wiki.jenkins.io/display/JENKINS/Slave+To+Master+Access+Control)
-   When auto-installing the Android SDK, version 24.4.1 is now used
-   Added support for newer screen densities that are in use (400, 420,
    560dpi)
-   Added support for Android 6.0

### Version 2.13.1 (April 9, 2015)

-   Fixed an issue where the plugin would prematurely declare that an
    emulator had fully started up
    ([JENKINS-27702](https://issues.jenkins-ci.org/browse/JENKINS-27702))
    -   Thanks to Mads Kalør

### Version 2.13 (March 12, 2015)

-   Reverted to the "localhost:XXXX" style of connecting to emulators,
    as using "emulator-XXXX" seemed to be a very common cause of
    emulator startup failures
    ([JENKINS-11952](https://issues.jenkins-ci.org/browse/JENKINS-11952))
-   Fixed inability to launch Android tools on Unix slaves from a
    Windows master due to a bad path separator
    ([JENKINS-23134](https://issues.jenkins-ci.org/browse/JENKINS-23134))
    -   Thanks to Dave Brown
-   Fixed the naming of emulators using x86-based Google APIs as Google
    changed the naming scheme again
    ([JENKINS-23252](https://issues.jenkins-ci.org/browse/JENKINS-23252))
-   Fixed the naming of emulators due to a change in the ABI naming
    format.
    ([JENKINS-25336](https://issues.jenkins-ci.org/browse/JENKINS-25336))
    -   Thanks to Louis Davin
-   Enabled the automated installation of tagged system images, e.g.
    `android-wear/x86`
-   Fixed issue where starting a named AVD ignored the configured
    emulator executable
    ([JENKINS-26338](https://issues.jenkins-ci.org/browse/JENKINS-26338))
    -   Thanks to Chiara Chiappini
-   Fixed the inability to start an emulator in certain cases on 64-bit
    Mac OS X machines with SDK tools version 23.0.4 or newer
    ([JENKINS-26893](https://issues.jenkins-ci.org/browse/JENKINS-26893))
-   Switched to using "init.svc.bootanim" to more reliably detect boot
    completion, where appropriate
    ([JENKINS-22555](https://issues.jenkins-ci.org/browse/JENKINS-22555))
-   Removed reliance on the `aapt` tool and the unreliable detection
    code surrounding it
-   APK install/uninstall build steps now wait for the system package
    manager to be available before trying to do anything
-   Added timeouts to APK install/uninstall build steps as it's not
    uncommon that `adb` hangs during installation (e.g. [Android bug
    \#10255](http://b.android.com/10255))
-   Added timeouts to `adb` when attempting to unlock the screen after
    boot
-   Increased minimum Jenkins requirement to 1.565.1 to get a crash fix
    important for Java 8 users
    ([JENKINS-21341](https://issues.jenkins-ci.org/browse/JENKINS-21341))
-   Added option to run monkey on multiple (or no) packages or intent
    categories
    ([JENKINS-13559](https://issues.jenkins-ci.org/browse/JENKINS-13559))
    -   Thanks to Mads Kalør
-   Added option to pass extra command line parameters to the monkey
    tool
    ([JENKINS-13559](https://issues.jenkins-ci.org/browse/JENKINS-13559))
-   The SDK tools and platform tools directories of the SDK in use are
    now prepended to `$PATH` during a build
    -   This means you no longer need to specify the full path to `adb`
        in an "Execute shell" build step, for example
-   When auto-installing the Android SDK, version 24.0.2 is now used
-   Added support for Android 5.1

### Version 2.12 (October 21, 2014)

-   Added support for Android 5.0, 64-bit system images, and xxxhdpi
    screen density
-   Fix naming of emulator, so that newer x86 images can be used
    ([JENKINS-23252](https://issues.jenkins-ci.org/browse/JENKINS-23252))
    -   Thanks to Thomas Keller
-   Wait for ADB server to start before starting the emulator
    ([JENKINS-11952](https://issues.jenkins-ci.org/browse/JENKINS-11952))
    -   Should help with cases where the emulator starts faster than the
        ADB server
    -   Thanks to Richard Mortimer

### Version 2.11.1 (May 19, 2014)

-   Added support for Android 4.3 and 4.4

### Version 2.11 (May 18, 2014)

-   Fixed problem connecting to ADB with non four-digit port numbers
    ([JENKINS-12821](https://issues.jenkins-ci.org/browse/JENKINS-12821),
    [JENKINS-20819](https://issues.jenkins-ci.org/browse/JENKINS-20819))
    -   This should enable connecting to emulators from the
        android-maven-plugin
    -   Thanks to Richard Mortimer
-   Use "emulator" instead of "localhost" when connecting to emulators
    ([JENKINS-12821](https://issues.jenkins-ci.org/browse/JENKINS-12821),
    [JENKINS-22334](https://issues.jenkins-ci.org/browse/JENKINS-22334))
    -   This should fix strange connection failures or multiple devices
        which started appearing in recent SDK tools versions
    -   Thanks to Richard Mortimer
-   Added support for build-tools so that aapt can be located in newer
    SDKs
    ([JENKINS-18584](https://issues.jenkins-ci.org/browse/JENKINS-18584))
    -   Thanks to Steve Moyer
-   Work around [Android bug \#34233](http://b.android.com/34233) when
    parsing the snapshot list
    -   Thanks to Matt McClure
-   Fixed parsing of snapshot list, for snapshots larger than 1GB
    -   Thanks to Matt McClure
-   Fixed automated opt-out of usage statistics
    ([JENKINS-14557](https://issues.jenkins-ci.org/browse/JENKINS-14557),
    [JENKINS-21280](https://issues.jenkins-ci.org/browse/JENKINS-21280))
    -   Thanks to Matt McClure
-   Increased emulator startup timeout from 180 to 360 seconds
    -   Thanks to Matt McClure
-   Fixed parsing of relative paths on Windows
    ([JENKINS-18970](https://issues.jenkins-ci.org/browse/JENKINS-18970)).
    -   Thanks to Aitor Mendaza-Ormaza
-   Accept multi-line properties when parsing project.properties files
    ([JENKINS-22530](https://issues.jenkins-ci.org/browse/JENKINS-22530))
    -   Thanks to xstex
-   Allow adding a suffix to generated AVD names
    ([JENKINS-11083](https://issues.jenkins-ci.org/browse/JENKINS-11083))
    -   This makes it possible to use the exact same emulator config in
        two jobs without one job having to block waiting for the other
        job to finish using the emulator.
    -   Thanks to Hasan Hosgel and Payman Delshad
-   Fixed paths to ensure the inline help text should always be properly
    displayed
    ([JENKINS-20303](https://issues.jenkins-ci.org/browse/JENKINS-20303))
-   Ensure that system images are installed in all cases where required
    ([JENKINS-17532](https://issues.jenkins-ci.org/browse/JENKINS-17532))
-   Ensure that named AVDs still work, even when "keep AVDs in
    workspace" is enabled
    ([JENKINS-18919](https://issues.jenkins-ci.org/browse/JENKINS-18919))
-   Ensure the "Create project files" build step always imports or
    installs an Android SDK
-   Emulator window is no longer shown by default
-   Emulator snapshots are no longer enabled by default as they are not
    very reliable
    ([JENKINS-17126](https://issues.jenkins-ci.org/browse/JENKINS-17126))
-   Don't allow multiple jobs to block each other, if they use build
    parameters to set emulator properties
-   Removed incorrect warnings about potentially incorrect
    density/resolution configuration
    ([JENKINS-13313](https://issues.jenkins-ci.org/browse/JENKINS-13313))
-   When auto-installing the Android SDK, version 22.6.2 is now used
-   Explicitly added MIT licence to the project config
    ([JENKINS-20009](https://issues.jenkins-ci.org/browse/JENKINS-20009))

### Version 2.10 (June 3, 2013)

-   Fixed problems with Android 2.3.3 emulators caused by renaming the
    x86 ABI package
    ([JENKINS-14741](https://issues.jenkins-ci.org/browse/JENKINS-14741))
-   Licence agreements are accepted when auto-installing SDK components
    ([JENKINS-17997](https://issues.jenkins-ci.org/browse/JENKINS-17997))
-   Fixed auto-detection of the SDK in PATH
    ([JENKINS-17816](https://issues.jenkins-ci.org/browse/JENKINS-17816))
-   Updated SDK auto-detection to handle the new 'build-tools' SDK
    component
    ([JENKINS-18015](https://issues.jenkins-ci.org/browse/JENKINS-18015))
-   Various new components are now automatically installed along with
    the SDK
    -   Build Tools
    -   Android and Google local m2 repositories for use with Gradle
        builds
-   When auto-installing the Android SDK, version 21.0.1 is now used

### Version 2.9.1 (April 12, 2013)

-   Fixed a regression in 2.9 which could cause problems running `adb`
    from certain build steps

### Version 2.9 (April 11, 2013)

-   Improved detection of app project when creating build files for a
    test project
    ([JENKINS-17531](https://issues.jenkins-ci.org/browse/JENKINS-17531))
-   ABI field is now ignored when creating emulators which don't support
    ABIs
    ([JENKINS-14741](https://issues.jenkins-ci.org/browse/JENKINS-14741))
-   Resolved issue when automatically installing SDK on a slave
    ([JENKINS-16720](https://issues.jenkins-ci.org/browse/JENKINS-16720))
-   Builds can now be failed if package installation fails
    ([JENKINS-13932](https://issues.jenkins-ci.org/browse/JENKINS-13932))
-   Builds can now be failed if package uninstallation fails
    ([JENKINS-16246](https://issues.jenkins-ci.org/browse/JENKINS-16246))
-   SD card value in matrix jobs is no longer altered when saving
    configuration
    ([JENKINS-13931](https://issues.jenkins-ci.org/browse/JENKINS-13931))
-   Set LD\_LIBRARY\_PATH for emulator to run on 64-bit Linux
    ([JENKINS-14901](https://issues.jenkins-ci.org/browse/JENKINS-14901))
-   Added environment variable pointing to temporary logcat file during
    a build
    ([JENKINS-12572](https://issues.jenkins-ci.org/browse/JENKINS-12572))
-   Added documentation for 'Create Android build files' step
    ([JENKINS-17456](https://issues.jenkins-ci.org/browse/JENKINS-17456))
-   Raised required Jenkins version to 1.466; Hudson is no longer
    supported

### Version 2.8.1 (February 11, 2013)

-   Fix issues with "Create Android build files" build step when running
    on slaves, or on projects in the workspace root
-   Updated all links to android.com in the inline help, since Android
    moved some pages without redirecting the URL (see
    [JENKINS-14860](https://issues.jenkins-ci.org/browse/JENKINS-14860))

### Version 2.8 (February 1, 2013)

-   Add build step which creates Android build files for app, library
    and test projects
-   When auto-installing the Android SDK, now a more up-to-date version
    is installed (20.0.1)

View older versions...

### Version 2.7.1 (November 29, 2012)

-   Fixed bug where emulators couldn't be launched with older SDK
    versions
    ([JENKINS-15967](https://issues.jenkins-ci.org/browse/JENKINS-15967))
    -   Thanks to Jørgen Tjernø

### Version 2.7 (November 26, 2012)

-   Fixed missing icons for monkey results
    ([JENKINS-15903](https://issues.jenkins-ci.org/browse/JENKINS-15903))
-   Add ability to specify `emulator` executable
    -   Thanks to Jan Berkel
-   Added support for Android 4.2 and the xxhdpi screen density
    -   Thanks to Hisayoshi Suehiro

### Version 2.6 (October 10, 2012)

-   Updates relating to use of the "Install Android project
    prerequisites" build step:
    -   Android SDK and tools will now be automatically installed, if
        required
    -   System images for a given platform won't be installed when not
        necessary
-   The name of the system image used is now included in the AVD name
    ([JENKINS-14740](https://issues.jenkins-ci.org/browse/JENKINS-14740))
-   Variables are now expanded in the "Target ABI" field
-   Fixed serialisation issue which could cause SDK install to fail on
    some setups
    ([JENKINS-13420](https://issues.jenkins-ci.org/browse/JENKINS-13420))
    -   Thanks to Kohsuke Kawaguchi

### Version 2.4 (September 17, 2012)

-   Fixed SDK version string parsing for preview builds of the Android
    SDK tools
    ([JENKINS-15097](https://issues.jenkins-ci.org/browse/JENKINS-15097))
    -   Thanks to Ryan Campbell
-   Added support for Android 4.1

### Version 2.3 (July 25, 2012)

-   Fixed SDK version string parsing, now that Android SDK tools uses a
    "major.minor.patch" format
    ([JENKINS-14497](https://issues.jenkins-ci.org/browse/JENKINS-14497))
    -   Thanks to Jan Berkel
-   Which ABI to use when creating an emulator can now be specified
    ([JENKINS-13906](https://issues.jenkins-ci.org/browse/JENKINS-13906))
    -   Thanks to Jan Berkel
-   Added global configuration option which causes emulators to be
    created in a job's workspace
    ([JENKINS-11973](https://issues.jenkins-ci.org/browse/JENKINS-11973))
    -   This allows jobs to be run concurrently on the same slave, and
        the ability to use identical emulator configurations in multiple
        jobs. Generated emulators can also be easily removed by wiping
        out the workspace
    -   Thanks to Jørgen Tjernø

### Version 2.2 (March 27, 2012)

-   Made automated component installation compatible with changes in SDK
    Tools r17
-   System images can now be automatically installed for all platforms,
    not just add-ons
-   Corrupt AVDs are now automatically re-created and creation errors
    are better handled
    ([JENKINS-12120](https://issues.jenkins-ci.org/browse/JENKINS-12120))
-   Snapshot-enabled jobs now get a clean SD card image for each build
    ([JENKINS-13205](https://issues.jenkins-ci.org/browse/JENKINS-13205))
-   Allowed static resources such as icons to be cached properly

### Version 2.1 (January 19, 2012)

-   Added new build step which can install prerequisites for any Android
    projects in the workspace
-   Android SDK is now automatically installed if required by a build
    step (and auto-install is enabled)
-   Prevented platforms from being possibly downloaded redundantly when
    installing SDK add-ons
-   Prevented "Send usage statistics to Google" dialog appearing for
    each build
    ([JENKINS-12326](https://issues.jenkins-ci.org/browse/JENKINS-12326))
-   Increased emulator startup timeout, to help Windows machines
    ([JENKINS-11014](https://issues.jenkins-ci.org/browse/JENKINS-11014))
-   Exposed ANDROID\_HOME environment variable pointing to the Android
    SDK in use
    ([JENKINS-12325](https://issues.jenkins-ci.org/browse/JENKINS-12325))

### Version 2.0 (December 26, 2011)

-   Added automated installation of Android SDK, build tools and OS
    images
    -   Where the SDK is already installed, prerequisites can be
        automatically installed, e.g. OS images, SDK add-ons
    -   Automated installation of Android 4.0 system images will be
        possible when SDK Tools r17 is released (see [Android bug
        \#21880](http://b.android.com/21880))
    -   HTTP proxy support is not yet included
    -   Thanks to Raphaël Moll at Google for implementing our feature
        requests!
-   Each build now runs its own instance of ADB, giving more stability
    and prevents ADB crashes from affecting parallel builds
    ([JENKINS-10148](https://issues.jenkins-ci.org/browse/JENKINS-10148))
    -   Thanks to Jørgen Tjernø for the idea and patch!
-   Added option to delete the emulator when a build ends
-   Added detection of missing ABIs (e.g. required for Android 4.0+)
    ([JENKINS-11516](https://issues.jenkins-ci.org/browse/JENKINS-11516))
-   Added support for Android 4.0.3
-   Relaxed the rules for determining whether a screen resolution alias
    is valid or not
-   More hints are given at configuration time to help ensure an
    appropriate screen resolution is entered
-   Fixed variable expansion where an existing environment variable
    clashed with a build variable
-   Temporary log files are now stored outside of the workspace
    ([JENKINS-11492](https://issues.jenkins-ci.org/browse/JENKINS-11492))
-   Connections to the emulator are now made via TCP, which makes
    startup from snapshot more stable
    ([JENKINS-11952](https://issues.jenkins-ci.org/browse/JENKINS-11952))
-   Made snapshot more likely to succeed on Windows

### Version 1.18 (September 12, 2011)

-   Fixed bug introduced by Android SDK Tools r12, where emulator
    startup was not detected properly on Windows
    ([JENKINS-10815](https://issues.jenkins-ci.org/browse/JENKINS-10815))
-   Fixed bug where build would get stuck during emulator startup if
    `adb` hangs
    ([JENKINS-10421](https://issues.jenkins-ci.org/browse/JENKINS-10421))
    -   Thanks to Jørgen Tjernø
-   Fixed bug where logcat processes were not always killed at the end
    of a build
    ([JENKINS-10785](https://issues.jenkins-ci.org/browse/JENKINS-10785))
-   Made emulator shutdown more robust and prevent builds from hanging
    if the emulator is unresponsive
    ([JENKINS-10778](https://issues.jenkins-ci.org/browse/JENKINS-10778))
    -   Thanks for Richard Mortimer for the investigation and fixes

### Version 1.17 (August 25, 2011)

-   Added ability to use variables when specifying the package ID to run
    monkey against
-   Minor monkey fixes and improvements

### Version 1.16 (August 19, 2011)

-   Added support for Android 3.2
-   Added ability to specify the psuedo-random seed value used when
    running monkey (including random and time-based values)
    -   Thanks to Jan Berkel
-   Stopped redundant logcat output from being included for each build
    when using snapshots
    ([JENKINS-9831](https://issues.jenkins-ci.org/browse/JENKINS-9831))
-   Changed startup behaviour to allow manual management of snapshots
    while the emulator is running
    ([JENKINS-10422](https://issues.jenkins-ci.org/browse/JENKINS-10422))

### Version 1.15 (May 20, 2011)

-   Added support for Android 3.1
-   Export ANDROID\_SERIAL environment variable, making it easier to use
    `adb`
    ([JENKINS-9692](https://issues.jenkins-ci.org/browse/JENKINS-9692))
-   Fixed bug where an APK with spaces in its filename could not be
    installed
    ([JENKINS-9700](https://issues.jenkins-ci.org/browse/JENKINS-9700))
-   Fixed regression in config UI, where checkbox states weren't shown
    properly
    ([JENKINS-9747](https://issues.jenkins-ci.org/browse/JENKINS-9747))

### Version 1.14 (May 13, 2011)

-   Added logic to ensure multiple builds which need the same AVD will
    not run in parallel on the same machine (see
    [JENKINS-7353](https://issues.jenkins-ci.org/browse/JENKINS-7353))
    -   Thanks to Kohsuke Kawaguchi and Andrew Bayer for the assistance
-   Added new build step that runs the
    [monkey](https://developer.android.com/guide/developing/tools/monkey.html)
    testing tool on an emulator or device
-   Added a result publisher that parses monkey tool output, publishes a
    summary on the build page and updates the build result accordingly

### Version 1.13 (Apr 20, 2011)

-   Fixed bug where snapshots would not function with "Show window"
    disabled (see
    [JENKINS-9462](https://issues.jenkins-ci.org/browse/JENKINS-9462))
    -   Thanks to Valdis Rigdon

### Version 1.12 (Apr 08, 2011)

-   Fixed bug which caused creation of a brand new emulator to fail if
    snapshots were enabled

### Version 1.11 (Apr 07, 2011)

-   Added automated reconnection of the emulator to ADB during startup,
    in case ADB crashes (see
    [JENKINS-7693](https://issues.jenkins-ci.org/browse/JENKINS-7693))
-   Now connects to ADB in the same way that manually-started emulators
    do, potentially also improving stability

### Version 1.10 (Apr 04, 2011)

-   Added automated support for emulator snapshots (added in [SDK Tools
    r9](http://tools.android.com/recent/emulatorsnapshots)), which
    enables *much* faster start-up times
-   Fixed bug which could prevent jobs from starting when SDK Tools
    version r7 or older was installed

### Version 1.9 (Mar 06, 2011)

-   Added new build step that can install an APK on an emulator or
    device
-   Added new build step that can uninstall an APK from an emulator or
    device

### Version 1.8.1 (Feb 23, 2011)

-   Added support for Android 3.0, including WXGA resolution (1280x800)
    and new locales

### Version 1.8 (Feb 21, 2011)

-   Added ability to set custom hardware properties such as device RAM,
    Dalvik heap size, keyboard present etc. (see
    [JENKINS-8124](https://issues.jenkins-ci.org/browse/JENKINS-8124))

### Version 1.7 (Feb 09, 2011)

-   Added support for Android 2.3.3
-   Improve detection of failures during startup, plus improved logging
    and minor cleanups

### Version 1.6 (Dec 26, 2010)

-   Added ability to set arbitrary command line options when starting
    the emulator (see
    [JENKINS-8125](https://issues.jenkins-ci.org/browse/JENKINS-8125))

### Version 1.5 (Dec 17, 2010)

-   Added support for Android 2.3 and the xhdpi screen density
-   Added detection to handle the new "platform-tools" directory used in
    SDK Tools r8
-   Added detection of when AVD creation fails due to the desired
    platform not being installed
-   Improved automated emulator unlocking to be more reliable,
    particularly on slower machines
-   Fixed bug which could cause build to hang when trying to shut-down
    the emulator

### Version 1.4 (Sep 28, 2010)

-   Added feature to automatically unlock emulator after startup has
    completed (see
    [JENKINS-7185](https://issues.jenkins-ci.org/browse/JENKINS-7185))
-   Now tries to shut down emulator instances in a cleaner (hopefully
    more reliable) fashion

### Version 1.3.1 (Sep 01, 2010)

-   Fixed bug that prevented custom screen resolutions from being
    recognised (see
    [JENKINS-7337](https://issues.jenkins-ci.org/browse/JENKINS-7337))
-   When verifying whether an AVD exists, ensure we check the same
    directory that the `android` tool creates AVDs in
    -   It was possible in some environments (more likely Windows) that
        this was not the case

### Version 1.3 (Jul 18, 2010)

-   Added ability to control whether AVDs have an SD card, and its size
-   Added option to reset emulator to its default state before each
    build
-   Added option allowing emulator UI to be hidden during a build
-   Added option to delay emulator start-up by a configurable period,
    e.g. to allow a VNC server to start up (see
    [JENKINS-6912](https://issues.jenkins-ci.org/browse/JENKINS-6912))

### Version 1.2 (Jun 17, 2010)

-   Fixed crash that sometimes occurred when creating an emulator.
-   Added more logging and error handling while creating an emulator.
-   Fixed bug that would prevent emulators from starting.
-   Added support for Android 2.2.

### Version 1.1 (May 18, 2010)

-   Added ability to create an AVD using platform add-ons (e.g. the
    Google Maps APIs)
-   Generated AVDs now include a blank SD card image (currently fixed at
    16MB)

### Version 1.0.3 (Apr 06, 2010)

-   Ensure correct environment variables are used when starting the
    emulator. Fixes a problem where the emulator may not start under the
    [Xvnc Plugin](https://wiki.jenkins.io/display/JENKINS/Xvnc+Plugin)

### Version 1.0.2 (Apr 06, 2010)

-   Added environment variables with ADB identifier, ports and skin
    being used
-   Ensured correct skins are used for new AVDs

### Version 1.0.1 (Apr 06, 2010)

-   Fix minor Java 5 compatibility issue

### Version 1.0 (Apr 05, 2010)

-   Initial release

