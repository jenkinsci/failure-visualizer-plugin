/*
 * The MIT License
 *
 * Copyright (c) 2014 Red Hat, Inc.
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
package org.jenkinsci.plugins.failurevisualizer;

import hudson.model.InvisibleAction;
import hudson.model.Result;
import hudson.model.Job;
import hudson.model.Run;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import org.apache.commons.jelly.XMLOutput;

public abstract class FailureLog extends InvisibleAction {

    public abstract Run<?, ?> build();

    /**
     * Write relevant part of the log.
     */
    public void writeTo(XMLOutput writer) throws IOException {
        ArrayList<String> actualLines = new ArrayList<String>(30);
        for (String line: build().getLog(30)) {
            if (line.trim().isEmpty()) continue; // Skip empty line to save space
            if (line.contains("Building ") && line.contains(" in workspace ")) {
                // Try to detect start of the build
                actualLines.clear();
            }

            actualLines.add(line);

            if (line.contains("' marked build as failure")) break; // End of build, the rest it not interesting
        }

        final Writer wrtr = writer.asWriter();
        for (String line: actualLines) {
            wrtr.write(line);
            wrtr.write('\n');
        }
    }

    public static class Build extends FailureLog {

        private final Run<?, ?> build;

        public Build(final Run<?, ?> build) {
            this.build = build;
        }

        @Override
        public Run<?, ?> build() {
            return build;
        }
    }

    public static class Project extends FailureLog {

        private final Job<?, ?> job;

        public Project(final Job<?, ?> job) {
            this.job = job;
        }

        @Override
        public Run<?, ?> build() {
            final Run<?, ?> last = job.getLastCompletedBuild();
            return last == null || last.getResult() != Result.FAILURE ? null : last;
        }
    }
}
