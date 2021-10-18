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

public final class AndroidSDKConstants {

    private AndroidSDKConstants() {
        // default constructor
    }

    public static final String ANDROID_CACHE = ".android";
    public static final String DDMS_CONFIG = "ddms.cfg";
    public static final String LOCAL_REPO_CONFIG = "repositories.cfg";

    public static final String ENV_ADB_TRACE = "ADB_TRACE";
    public static final String ENV_ADB_LOCAL_TRANSPORT_MAX_PORT = "ADB_LOCAL_TRANSPORT_MAX_PORT";
    /**
     * Sets the path to the directory that contains all AVD-specific files,
     * which mostly consist of very large disk images.
     * <p>
     * The default location is $ANDROID_EMULATOR_HOME/avd/. You might want to
     * specify a new location if the default location is low on disk space.
     */
    public static final String ENV_ANDROID_AVD_HOME = "ANDROID_AVD_HOME";
    /**
     * Sets the path to the user-specific emulator configuration directory.
     * <p>
     * The default location is $ANDROID_SDK_HOME/.android/.
     */
    public static final String ENV_ANDROID_EMULATOR_HOME = "ANDROID_EMULATOR_HOME";

    /**
     * The Android Debug Bridge (adb) server default TCP port. 
     */
    public static final int ADB_DEFAULT_SERVER_PORT = 5037;
    public static final int ADB_CONNECT_TIMEOUT = 60;
}
