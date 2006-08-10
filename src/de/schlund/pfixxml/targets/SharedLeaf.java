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
import java.util.TreeSet;

import de.schlund.pfixxml.resources.FileResource;

/**
 * SharedLeaf.java
 *
 *
 * Created: Tue Jul 24 02:18:20 2001
 *
 * @author <a href="mailto: "Jens Lautenbacher</a>
 *
 *
 */

public class SharedLeaf implements Comparable {
    private FileResource path;
    private TreeSet pageinfos = new TreeSet();
    private long    modtime   = 0;
    
    protected SharedLeaf(FileResource path) {
        this.path = path;
    }

    public FileResource getPath() { return path; }
    
    public void addPageInfo(PageInfo info) {
        synchronized (pageinfos) {
            pageinfos.add(info);
        }
    }
    
    public TreeSet getPageInfos() {
        synchronized (pageinfos) {
            return (TreeSet) pageinfos.clone();
        }
    }
    
    public void setModTime(long mtime) {
        modtime = mtime;
    }

    public long getModTime() {
        if (modtime == 0) {
            if (path.exists() && path.isFile()) {
                setModTime(path.lastModified());
            }
        }
        return modtime;
    }

   
    // comparable interface
    
    public int compareTo(Object inobj) {
        SharedLeaf in = (SharedLeaf) inobj;
        return path.compareTo(in.path);
    }

}// SharedLeaf
