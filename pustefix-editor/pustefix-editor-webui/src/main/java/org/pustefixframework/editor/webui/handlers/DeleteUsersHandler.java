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

import org.pustefixframework.editor.common.exception.EditorUserNotExistingException;
import org.pustefixframework.editor.webui.resources.UsersResource;
import org.pustefixframework.editor.webui.wrappers.DeleteUsers;
import org.pustefixframework.generated.CoreStatusCodes;
import org.springframework.beans.factory.annotation.Autowired;

import de.schlund.pfixcore.editor2.core.spring.SecurityManagerService;
import de.schlund.pfixcore.generator.IHandler;
import de.schlund.pfixcore.generator.IWrapper;
import de.schlund.pfixcore.workflow.Context;

/**
 * Handles user removal
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public class DeleteUsersHandler implements IHandler {
    
    private SecurityManagerService securitymanager;
    
    private UsersResource usersResource;
    
    public void handleSubmittedData(Context context, IWrapper wrapper)
            throws Exception {
        DeleteUsers input = (DeleteUsers) wrapper;
        if (input.getUsername() != null) {
            String[] usernames = input.getUsername();
            try {
                usersResource.deleteUsers(usernames);
            } catch (EditorUserNotExistingException e) {
                input.addSCodeUsername(CoreStatusCodes.GEN_ERROR);
            }
        }
    }

    public void retrieveCurrentStatus(Context context, IWrapper wrapper)
            throws Exception {
        // Do not insert data
    }

    public boolean prerequisitesMet(Context context) throws Exception {
        // Only admins can delete users
        return securitymanager.mayAdmin();
    }

    public boolean isActive(Context context) throws Exception {
        // Always await input
        return true;
    }

    public boolean needsData(Context context) throws Exception {
        // Always ask for selection
        return true;
    }

    @Autowired
    public void setSecurityManagerService(SecurityManagerService securitymanager) {
        this.securitymanager = securitymanager;
    }

    @Autowired
    public void setUsersResource(UsersResource usersResource) {
        this.usersResource = usersResource;
    }

}
