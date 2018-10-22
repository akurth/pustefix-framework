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

package org.pustefixframework.editor.webui.handlers;

import org.pustefixframework.editor.generated.EditorStatusCodes;
import org.pustefixframework.editor.webui.resources.ImagesResource;
import org.pustefixframework.editor.webui.resources.ProjectsResource;
import org.pustefixframework.editor.webui.wrappers.SelectImage;
import org.springframework.beans.factory.annotation.Autowired;

import de.schlund.pfixcore.generator.IHandler;
import de.schlund.pfixcore.generator.IWrapper;
import de.schlund.pfixcore.workflow.Context;

/**
 * Handles image selection
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public class SelectImageHandler implements IHandler {

    private ImagesResource imagesResource;
    private ProjectsResource projectsResource;

    public void handleSubmittedData(Context context, IWrapper wrapper)
            throws Exception {
        SelectImage input = (SelectImage) wrapper;
        if (!imagesResource.selectImage(input.getPath())) {
            input.addSCodePath(EditorStatusCodes.IMAGES_IMAGE_UNDEF);
        }
    }

    public void retrieveCurrentStatus(Context context, IWrapper wrapper)
            throws Exception {
        // Do not insert any data
    }

    public boolean prerequisitesMet(Context context) throws Exception {
        // Allow only if project is selected
        return (projectsResource.getSelectedProject() != null);
    }

    public boolean isActive(Context context) throws Exception {
        // Allways allow image selection
        return true;
    }

    public boolean needsData(Context context) throws Exception {
        // Always ask to select image
        return true;
    }

    @Autowired
    public void setImagesResource(ImagesResource imagesResource) {
        this.imagesResource = imagesResource;
    }

    @Autowired
    public void setProjectsResource(ProjectsResource projectsResource) {
        this.projectsResource = projectsResource;
    }

}
