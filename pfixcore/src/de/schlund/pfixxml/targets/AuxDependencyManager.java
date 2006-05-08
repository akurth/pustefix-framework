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
 *
 */

package de.schlund.pfixxml.targets;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import de.schlund.pfixxml.resources.FileResource;
import de.schlund.pfixxml.resources.ResourceUtil;
import de.schlund.pfixxml.util.Xml;

/**
 * AuxDependencyManager.java
 *
 *
 * Created: Tue Jul 17 12:24:15 2001
 *
 * @author <a href="mailto: jtl@schlund.de">Jens Lautenbacher</a>
 *
 *
 */

public class AuxDependencyManager {
    private static Category CAT    = Category.getInstance(AuxDependencyManager.class.getName());
    private static String   DEPAUX = "depaux";
    private TargetImpl   target;
    
    public static AuxDependency root = 
        AuxDependencyFactory.getInstance().getAuxDependencyRoot();
    
    public AuxDependencyManager(Target target) {
        this.target = (TargetImpl) target;
    }
        
    public synchronized void tryInitAuxdepend() throws Exception {
        FileResource auxfile = ResourceUtil.getFileResource(target.getTargetGenerator().getDisccachedir(), target.getTargetKey() + ".aux");
        if (auxfile.exists() && auxfile.canRead() && auxfile.isFile()) {
            Document        doc     = Xml.parseMutable(auxfile);
            NodeList        auxdeps = doc.getElementsByTagName(DEPAUX);
            if (auxdeps.getLength() > 0) {
                for (int j = 0; j < auxdeps.getLength(); j++) {
                    String          type           = ((Element) auxdeps.item(j)).getAttribute("type");
                    String          path_attr      = ((Element) auxdeps.item(j)).getAttribute("path");
                    FileResource    path           = "".equals(path_attr)? null : ResourceUtil.getFileResourceFromDocroot(path_attr);
                    String          part           = ((Element) auxdeps.item(j)).getAttribute("part");
                    String          product        = ((Element) auxdeps.item(j)).getAttribute("product");
                    String          parent_attr    = ((Element) auxdeps.item(j)).getAttribute("parent_path");
                    FileResource    parent_path    = "".equals(parent_attr)? null : ResourceUtil.getFileResourceFromDocroot(parent_attr);
                    String          parent_part    = ((Element) auxdeps.item(j)).getAttribute("parent_part");
                    String          parent_product = ((Element) auxdeps.item(j)).getAttribute("parent_product");
                    String          target_attr    = ((Element) auxdeps.item(j)).getAttribute("target");

                    DependencyType thetype        = DependencyType.getByTag(type);
                    if (thetype == DependencyType.TEXT) {
                        addDependencyInclude(path, part, product, parent_path, parent_part, parent_product);
                    } else if (thetype == DependencyType.IMAGE) {
                        addDependencyImage(path, parent_path, parent_part, parent_product);
                    } else if (thetype == DependencyType.TARGET) {
                        addDependencyTarget(target_attr);
                    }
                }
            }
        }
    }
    
    private AuxDependency getParentDependency(FileResource parent_path,
            String parent_part, String parent_theme) {
        AuxDependency parent = null;

        if (parent_part != null && parent_part.equals(""))
            parent_part = null;
        if (parent_theme != null && parent_theme.equals(""))
            parent_theme = null;

        if (parent_path != null && parent_part != null && parent_theme != null) {
            CAT.debug("*** Found another AuxDependency as Parent...");
            parent = AuxDependencyFactory.getInstance()
                    .getAuxDependencyInclude(parent_path, parent_part,
                            parent_theme);
        } else if (parent_path == null && parent_part == null
                && parent_theme == null) {
            parent = root;
        }

        if (parent != null) {
            return parent;
        } else {
            throw new IllegalArgumentException(
                    "Mixed null and non-null values for parent arguments!");
        }
    }
    
    public synchronized void addDependencyInclude(FileResource path, String part, String theme, FileResource parent_path, String parent_part, String parent_theme) {
        if (path == null || part == null || theme == null) {
            throw new IllegalArgumentException("Null pointer is not allowed here");
        }
        
        AuxDependency child  = null;
        AuxDependency parent = null;
        
        if (part != null && part.equals("")) part = null;
        if (theme != null && theme.equals("")) theme = null;
        CAT.info("Adding Dependency of type 'text' to Target '" + target.getFullName() + "':");
        CAT.info("*** [" + path.toURI().getPath().substring(1) + "][" + part + "][" + theme + "][" +
                 ((parent_path == null)? "null" : parent_path.toURI().getPath().substring(1)) + "][" + parent_part + "][" + parent_theme + "]");

        child = AuxDependencyFactory.getInstance().getAuxDependencyInclude(path, part, theme);
        parent = getParentDependency(parent_path, parent_part, parent_theme);
        
        TargetDependencyRelation.getInstance().addRelation(parent, child, target);
    }
    
    public synchronized void addDependencyImage(FileResource path, FileResource parent_path, String parent_part, String parent_theme) {
        if (path == null) {
            throw new IllegalArgumentException("Null pointer is not allowed here");
        }
        
        AuxDependency child  = null;
        AuxDependency parent = null;

        CAT.info("Adding Dependency of type 'text' to Target '" + target.getFullName() + "':");
        CAT.info("*** [" + path.toURI().getPath().substring(1) + "][" +
                 ((parent_path == null)? "null" : parent_path.toURI().getPath().substring(1)) + "][" + parent_part + "][" + parent_theme + "]");

        child = AuxDependencyFactory.getInstance().getAuxDependencyImage(path);
        parent = getParentDependency(parent_path, parent_part, parent_theme);
        
        TargetDependencyRelation.getInstance().addRelation(parent, child, target);
    }
    
    public synchronized void addDependencyFile(FileResource path) {
        if (path == null) {
            throw new IllegalArgumentException("Null pointer is not allowed here");
        }
        
        AuxDependency child  = null;

        CAT.info("Adding Dependency of type 'text' to Target '" + target.getFullName() + "':");
        CAT.info("*** [" + path.toURI().getPath().substring(1) + "]");

        child = AuxDependencyFactory.getInstance().getAuxDependencyFile(path);
        
        TargetDependencyRelation.getInstance().addRelation(root, child, target);
    }
    
    public synchronized void addDependencyTarget(String targetkey) {
        if (target == null) {
            throw new IllegalArgumentException("Null pointer is not allowed here");
        }
        
        AuxDependency child  = null;

        CAT.info("Adding Dependency of type 'text' to Target '" + target.getFullName() + "':");
        CAT.info("*** [" + target.getTargetKey() + "]");

        child = AuxDependencyFactory.getInstance().getAuxDependencyTarget(target.getTargetGenerator(), targetkey);
        
        TargetDependencyRelation.getInstance().addRelation(root, child, target);
    }

    public synchronized void reset() {
        TargetDependencyRelation.getInstance().resetRelation(target);
    }

    /**
     * Returns the highest (= newest) timestamp of all aux dependencies
     * (include parts, images, files) managed through this manager.
     * This does NOT include any aux targets.
     * 
     * @return Timestamp of latest change in any dependency
     */
    public long getMaxTimestamp() {
        Set<AuxDependency> allaux = TargetDependencyRelation.getInstance().getDependenciesForTarget(target);
        long               max    = 0;
        
        if (allaux != null) {
            for (Iterator<AuxDependency> i = allaux.iterator(); i.hasNext();) {
                AuxDependency aux  = i.next();
                if (aux.getType() != DependencyType.TARGET) {
                    max = Math.max(max, aux.getModTime());
                }
            }
        }
        return max;
    }
    

    public synchronized void saveAuxdepend() throws IOException  {
        CAT.info("===> Trying to save aux info of Target '" + target.getTargetKey() + "'");

        Set<AuxDependency> allaux = TargetDependencyRelation.getInstance().getDependenciesForTarget(target);
        FileResource       path   = ResourceUtil.getFileResource(target.getTargetGenerator().getDisccachedir(),
                                                                 target.getTargetKey() + ".aux");
        FileResource       dir    = path.getParentAsFileResource();
        
        // Make sure parent directory is existing (for leaf targets)
        if (dir != null) {
            dir.mkdirs();
        }

        HashMap<AuxDependency, HashSet<AuxDependency>> parentchild = 
            TargetDependencyRelation.getInstance().getParentChildMapForTarget(target);
        
        Document auxdoc   = Xml.createDocument();
        Element  rootelem = auxdoc.createElement("aux");
        
        auxdoc.appendChild(rootelem);

        if (parentchild != null) {
            for (Iterator<AuxDependency> i = parentchild.keySet().iterator(); i.hasNext(); ) {
                AuxDependency parent       = i.next();
                FileResource  parent_path  = null;
                String        parent_part  = null;
                String        parent_theme = null;
                
                if (parent != root) {
                    AuxDependencyInclude aux = (AuxDependencyInclude) parent;
                    parent_path  = aux.getPath();
                    parent_part  = aux.getPart();
                    parent_theme = aux.getTheme();
                }
                
                HashSet<AuxDependency> children = parentchild.get(parent);
                
                for (Iterator<AuxDependency> j = children.iterator(); j.hasNext(); ) {
                    AuxDependency  aux  = j.next();
                    DependencyType type = aux.getType();
                    
                    if (type.isDynamic()) {
                        Element depaux = auxdoc.createElement(DEPAUX);
                        rootelem.appendChild(depaux);
                        depaux.setAttribute("type", type.getTag());
                        if (aux.getType() == DependencyType.TEXT) {
                            AuxDependencyInclude a = (AuxDependencyInclude) aux;
                            depaux.setAttribute("path", a.getPath().toURI().getPath().substring(1));
                            depaux.setAttribute("part", a.getPart());
                            depaux.setAttribute("product", a.getTheme());
                            if (parent_path != null) 
                                depaux.setAttribute("parent_path", parent_path.toURI().getPath().substring(1));
                            if (parent_part != null) 
                                depaux.setAttribute("parent_part", parent_part);
                            if (parent_theme != null) 
                                depaux.setAttribute("parent_product", parent_theme);
                        } else if (aux.getType() == DependencyType.IMAGE) {
                            AuxDependencyImage a = (AuxDependencyImage) aux;
                            depaux.setAttribute("path", a.getPath().toURI().getPath().substring(1));
                            if (parent_path != null) 
                                depaux.setAttribute("parent_path", parent_path.toURI().getPath().substring(1));
                            if (parent_part != null) 
                                depaux.setAttribute("parent_part", parent_part);
                            if (parent_theme != null) 
                                depaux.setAttribute("parent_product", parent_theme);
                        } else if (aux.getType() == DependencyType.TARGET) {
                            Target target = ((AuxDependencyTarget) aux).getTarget();
                            depaux.setAttribute("target", target.getTargetKey());
                        }
                    }
                }
            }
        }
        Xml.serialize(auxdoc, path, true, true);
    }

    public TreeSet<AuxDependency> getChildren() {
        HashMap<AuxDependency, HashSet<AuxDependency>> parentchild = 
            TargetDependencyRelation.getInstance().getParentChildMapForTarget(target);

        TreeSet<AuxDependency> retval = new TreeSet<AuxDependency>();
        
        if (parentchild != null && parentchild.get(root) != null) {
            retval.addAll(parentchild.get(root));
        }
        
        return retval;
    }
        
}// AuxDependencyManager
