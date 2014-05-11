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
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixRun;
import hudson.matrix.MatrixProject;
import hudson.model.Action;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.Job;
import hudson.model.listeners.RunListener;

import java.io.IOException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

public class VisualizeFailure extends JobProperty<Job<?, ?>> {

    @Override
    public Descriptor getDescriptor() {
        return (Descriptor) super.getDescriptor();
    }

    @Override
    public Action getJobAction(Job<?, ?> job) {
        return !(job instanceof MatrixProject)
                ? new FailureLog.Project(job)
                : null
        ;
    }

    @Extension
    public static class Prop extends RunListener<MatrixRun> {
        @Override
        public void onCompleted(MatrixRun run, TaskListener listener) {
            if (run.getResult() != Result.FAILURE) return;

            MatrixConfiguration cfg = run.getParent();
            VisualizeFailure property = cfg.getProperty(VisualizeFailure.class);
            if (property != null) return;

            VisualizeFailure source = cfg.getParent().getProperty(VisualizeFailure.class);
            if (source == null) return;
            try {
                cfg.addProperty(source);
            } catch (IOException ex) {
                // Best effort
            }
        }
    }

    @Extension
    public static class Descriptor extends JobPropertyDescriptor {

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Show console log in case of build failure";
        }

        @Override
        public VisualizeFailure newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return formData.get("showConsoleLog") != null ? new VisualizeFailure() : null;
        }
    }
}
