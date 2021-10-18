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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.LocaleUtils;

import hudson.Util;
import hudson.plugins.android_emulator.ScreenDensity;
import hudson.plugins.android_emulator.ScreenResolution;

public class EmulatorConfig {
    public static class ValidationError {
        private final String message;

        public ValidationError(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private String osVersion;
    private String screenDensity;
    private String screenResolution;
    private String avdName;
    private String locale;
    private String definition;
    private String cardSize;
    private String targetABI;
    private List<HardwareProperty> hardwareProperties;
    private int adbServerPort = AndroidSDKConstants.ADB_DEFAULT_SERVER_PORT;
    private int adbConnectionTimeout;
    private int reportConsolePort = 50000;

    public void setADBServerPort(int port) {
        this.adbServerPort = port;
    }

    public int getADBServerPort() {
        return adbServerPort;
    }

    public String getOSVersion() {
        return osVersion;
    }

    public void setOSVersion(String osVersion) {
        this.osVersion = Util.fixEmptyAndTrim(osVersion);
    }

    public String getScreenDensity() {
        return screenDensity;
    }

    public void setScreenDensity(String screenDensity) {
        this.screenDensity = Util.fixEmptyAndTrim(screenDensity);
    }

    public String getScreenResolution() {
        return screenResolution;
    }

    public void setScreenResolution(String screenResolution) {
        this.screenResolution = Util.fixEmptyAndTrim(screenResolution);
    }

    public String getAVDName() {
        return avdName;
    }

    public void setAVDName(String avdName) {
        this.avdName = Util.fixEmptyAndTrim(avdName);
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = Util.fixEmptyAndTrim(locale);
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String deviceDefinition) {
        this.definition = Util.fixEmptyAndTrim(deviceDefinition);
    }

    public String getCardSize() {
        return cardSize;
    }

    public void setCardSize(String cardSize) {
        this.cardSize = Util.fixEmptyAndTrim(cardSize);
    }

    public String getTargetABI() {
        return targetABI;
    }

    public void setTargetABI(String targetABI) {
        this.targetABI = Util.fixEmptyAndTrim(targetABI);
    }

    public List<HardwareProperty> getHardwareProperties() {
        return hardwareProperties;
    }
    
    public void setHardware(List<HardwareProperty> properties) {
        this.hardwareProperties = properties == null ? new ArrayList<>() : properties;
    }

    public Collection<ValidationError> validate() {
        Collection<ValidationError> errors = new ArrayList<>();
        if (osVersion == null) {
            errors.add(new ValidationError("osVersion is required"));
        }
        if (ScreenDensity.valueOf(screenDensity) == null) {
            errors.add(new ValidationError("screen density '" + screenDensity + "' not valid"));
        }
        if (ScreenResolution.valueOf(screenResolution) == null) {
            errors.add(new ValidationError("screen resolution '" + screenResolution + "' not valid"));
        }
        if (targetABI == null) {
            errors.add(new ValidationError("Target ABI is required"));
        }
        if (locale != null) {
            try {
                // parse locale with _ or -
                if (Util.fixEmpty(Locale.forLanguageTag(locale).getLanguage()) != null || LocaleUtils.toLocale(locale) != null) {
                    // it's ok
                }
            } catch (IllegalArgumentException e) {
                errors.add(new ValidationError("Invalid locale format " + locale));
            }
        }
        if (cardSize != null) {
            try {
                if (Integer.parseInt(cardSize) < 9) {
                    errors.add(new ValidationError(Messages.AndroidEmulatorBuild_sdCardTooSmall()));
                }
            } catch (NumberFormatException e) {
                errors.add(new ValidationError("Invalid SD card size " + cardSize));
            }
        }
        return errors;
    }

    public int getADBConnectionTimeout() {
        return adbConnectionTimeout;
    }

    public void setADBConnectionTimeout(int adbConnectionTimeout) {
        this.adbConnectionTimeout = adbConnectionTimeout;
    }

    public int getReportPort() {
        return reportConsolePort;
    }

    public void setReportPort(int port) {
        this.reportConsolePort = port;
    }

}