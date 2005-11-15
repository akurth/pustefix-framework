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

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import org.w3c.dom.Element;

import de.schlund.pfixcore.editor2.core.dom.Image;
import de.schlund.pfixcore.editor2.core.dom.Page;
import de.schlund.pfixcore.editor2.core.dom.Project;
import de.schlund.pfixcore.editor2.core.exception.EditorSecurityException;
import de.schlund.pfixcore.editor2.frontend.util.EditorResourceLocator;
import de.schlund.pfixcore.editor2.frontend.util.SpringBeanLocator;
import de.schlund.pfixcore.workflow.Context;
import de.schlund.pfixxml.PathFactory;
import de.schlund.pfixxml.ResultDocument;

/**
 * Implementation of ImagesResource
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public class ImagesResourceImpl implements ImagesResource {

    private Context context;

    private Image selectedImage;

    public boolean selectImage(String path) {
        if (path == null || path.equals("")) {
            return false;
        }
        Project project = EditorResourceLocator.getProjectsResource(
                this.context).getSelectedProject();
        if (project == null) {
            return false;
        }
        Collection allimages = project.getAllImages();
        for (Iterator i = allimages.iterator(); i.hasNext();) {
            Image image = (Image) i.next();
            if (image.getPath().equals(path)) {
                this.selectedImage = image;
                return true;
            }
        }
        return false;
    }

    public void unselectImage() {
        this.selectedImage = null;
    }

    public Image getSelectedImage() {
        return this.selectedImage;
    }

    public void init(Context context) throws Exception {
        this.context = context;
    }

    public void insertStatus(ResultDocument resdoc, Element elem)
            throws Exception {
        Project project = EditorResourceLocator.getProjectsResource(
                this.context).getSelectedProject();

        if (project != null) {
            TreeSet allimages = new TreeSet(project.getAllImages());
            HashMap directoryNodes = new HashMap();
            for (Iterator i = allimages.iterator(); i.hasNext();) {
                Image image = (Image) i.next();
                String path = image.getPath();
                String directory = path.substring(0, path.lastIndexOf("/"));
                Element directoryNode = (Element) directoryNodes.get(directory);
                if (directoryNode == null) {
                    directoryNode = resdoc.createSubNode(elem, "directory");
                    directoryNode.setAttribute("path", directory);
                    directoryNodes.put(directory, directoryNode);
                }
                Element imageNode = resdoc
                        .createSubNode(directoryNode, "image");
                imageNode.setAttribute("path", path);
                imageNode.setAttribute("filename", path.substring(path
                        .lastIndexOf("/") + 1));
                if (image.getLastModTime() == 0) {
                    imageNode.setAttribute("missing", "true");
                }
                if (this.selectedImage != null
                        && image.equals(this.selectedImage)) {
                    imageNode.setAttribute("selected", "true");
                }
            }
            if (this.selectedImage != null) {
                Element currentImage = resdoc.createSubNode(elem,
                        "currentimage");
                String path = this.selectedImage.getPath();
                currentImage.setAttribute("path", path);
                currentImage.setAttribute("filename", path.substring(path
                        .lastIndexOf("/") + 1));
                File imageFile = PathFactory.getInstance().createPath(path)
                        .resolve();
                long modtime = imageFile.lastModified();
                currentImage.setAttribute("modtime", Long.toString(modtime));
                if (SpringBeanLocator.getSecurityManagerService().mayEditImage(
                        this.selectedImage)) {
                    currentImage.setAttribute("mayEdit", "true");
                } else {
                    currentImage.setAttribute("mayEdit", "false");
                }

                // Render backups
                Collection backups = SpringBeanLocator.getBackupService()
                        .listImageVersions(this.selectedImage);
                if (!backups.isEmpty()) {
                    Element backupsNode = resdoc.createSubNode(currentImage,
                            "backups");
                    for (Iterator i = backups.iterator(); i.hasNext();) {
                        String version = (String) i.next();
                        ResultDocument.addTextChild(backupsNode, "option",
                                version);
                    }
                }

                // Render affected pages
                Collection pages = this.selectedImage.getAffectedPages();
                if (!pages.isEmpty()) {
                    Element pagesNode = resdoc.createSubNode(currentImage,
                            "pages");
                    HashMap projectNodes = new HashMap();
                    for (Iterator i = pages.iterator(); i.hasNext();) {
                        Page page = (Page) i.next();
                        Element projectNode;
                        if (projectNodes.containsKey(page.getProject())) {
                            projectNode = (Element) projectNodes.get(page
                                    .getProject());
                        } else {
                            projectNode = resdoc.createSubNode(pagesNode,
                                    "project");
                            projectNode.setAttribute("name", page.getProject()
                                    .getName());
                            projectNodes.put(page.getProject(), projectNode);
                        }
                        Element pageNode = resdoc.createSubNode(projectNode,
                                "page");
                        pageNode.setAttribute("name", page.getName());
                        if (page.getVariant() != null) {
                            pageNode.setAttribute("variant", page.getVariant()
                                    .getName());
                        }
                    }
                }
            }
        }
    }

    public void reset() throws Exception {
        this.selectedImage = null;
    }

    public int restoreBackup(String version, long modtime) {
        if (this.selectedImage == null) {
            return 1;
        }
        if (this.selectedImage.getLastModTime() != modtime) {
            return 2;
        }

        try {
            if (SpringBeanLocator.getBackupService().restoreImage(
                    this.selectedImage, version)) {
                return 0;
            } else {
                return 1;
            }
        } catch (EditorSecurityException e) {
            return 1;
        }
    }

}
