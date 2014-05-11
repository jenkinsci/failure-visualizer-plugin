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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import hudson.matrix.MatrixProject;
import hudson.maven.MavenModuleSet;
import hudson.model.FreeStyleProject;
import hudson.tasks.Maven;
import hudson.tasks.Shell;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class VisualizationTest {

    private static final String MARK = "2fccfb266cd62bdd947917259625e0dc";

    @Rule public JenkinsRule j = new JenkinsRule();

    @Test public void noop() throws Exception {
        FreeStyleProject freestyle = j.createFreeStyleProject();
        freestyle.getBuildersList().add(marker());
        freestyle.scheduleBuild2(0).get();
        containsNoMark(freestyle.getUrl());
        containsNoMark(freestyle.getLastBuild().getUrl());

        MatrixProject matrix = j.createMatrixProject();
        matrix.getBuildersList().add(marker());
        matrix.scheduleBuild2(0).get();
        containsNoMark(matrix.getUrl());

        MavenModuleSet maven = j.createMavenProject();
        maven.getPrebuilders().add(marker());
        maven.scheduleBuild2(0).get();
        containsNoMark(maven.getUrl());
    }

    @Test public void visualizeFreestyle() throws Exception {
        FreeStyleProject freestyle = j.createFreeStyleProject();
        freestyle.addProperty(new VisualizeFailure());
        freestyle.getBuildersList().add(marker());
        freestyle.scheduleBuild2(0).get();
        containsMark(freestyle.getLastBuild().getUrl());
        containsMark(freestyle.getUrl());
    }

    @Test public void visualizeMaven() throws Exception {
        j.configureMaven3();
        MavenModuleSet maven = j.createMavenProject();
        maven.addProperty(new VisualizeFailure());
        maven.getPrebuilders().add(new Maven(
                "archetype:generate -DarchetypeGroupId=org.apache.maven.archetypes -DgroupId=com.mycompany.app -DartifactId=my-app -Dversion=1.0 -B",
                "apache-maven-3.0.1"
        ));
        maven.getPrebuilders().add(new Shell(
                "echo class > my-app/src/main/java/" + MARK + ".java"
        ));
        maven.setGoals("package");
        maven.setRootPOM("my-app/pom.xml");
        maven.scheduleBuild2(0).get();
        containsMark(maven.getLastBuild().getUrl());
        containsMark(maven.getUrl());
        containsMark(maven.getModule("com.mycompany.app$my-app").getLastBuild().getUrl());
        // containsMark(maven.getModule("com.mycompany.app$my-app").getUrl()); // this does not work
    }

    @Test public void visualizeMatrix() throws Exception {
        MatrixProject matrix = j.createMatrixProject();
        matrix.addProperty(new VisualizeFailure());
        matrix.getBuildersList().add(marker());
        matrix.scheduleBuild2(0).get();
        containsNoMark(matrix.getUrl());
        containsNoMark(matrix.getLastBuild().getUrl());
        containsMark(matrix.getItem("default").getUrl());
        containsMark(matrix.getItem("default").getLastBuild().getUrl());
    }

    private void containsMark(String url) throws Exception {
        assertThat(text(url), containsString(MARK));
    }

    private void containsNoMark(String url) throws Exception {
        assertThat(text(url), not(containsString(MARK)));
    }

    private String text(String url) throws Exception {
        return j.createWebClient().goTo(url).asText();
    }

    private Shell marker() {
        return new Shell("echo " + MARK + " && exit 1");
    }
}
