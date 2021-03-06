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

package de.schlund.pfixcore.editor2.core.spring;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.xml.transform.TransformerException;

import org.pustefixframework.editor.common.dom.Project;
import org.pustefixframework.editor.common.exception.EditorDuplicateUsernameException;
import org.pustefixframework.editor.common.exception.EditorIOException;
import org.pustefixframework.editor.common.exception.EditorParsingException;
import org.pustefixframework.editor.common.exception.EditorSecurityException;
import org.pustefixframework.editor.common.exception.EditorUserNotExistingException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.context.ServletContextAware;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import de.schlund.pfixcore.editor2.core.vo.EditorGlobalPermissions;
import de.schlund.pfixcore.editor2.core.vo.EditorProjectPermissions;
import de.schlund.pfixcore.editor2.core.vo.EditorUser;
import de.schlund.pfixxml.util.XPath;
import de.schlund.pfixxml.util.Xml;

/**
 * Implementation of UserManagementService using a XML file to store userdata.
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public class UserManagementServiceImpl implements UserManagementService, ApplicationContextAware, ServletContextAware, InitializingBean {
    
    private final static String CONTEXT_PARAM_EDITOR_USERDATA = "editor.userdata";
    private final static String DEFAULT_EDITOR_USERDATA = "WEB-INF/userdata.xml";
    
    private SecurityManagerService securitymanager;
    
    private ProjectPool projectPool;

    private Map<String, EditorUser> users;

    private boolean initialized;
    
    private Object usersFileLock = new Object();

    private ApplicationContext applicationContext;
    private ServletContext servletContext;
    
    private Resource userDataResource;
    
    public UserManagementServiceImpl() {
        this.users = new LinkedHashMap<String, EditorUser>();
        this.initialized = false;
    }

    public void setProjectPool(ProjectPool projectPool) {
        this.projectPool = projectPool;
    }
    
    public void setSecurityManagerService(SecurityManagerService securitymanager) {
        this.securitymanager = securitymanager;
    }
    
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void afterPropertiesSet() throws Exception {
        String editorUserData = servletContext.getInitParameter(CONTEXT_PARAM_EDITOR_USERDATA);
        if(editorUserData == null) {
            userDataResource = applicationContext.getResource(DEFAULT_EDITOR_USERDATA);
        } else {
            userDataResource = applicationContext.getResource(editorUserData);
        }
    }
    
    public void init() throws EditorParsingException, EditorIOException {
        // Load configuration
        this.loadFromResource(userDataResource);
        this.initialized = true;
    }

    public void loadFromResource(Resource resource) throws EditorParsingException, EditorIOException {
        Document xml = null;
        
        synchronized (usersFileLock) {
            try {
                xml = Xml.parseMutable(resource.getInputStream());
            } catch (FileNotFoundException e) {
                String err = "File " + resource + " could not be found!";
                LoggerFactory.getLogger(this.getClass()).error(err, e);
                throw new EditorIOException(err, e);
            } catch (SAXException e) {
                String err = "Error during parsing file " + resource + "!";
                LoggerFactory.getLogger(this.getClass()).error(err, e);
                throw new EditorParsingException(err, e);
            } catch (IOException e) {
                String err = "File " + resource + " could not be read!";
                LoggerFactory.getLogger(this.getClass()).error(err, e);
                throw new EditorIOException(err, e);
            }
        }
        synchronized (this.users) {
            this.users.clear();
            try {
                for (Iterator<Node> i = XPath.select(xml, "/userinfo/user")
                        .iterator(); i.hasNext();) {
                    Element userNode = (Element) i.next();
                    String userName = userNode.getAttribute("id");
                    if (userName == null) {
                        String err = "<user> has to have id attribute!";
                        LoggerFactory.getLogger(this.getClass()).error(err);
                        throw new EditorParsingException(err);
                    }
                    EditorUser user = new EditorUser(userName);

                    String userFullname = userNode.getAttribute("name");
                    if (userFullname == null) {
                        String msg = "User " + userName
                                + " has no name attribute.";
                        LoggerFactory.getLogger(this.getClass()).info(msg);
                    } else {
                        user.setFullname(userFullname);
                    }
                    String userPhone = userNode.getAttribute("phone");
                    if (userPhone == null) {
                        String msg = "User " + userName
                                + " has no phone attribute.";
                        LoggerFactory.getLogger(this.getClass()).info(msg);
                    } else {
                        user.setPhoneNumber(userPhone);
                    }
                    String userPwd = userNode.getAttribute("pwd");
                    if (userPwd == null) {
                        String msg = "User " + userName
                                + " has no pwd attribute.";
                        LoggerFactory.getLogger(this.getClass()).info(msg);
                    } else {
                        user.setCryptedPassword(userPwd);
                    }
                    String userSection = userNode.getAttribute("sect");
                    if (userSection == null) {
                        String msg = "User " + userName
                                + " has no sect attribute.";
                        LoggerFactory.getLogger(this.getClass()).info(msg);
                    } else {
                        user.setSectionName(userSection);
                    }

                    Element globalNode = (Element) XPath.selectNode(userNode,
                            "permissions/global");
                    boolean userIsAdmin = false;
                    if (globalNode != null) {
                        String temp;
                        temp = globalNode.getAttribute("admin");
                        if (temp != null) {
                            userIsAdmin = temp.equals("true");
                        }
                    }
                    EditorGlobalPermissions globalPermissions = new EditorGlobalPermissions();
                    globalPermissions.setAdmin(userIsAdmin);
                    user.setGlobalPermissions(globalPermissions);

                    for (Node projectNode : XPath.select(userNode,
                            "permissions/project")) {
                        Element projectElement = (Element) projectNode;
                        String projectName = projectElement.getAttribute("name");
                        if (projectName == null) {
                            String err = "<project> has to have name attribute!";
                            LoggerFactory.getLogger(this.getClass()).error(err);
                            throw new EditorParsingException(err);
                        }
                        
                        boolean editIncludes = false;
                        boolean editImages = false;
                        String temp;
                        temp = projectElement.getAttribute("editIncludes");
                        if (temp != null) {
                            editIncludes = temp.equals("true");
                        }
                        temp = projectElement.getAttribute("editImages");
                        if (temp != null) {
                            editImages = temp.equals("true");
                        }
                        EditorProjectPermissions projectPermissions = new EditorProjectPermissions();
                        projectPermissions.setEditImages(editImages);
                        projectPermissions.setEditIncludes(editIncludes);

                        user.setProjectPermissions(projectName, projectPermissions);
                    }

                    this.users.put(user.getUsername(), user);
                }
            } catch (TransformerException e) {
                // Should never happen as a DOM document is always well-formed!
                String err = "XPath error!";
                LoggerFactory.getLogger(this.getClass()).error(err, e);
                throw new RuntimeException(err, e);
            }
        }

    }

    private void storeToResource() {
        File file;

        Document doc = Xml.createDocument();
        Element root = doc.createElement("userinfo");
        doc.appendChild(root);
        if (userDataResource instanceof FileSystemResource) {
            file = ((FileSystemResource) userDataResource).getFile();
        } else if (userDataResource instanceof org.springframework.core.io.UrlResource) {
            try {
                file = ((org.springframework.core.io.UrlResource) userDataResource).getFile();
            } catch (IOException e) {
                throw new IllegalArgumentException("Userdata resource can't be stored because it's no file system resource", e);
            }
        } else {
            throw new IllegalArgumentException("Userdata resource can't be stored because it's no file system resource: " + (userDataResource == null ? "null" : userDataResource.getClass()));
        }

        synchronized (this.users) {
            for (EditorUser user : this.users.values()) {
                Element userElement = doc.createElement("user");
                root.appendChild(userElement);
                userElement.setAttribute("id", user.getUsername());
                userElement.setAttribute("name", user.getFullname());
                userElement.setAttribute("phone", user.getPhoneNumber());
                userElement.setAttribute("pwd", user.getCryptedPassword());
                userElement.setAttribute("sect", user.getSectionName());

                Element permissionsElement = doc.createElement("permissions");
                userElement.appendChild(permissionsElement);

                Element globalElement = doc.createElement("global");
                permissionsElement.appendChild(globalElement);
                if (user.getGlobalPermissions().isAdmin()) {
                    globalElement.setAttribute("admin", "true");
                } else {
                    globalElement.setAttribute("admin", "false");
                }

                for (String projectName : user.getProjectsWithPermissions()) {
                    EditorProjectPermissions projectPermissions = user
                            .getProjectPermissions(projectName);
                    Element projectElement = doc.createElement("project");
                    permissionsElement.appendChild(projectElement);
                    projectElement.setAttribute("name", projectName);
                    if (projectPermissions.isEditImages()) {
                        projectElement.setAttribute("editImages", "true");
                    } else {
                        projectElement.setAttribute("editImages", "false");
                    }
                    if (projectPermissions.isEditIncludes()) {
                        projectElement.setAttribute("editIncludes", "true");
                    } else {
                        projectElement
                                .setAttribute("editIncludes", "false");
                    }
                }
            }

            try {
                Xml.serialize(doc, file, true, true);
            } catch (IOException e) {
                // Ooops, something went wrong.
                // However we will not be able to recover from
                // this error on a higher level, so log this
                // error and continue
                String err = "Error during writing userdata file!";
                LoggerFactory.getLogger(this.getClass()).error(err, e);
            }
        }
    }

    public EditorUser getUser(String username)
            throws EditorUserNotExistingException {
        this.checkInitialized();
        synchronized (this.users) {
            if (!this.users.containsKey(username)) {
                throw new EditorUserNotExistingException(
                        "No user found for username " + username + "!");
            }
            return ((EditorUser) ((EditorUser) this.users.get(username))
                    .clone());
        }
    }

    public void updateUser(EditorUser user)
            throws EditorUserNotExistingException, EditorSecurityException {
        this.checkInitialized();
        EditorUser newuser;
        if (!this.securitymanager.mayAdmin()
                && user.getUsername().equals(
                        this.securitymanager.getPrincipal().getName())) {
            // Users may edit themselves but not change rights
            synchronized (this.users) {
                newuser = this.getUser(user.getUsername());
            }
            if (newuser == null) {
                throw new EditorUserNotExistingException(
                        "No user found for username " + user.getUsername()
                                + "!");
            }
            newuser.setCryptedPassword(user.getCryptedPassword());
            newuser.setFullname(user.getFullname());
            newuser.setPhoneNumber(user.getPhoneNumber());
            newuser.setSectionName(user.getSectionName());
        } else {
            this.securitymanager.checkAdmin();
            newuser = user;
        }
        synchronized (this.users) {
            if (!this.users.containsKey(newuser.getUsername())) {
                throw new EditorUserNotExistingException(
                        "No user found for username " + newuser.getUsername()
                                + "!");
            }
            this.users.put(newuser.getUsername(), newuser);
        }
        this.storeToResource();
    }

    public void createUser(EditorUser user)
            throws EditorDuplicateUsernameException, EditorSecurityException {
        this.checkInitialized();
        this.securitymanager.checkAdmin();
        synchronized (this.users) {
            if (this.users.containsKey(user.getUsername())) {
                throw new EditorDuplicateUsernameException("User "
                        + user.getUsername() + " already existing!");
            }
            this.users.put(user.getUsername(), user);
        }
        this.storeToResource();
    }

    public void deleteUser(EditorUser user)
            throws EditorUserNotExistingException, EditorSecurityException {
        this.checkInitialized();
        this.securitymanager.checkAdmin();
        synchronized (this.users) {
            if (!this.users.containsKey(user.getUsername())) {
                throw new EditorUserNotExistingException(
                        "No user found for username " + user.getUsername()
                                + "!");
            }
            this.users.remove(user.getUsername());
        }
        this.storeToResource();
    }

    public Collection<EditorUser> getUsers() {
        this.checkInitialized();
        synchronized (this.users) {
            HashSet<EditorUser> users = new HashSet<EditorUser>();
            for (Iterator<EditorUser> i = this.users.values().iterator(); i.hasNext();) {
                EditorUser user = i.next();
                users.add((EditorUser) user.clone());
            }
            return users;
        }
    }

    private void checkInitialized() {
        if (!this.initialized) {
            throw new RuntimeException(
                    "Service has to be initialized before use!");
        }
    }

    public boolean hasUser(String username) {
        synchronized (this.users) {
            return this.users.containsKey(username);
        }
    }

    public Collection<String> getKnownProjects() {
        LinkedHashSet<String> projectNames = new LinkedHashSet<String>();
        synchronized (this.users) {
            for (EditorUser user: users.values()) {
                projectNames.addAll(user.getProjectsWithPermissions());
            }
        }
        for (Project project : projectPool.getProjects()) {
            try {
                projectNames.add(project.getName());
            } catch (Exception e) {
                // Ignore exception and proceed with next project
            }
        }
        return projectNames;
    }

}
