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
package jenkins.plugin.android.emulator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import jenkins.model.Jenkins;
import jenkins.plugin.android.emulator.tools.AndroidSDKInstallation;
import jenkins.plugin.android.emulator.tools.AndroidSDKInstallation.DescriptorImpl;

public final class AndroidSDKUtil {
    private AndroidSDKUtil() {
        // default constructor
    }

    /**
     * Gets the AndroidSDK to use.
     *
     * @param name the name of AndroidSDK installation
     * @return a AndroidSDK installation for the given name if exists,
     *         {@code null} otherwise.
     */
    @Nullable
    public static AndroidSDKInstallation getAndroidSDK(@Nullable String name) {
        if (name != null) {
            for (AndroidSDKInstallation installation : getInstallations()) {
                if (name.equals(installation.getName()))
                    return installation;
            }
        }
        return null;
    }

    /**
     * Get all AndroidSDK installation defined in Jenkins.
     *
     * @return an array of AndroidSDK tool installation
     */
    @Nonnull
    public static AndroidSDKInstallation[] getInstallations() {
        DescriptorImpl descriptor = (DescriptorImpl) Jenkins.get().getDescriptorOrDie(AndroidSDKInstallation.class);
        return descriptor.getInstallations();
    }

}
