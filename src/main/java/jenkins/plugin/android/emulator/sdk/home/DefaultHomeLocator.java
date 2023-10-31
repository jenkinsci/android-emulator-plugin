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

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;
import jenkins.model.Jenkins;
import jenkins.plugin.android.emulator.Messages;

/**
 * Uses NPM's default global cache, which is actually {@code ~/.android} on Unix
 * system or {@code %HOME%\.android} on Windows system.
 */
public class DefaultHomeLocator extends HomeLocator {

    private static final long serialVersionUID = 3368523530762397938L;

    @DataBoundConstructor
    public DefaultHomeLocator() {
        // default constructor
    }

    @Override
    public FilePath locate(@NonNull FilePath workspace) {
        try {
            return FilePath.getHomeDirectory(workspace.getChannel());
        } catch (InterruptedException | IOException e) {
            return Jenkins.get().getRootPath();
        }
    }

    @Extension
    @Symbol("home")
    public static class DescriptorImpl extends HomeLocatorDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.DefaultHomeLocationLocator_displayName();
        }
    }

}