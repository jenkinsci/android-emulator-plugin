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
package jenkins.plugin.android.emulator.tools;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import hudson.model.Computer;
import hudson.model.Node;
import jenkins.plugin.android.emulator.Messages;

/**
 * Supported platform.
 */
public enum Platform {
    LINUX(".sh", "bin"), WINDOWS(".bat", "bin"), OSX(".sh", "bin");

    /**
     * Choose the extension file name suitable to run cli commands.
     */
    public final String extension;
    /**
     * Choose the folder path suitable bin folder of the bundle.
     */
    public final String binFolder;

    Platform(String extension, String binFolder) {
        this.extension = extension;
        this.binFolder = binFolder;
    }

    public boolean is(String line) {
        return line.contains(name());
    }

    /**
     * Determines the platform of the given node.
     *
     * @param node
     *            the computer node
     * @return a platform value that represent the given node
     * @throws DetectionFailedException
     *             when the current platform node is not supported.
     */
    public static Platform of(Node node) throws DetectionFailedException {
        try {
            Computer computer = node.toComputer();
            if (computer == null) {
                throw new DetectionFailedException(Messages.nodeNotAvailable());
            }
            return detect(computer.getSystemProperties());
        } catch (IOException | InterruptedException e) {
            throw new DetectionFailedException(Messages.SystemTools_failureOnProperties(), e);
        }
    }

    public static Platform current() throws DetectionFailedException {
        return detect(System.getProperties());
    }

    private static Platform detect(Map<Object, Object> systemProperties) throws DetectionFailedException {
        String arch = ((String) systemProperties.get("os.name")).toLowerCase(Locale.ENGLISH);
        if (arch.contains("linux")) {
            return LINUX;
        }
        if (arch.contains("windows")) {
            return WINDOWS;
        }
        if (arch.contains("mac")) {
            return OSX;
        }
        throw new DetectionFailedException(Messages.Platform_unknown(arch));
    }

}
