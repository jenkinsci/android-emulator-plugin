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

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

public class HardwareProperty extends AbstractDescribableImpl<HardwareProperty> {

    private final String key;
    private final String value;

    @DataBoundConstructor
    public HardwareProperty(String key, String value) {
        this.key = Util.fixEmptyAndTrim(key);
        this.value = Util.fixEmptyAndTrim(value);
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Symbol("hwProperty")
    @Extension
    public static final class DescriptorImpl extends Descriptor<HardwareProperty> {
        @Override
        public String getDisplayName() {
            return "Property";
        }

        public FormValidation doCheckKey(@QueryParameter String key) {
            if (StringUtils.isBlank(key)) {
                return FormValidation.error(Messages.required());
            }
            return FormValidation.ok();
        }
    }

}