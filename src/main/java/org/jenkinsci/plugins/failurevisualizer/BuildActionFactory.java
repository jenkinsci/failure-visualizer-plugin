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
import hudson.matrix.MatrixBuild;
import hudson.model.Action;
import hudson.model.ModelObject;
import hudson.model.Result;
import hudson.model.TopLevelItem;
import hudson.model.TransientBuildActionFactory;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.Run;

import java.util.Collection;
import java.util.Collections;

@Extension
public class BuildActionFactory extends TransientBuildActionFactory {

    @Override
    public Collection<? extends Action> createFor(
            @SuppressWarnings("rawtypes") AbstractBuild build
    ) {
        // For reason unknown to me summary.jelly contributed from transient
        // actions is not shown, even after https://github.com/jenkinsci/jenkins/commit/f1a751f79dfbb975e4c436fd0967323e1ae7b8c6.
        // Add permanent action when requested for the first time.
        final Action action = action(build);
        if (action != null) {
            build.addAction(action);
        }

        return Collections.emptyList();
    }

    private static Action action(Run<?, ?> build) {
        if (build == null || build.getResult() != Result.FAILURE) return null;
        if (build instanceof MatrixBuild) return null;
        if (rootProject(build).getProperty(VisualizeFailure.class) == null) return null;
        if (build.getAction(FailureLog.class) != null) return null;

        return new FailureLog.Build(build);
    }

    /*package*/ static AbstractProject<?, ?> rootProject(Run<?, ?> build) {
        ModelObject job = build.getParent();
        while (!(job instanceof TopLevelItem)) {
            job = ((Job<?, ?>) job).getParent();
        }

        return (AbstractProject<?, ?>) job;
    }
}