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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import de.schlund.pfixcore.editor2.core.dom.Project;
import de.schlund.pfixcore.editor2.core.exception.EditorDuplicateUsernameException;
import de.schlund.pfixcore.editor2.core.exception.EditorIOException;
import de.schlund.pfixcore.editor2.core.exception.EditorParsingException;
import de.schlund.pfixcore.editor2.core.exception.EditorSecurityException;
import de.schlund.pfixcore.editor2.core.exception.EditorUserNotExistingException;
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
public class UserManagementServiceImpl implements UserManagementService {
    private static final String CONFIG_FILE = "common/conf/userdata.xml";

    private FileSystemService filesystem;

    private SecurityManagerService securitymanager;

    private PathResolverService pathresolver;

    private HashMap users;

    private ProjectFactoryService projectfactory;

    private boolean initialized;

    public UserManagementServiceImpl() {
        this.users = new HashMap();
        this.initialized = false;
    }

    public void setFileSystemService(FileSystemService filesystem) {
        this.filesystem = filesystem;
    }

    public void setPathResolverService(PathResolverService pathresolver) {
        this.pathresolver = pathresolver;
    }

    public void setSecurityManagerService(SecurityManagerService securitymanager) {
        this.securitymanager = securitymanager;
    }

    public void setProjectFactoryService(ProjectFactoryService projectfactory) {
        this.projectfactory = projectfactory;
    }

    public void init() throws EditorParsingException, EditorIOException {
        // Load configuration
        this.loadFromFile();
        this.initialized = true;
    }

    public void loadFromFile() throws EditorParsingException, EditorIOException {
        File configFile = new File(this.pathresolver.resolve(CONFIG_FILE));
        Document xml = null;
        synchronized (this.filesystem.getLock(configFile)) {
            try {
                xml = this.filesystem.readXMLDocumentFromFile(configFile);
            } catch (FileNotFoundException e) {
                String err = "File " + configFile.getAbsolutePath()
                        + " could not be found!";
                Logger.getLogger(this.getClass()).error(err, e);
                throw new EditorIOException(err, e);
            } catch (SAXException e) {
                String err = "Error during parsing file "
                        + configFile.getAbsolutePath() + "!";
                Logger.getLogger(this.getClass()).error(err, e);
                throw new EditorParsingException(err, e);
            } catch (IOException e) {
                String err = "File " + configFile.getAbsolutePath()
                        + " could not be read!";
                Logger.getLogger(this.getClass()).error(err, e);
                throw new EditorIOException(err, e);
            }
        }
        synchronized (this.users) {
            this.users.clear();
            try {
                for (Iterator i = XPath.select(xml, "/userinfo/user")
                        .iterator(); i.hasNext();) {
                    Element userNode = (Element) i.next();
                    String userName = userNode.getAttribute("id");
                    if (userName == null) {
                        String err = "<user> has to have id attribute!";
                        Logger.getLogger(this.getClass()).error(err);
                        throw new EditorParsingException(err);
                    }
                    EditorUser user = new EditorUser(userName);

                    String userFullname = userNode.getAttribute("name");
                    if (userFullname == null) {
                        String msg = "User " + userName
                                + " has no name attribute.";
                        Logger.getLogger(this.getClass()).info(msg);
                    } else {
                        user.setFullname(userFullname);
                    }
                    String userPhone = userNode.getAttribute("phone");
                    if (userPhone == null) {
                        String msg = "User " + userName
                                + " has no phone attribute.";
                        Logger.getLogger(this.getClass()).info(msg);
                    } else {
                        user.setPhoneNumber(userPhone);
                    }
                    String userPwd = userNode.getAttribute("pwd");
                    if (userPwd == null) {
                        String msg = "User " + userName
                                + " has no pwd attribute.";
                        Logger.getLogger(this.getClass()).info(msg);
                    } else {
                        user.setCryptedPassword(userPwd);
                    }
                    String userSection = userNode.getAttribute("sect");
                    if (userSection == null) {
                        String msg = "User " + userName
                                + " has no sect attribute.";
                        Logger.getLogger(this.getClass()).info(msg);
                    } else {
                        user.setSectionName(userSection);
                    }

                    Element globalNode = (Element) XPath.selectNode(userNode,
                            "permissions/global");
                    boolean userIsAdmin = false;
                    boolean userMayEditDynIncludes = false;
                    if (globalNode != null) {
                        String temp;
                        temp = globalNode.getAttribute("admin");
                        if (temp != null) {
                            userIsAdmin = temp.equals("true");
                        }
                        temp = globalNode.getAttribute("editDynIncludes");
                        if (temp != null) {
                            userMayEditDynIncludes = temp.equals("true");
                        }
                    }
                    EditorGlobalPermissions globalPermissions = new EditorGlobalPermissions();
                    globalPermissions.setAdmin(userIsAdmin);
                    globalPermissions
                            .setEditDynIncludes(userMayEditDynIncludes);
                    user.setGlobalPermissions(globalPermissions);

                    for (Iterator i2 = XPath.select(userNode,
                            "permissions/project").iterator(); i2.hasNext();) {
                        Element projectNode = (Element) i2.next();
                        String projectName = projectNode.getAttribute("name");
                        if (projectName == null) {
                            String err = "<project> has to have name attribute!";
                            Logger.getLogger(this.getClass()).error(err);
                            throw new EditorParsingException(err);
                        }

                        Project project = this.projectfactory
                                .getProjectByName(projectName);
                        if (project == null) {
                            // Log this and skip permissions for this
                            // project, but continue
                            String msg = "Found permissions for non-existent project "
                                    + projectName
                                    + " for user "
                                    + user.getUsername()
                                    + " - skipping these project permissions!";
                            Logger.getLogger(this.getClass()).warn(msg);
                            continue;
                        }

                        boolean editIncludes = false;
                        boolean editImages = false;
                        String temp;
                        temp = projectNode.getAttribute("editIncludes");
                        if (temp != null) {
                            editIncludes = temp.equals("true");
                            Logger.getLogger(this.getClass()).debug("Setting permission editIncludes for project " + project.getName() + " and user " + user.getUsername() + ".");
                        }
                        temp = projectNode.getAttribute("editImages");
                        if (temp != null) {
                            editImages = temp.equals("true");
                            Logger.getLogger(this.getClass()).debug("Setting permission editImages for project " + project.getName() + " and user " + user.getUsername() + ".");
                        }
                        EditorProjectPermissions projectPermissions = new EditorProjectPermissions();
                        projectPermissions.setEditImages(editImages);
                        projectPermissions.setEditIncludes(editIncludes);

                        user.setProjectPermissions(project, projectPermissions);
                    }

                    this.users.put(user.getUsername(), user);
                }
            } catch (TransformerException e) {
                // Should never happen as a DOM document is always well-formed!
                String err = "XPath error!";
                Logger.getLogger(this.getClass()).error(err, e);
                throw new RuntimeException(err, e);
            }
        }

    }

    private void storeToFile() {
        Document doc = Xml.createDocument();
        Element root = doc.createElement("userinfo");
        doc.appendChild(root);

        File configFile = new File(this.pathresolver.resolve(CONFIG_FILE));

        synchronized (this.users) {
            for (Iterator i = this.users.values().iterator(); i.hasNext();) {
                EditorUser user = (EditorUser) i.next();
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
                if (user.getGlobalPermissions().isEditDynIncludes()) {
                    globalElement.setAttribute("editDynIncludes", "true");
                } else {
                    globalElement.setAttribute("editDynIncludes", "false");
                }

                for (Iterator i2 = user.getProjectsWithPermissions().iterator(); i2
                        .hasNext();) {
                    Project project = (Project) i2.next();
                    EditorProjectPermissions projectPermissions = user
                            .getProjectPermissions(project);
                    Element projectElement = doc.createElement("project");
                    permissionsElement.appendChild(projectElement);
                    if (projectPermissions.isEditImages()) {
                        permissionsElement.setAttribute("editImages", "true");
                    } else {
                        permissionsElement.setAttribute("editImages", "false");
                    }
                    if (projectPermissions.isEditIncludes()) {
                        permissionsElement.setAttribute("editIncludes", "true");
                    } else {
                        permissionsElement
                                .setAttribute("editIncludes", "false");
                    }
                }
            }

            try {
                Xml.serialize(doc, configFile, true, true);
            } catch (IOException e) {
                // Ooops, something went wrong.
                // However we will not be able to recover from
                // this error on a higher level, so log this
                // error and continue
                String err = "Error during writing userdata file!";
                Logger.getLogger(this.getClass()).error(err, e);
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
        this.securitymanager.checkAdmin();
        synchronized (this.users) {
            if (!this.users.containsKey(user.getUsername())) {
                throw new EditorUserNotExistingException(
                        "No user found for username " + user.getUsername()
                                + "!");
            }
            this.users.put(user.getUsername(), user);
        }
        this.storeToFile();
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
        this.storeToFile();
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
        this.storeToFile();
    }

    public Collection getUsers() {
        this.checkInitialized();
        synchronized (this.users) {
            HashSet users = new HashSet();
            for (Iterator i = this.users.values().iterator(); i.hasNext();) {
                EditorUser user = (EditorUser) i.next();
                users.add(user.clone());
            }
            return users;
        }
    }
    
    private void checkInitialized() {
        if (!this.initialized) {
            throw new RuntimeException("Service has to be initialized before use!");
        }
    }

}
