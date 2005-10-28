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

import de.schlund.pfixcore.editor2.core.exception.EditorDuplicateUsernameException;
import de.schlund.pfixcore.editor2.frontend.util.EditorResourceLocator;
import de.schlund.pfixcore.editor2.frontend.util.SpringBeanLocator;
import de.schlund.pfixcore.editor2.frontend.wrappers.SelectUser;
import de.schlund.pfixcore.generator.IHandler;
import de.schlund.pfixcore.generator.IWrapper;
import de.schlund.pfixcore.workflow.Context;
import de.schlund.util.statuscodes.StatusCodeLib;

/**
 * Handles user selection
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public class SelectUserHandler implements IHandler {

    public void handleSubmittedData(Context context, IWrapper wrapper)
            throws Exception {
        SelectUser input = (SelectUser) wrapper;
        if (input.getCreate() != null && input.getCreate().booleanValue()) {
            try {
                EditorResourceLocator.getUsersResource(context)
                        .createAndSelectUser(input.getUsername());
            } catch (EditorDuplicateUsernameException e) {
                input.addSCodeUsername(StatusCodeLib.PFIXCORE_EDITOR_ADDUSER_USER_EXISTS);
            }
        } else {
            if (SpringBeanLocator.getSecurityManagerService().mayAdmin()
                    || input.getUsername().equals(
                            SpringBeanLocator.getSecurityManagerService()
                                    .getPrincipal().getName())) {
                EditorResourceLocator.getUsersResource(context).selectUser(
                        input.getUsername());
            }
        }
    }

    public void retrieveCurrentStatus(Context context, IWrapper wrapper)
            throws Exception {
        // Do not insert data
    }

    public boolean prerequisitesMet(Context context) throws Exception {
        // Can always select a user
        return true;
    }

    public boolean isActive(Context context) throws Exception {
        // Always await input
        return true;
    }

    public boolean needsData(Context context) throws Exception {
        // Do not affect pageflow
        return false;
    }

}
