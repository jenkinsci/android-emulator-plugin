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

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Node;
import jenkins.plugin.android.emulator.Messages;

/**
 * Relocates the default SDk home to a folder specific for the executor in the
 * node home folder {@code ~/android_$executorNumber/.android}.
 */
public class PerExecutorHomeLocator extends HomeLocator {

    private static final long serialVersionUID = 5353670448852887996L;

    @DataBoundConstructor
    public PerExecutorHomeLocator() {
        // default constructor
    }

    @Override
    public FilePath locate(@NonNull FilePath workspace) {
        final Computer computer = workspace.toComputer();
        if (computer == null) {
            throw new IllegalStateException(Messages.nodeNotAvailable());
        }
        final Node node = computer.getNode();
        if (node == null) {
            throw new IllegalStateException(Messages.nodeNotAvailable());
        }
        final FilePath rootPath = node.getRootPath();
        final Executor executor = Executor.currentExecutor();
        if (rootPath == null || executor == null) {
            return null;
        }
        return rootPath.child("android_" + executor.getNumber());
    }

    @Extension
    @Symbol("executor")
    public static class DescriptorImpl extends HomeLocatorDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.ExecutorHomeLocationLocator_displayName();
        }
    }

}
