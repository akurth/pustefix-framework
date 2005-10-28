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

package de.schlund.pfixcore.editor2.frontend.handlers;

import de.schlund.pfixcore.editor2.frontend.util.EditorResourceLocator;
import de.schlund.pfixcore.editor2.frontend.wrappers.SelectImage;
import de.schlund.pfixcore.generator.IHandler;
import de.schlund.pfixcore.generator.IWrapper;
import de.schlund.pfixcore.workflow.Context;
import de.schlund.util.statuscodes.StatusCodeLib;

/**
 * Handles image selection
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public class SelectImageHandler implements IHandler {

    public void handleSubmittedData(Context context, IWrapper wrapper)
            throws Exception {
        SelectImage input = (SelectImage) wrapper;
        if (!EditorResourceLocator.getImagesResource(context).selectImage(
                input.getPath())) {
            input.addSCodePath(StatusCodeLib.PFIXCORE_EDITOR_IMAGES_IMAGE_UNDEF);
        }
    }

    public void retrieveCurrentStatus(Context context, IWrapper wrapper)
            throws Exception {
        // Do not insert any data
    }

    public boolean prerequisitesMet(Context context) throws Exception {
        // Allow only if project is selected
        return (EditorResourceLocator.getProjectsResource(context)
                .getSelectedProject() != null);
    }

    public boolean isActive(Context context) throws Exception {
        // Allways allow image selection
        return true;
    }

    public boolean needsData(Context context) throws Exception {
        // Always ask to select image
        return true;
    }

}
