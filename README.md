# Android Emulator Plugin for Jenkins

Provides numerous features for [Android development](http://developer.android.com/) and testing during Jenkins builds, including:

* Creating Android emulators on-demand
* Running an Android emulator during a build
* Automatically installing the Android SDK on Jenkins agents, where required
* Detecting which Android platforms are required to build one or more projects and installing them automatically
* Generating Ant build files for any app, test, or library projects found in the workspace
* Installing/uninstalling Android packages
* Running the monkey stress-testing tool
* Parsing output from running monkey and marking a build as unstable/failed

Automates many [Android development](https://developer.android.com/)
tasks including SDK installation, build file generation, emulator
creation and launch, APK (un)installation, monkey testing and
analysis...  
See also: [Android Lint
Plugin](https://wiki.jenkins.io/display/JENKINS/Android+Lint+Plugin).  
See also: [Google Play Android Publisher
Plugin](https://wiki.jenkins.io/display/JENKINS/Google+Play+Android+Publisher+Plugin).

#### Table of Contents

## Features

This plugin lets you automate a number of Android-related tasks during a
build:

-   Creating a new Android emulator
    -   Its configuration can be parameterised, including OS version,
        screen size, locale and hardware properties
    -   Android SDK dependencies are automatically downloaded and
        installed
-   Running any Android emulator
    -   Waits until the emulator is fully started-up
    -   Emulator snapshots can be automatically created
        -   This allows a very fast startup time for subsequent builds
        -   This ensures subsequent builds will start from the same
            clean state
    -   Logs are automatically captured and saved
    -   Emulator will be shut down automatically when the build has
        finished
    -   Multiple instances of the same emulator are prevented from
        running concurrently
-   Detecting which Android platforms are required to build one or more
    projects and installing them automatically
-   Generating Ant build files for any app, test or library projects
    found in the workspace
-   Installing an Android package onto an emulator
-   Uninstalling an Android package from an emulator
-   Running the `monkey` stress-testing tool
-   Parsing output from running `monkey`
    -   The build outcome can be automatically marked as unstable or
        failed in case a monkey-induced crash is detected

## Requirements

### Jenkins

Jenkins [version 2.479.1](https://jenkins.io/changelog/#v2.479.1) or newer is required.

### Android

The plugin will automatically download and install the [Android
SDK](https://developer.android.com/sdk/), if it's not already installed
when a build starts.  
This means that no manual effort is required to start running Android
emulators with Jenkins.

You can, however, disable automated installation via the "Automatically
install Android components when required" option on the main Jenkins
configuration page.

View manual SDK installation requirements...

If you install the Android SDK yourself, you must install at least two
further components, via the [Android SDK and AVD
Manager](https://developer.android.com/sdk/installing.html#components):

-   SDK Tools
-   SDK Platform-tools

If you only wish to run pre-existing emulator instances (aka Android
Virtual Devices, or AVDs), there are no further requirements — only
these two components are required.

If you want the plugin to automatically generate new emulator instances,
but disable auto-installation, you must install one or more Android
platform versions into the SDK.  
By default, the SDK only comes with the bare minimum; in this case you
will need to separately download each individual platform version you
wish to build or test against.

Again, this is done via the [Android SDK and AVD
Manager](https://developer.android.com/sdk/adding-components.html) tool,
accessible via the command line "`android`", or via the "Window" menu in
Eclipse, if you use the Eclipse ADT plugin. From the SDK Manager, you
can easily install the desired "[SDK
Packages](https://developer.android.com/sdk/installing.html#components)".

## Configuration

### System configuration

Generally no global configuration is needed — the plugin will try hard
to locate an installed Android SDK whenever it is needed. If one is not
found, it will be installed automatically.

See how the SDK is located...

Via the main configuration page, you can optionally specify the location
where each build node can locate a copy of the Android SDK.

![](docs/images/android_global-config.png)

This can either be an absolute path, or can contain environment
variables in the format `$``VARIABLE_NAME`. This will be replaced at
build time from the node's environment variables (see the "Node
Properties" settings for each agent), or from the build's environment.

If no value is entered, or the plugin cannot find a valid SDK under the
configured path, it will search under the following environment
variables on the build node to try and locate a usable SDK:

-   `ANDROID_SDK_ROOT`
-   `ANDROID_SDK_HOME`
-   `ANDROID_HOME`
-   `ANDROID_SDK`

If nothing is found, the plugin will search on the `PATH` to attempt to
locate a usable set of SDK tools. If, after all these steps, the
required Android SDK tools are not found and auto-installation is
disabled, the build will be marked as "not built" and will stop.

### Job configuration

##### Running on headless build machines

If you have build agents which are headless (e.g. Linux servers that
don't have a graphical user interface), you can still run an Android
Emulator even although, by default, the emulator does require a
graphical environment.

Just untick the "Show emulator window" configuration option in your job
configuration. This is the equivalent of using the emulator's
"`-no-window`" command-line option.

Using an artificial graphical environment...

It is also possible to run the Android emulator UI on a headless build
agent by making use of an artificial X server, such as
[Xvnc](http://www.realvnc.com/products/free/4.1/man/Xvnc.html).

In this case, a recommended setup is to install both Xvnc and the [Xvnc
Plugin](https://wiki.jenkins.io/display/JENKINS/Xvnc+Plugin) for Jenkins.
With this plugin enabled in your job — and Xvnc configured to run
without requesting a password — you can run multiple Android emulators
in parallel on a headless agent, while keeping the "Show emulator
window" option enabled.

Although the Android Emulator plugin has been designed to ensure it
always runs after an Xvnc server has been started, the Xvnc plugin does
not wait for the Xvnc server to be fully up-and-running before handing
control over to the Android Emulator plugin.

For this reason, you may want to delay emulator startup by a few seconds
(e.g. three to five), giving the Xvnc server time to finish starting-up
before attempting to launch an Android emulator into it. To do so, enter
the desired number of seconds in the "Startup delay" field under
"Advanced" options.

##### Other requirements

In addition, while the Android Emulator plugin requires the [Port
Allocator
Plugin](https://wiki.jenkins.io/display/JENKINS/Port+Allocator+Plugin),
there is no job configuration required for this; everything is handled
automatically — you need not select the "Assign unique TCP ports"
checkbox in the job config.

#### Selecting an emulator

After ticking "Run an Android emulator during build", you will be asked
whether you want to run an existing AVD, or whether you want to create a
new one on-the-fly with certain properties.

![](docs/images/android_job-named.png)

Using an existing emulator for a job just requires that you enter the
name of the AVD you want to be started. This AVD must exist on each
build node the job will be executed on. Existing AVDs are found in your
`$HOME/.android/avd` directory and can be listed using the
"`android list avd`" command.  
As with all other properties, you can enter environment variables here
using the format `$``VARIABLE_NAME`.

Alternatively, if you don't have a particular AVD accessible on each
build node, the plugin can automatically generate a new emulator if one
doesn't already exist:

![](docs/images/android_job-custom.png)

Each property is mandatory, aside from the device locale. If this is not
entered, the Android emulator default locale of US English (en\_US) will
be used when starting the emulator.

Each field will auto-complete with the default Android SDK values, e.g.
120, 160, 240dpi densities and named screen resolutions including QVGA,
HVGA, WVGA etc. However, you can enter your own values if you wish to
use a custom OS image, screen density, resolution or locale.  
Screen resolutions can be entered either using the named values, or as a
"width times height" dimension, e.g. `480x800`.

You can specify multiple hardware properties such as the heap size for
each Android app, or whether the device has a GPS by clicking the button
"Add custom hardware property" and entering the values. See the inline
help for more details on the values to enter.

Using Google Maps and other SDK add-ons...

As mentioned above, the "Android OS version" field will auto-complete to
existing SDK versions such as "1.5" or "2.2".

However, it is possible to enter different values in this field, for
example if you want to use an Android SDK add-on that you have
installed, e.g. the Google APIs add-on or the Samsung GALAXY Tab add-on.

In these cases, just enter the appropriate value given by the
"`android list target`" command. For example:

-   The Google APIs add-on, based on an Android 1.6 emulator:
    `Google Inc.:Google APIs:4`
-   The Samsung GALAXY Tab add-on, based on an Android 2.2 emulator:
    `Samsung Electronics Co., Ltd.:GALAXY Tab Addon:8`

#### Multi-configuration (matrix) job

The real awesomeness of this plugin comes when used in conjunction with
a multi-configuration job type.

By using the "Run emulator with properties" setting, in conjunction with
one-or-more matrix axes and the Android Emulator plugin's variable
expansion, you can generate and test with a large number of distinct
Android emulator configurations with very little effort.

To give a full example, if you want to test your application across
multiple Android OS versions, multiple screen densities, multiple screen
resolutions and for several target locales, you might set up your matrix
axes as follows:

![](docs/images/android_matrix-axes.png)

As each of these axis names (i.e. "density", "locale", "os",
"resolution") are exported by Jenkins as environment variables, you can
make use of these when launching a new Android emulator:

![](docs/images/android_matrix-result.png)  
![](docs/images/android_job-variables.png)

When the build executes, this would automatically generate and allow you
to test your application against 64 unique device configurations.

However, you should note that not **all** combinations are valid. For
example, a WVGA (800x480) resolution device makes no sense with a screen
density of 120 (unless you have superhuman eyesight).

For this purpose, you can use the "Combination Filter" feature, which
tells Jenkins which combinations of the matrix axes are valid. In the
case of screen densities and resolutions, a configuration like this
should instruct Jenkins to only build for [configurations which make
sense](http://developer.android.com/guide/practices/screens_support.html#range):

``` syntaxhighlighter-pre
(density=="120").implies(resolution=="QVGA" || resolution=="WQVGA" || resolution=="FWQVGA") &&
(density=="160").implies(resolution=="HVGA" || resolution=="WVGA" || resolution=="FWVGA") &&
(density=="240").implies(resolution=="WVGA" || resolution=="FWVGA")
```

Note that each variable refers to one of the matrix axes, *not* an
Android Emulator plugin property.

## Build execution

### Environment

For convenience, the plugin places a number of variables into the build
environment relating to the emulator in use:

| Variable name             | Example value                     | Description                                                                               |
|---------------------------|-----------------------------------|-------------------------------------------------------------------------------------------|
| `ANDROID_SERIAL`          | `localhost:34564`                 | Identifier for connecting to this AVD, e.g. `adb -s localhost:34564 shell`                |
| `ANDROID_AVD_DEVICE`      | `localhost:34564`                 | Identifier for connecting to this AVD, e.g. `adb -s localhost:34564 shell`                |
| `ANDROID_AVD_ADB_PORT`    | `34564`                           | Port used by ADB to communicate with the AVD (random for each build)                      |
| `ANDROID_AVD_USER_PORT`   | `40960`                           | Port used to access the AVD's telnet user interface (random for each build)               |
| `ANDROID_AVD_NAME`        | `hudson_en-GB_160_HVGA_android-7` | Name of the AVD running for the build                                                     |
| `ANDROID_AVD_LOCALE`      | `en_GB`                           | Locale of the AVD                                                                         |
| `ANDROID_AVD_OS`          | `2.1`                             | OS version of the running AVD                                                             |
| `ANDROID_AVD_DENSITY`     | `160`                             | Screen density in dpi of the AVD                                                          |
| `ANDROID_AVD_RESOLUTION`  | `HVGA`                            | Screen resolution, named or dimension, of the AVD                                         |
| `ANDROID_AVD_SKIN`        | `HVGA`                            | Skin being used by the AVD, e.g. `WQVGA432` or `480x800`                                  |
| `ANDROID_ADB_SERVER_PORT` | `51292`                           | Port that the AVD server for this build is running on (random for each build)             |
| `ANDROID_TMP_LOGCAT_FILE` | `/var/tmp/logcat_943239.log`      | Temporary file to which logcat output is written during the build (random for each build) |
| `JENKINS_ANDROID_HOME`    | `/home/jenkins/tools/android-sdk` | The path to the Android SDK being used for this build (optional)                          |
| `ANDROID_HOME`            | `/home/jenkins/tools/android-sdk` | The path to the Android SDK being used for this build (optional)                          |

### Using the emulator

Now that you have an Android emulator running, you'll probably want to
install one or more Android applications (APKs) and start running some
tests.

Basically, whenever you want to call `adb` as part of your build, just
call it as you normally would, e.g. `adb install my-app.apk`.

If you're using Android's default Ant build system, you should specify
the `sdk.dir` property, to tell Ant it can find the Android build
scripts:  
Just add "`sdk.dir=$ANDROID_HOME`" to the "Properties" field of your
"Invoke Ant" build step.

Documentation for older plugin versions...

#### Using version 1.18 or older...

Normally, when running an Android application using Eclipse or the
command line, either your APK is automatically installed (because there
is only one emulator/device attached to your PC), or Eclipse allows you
to choose from a list. Similarly, when building from the command line,
installation is done with a command like:
"`adb -e install -r my-app.apk`", where "`-e`" specifies the emulator
(or "`-d`" a USB-attached device).

However, as Jenkins may be running multiple Android-related builds at
once, with several emulators running in parallel, it's not possible to
automatically determine which emulator should be used. Nor can the user
be prompted at build time.

Since version 1.15, the `ANDROID_SERIAL` environment variable has been
automatically set by the plugin. Because the `adb` tool automatically
uses this variable to determine which Android device to communicate
with, you no longer need to pass in parameters like "`-s`" or "`-e`" to
`adb`.

Furthermore, since version 2.13, if the plugin detects (or automatically
installs) your Android SDK, the SDK tools and platform tools directories
will be prepended to your `$PATH` during a build. This means you don't
have to supply the full path to tools like `adb` or `monkeyrunner`, even
if those tools would not normally be on the `$PATH`.

#### Using version 1.14 or older...

##### Working with Android's default Ant build system

The default build system for Android is Apache Ant, which is well
supported by Jenkins.

When calling targets like "`ant install`" or "`ant run-tests`", the
Android build system allows you to use the `adb.device.arg` property to
specify where the application should be installed to.

To make use of this in an "Invoke Ant" build step, just add the
following to your Ant "Properties" section:  
`adb.device.arg="-s $ANDROID_AVD_DEVICE"`

##### Using shell commands

If you aren't using Ant, but want Jenkins to run `adb` commands for you
via an "Execute shell" build step, the process is similar.

To install, use the `ANDROID_AVD_DEVICE` environment variable with the
`-s` flag:  
`adb -s $ANDROID_AVD_DEVICE install -r my-app.apk`

This would be automatically expanded by the shell to something like:  
`adb -s localhost:34564 install -r my-app.apk`

The same principle applies for any other `adb` commands you wish to
perform, for example to start running tests:  
`adb -s $ANDROID_AVD_DEVICE shell am instrument -r -w com.example.tests/android.test.InstrumentationTestRunner | tee test-result.txt`

#### Installing project prerequisites

When compiling an Android project, you must have all the prerequisite
Android platform images installed. For example, if you have an Android
app which relies on an Android library project, plus you have a unit
test project — these may all be targeting different Android SDK
versions, all of which must be present at compile time.

Normally, with the (deprecated) Ant build system, these target versions
are specified in either a "`project.properties`" or
"`default.properties`" file.

Since version 2.1, the plugin provides a "**Install Android project
prerequisites**" build step for the Ant build system, which
automatically detects the target versions in the build workspace, then
automatically installs any of the corresponding Android platform images
that are not yet installed.

This build step requires no configuration — just add it before the build
step that compiles your Android projects.

For the Gradle build system, I would recommend including the [Android
SDK Manager Gradle
Plugin](https://github.com/JakeWharton/sdk-manager-plugin) in your
project. You may have to use [JitPack](https://jitpack.io/) to get the
latest version.

#### Creating project build files

If you only build a project in Eclipse or using another IDE, you may not
have the required Ant build files created or checked into your
repository.

Since version 2.8, the "**Create Android build files**" build step will
automatically find any Android app, library or test projects in a
build's workspace and will create the build files for them, using the
appropriate "`android update project`" command.

#### Installing and uninstalling APKs

Since version 1.9, the plugin can automatically install an APK on the
started emulator for you.

Under the "Build" section of your job configuration, select "Add build
step" and choose "**Install Android package**".

![](docs/images/android_install-package.png)

In the "APK file" field that appears, enter the filename of the APK you
wish to install. When a build runs, the APK will be automatically
installed after the emulator has started up.

Note: It is also possible to use this build step without having started
an emulator via this plugin — you can install an APK on an attached
device or other emulator.

#### Running the Android `monkey` tool

The plugin provides a Build Step called "**Run Android monkey tester**"
which will run the
[monkey](https://developer.android.com/guide/developing/tools/monkey.html)
stress-testing tool against the given Android package.

The output is saved to a file — by default "`monkey.txt`" in the root of
the build workspace.  
Don't forget to archive this file by using "Archive the artifacts"
option under "Post-build Actions" if you want to keep the monkey output
for future reference!

![](docs/images/android_monkey-run.png)

#### Parsing `monkey` output

Also provided is a method of parsing the output of the monkey testing
tool.

Under the "Post-build Actions" section, enable the "**Publish Android
monkey tester result**". No further configuration is required — by
default the plugin will search for "`monkey.txt`" in the root of the
build workspace, parse the file's contents and display the result on the
build page.

If the monkey output reveals your Android application crashed or caused
an "Application Not Responding" situation, the build will be marked as
UNSTABLE.

![](docs/images/android_monkey-publish.png)

You can, of course, specify a different filename (including the use of
variables) or change the "Set build result" option to mark the build as
a FAILURE rather than just UNSTABLE, or leave its status untouched in
case the monkey information does not indicate success.

![](docs/images/android_monkey-result.png)

![](docs/images/android_artifacts.png)

### Artifacts

Once the emulator is ready for use, its log is captured until the build
finishes. This corresponds to the output of "`adb logcat -v time`", i.e.
the main log output including timestamps.  
This will be archived automatically as a build artifact, with the name
`logcat.txt`.

## Known issues

View known issues...

### Pipeline not yet supported

This plugin is currently still only compatible with Freestyle jobs
— Pipeline support is planned!

### Emulator v2

The new-and-improved emulator engine — first released as part of SDK
Tools 25 (and Android Studio 2.0) — is supported as of version 3.0 of
this plugin.

However, the Quick Boot feature (formerly known as snapshots) is
currently not supported, as the command line tools do not appear to
support this.

### Android SDK Tools

Due to a regression in SDK Tools r12 and r13 (see [Android bug
\#18444](http://b.android.com/18444)), running any builds with the "Use
emulator snapshots" option enabled (which is the default), would likely
fail to load in the state you expect. For example the emulator may not
be ready for use, and the screen may not be unlocked.

To avoid this, either keep using r11 or earlier, or update to r14 or
newer. However, if you update, you will have to manually delete all
existing snapshot images and allow this plugin to re-create them.  
See the [Known Issues](http://tools.android.com/knownissues) page on the
Android Tools site for more information.

Similarly, snapshot support does not fully function for Android 4.0
until SDK Tools r15. An initial snapshot can be created, but
subsequently loading from that snapshot will crash the emulator
immediately. Earlier Android versions are not affected, i.e. you can
still use snapshots with Android 3.2 and earlier. Upgrading to SDK Tools
r15+ should fix this.

As a workaround, you can also uncheck "Use emulator snapshots" in any
jobs where you are seeing problems.

### Running in a Windows service as "Local System"

New AVDs cannot be generated and run if Jenkins is running as a Windows
service, using the "Local System" account (see
[JENKINS-7355](https://issues.jenkins-ci.org/browse/JENKINS-7355)).

-   Workaround: configure the Jenkins service to "run as" a real user

### Emulator UI doesn't appear when running on Windows 7

If running Jenkins as a service on Windows 7 or newer, you may find that
while the plugin can start an emulator, its user interface may not
appear, even if configured to do so.  
This is due to something called Session 0 Isolation, which prevents
services from starting UIs for security reasons.

If you really need to see the emulator UI, you can either run Jenkins
not as a service, or add an agent node to Jenkins (e.g. launch agent via
JNLP on the same machine) which will bypass this isolation.

### Unexpected timeouts or hanging during build

AVDs can, on occasion, time-out unexpectedly or stop responding during a
build, e.g. when trying to install an APK (see
[JENKINS-7354](https://issues.jenkins-ci.org/browse/JENKINS-7354)).

-   This is generally caused by bugs in the ADB process. It can be prone
    to hanging or crashing. Over time, more safeguards have been added
    to the plugin, including timing-out after a while and isolating ADB
    instances, so this shouldn't happen too often.

This should also be more stable with version 3.0 of this plugin, which
allows the new emulator engine to be used.

### AVDs may not shut down fully at the end of a build

Sometimes the `emulator` process does not shut down fully at the end of
a build (requiring a `kill -9` on Linux); the plugin sends a [console
command to terminate the
emulator](https://developer.android.com/guide/developing/devices/emulator.html#terminating)
and the UI window closes, but the actual `emulator` process does not
die.

-   This issue will be fixed once
    [JENKINS-11995](https://issues.jenkins-ci.org/browse/JENKINS-11995)
    is implemented.
-   If your agent is running Linux, you may be running into [Android
    issue \#17294](http://b.android.com/17294)  
    In this case, there is a workaround assuming your build doesn't need
    to use the emulator's audio input:
    -   Add a custom hardware property called "`hw.audioInput`" with the
        value "`no`"

## Potential upcoming features

-   Support for the [Pipeline
    Plugin](https://wiki.jenkins.io/display/JENKINS/Pipeline+Plugin) is
    planned
-   Within the 'android-emulator' component of Jenkins' issue tracker
    you can:
    -   [Add a new feature
        request](https://issues.jenkins-ci.org/secure/CreateIssueDetails!init.jspa?Create=Create&pid=10172&priority=4&assignee=-1&components=15725&issuetype=2)
    -   [View existing
        requests](https://issues.jenkins-ci.org/secure/IssueNavigator.jspa?reset=true&jqlQuery=project+%3D+JENKINS+AND+issuetype+in+%28%22New+Feature%22%2C+Improvement%29+AND+component+%3D+android-emulator-plugin+AND+status+in+%28Open%2C+%22In+Progress%22%2C+Reopened%29)

## Changelog

Please have a look at [CHANGELOG.md](CHANGELOG.md).
