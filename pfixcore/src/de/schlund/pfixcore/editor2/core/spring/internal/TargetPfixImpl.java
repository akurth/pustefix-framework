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

package de.schlund.pfixcore.editor2.core.spring.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import de.schlund.pfixcore.editor2.core.dom.AbstractTarget;
import de.schlund.pfixcore.editor2.core.dom.AbstractTheme;
import de.schlund.pfixcore.editor2.core.dom.Image;
import de.schlund.pfixcore.editor2.core.dom.IncludePartThemeVariant;
import de.schlund.pfixcore.editor2.core.dom.Page;
import de.schlund.pfixcore.editor2.core.dom.Project;
import de.schlund.pfixcore.editor2.core.dom.Target;
import de.schlund.pfixcore.editor2.core.dom.TargetType;
import de.schlund.pfixcore.editor2.core.dom.Theme;
import de.schlund.pfixcore.editor2.core.dom.ThemeList;
import de.schlund.pfixcore.editor2.core.dom.Variant;
import de.schlund.pfixcore.editor2.core.exception.EditorIOException;
import de.schlund.pfixcore.editor2.core.exception.EditorParsingException;
import de.schlund.pfixcore.editor2.core.spring.FileSystemService;
import de.schlund.pfixcore.editor2.core.spring.ImageFactoryService;
import de.schlund.pfixcore.editor2.core.spring.IncludeFactoryService;
import de.schlund.pfixcore.editor2.core.spring.PathResolverService;
import de.schlund.pfixcore.editor2.core.spring.ProjectFactoryService;
import de.schlund.pfixcore.editor2.core.spring.TargetFactoryService;
import de.schlund.pfixcore.editor2.core.spring.ThemeFactoryService;
import de.schlund.pfixcore.editor2.core.spring.VariantFactoryService;
import de.schlund.pfixxml.targets.AuxDependency;
import de.schlund.pfixxml.targets.AuxDependencyManager;
import de.schlund.pfixxml.targets.DependencyType;
import de.schlund.pfixxml.targets.PageInfo;
import de.schlund.pfixxml.targets.TargetGenerationException;
import de.schlund.pfixxml.targets.VirtualTarget;

/**
 * Implementation of Target using the Target object provided by the Pustefix
 * generator.
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public class TargetPfixImpl extends AbstractTarget {

    private de.schlund.pfixxml.targets.Target pfixTarget;

    private Project project;

    private TargetFactoryService targetfactory;

    private ProjectFactoryService projectfactory;

    private VariantFactoryService variantfactory;

    private IncludeFactoryService includefactory;

    private ThemeFactoryService themefactory;

    private ImageFactoryService imagefactory;

    private PathResolverService pathresolver;

    private FileSystemService filesystem;

    public TargetPfixImpl(TargetFactoryService targetfactory,
            ProjectFactoryService projectfactory,
            VariantFactoryService variantfactory,
            IncludeFactoryService includefactory,
            ThemeFactoryService themefactory, ImageFactoryService imagefactory,
            PathResolverService pathresolver, FileSystemService filesystem,
            de.schlund.pfixxml.targets.Target pfixTarget, Project project) {
        this.pfixTarget = pfixTarget;
        this.project = project;
        this.targetfactory = targetfactory;
        this.projectfactory = projectfactory;
        this.variantfactory = variantfactory;
        this.includefactory = includefactory;
        this.themefactory = themefactory;
        this.imagefactory = imagefactory;
        this.pathresolver = pathresolver;
        this.filesystem = filesystem;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.schlund.pfixcore.editor2.core.dom.Target#getName()
     */
    public String getName() {
        // TODO Check whether getTargetKey() really returns "name"
        return this.pfixTarget.getTargetKey();
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.schlund.pfixcore.editor2.core.dom.Target#getType()
     */
    public TargetType getType() {
        if (this.pfixTarget.getType() == de.schlund.pfixxml.targets.TargetType.XML_LEAF
                || this.pfixTarget.getType() == de.schlund.pfixxml.targets.TargetType.XML_VIRTUAL) {
            return TargetType.TARGET_XML;
        } else if (this.pfixTarget.getType() == de.schlund.pfixxml.targets.TargetType.XSL_LEAF
                || this.pfixTarget.getType() == de.schlund.pfixxml.targets.TargetType.XSL_VIRTUAL) {
            return TargetType.TARGET_XSL;
        }
        // FIXME Do not return null but throw an exception
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.schlund.pfixcore.editor2.core.dom.Target#getContentXML()
     */
    public Document getContentXML() throws EditorIOException,
            EditorParsingException {
        File file;
        if (this.pfixTarget.getType() == de.schlund.pfixxml.targets.TargetType.XML_LEAF
                || this.pfixTarget.getType() == de.schlund.pfixxml.targets.TargetType.XSL_LEAF) {
            file = new File(this.pathresolver.resolve(this.pfixTarget
                    .getTargetKey()));
        } else {
            // Make sure file is existing
            try {
                this.pfixTarget.getValue();
            } catch (TargetGenerationException e) {
                String msg = "Could not generate target " + this.getName()
                        + "!";
                Logger.getLogger(this.getClass()).warn(msg);
            }
            file = new File(this.pathresolver.resolve(this.pfixTarget
                    .getTargetGenerator().getDisccachedir().getRelative()
                    + "/" + this.pfixTarget.getTargetKey()));
        }
        Object lock = this.filesystem.getLock(file);
        synchronized (lock) {
            try {
                return this.filesystem.readXMLDocumentFromFile(file);
            } catch (FileNotFoundException e) {
                String err = "File " + file.getAbsolutePath()
                        + " could not be found!";
                Logger.getLogger(this.getClass()).error(err, e);
                throw new EditorIOException(err, e);
            } catch (SAXException e) {
                String err = "Error during parsing file "
                        + file.getAbsolutePath() + "!";
                Logger.getLogger(this.getClass()).error(err, e);
                throw new EditorParsingException(err, e);
            } catch (IOException e) {
                String err = "File " + file.getAbsolutePath()
                        + " could not be read!";
                Logger.getLogger(this.getClass()).error(err, e);
                throw new EditorIOException(err, e);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.schlund.pfixcore.editor2.core.dom.Target#getParentXML()
     */
    public Target getParentXML() {
        if (this.pfixTarget.getXMLSource() == null) {
            return null;
        }
        return this.targetfactory.getTargetFromPustefixTarget(this.pfixTarget
                .getXMLSource(), this.getProject());
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.schlund.pfixcore.editor2.core.dom.Target#getParentXSL()
     */
    public Target getParentXSL() {
        if (this.pfixTarget.getXSLSource() == null) {
            return null;
        }
        return this.targetfactory.getTargetFromPustefixTarget(this.pfixTarget
                .getXSLSource(), this.getProject());
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.schlund.pfixcore.editor2.core.dom.Target#getAuxDependencies(boolean)
     */
    public Collection getAuxDependencies(boolean recursive) {
        ArrayList deps = new ArrayList();
        if (this.pfixTarget instanceof VirtualTarget) {
            VirtualTarget vtarget = (VirtualTarget) this.pfixTarget;
            AuxDependencyManager auxmanager = vtarget.getAuxDependencyManager();
            if (auxmanager == null) {
                return new ArrayList();
            }

            for (Iterator i = auxmanager.getChildren().iterator(); i.hasNext();) {
                AuxDependency auxdep = (AuxDependency) i.next();
                if (auxdep.getType() == DependencyType.FILE) {
                    deps.add(this.targetfactory
                            .getLeafTargetFromPustefixAuxDependency(auxdep));
                }
            }

            if (recursive) {
                if (this.getParentXML() != null) {
                    deps.addAll(this.getParentXML().getAuxDependencies(true));
                }
                if (this.getParentXSL() != null) {
                    deps.addAll(this.getParentXSL().getAuxDependencies(true));
                }
            }

            return deps;
        } else {
            return new ArrayList();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.schlund.pfixcore.editor2.core.dom.Target#getIncludeDependencies(boolean)
     */
    public Collection getIncludeDependencies(boolean recursive)
            throws EditorParsingException {
        ArrayList deps = new ArrayList();
        if (this.pfixTarget instanceof VirtualTarget) {
            VirtualTarget vtarget = (VirtualTarget) this.pfixTarget;
            ThemeList themes = new ThemeListImpl(this.themefactory, vtarget
                    .getThemes());
            AuxDependencyManager auxmanager = vtarget.getAuxDependencyManager();
            if (auxmanager == null) {
                String msg = "Could not get AuxDependencyManager for target "
                        + this.getName() + "!";
                Logger.getLogger(this.getClass()).warn(msg);
                return new ArrayList();
            }

            for (Iterator i = auxmanager.getChildren().iterator(); i.hasNext();) {
                AuxDependency auxdep = (AuxDependency) i.next();
                if (auxdep.getType() == DependencyType.TEXT) {
                    IncludePartThemeVariant variant = this.includefactory
                            .getIncludeFile(auxdep.getPath().getRelative())
                            .createPart(auxdep.getPart()).createThemeVariant(
                                    this.themefactory.getTheme(auxdep
                                            .getProduct()));
                    deps.add(variant);
                    if (recursive) {
                        deps.addAll(variant
                                .getIncludeDependencies(themes, true));
                    }
                }
            }

            if (recursive) {
                if (this.getParentXML() != null) {
                    deps.addAll(this.getParentXML()
                            .getIncludeDependencies(true));
                }
                if (this.getParentXSL() != null) {
                    deps.addAll(this.getParentXSL()
                            .getIncludeDependencies(true));
                }
            }

            return deps;
        } else {
            String msg = "Page target " + this.getName()
                    + " is no VirtualTarget!";
            Logger.getLogger(this.getClass()).warn(msg);
            return new ArrayList();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.schlund.pfixcore.editor2.core.dom.Target#getImageDependencies(boolean)
     */
    public Collection getImageDependencies(boolean recursive)
            throws EditorParsingException {
        ArrayList deps = new ArrayList();
        if (this.pfixTarget instanceof VirtualTarget) {
            VirtualTarget vtarget = (VirtualTarget) this.pfixTarget;
            ThemeList themes = new ThemeListImpl(this.themefactory, vtarget
                    .getThemes());
            AuxDependencyManager auxmanager = vtarget.getAuxDependencyManager();
            if (auxmanager == null) {
                return new ArrayList();
            }

            for (Iterator i = auxmanager.getChildren().iterator(); i.hasNext();) {
                AuxDependency auxdep = (AuxDependency) i.next();
                if ((auxdep.getType() == DependencyType.TEXT) && recursive) {
                    IncludePartThemeVariant variant = this.includefactory
                            .getIncludeFile(auxdep.getPath().getRelative())
                            .createPart(auxdep.getPart()).createThemeVariant(
                                    this.themefactory.getTheme(auxdep
                                            .getProduct()));

                    deps.addAll(variant.getImageDependencies(themes, true));
                } else if (auxdep.getType() == DependencyType.IMAGE) {
                    Image dep = this.imagefactory.getImage(auxdep.getPath()
                            .getRelative());
                    deps.add(dep);
                }
            }

            if (recursive) {
                if (this.getParentXML() != null) {
                    deps.addAll(this.getParentXML().getImageDependencies(true));
                }
                if (this.getParentXSL() != null) {
                    deps.addAll(this.getParentXSL().getImageDependencies(true));
                }
            }

            return deps;
        } else {
            return new ArrayList();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.schlund.pfixcore.editor2.core.dom.Target#getAffectedPages()
     */
    public Collection getAffectedPages() {
        Collection pageinfos = this.pfixTarget.getPageInfos();
        HashSet pages = new HashSet();
        for (Iterator i2 = pageinfos.iterator(); i2.hasNext();) {
            PageInfo pageinfo = (PageInfo) i2.next();
            if (pageinfo == null) {
                continue;
            }
            Project project = projectfactory
                    .getProjectByPustefixTargetGenerator(pageinfo
                            .getTargetGenerator());
            if (project == null) {
                String msg = "Project for generator "
                        + pageinfo.getTargetGenerator().getName()
                        + " not found, omitting affected page!";
                Logger.getLogger(this.getClass()).warn(msg);
                continue;
            }
            Variant variant = null;
            if (pageinfo.getVariant() != null) {
                variant = this.variantfactory.getVariant(pageinfo.getVariant());
            }
            Page page = project.getPage(pageinfo.getName(), variant);
            if (page == null) {
                String msg = "Could not get page " + pageinfo.getName()
                        + " with variant " + variant.getName()
                        + " from project " + project.getName()
                        + "! Omitting page!";
                Logger.getLogger(this.getClass()).warn(msg);
                continue;
            }
            pages.add(page);
        }
        return pages;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.schlund.pfixcore.editor2.core.dom.Target#getProject()
     */
    public Project getProject() {
        return this.project;
    }

    public Map getTransformationParameters() {
        return this.pfixTarget.getParams();
    }

    public Map getParameters() {
        Map map = this.pfixTarget.getParams();
        if (map != null)
            return map;
        else
            return new HashMap();
    }

    public ThemeList getThemeList() {
        if (this.pfixTarget instanceof VirtualTarget) {
            VirtualTarget vtarget = (VirtualTarget) this.pfixTarget;
            ThemeList themes = new ThemeListImpl(this.themefactory, vtarget
                    .getThemes());
            return themes;
        } else {
            return new ThemeList() {

                public Collection getThemes() {
                    ArrayList list = new ArrayList();
                    list.add(new AbstractTheme("default") {
                    });
                    return list;
                }

                public boolean includesTheme(Theme theme) {
                    if (theme.getName().equals("default")) {
                        return true;
                    } else {
                        return false;
                    }
                }

            };
        }
    }

}
