/*
 * This file is part of PFIXCORE.
 *
 * PFIXCORE is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * PFIXCORE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with PFIXCORE; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package de.schlund.pfixcore.editor2.frontend.resources;

import java.util.Iterator;

import org.w3c.dom.Element;

import de.schlund.pfixcore.editor2.core.dom.Project;
import de.schlund.pfixcore.editor2.core.spring.ProjectFactoryService;
import de.schlund.pfixcore.editor2.core.spring.SecurityManagerService;
import de.schlund.pfixcore.editor2.frontend.util.EditorResourceLocator;
import de.schlund.pfixcore.editor2.frontend.util.SpringBeanLocator;
import de.schlund.pfixcore.workflow.Context;
import de.schlund.pfixxml.ResultDocument;

public class ProjectsResourceImpl implements ProjectsResource {
    private Project selectedProject;

    private Context context;

    public void init(Context context) throws Exception {
        this.selectedProject = null;
        this.context = context;
    }

    public void insertStatus(ResultDocument resdoc, Element elem)
            throws Exception {
        // TODO Auto-generated method stub
        ProjectFactoryService projectfactory = SpringBeanLocator
                .getProjectFactoryService();
        SecurityManagerService securitymanager = SpringBeanLocator
                .getSecurityManagerService();

        for (Iterator i = projectfactory.getProjects().iterator(); i.hasNext();) {
            Project project = (Project) i.next();
            if (securitymanager.mayEditProject(project)) {
                Element projectElement = resdoc.createSubNode(elem, "project");
                projectElement.setAttribute("name", project.getName());
                projectElement.setAttribute("comment", project.getComment());
                if (this.selectedProject != null
                        && project.equals(this.selectedProject)) {
                    projectElement.setAttribute("selected", "true");
                }
            }
        }

    }

    public void reset() throws Exception {
        this.selectedProject = null;
    }

    public boolean selectProject(String projectName) {
        Project project = SpringBeanLocator.getProjectFactoryService()
                .getProjectByName(projectName);
        if (project == null) {
            return false;
        } else {
            this.selectedProject = project;
            // Reset page selection
            EditorResourceLocator.getPagesResource(this.context).unselectPage();
            EditorResourceLocator.getTargetsResource(this.context)
                    .unselectTarget();
            EditorResourceLocator.getImagesResource(this.context)
                    .unselectImage();
            EditorResourceLocator.getIncludesResource(this.context)
                    .unselectIncludePart();
            return true;
        }
    }

    public Project getSelectedProject() {
        return this.selectedProject;
    }

    public boolean selectProjectByTargetGeneratorName(String targetGenerator) {
        Project project = SpringBeanLocator.getProjectFactoryService()
                .getProjectByPustefixTargetGeneratorName(targetGenerator);
        if (project == null) {
            return false;
        } else {
            this.selectedProject = project;
            // Reset page selection
            EditorResourceLocator.getPagesResource(this.context).unselectPage();
            EditorResourceLocator.getTargetsResource(this.context)
                    .unselectTarget();
            EditorResourceLocator.getImagesResource(this.context)
                    .unselectImage();
            EditorResourceLocator.getIncludesResource(this.context)
                    .unselectIncludePart();
            return true;
        }
    }

}
