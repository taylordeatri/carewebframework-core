/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 * 
 * This Source Code Form is also subject to the terms of the Health-Related Additional
 * Disclaimer of Warranty and Limitation of Liability available at
 * http://www.carewebframework.org/licensing/disclaimer.
 */
package org.carewebframework.maven.plugin.theme;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.Set;

import com.google.common.io.Files;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import org.carewebframework.maven.plugin.core.BaseMojo;

import org.codehaus.plexus.util.FileUtils;

/**
 * <p>
 * Goal which produces CareWeb theme modules from selected source jars or from source stylesheets.
 * </p>
 * 
 * <pre>
 * {@code
 * 
 *             <plugin>
 *                 <groupId>org.carewebframework</groupId>
 *                 <artifactId>org.carewebframework.maven.plugin.themegenerator</artifactId>
 *                 <version>3.0.0-SNAPSHOT</version>
 *                 <configuration>
 *                     <themes>
 *                         <theme>
 *                             <themeName>green</themeName>
 *                             <baseColor>003300</baseColor>
 *                         </theme>
 *                         <theme>
 *                              <themeName>cerulean</themeName>
 *                              <themeUri>src/main/themes/cerulean/bootstrap.min.css</themeUri>
 *                         </theme>
 *                     </themes>
 *                 </configuration>
 *                 <executions>
 *                     <execution>
 *                         <goals>
 *                             <goal>prepare</goal>
 *                         </goals>
 *                     </execution>
 *                 </executions>
 *             </plugin>
 * }
 * </pre>
 * <p>
 * In most cases, you will want your build to only consider certain artifacts and not all resolved
 * dependencies. If this is the case consider adding the following:
 * </p>
 * 
 * <pre>
 * {@code
 *                     <themeSources>
 *                        <themeSource>org.zkoss.zk:zk:jar</themeSource>
 *                        <themeSource>org.zkoss.zkforge.el:zcommons-el:jar</themeSource>
 *                        <themeSource>org.zkoss.zk:zkex:jar</themeSource>
 *                        <themeSource>org.zkoss.zk:zkmax:jar</themeSource>
 *                        <themeSource>org.zkoss.zk:zul:jar</themeSource>
 *                        <themeSource>org.zkoss.common:zweb:jar</themeSource>
 *                     </themeSources>
 * 
 * }
 */
@Mojo(name = "prepare", requiresDependencyResolution = ResolutionScope.COMPILE)
@Execute(goal = "prepare", phase = LifecyclePhase.PREPARE_PACKAGE)
public class ThemeGeneratorMojo extends BaseMojo {
    
    /**
     * Directory containing source to consider when generating themes.
     */
    @Parameter(property = "maven.careweb.theme.sourceDirectory", defaultValue = "${project.build.directory}/theme-source", required = true)
    private File sourceDirectory;
    
    /**
     * Exclude files.
     */
    @Parameter(property = "maven.careweb.theme.exclusions", required = true)
    private List<String> exclusions;
    
    /**
     * Additional resources to copy.
     */
    @Parameter(property = "resources", required = false)
    private List<String> resources;
    
    /**
     * Themes to be built.
     */
    @Parameter(property = "themes", required = true)
    private List<Theme> themes;
    
    /**
     * By default, all resolved dependencies will be considered in theme source. This parameter will
     * allow an explicit list. Values required as in the format of an Artifact.dependencyConflictId
     * (i.e. groupId:artifactId:type:classifier)
     */
    @Parameter(property = "themeSources", required = false)
    private List<String> themeSources;
    
    /**
     * Theme version
     */
    @Parameter(property = "maven.careweb.theme.version", defaultValue = "${project.version}", required = true)
    private String themeVersion;
    
    /**
     * Theme base path
     */
    @Parameter(property = "themeBase", defaultValue = "org/carewebframework/themes/${buildNumber}/", required = true)
    private String themeBase;
    
    /**
     * Whether to exclude transitive dependencies when considering theme source. Default is false.
     * In other words, all resolved dependencies are considered unless
     * <code>excludeTransitiveDependencies</code> is true, in which case only direct dependencies
     * are considered.
     */
    @Parameter(property = "maven.careweb.theme.excludeTransitiveDependencies", defaultValue = "false", required = false)
    private boolean excludeTransitiveDependencies;
    
    /**
     * Whether to skip processing
     */
    @Parameter(property = "maven.careweb.theme.skip", defaultValue = "false", required = false)
    private boolean skip;
    
    private FileFilter exclusionFilter;
    
    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping theme generation.");
            return;
        }
        
        init("theme", themeVersion);
        exclusionFilter = new WildcardFileFilter(exclusions);
        
        // Copy theme dependencies
        try {
            validateSource();
            copyDependencies();
        } catch (final Exception e) {
            throwMojoException("Exception occurred validating theme source.", e);
        }
        
        // Process the theme sources.
        try {
            getLog().info("Processing theme sources.");
            
            for (Theme theme : themes) {
                if (theme.getThemeVersion() == null) {
                    theme.setThemeVersion(moduleVersion);
                }
                
                processTheme(theme);
            }
            
            processTheme(null);
        } catch (final Exception e) {
            throwMojoException("Exception occurred processing source files for theme(s).", e);
        }
        
        // Assemble the archive
        try {
            assembleArchive();
        } catch (final Exception e) {
            throwMojoException("Exception occurred creating theme config and assembly", e);
        }
    }
    
    protected String getThemeBase() {
        return themeBase;
    }
    
    protected File getSourceDirectory() {
        return sourceDirectory;
    }
    
    protected List<String> getResources() {
        return resources;
    }
    
    protected MavenProject getMavenProject() {
        return mavenProject;
    }
    
    /**
     * Returns true if the specified file is in the exclusion list.
     * 
     * @param fileName Name of file to check.
     * @return True if the file is to be excluded.
     */
    public boolean isExcluded(String fileName) {
        return isExcluded(new File(fileName));
    }
    
    /**
     * Returns true if the specified file is in the exclusion list.
     * 
     * @param file File instance to check.
     * @return True if the file is to be excluded.
     */
    public boolean isExcluded(File file) {
        return exclusionFilter.accept(file);
    }
    
    /**
     * Ensure that source directory exists.
     * 
     * @throws Exception Unspecified exception.
     */
    private void validateSource() throws Exception {
        getLog().info("Validating theme source.");
        FileUtils.forceMkdir(sourceDirectory);
        
        if (!sourceDirectory.exists() || !sourceDirectory.isDirectory()) {
            throw new Exception("Source directory was not found: " + sourceDirectory);
        }
    }
    
    /**
     * Copies dependencies to build directory for processing.
     * 
     * @return True if dependencies were copied.
     * @throws Exception Unspecified exception.
     */
    private boolean copyDependencies() throws Exception {
        boolean copyExplicitThemeSourceArtifacts = false;
        boolean hasSource = false;
        
        if (themeSources != null) {
            getLog().debug("Theme-sources list: " + themeSources);
            copyExplicitThemeSourceArtifacts = true;
            getLog().info("Default theme source based on dependencies overridden by configuration");
        }
        
        final Set<?> deps = (excludeTransitiveDependencies ? mavenProject.getDependencyArtifacts() : mavenProject
                .getArtifacts());
        
        for (Object o : deps) {
            boolean copyDependency = true;
            Artifact a = (Artifact) o;
            getLog().debug("Artifact: " + a);
            
            if (copyExplicitThemeSourceArtifacts) {
                String dependencyConflictId = a.getDependencyConflictId();
                
                if (!this.themeSources.contains(dependencyConflictId)) {
                    getLog().debug("Ignoring dependency from theme source: " + dependencyConflictId);
                    copyDependency = false;
                }
            }
            if (copyDependency) {
                hasSource = true;
                File artifactFile = a.getFile();
                File artifactCopyLocation = new File(this.sourceDirectory, artifactFile.getName());
                getLog().debug("Copying dependency : " + artifactFile);
                Files.copy(artifactFile, artifactCopyLocation);
            }
        }
        
        return hasSource;
    }
    
    /**
     * Processes a theme.
     * 
     * @param theme The theme.
     * @throws Exception Unspecified exception.
     */
    private void processTheme(Theme theme) throws Exception {
        ThemeGeneratorBase themeGenerator;
        
        if (theme == null) {
            themeGenerator = new ThemeGeneratorResource(theme, this);
        } else if (theme.getBaseColor() != null) {
            themeGenerator = new ThemeGeneratorZK(theme, this);
        } else {
            themeGenerator = new ThemeGeneratorCSS(theme, this);
        }
        
        themeGenerator.process();
    }
    
}
