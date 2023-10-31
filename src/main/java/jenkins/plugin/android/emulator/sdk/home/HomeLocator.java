/*
 * The MIT License
 *
 * Copyright (c) 2020, Nikolas Falco
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.plugin.android.emulator.sdk.home;

import java.io.IOException;
import java.io.Serializable;

import edu.umd.cs.findbugs.annotations.CheckForNull;

import hudson.AbortException;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.ExtensionPoint;
import hudson.FilePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Computer;
import hudson.plugins.android_emulator.Constants;
import jenkins.plugin.android.emulator.AndroidSDKConstants;

/**
 * Strategy pattern that decides the location of the SDK home location for a
 * build.
 */
public abstract class HomeLocator extends AbstractDescribableImpl<HomeLocator> implements ExtensionPoint, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Called during the build on the master to determine the location of the
     * local SDK home location.
     *
     * @param workspace the workspace file path locator
     * @return null to let SDK build tool uses its default location. Otherwise
     *         this must be located on the same node as described by this path.
     */
    public abstract FilePath locate(@NonNull FilePath workspace);

    @Override
    public HomeLocatorDescriptor getDescriptor() {
        return (HomeLocatorDescriptor) super.getDescriptor();
    }

    public static void buildEnvVars(@NonNull FilePath homeLocation, @CheckForNull EnvVars env) throws IOException, InterruptedException {
        if (env == null) {
            env = new EnvVars();
        }
        env.put(Constants.ENV_VAR_ANDROID_SDK_HOME, homeLocation.getRemote());

        FilePath emulatorLocation = homeLocation.child(AndroidSDKConstants.ANDROID_CACHE);
        env.put(AndroidSDKConstants.ENV_ANDROID_EMULATOR_HOME, emulatorLocation.getRemote());

        FilePath avdPath = emulatorLocation.child("avd");
        avdPath.mkdirs(); // ensure that this folder exists
        env.put(AndroidSDKConstants.ENV_ANDROID_AVD_HOME, avdPath.getRemote());
    }
}