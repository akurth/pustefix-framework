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

package de.schlund.pfixxml.targets;

import java.util.Iterator;
import java.util.TreeSet;

import de.schlund.pfixxml.resources.FileResource;

/**
 * Dependency referencing a static file on the filesystem
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public class AuxDependencyFile extends AbstractAuxDependency {
    private FileResource path;

    private long last_lastModTime = -1;
    
    protected int hashCode;
    
    public AuxDependencyFile(FileResource path) {
        this.type = DependencyType.FILE;
        this.path = path;
        this.hashCode = (type.getTag() + ":" + path.toString()).hashCode();
    }
    
    /**
     * Returns path to the file containing the referenced include part
     * 
     * @return path to the include file
     */
    public FileResource getPath() {
        return path;
    }
    
    public long getModTime() {
        if (path.exists() && path.canRead() && path.isFile()) {
            if (last_lastModTime == 0) {
                // We change from the file being checked once to not exist to "it exists now".
                // so we need to make sure that all targets using it will be rebuild.
                TreeSet targets = TargetDependencyRelation.getInstance()
                        .getAffectedTargets(this);
                for (Iterator i = targets.iterator(); i.hasNext();) {
                    VirtualTarget target = (VirtualTarget) i.next();
                    target.setForceUpdate();
                }
            }
            last_lastModTime = path.lastModified();
            return last_lastModTime;
        } else {
            if (last_lastModTime > 0) {
                // The file existed when last check has been made,
                // so make sure each target using it is being rebuild
                TreeSet targets = TargetDependencyRelation.getInstance()
                        .getAffectedTargets(this);
                for (Iterator i = targets.iterator(); i.hasNext();) {
                    VirtualTarget target = (VirtualTarget) i.next();
                    target.setForceUpdate();
                }
            }
            last_lastModTime = 0;
            return 0;
        }
    }

    public boolean equals(Object obj) {
        if (obj instanceof AuxDependencyFile) {
            return (this.compareTo((AuxDependency) obj) == 0);
        } else {
            return false;
        }
    }

    public int compareTo(AuxDependency o) {
        int comp;
        
        comp = super.compareTo(o);
        if (comp != 0) {
            return comp;
        }
        
        AuxDependencyFile a = (AuxDependencyFile) o;
        return path.compareTo(a.path);
    }

    public int hashCode() {
        return this.hashCode;
    }

    public String toString() {
        return "[AUX/" + getType() + " " + getPath().toURI().getPath().substring(1) + "]";
    }

}
