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

import hudson.Extension;
import hudson.model.Action;
import hudson.model.InvisibleAction;
import hudson.model.Result;
import hudson.model.TransientBuildActionFactory;
import hudson.model.TransientProjectActionFactory;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Run;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.jelly.XMLOutput;

public class FailureLog extends InvisibleAction {

	final public Run<?, ?> build;

	public FailureLog(final Run<?, ?> build) {

	    this.build = build;
	}

	/**
	 * Write relevant part of the log.
	 * @throws IOException
	 */
	public void writeTo(XMLOutput writer) throws IOException {
	    ArrayList<String> actualLines = new ArrayList<String>(30);
	    for (String line: build.getLog(30)) {
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

    @Extension
    public static class ProjectFactory extends TransientProjectActionFactory {
        @Override public Collection<? extends Action> createFor(
                @SuppressWarnings("rawtypes") AbstractProject project
        ) {
            return actions(project.getLastCompletedBuild());
        }
    }

    @Extension
    public static class BuildFactory extends TransientBuildActionFactory {
        @Override public Collection<? extends Action> createFor(
                @SuppressWarnings("rawtypes") AbstractBuild build
        ) {
            // For reason unknown to me summary.jelly contributed from transient
            // actions are not shown, even after https://github.com/jenkinsci/jenkins/commit/f1a751f79dfbb975e4c436fd0967323e1ae7b8c6.
            // Add permanent action when requested for the first time.
            if (build.getAction(FailureLog.class) != null) return Collections.emptyList();
            List<? extends Action> a = actions(build);
            if (!a.isEmpty()) build.addAction(a.get(0));
            return a;
        }
    }

    private static List<? extends Action> actions(Run<?, ?> build) {
        if (build == null || build.getResult() != Result.FAILURE) return Collections.emptyList();

        return Arrays.asList(new FailureLog(build));
    }
}
