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

package org.pustefixframework.editor.webui.resources;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.pustefixframework.editor.common.dom.IncludePartThemeVariant;
import org.pustefixframework.editor.common.dom.Project;
import org.pustefixframework.editor.webui.resources.util.SessionInfoStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;

import de.schlund.pfixcore.beans.InsertStatus;
import de.schlund.pfixcore.editor2.core.dom.SessionInfo;
import de.schlund.pfixcore.editor2.core.spring.ProjectPool;
import de.schlund.pfixcore.editor2.core.vo.EditorUser;
import de.schlund.pfixxml.ResultDocument;

public class SessionInfoResource {
    private SessionInfoStore sessionInfoStore;
    
    private ProjectPool projectPool;
    
    @InsertStatus
    public void insertStatus(ResultDocument resdoc, Element elem) throws Exception {
        DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (SessionInfo info : sessionInfoStore.getSessionInfos()) {
            EditorUser userinfo = info.getUser();
            if (userinfo != null) {
                Element sessionNode = resdoc.createSubNode(elem, "session");
                sessionNode.setAttribute("username", userinfo.getUsername());
                sessionNode.setAttribute("userphone", userinfo.getPhoneNumber());
                sessionNode.setAttribute("userfullname", userinfo.getFullname());
                IncludePartThemeVariant incPart = info.getIncludePart();
                if (incPart != null) {
                    sessionNode.setAttribute("incpart", incPart.toString());
                }
                Project project = info.getProject();
                if (project != null) {
                    sessionNode.setAttribute("projecturl", projectPool.getURIForProject(project));
                }
                sessionNode.setAttribute("lastAccess", dateformat.format(info.getLastAccess()));
            }
        }
    }
    
    @Autowired
    public void setSessionInfoStore(SessionInfoStore sessionInfoStore) {
        this.sessionInfoStore = sessionInfoStore;
    }
    
    @Autowired
    public void setProjectPool(ProjectPool projectPool) {
        this.projectPool = projectPool;
    }
    
}
