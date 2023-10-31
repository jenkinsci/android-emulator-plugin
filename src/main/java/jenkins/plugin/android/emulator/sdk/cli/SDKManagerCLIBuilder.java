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
package jenkins.plugin.android.emulator.sdk.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.CheckForNull;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.ProxyConfiguration;
import hudson.Util;
import hudson.util.ArgumentListBuilder;
import hudson.util.Secret;
import jenkins.plugin.android.emulator.sdk.cli.CLICommand.OutputParser;
import jenkins.plugin.android.emulator.sdk.cli.SDKPackages.SDKPackage;
import jenkins.plugin.android.emulator.tools.AndroidSDKInstaller.Channel;

/**
 * Build a command line argument for sdkmanager command.
 * 
 * @author Nikolas Falco
 */
public class SDKManagerCLIBuilder {
    private enum Column {
        NAME, VERSION, LOCATION, AVAILABLE, DESCRIPTION, UNSUPPORTED
    }

    static class ListPackagesParser implements OutputParser<SDKPackages> {
        @Override
        public SDKPackages parse(InputStream input) throws IOException {
            SDKPackages result = new SDKPackages();

            List<Column> columns = null;
            List<SDKPackage> bucket = null;
            for (String line : IOUtils.readLines(input, "UTF-8")) { // NOSONAR
                line = Util.fixEmptyAndTrim(line);
                if (StringUtils.isBlank(line)) {
                    continue;
                }

                String lcLine = line.toLowerCase();
                if (StringUtils.isBlank(line)) {
                    continue;
                } else if (lcLine.startsWith("available packages")) {
                    bucket = result.getAvailable();
                    continue;
                } else if (lcLine.startsWith("installed packages")) {
                    bucket = result.getInstalled();
                    continue;
                } else if (lcLine.startsWith("available updates")) {
                    bucket = result.getUpdates();
                    continue;
                } else if (bucket == null || lcLine.startsWith("--")) {
                    continue;
                } else if (isHeader(lcLine)) {
                    columns = createMapping(lcLine);
                    continue;
                }

                // finally it's a table row
                SDKPackage sdkPackage = new SDKPackage();

                StringTokenizer st = new StringTokenizer(line, "|");
                for (Column column : columns) { // NOSONAR
                    if (!st.hasMoreTokens()) {
                        // guard in case cells are empty
                        continue;
                    }

                    String value = Util.fixEmptyAndTrim(st.nextToken());
                    if (value == null) {
                        continue;
                    }

                    switch (column) {
                    case NAME:
                        sdkPackage.setId(value);
                        break;
                    case DESCRIPTION:
                        sdkPackage.setDescription(value);
                        break;
                    case VERSION:
                    case AVAILABLE:
                        sdkPackage.setVersion(new Version(value));
                        break;
                    case LOCATION:
                        sdkPackage.setDescription(value);
                        break;
                    case UNSUPPORTED:
                        // skip
                        break;
                    }
                }

                bucket.add(sdkPackage);
            }
            return result;
        }

        private List<Column> createMapping(String headers) {
            List<Column> columns = new ArrayList<>();
            StringTokenizer st = new StringTokenizer(headers, "|");
            while (st.hasMoreTokens()) {
                switch (st.nextToken().trim()) {
                case "path":
                case "id":
                    columns.add(Column.NAME);
                    break;
                case "version":
                case "installed":
                    columns.add(Column.VERSION);
                    break;
                case "description":
                    columns.add(Column.DESCRIPTION);
                    break;
                case "location":
                    columns.add(Column.LOCATION);
                    break;
                case "available":
                    columns.add(Column.AVAILABLE);
                    break;
                default:
                    // unsupported
                    break;
                }
            }
            return columns;
        }

        private boolean isHeader(String line) {
            return line.startsWith("id") || line.startsWith("path");
        }
    }

    private static final String NO_PREFIX = "";
    private static final String ARG_OBSOLETE = "--include_obsolete";
    private static final String ARG_VERBOSE = "--verbose";
    private static final String ARG_CHANNEL = "--channel";
    private static final String ARG_SDK_ROOT = "--sdk_root";
    private static final String ARG_INSTALL = "--install";
    private static final String ARG_UPDATE = "--update";
    private static final String ARG_LIST = "--list";
    private static final String ARG_PROXY_HOST = "--proxy_host";
    private static final String ARG_PROXY_PORT = "--proxy_port";
    private static final String ARG_PROXY_PROTOCOL = "--proxy";
//    private static final String ARG_FORCE_HTTP = "--no_https";

    private final FilePath executable;
    private ProxyConfiguration proxy;
    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private String sdkRoot;
    private Channel channel;
    private boolean verbose;
    private boolean obsolete;

    private SDKManagerCLIBuilder(@CheckForNull FilePath sdkmanager) {
        if (sdkmanager == null) {
            throw new IllegalArgumentException("Invalid empty or null executable");
        }
        this.executable = sdkmanager;
    }

    public static SDKManagerCLIBuilder with(@NonNull FilePath sdkmanager) {
        return new SDKManagerCLIBuilder(sdkmanager);
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2")
    public SDKManagerCLIBuilder proxy(ProxyConfiguration proxy) {
        this.proxy = proxy;
        return this;
    }

    public SDKManagerCLIBuilder sdkRoot(File sdkRoot) {
        this.sdkRoot = sdkRoot.toString();
        return this;
    }

    public SDKManagerCLIBuilder sdkRoot(String sdkRoot) {
        this.sdkRoot = sdkRoot;
        return this;
    }

    public SDKManagerCLIBuilder channel(Channel channel) {
        this.channel = channel;
        return this;
    }

    public SDKManagerCLIBuilder verbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    public SDKManagerCLIBuilder obsolete(boolean obsolete) {
        this.obsolete = obsolete;
        return this;
    }

    /**
     * Prepare the CLI command of sdkmanager to perform install operation.
     * 
     * @return the command line to execute.
     */
    public CLICommand<Void> install(Collection<String> packages) {
        if (packages == null || packages.isEmpty()) {
            throw new IllegalArgumentException("At least a packge must be specified");
        }

        ArgumentListBuilder arguments = buildCommonOptions();

        arguments.add(ARG_INSTALL);
        for (String p : packages) {
            arguments.addQuoted(p);
        }

        EnvVars env = new EnvVars();
        try {
            buildProxyEnvVars(env);
        } catch (URISyntaxException e) {
            // fallback to CLI arguments
            buildProxyArguments(arguments);
        }

        return new CLICommand<Void>(executable, arguments, env) //
                .withInput(StringUtils.repeat("y", "\r\n", packages.size()));
    }

    public CLICommand<SDKPackages> list() {
        ArgumentListBuilder arguments = buildCommonOptions();

        arguments.add(ARG_LIST);

        EnvVars env = new EnvVars();
        try {
            buildProxyEnvVars(env);
        } catch (URISyntaxException e) {
            // fallback to CLI arguments
            buildProxyArguments(arguments);
        }

        return new CLICommand<SDKPackages>(executable, arguments, env) //
                .withParser(new ListPackagesParser());
    }

    private ArgumentListBuilder buildCommonOptions() {
        ArgumentListBuilder arguments = new ArgumentListBuilder();

        if (sdkRoot == null) {
            sdkRoot = getSDKRoot();
        }
        // required
        arguments.addKeyValuePair(NO_PREFIX, ARG_SDK_ROOT, quote(sdkRoot), false);

        if (channel != null) {
            arguments.addKeyValuePair(NO_PREFIX, ARG_CHANNEL, String.valueOf(channel.getValue()), false);
        }

        if (verbose) {
            arguments.add(ARG_VERBOSE);
        }

        if (obsolete) {
            arguments.add(ARG_OBSOLETE);
        }
        // arguments.add(ARG_FORCE_HTTP);

        return arguments;
    }

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private String getSDKRoot() {
        return executable.getParent().getParent().getParent().getRemote();
    }

    private void buildProxyEnvVars(EnvVars env) throws URISyntaxException {
        if (proxy == null) {
            // no proxy configured
            return;
        }

        for (Pattern proxyPattern : proxy.getNoProxyHostPatterns()) {
            if (proxyPattern.matcher("https://dl.google.com/android/repository").find()) {
                // no proxy for google download repositories
                return;
            }
        }

        String userInfo = Util.fixEmptyAndTrim(proxy.getUserName());
        // append password only if userName is defined
        if (userInfo != null && StringUtils.isNotBlank(proxy.getSecretPassword().getEncryptedValue())) {
            Secret secret = Secret.decrypt(proxy.getSecretPassword().getEncryptedValue());
            if (secret != null) {
                userInfo += ":" + Util.fixEmptyAndTrim(secret.getPlainText());
            }
        }

        // ENV variables are used by
        // com.android.sdklib.tool.sdkmanager.SdkManagerCliSettings
        // actually authentication is not supported by the build tools !!!
        String proxyURL = new URI("http", userInfo, proxy.name, proxy.port, null, null, null).toString();
        env.put("HTTP_PROXY", proxyURL);
        env.put("HTTPS_PROXY", proxyURL);
    }

    private void buildProxyArguments(ArgumentListBuilder arguments) {
        if (proxy == null) {
            // no proxy configured
            return;
        }

        for (Pattern proxyPattern : proxy.getNoProxyHostPatterns()) {
            if (proxyPattern.matcher("https://dl.google.com/android/repository").find()) {
                // no proxy for google download repositories
                return;
            }
        }

        arguments.addKeyValuePair(NO_PREFIX, ARG_PROXY_PROTOCOL, "http", false);
        arguments.addKeyValuePair(NO_PREFIX, ARG_PROXY_HOST, proxy.name, false);
        if (proxy.port != -1) {
            arguments.addKeyValuePair(NO_PREFIX, ARG_PROXY_PORT, String.valueOf(proxy.port), false);
        }
    }

    private String quote(String quote) {
        if (!StringUtils.isNotBlank(quote)) {
            return "\"" + quote + "\"";
        }
        return quote;
    }

    public CLICommand<Void> update(Set<String> packages) {
        if (packages == null || packages.isEmpty()) {
            throw new IllegalArgumentException("At least a packge must be specified");
        }

        ArgumentListBuilder arguments = buildCommonOptions();

        arguments.add(ARG_UPDATE);
        for (String p : packages) {
            arguments.addQuoted(p);
        }

        EnvVars env = new EnvVars();
        try {
            buildProxyEnvVars(env);
        } catch (URISyntaxException e) {
            // fallback to CLI arguments
            buildProxyArguments(arguments);
        }

        return new CLICommand<Void>(executable, arguments, env) //
                .withInput(StringUtils.repeat("y", "\r\n", packages.size()));
    }

    public CLICommand<Void> arguments(String[] args) {
        ArgumentListBuilder arguments = buildCommonOptions();
        arguments.add(args);

        EnvVars env = new EnvVars();
        try {
            buildProxyEnvVars(env);
        } catch (URISyntaxException e) {
            // fallback to CLI arguments
            buildProxyArguments(arguments);
        }

        return new CLICommand<>(executable, arguments, env);
    }

}
