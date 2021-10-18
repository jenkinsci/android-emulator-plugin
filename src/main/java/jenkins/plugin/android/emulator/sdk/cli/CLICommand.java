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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.io.input.NullInputStream;
import org.apache.tools.ant.filters.StringInputStream;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import hudson.util.ForkOutputStream;
import hudson.util.NullStream;
import hudson.util.StreamTaskListener;

public class CLICommand<R> {

    public interface OutputParser<R> {
        R parse(InputStream input) throws IOException;
    }

    private final FilePath command;
    private final ArgumentListBuilder arguments;
    private final EnvVars env;
    private InputStream stdin = new NullInputStream(0);
    private FilePath root;
    private OutputParser<R> parser;

    CLICommand(FilePath command, ArgumentListBuilder arguments, EnvVars env) {
        this.command = command;
        this.arguments = arguments;
        this.env = env;
    }

    public ArgumentListBuilder arguments() {
        return arguments;
    }

    public CLICommand<R> withEnv(String key, String value) {
        env.put(key, value);
        return this;
    }

    public CLICommand<R> withEnv(EnvVars env) {
        this.env.putAll(env);
        return this;
    }

    public R execute() throws IOException, InterruptedException {
        return execute(new StreamTaskListener(new NullStream()));
    }

    public R execute(@Nonnull TaskListener output) throws IOException, InterruptedException {
        List<String> args = getArguments();

        // command.createLauncher(output)
        ProcStarter starter = command.createLauncher(output).launch() //
                .envs(env) //
                .stdin(stdin) //
                .pwd(root == null ? command.getParent() : root) //
                .cmds(args) //
                .masks(getMasks(args.size()));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (output != null) {
            if (parser != null) {
                // clone output to make content available to the parser
                starter.stdout(new ForkOutputStream(output.getLogger(), baos));
            } else {
                starter.stdout(output);
            }
        } else if (parser != null) {
            starter.stdout(baos);
        }

        int exitCode = starter.join();
        if (exitCode != 0) {
            throw new IOException(command.getBaseName() + " " + arguments.toString() + " failed. exit code: " + exitCode + ".");
        }

        if (parser != null) {
            return parser.parse(new ByteArrayInputStream(baos.toByteArray()));
        }
        return null;
    }

    public Proc executeAsync(@Nullable TaskListener output) throws IOException, InterruptedException {
        List<String> args = getArguments();

        // command.createLauncher(output)
        ProcStarter starter = command.createLauncher(output).launch() //
                .envs(env) //
                .stdin(stdin) //
                .pwd(root == null ? command.getParent() : root) //
                .cmds(args) //
                .masks(getMasks(args.size()));

        if (output != null) {
            starter.stdout(output);
        }

        return starter.start();
    }

    private boolean[] getMasks(final int size) {
        boolean[] masks = new boolean[size];
        masks[0] = false;
        System.arraycopy(arguments.toMaskArray(), 0, masks, 1, size - 1);
        return masks;
    }

    private List<String> getArguments() {
        List<String> args = new ArrayList<>(arguments.toList());
        args.add(0, command.getRemote());
        return args;
    }

    CLICommand<R> withRoot(FilePath root) {
        this.root = root;
        return this;
    }

    CLICommand<R> withParser(OutputParser<R> parser) {
        this.parser = parser;
        return this;
    }

    CLICommand<R> withInput(String input) {
        this.stdin = new StringInputStream(input);
        return this;
    }

}