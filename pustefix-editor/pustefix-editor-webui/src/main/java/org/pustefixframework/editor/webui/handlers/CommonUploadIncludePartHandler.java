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

import org.pustefixframework.container.annotations.Inject;
import org.pustefixframework.editor.common.exception.EditorException;
import org.pustefixframework.editor.common.exception.EditorIncludeHasChangedException;
import org.pustefixframework.editor.generated.EditorStatusCodes;
import org.pustefixframework.editor.webui.resources.CommonIncludesResource;
import org.pustefixframework.editor.webui.resources.SessionResource;
import org.pustefixframework.editor.webui.wrappers.CommonUploadIncludePart;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import de.schlund.pfixcore.generator.IHandler;
import de.schlund.pfixcore.generator.IWrapper;
import de.schlund.pfixcore.workflow.Context;

/**
 * Handles common include part upload
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public abstract class CommonUploadIncludePartHandler implements IHandler {

    protected abstract CommonIncludesResource getResource(Context context);

    private SessionResource sessionResource;

    public void handleSubmittedData(Context context, IWrapper wrapper)
            throws Exception {
        CommonUploadIncludePart input = (CommonUploadIncludePart) wrapper;
        // System.out.println("In HSD");
        
        // Set flag indicating we are in edit mode
       sessionResource.setInIncludeEditView(true);
       sessionResource.setSelectedIncludePart(this.getResource(context).getSelectedIncludePart());
        
        if (input.getDoUpload() != null && input.getDoUpload().booleanValue()
                && input.getHash() != null) {
            String content = input.getContent();
            if (content == null) {
                content = "";
            }
            try {
                String path = this.getResource(context).getSelectedIncludePart().getIncludePart().getIncludeFile().getPath();
                if (path.lastIndexOf('/') == -1 || path.lastIndexOf('/') == 0) {
                    input.addSCodeContent(EditorStatusCodes.INCLUDESUPLOAD_FILE_IS_IN_ROOT);
                    return;
                }
                if (input.getDoOverwriteWithStoredContent() != null && input.getDoOverwriteWithStoredContent().booleanValue() && input.getStoredContent() != null && input.getStoredContent().length() != 0) {
                    content = input.getStoredContent();
                    input.setContent(content);
                }
                this.getResource(context).setContent(content, (input.getPreserveFormat() != null) ? !input.getPreserveFormat().booleanValue() : true, input.getHash());
                input.setHash(this.getResource(context).getMD5());
            } catch (SAXException e) {
                LoggerFactory.getLogger(this.getClass()).warn(e.getMessage(), e);
                input
                        .addSCodeContent(EditorStatusCodes.INCLUDESUPLOAD_PARSE_ERR);
            } catch (EditorIncludeHasChangedException e) {
                input.setHash(e.getNewHash());
                input.setStoredContent(content);
                input.setContent(e.getMerged());
                input
                        .addSCodeContent(EditorStatusCodes.INCLUDESUPLOAD_INCLUDE_HAS_CHANGED);
                return;
            } catch (EditorException e) {
                input
                        .addSCodeContent(EditorStatusCodes.INCLUDESUPLOAD_GEN_ERR);
                return;
            }
        }
    }

    public void retrieveCurrentStatus(Context context, IWrapper wrapper)
            throws Exception {
        // Insert XML and hash value
        CommonUploadIncludePart input = (CommonUploadIncludePart) wrapper;
        if (input.getDoUpload() == null || !input.getDoUpload().booleanValue()) {
            input.setContent(this.getResource(context).getContent());
            input.setHash(this.getResource(context).getMD5());
            input.setPreserveFormat(!this.getResource(context).isContentIndented());

            // Set flag indicating we are in edit mode
            sessionResource.setInIncludeEditView(true);
            sessionResource.setSelectedIncludePart(this.getResource(context).getSelectedIncludePart());
        }
    }

    public boolean prerequisitesMet(Context context) throws Exception {
        // Always allow upload
        return true;
    }

    public boolean isActive(Context context) throws Exception {
        // Handler is only active, if part is selected
        return (this.getResource(context).getSelectedIncludePart() != null);
    }

    public boolean needsData(Context context) throws Exception {
        // Always ask to upload include part
        return true;
    }

    @Inject
    public void setSessionResource(SessionResource sessionResource) {
        this.sessionResource = sessionResource;
    }

}
