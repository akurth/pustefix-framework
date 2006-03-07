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
import java.util.HashSet;
import java.util.Iterator;

import de.schlund.pfixcore.editor2.core.dom.Image;
import de.schlund.pfixcore.editor2.core.dom.IncludePart;
import de.schlund.pfixcore.editor2.core.dom.IncludePartThemeVariant;
import de.schlund.pfixcore.editor2.core.dom.Page;
import de.schlund.pfixcore.editor2.core.dom.Project;
import de.schlund.pfixcore.editor2.core.dom.Theme;
import de.schlund.pfixcore.editor2.core.exception.EditorSecurityException;
import de.schlund.pfixcore.editor2.core.exception.EditorUserNotExistingException;
import de.schlund.pfixcore.editor2.core.vo.EditorUser;

public class SecurityManagerServiceImpl implements SecurityManagerService {
    private SessionService session;

    private UserManagementService usermanagement;

    private final static String PRINCIPAL_KEY = SecurityManagerServiceImpl.class
            .getName()
            + ".principal";

    public void setSessionService(SessionService session) {
        this.session = session;
    }

    public void setUserManagementService(UserManagementService usermanagement) {
        this.usermanagement = usermanagement;
    }

    public void setPrincipal(Principal principal) {
        this.session.set(PRINCIPAL_KEY, principal);
    }

    public Principal getPrincipal() {
        return (Principal) this.session.get(PRINCIPAL_KEY);
    }

    public boolean mayEditIncludePartThemeVariant(IncludePartThemeVariant part) {
        for (Iterator i = part.getAffectedPages().iterator(); i.hasNext();) {
            Page page = (Page) i.next();
            if (page != null && !mayEditIncludePartForProject(page.getProject()))
                return false;
        }
        return true;
    }

    public boolean mayEditIncludePartForProject(Project project) {
        if (this.getPrincipal() == null) {
            return false;
        }
        String username = this.getPrincipal().getName();
        if (username == null || username.equals("")) {
            return false;
        }

        try {
            EditorUser user = this.usermanagement.getUser(username);
            if (user.getGlobalPermissions().isAdmin()) {
                return true;
            }
            if (user.getProjectPermissions(project).isEditIncludes()) {
                return true;
            }
        } catch (EditorUserNotExistingException e) {
            return false;
        }

        return false;
    }

    public void checkEditIncludePartThemeVariant(IncludePartThemeVariant part)
            throws EditorSecurityException {
        if (!mayEditIncludePartThemeVariant(part)) {
            throw new EditorSecurityException(
                    "Operation editIncludePartThemeVariant not permitted!");
        }
    }

    public boolean mayCreateIncludePartThemeVariant(IncludePart part,
            Theme theme) {
        HashSet<Page> pages = new HashSet<Page>();
        HashSet<Theme> themes = new HashSet<Theme>();
        for (Iterator i = part.getThemeVariants().iterator(); i.hasNext();) {
            IncludePartThemeVariant variant = (IncludePartThemeVariant) i
                    .next();
            pages.addAll(variant.getAffectedPages());
            themes.add(variant.getTheme());
        }
        // For all pages which are affected by this IncludePart:
        // Check whether they will use a variant using theme.
        for (Iterator i = pages.iterator(); i.hasNext();) {
            Page page = (Page) i.next();
            for (Iterator i2 = page.getThemes().getThemes().iterator(); i2
                    .hasNext();) {
                Theme currentTheme = (Theme) i2.next();
                if (currentTheme.equals(theme)) {
                    // This page WILL use the new variant
                    if (!mayEditIncludePartForProject(page.getProject())) {
                        return false;
                    }
                    break;
                }
                if (themes.contains(currentTheme)) {
                    // This page will not use the newly created variant
                    // as it is already using another theme with a higher
                    // priority.
                    break;
                }
            }
        }
        // No test failed, so the user is allowed to create the part
        return true;
    }

    public void checkCreateIncludePartThemeVariant(IncludePart part, Theme theme)
            throws EditorSecurityException {
        if (!mayCreateIncludePartThemeVariant(part, theme)) {
            throw new EditorSecurityException(
                    "Operation createIncludePartThemeVariant not permitted!");
        }
    }

    public boolean mayEditImage(Image image) {
        for (Iterator i = image.getAffectedPages().iterator(); i.hasNext();) {
            Page page = (Page) i.next();
            if (!mayEditImageForProject(page.getProject()))
                return false;
        }
        return true;
    }

    public boolean mayEditImageForProject(Project project) {
        if (this.getPrincipal() == null) {
            return false;
        }
        String username = this.getPrincipal().getName();
        if (username == null || username.equals("")) {
            return false;
        }

        try {
            EditorUser user = this.usermanagement.getUser(username);
            if (user.getGlobalPermissions().isAdmin()) {
                return true;
            }
            if (user.getProjectPermissions(project).isEditImages()) {
                return true;
            }
        } catch (EditorUserNotExistingException e) {
            return false;
        }

        return false;
    }

    public void checkEditImage(Image image) throws EditorSecurityException {
        if (!mayEditImage(image)) {
            throw new EditorSecurityException(
                    "Operation editImage not permitted!");
        }
    }

    public boolean mayEditDynInclude() {
        if (this.getPrincipal() == null) {
            return false;
        }
        String username = this.getPrincipal().getName();
        if (username == null || username.equals("")) {
            return false;
        }

        try {
            EditorUser user = this.usermanagement.getUser(username);
            if (user.getGlobalPermissions().isAdmin()
                    || user.getGlobalPermissions().isEditDynIncludes()) {
                return true;
            } else {
                return false;
            }
        } catch (EditorUserNotExistingException e) {
            return false;
        }
    }

    public void checkEditDynInclude() throws EditorSecurityException {
        if (!mayEditDynInclude()) {
            throw new EditorSecurityException(
                    "Operation editDynInclude not permitted!");
        }
    }

    public boolean mayAdmin() {
        if (this.getPrincipal() == null) {
            return false;
        }
        String username = this.getPrincipal().getName();
        if (username == null || username.equals("")) {
            return false;
        }

        try {
            EditorUser user = this.usermanagement.getUser(username);
            if (user.getGlobalPermissions().isAdmin()) {
                return true;
            } else {
                return false;
            }
        } catch (EditorUserNotExistingException e) {
            return false;
        }
    }

    public void checkAdmin() throws EditorSecurityException {
        if (!mayAdmin()) {
            throw new EditorSecurityException("User is not an admin!");
        }
    }
}
