/*
 * This file is part of Pustefix.
 *
 * Pustefix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Pustefix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Pustefix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.pustefixframework.maven.plugins;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;


/**
 * Generate all XSL targets with TargetGenerator
 *
 * @author mleidig@schlund.de
 *
 * @goal generate
 * @phase prepare-package
 *
 * @requiresDependencyResolution compile
 */
public class GenerateMojo extends AbstractMojo {

    /**
     * @parameter default-value="${basedir}/src/main/webapp"
     * @required
     */
    private File docroot;

    /**
     * Webapp deployment directory.
     * @parameter default-value="${basedir}/target/${project.artifactId}-${project.version}/"
     */
    private File webappdir;
    
    /**
     * @parameter default-value="error"
     * @required
     */
    private String loglevel;

    /** @parameter default-value="${project}" */
    private MavenProject mavenProject;

    public void execute() throws MojoExecutionException {
        if ("pom".equals(mavenProject.getPackaging())) {
            getLog().info("Generated Plugin invoked for packaging pom - ignored.");
            getLog().info("(This happens if you declare the plugin in your parent pom - which is fine)");
            return;
        }

        if(!webappdir.exists()) webappdir.mkdirs();
        
        File cache = new File(webappdir, ".cache");

        URLClassLoader loader = getProjectRuntimeClassLoader();
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        try {
            Class<?> generator = Class.forName("de.schlund.pfixxml.targets.TargetGeneratorRunner", true, loader);
            Method meth =
                    generator.getMethod("run", File.class, File.class, String.class,
                            Writer.class, String.class);
            Object instance = generator.newInstance();
            StringWriter output = new StringWriter();
            Thread.currentThread().setContextClassLoader(loader);
            boolean ok =
                    (Boolean) meth.invoke(instance, docroot, cache, "prod", output, loglevel);
            getLog().info(output.toString());
            if (!ok)
                throw new MojoExecutionException("Target generation errors occurred.");
        } catch (Exception x) {
            throw new MojoExecutionException("Can't create targets", x);
        } finally {
            Thread.currentThread().setContextClassLoader(contextLoader);
        }

    }

    private URLClassLoader getProjectRuntimeClassLoader() throws MojoExecutionException {
        try {
            List<?> elements = mavenProject.getCompileClasspathElements();
            URL[] urls = new URL[elements.size()];
            for (int i = 0; i < elements.size(); i++) {
                String element = (String) elements.get(i);
                urls[i] = new File(element).toURI().toURL();
            }
            return new URLClassLoader(urls);
        } catch (Exception x) {
            throw new MojoExecutionException("Can't create project runtime classloader", x);
        }
    }

}
