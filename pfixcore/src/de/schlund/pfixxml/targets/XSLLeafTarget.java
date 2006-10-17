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



import de.schlund.pfixxml.PathFactory;
import de.schlund.pfixxml.util.*;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;

/**
 * XSLLeafTarget.java
 *
 *
 * Created: Mon Jul 23 21:53:06 2001
 *
 * @author <a href="mailto:jtl@schlund.de">Jens Lautenbacher</a>
 *
 *
 */

public class XSLLeafTarget extends LeafTarget {

    public XSLLeafTarget(TargetType type, TargetGenerator gen, String key, Themes themes) throws Exception {
        this.type      = type;
        this.generator = gen;
        this.targetkey = key;
        this.themes    = themes;
        Path targetpath = PathFactory.getInstance().createPath(key);
        this.sharedleaf = SharedLeafFactory.getInstance().getSharedLeaf(targetpath.resolve().getPath());
        this.auxdepmanager = new AuxDependencyManager(this);
        this.auxdepmanager.tryInitAuxdepend();
    }

    /**
     * @see de.schlund.pfixxml.targets.TargetImpl#getValueFromDiscCache()
     */
    protected Object getValueFromDiscCache() throws TransformerException {
        Path thepath = PathFactory.getInstance().createPath(getTargetKey());
        File thefile = thepath.resolve();
        if (thefile.exists() && thefile.isFile()) {
            // reset the target dependency list as they will be set up again
            this.getAuxDependencyManager().reset();
            
            Templates tmpl = Xslt.loadTemplates(thepath, this);
            
            // save aux dependencies
            try {

                this.getAuxDependencyManager().saveAuxdepend();
            } catch (IOException e) {
                throw new TransformerException("Error while writing auxdependency information");
            }
            
            return tmpl;
        } else {
            return null;
        }
    }

    public Document getDOM() throws TargetGenerationException {
        File thefile = PathFactory.getInstance().createPath(getTargetKey()).resolve();
        if (thefile.exists() && thefile.isFile()) {
            try {
                return Xml.parse(thefile);
            } catch (TransformerException e) {
                throw new TargetGenerationException("Error while reading DOM from disccache for target "
                                                    + getTargetKey(), e);
            }
        } else {
            return null;
        }
    }
    
}// XSLLeafTarget
