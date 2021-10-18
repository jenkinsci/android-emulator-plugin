# Android Emulator Plugin for Jenkins

Provides numerous features for [Android development](http://developer.android.com/) and testing during Jenkins builds, including:

* Creating Android emulators on-demand
* Running an Android emulator during a build
* Automatically installing the Android SDK on Jenkins slaves, where required
* Detecting which Android platforms are required to build one or more projects and installing them automatically
* Generating Ant build files for any app, test or library projects found in the workspace
* Installing/uninstalling Android packages
* Running the monkey stress-testing tool
* Parsing output from running monkey and marking a build as unstable/faild

For more information, visit the wiki page:  
<https://wiki.jenkins-ci.org/display/JENKINS/Android+Emulator+Plugin>

## Seeking New Maintainer
Due to time constraints, other commitments, and the values of the Jenkins project not aligning to my own, I am seeking 
a new maintainer. Create a ticket on https://issues.jenkins-ci.org/ if interested and/or follow guidance in 
https://www.jenkins.io/doc/developer/plugin-governance/adopt-a-plugin/ if you're interested in becoming the maintainer of the plugin.