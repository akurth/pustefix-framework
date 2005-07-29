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

package de.schlund.pfixcore.editor2.core.spring;

import java.security.Principal;

import de.schlund.pfixcore.editor2.core.exception.EditorUserNotExistingException;
import de.schlund.pfixcore.editor2.core.vo.EditorUser;
import de.schlund.pfixcore.util.UnixCrypt;

public class UserPasswordAuthenticationServiceImpl implements
        UserPasswordAuthenticationService {
    private UserManagementService usermanagement;

    public void setUserManagementService(UserManagementService usermanagement) {
        this.usermanagement = usermanagement;
    }
    
    public Principal getPrincipalForUser(String username, String password) {
        EditorUser user;
        try {
            user = this.usermanagement.getUser(username);
        } catch (EditorUserNotExistingException e) {
            return null;
        }
        if (!UnixCrypt.matches(user.getCryptedPassword(), password)) {
            return null;
        }
       
        return new PrincipalImpl(username);
    }
    
    private class PrincipalImpl implements Principal {
        private String username;
        
        private PrincipalImpl(String username) {
            this.username = username;
        }
        
        public String getName() {
            return this.username;
        } 
    }
}
