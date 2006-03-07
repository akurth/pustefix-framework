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

import org.w3c.dom.Element;

import de.schlund.pfixcore.editor2.core.dom.IncludePart;
import de.schlund.pfixcore.editor2.core.spring.BackupService;
import de.schlund.pfixcore.editor2.core.spring.FileSystemService;
import de.schlund.pfixcore.editor2.core.spring.IncludeFactoryService;
import de.schlund.pfixcore.editor2.core.spring.PathResolverService;
import de.schlund.pfixcore.editor2.core.spring.SecurityManagerService;
import de.schlund.pfixcore.editor2.core.spring.ThemeFactoryService;

/**
 * Implementation of IncludeFile using a Pustefix IncludeDocument as backend
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public class IncludeFileImpl extends CommonIncludeFileImpl {
    private ThemeFactoryService themefactory;

    private IncludeFactoryService includefactory;

    private FileSystemService filesystem;

    private PathResolverService pathresolver;

    private BackupService backup;

    private SecurityManagerService securitymanager;

    public IncludeFileImpl(ThemeFactoryService themefactory,
            IncludeFactoryService includefactory, FileSystemService filesystem,
            PathResolverService pathresolver, BackupService backup,
            SecurityManagerService securitymanager, String path) {
        super(filesystem, pathresolver, path);
        this.themefactory = themefactory;
        this.includefactory = includefactory;
        this.filesystem = filesystem;
        this.pathresolver = pathresolver;
        this.backup = backup;
        this.securitymanager = securitymanager;
    }

    protected IncludePart createIncludePartInstance(String name, Element el, long serial) {
        return new IncludePartImpl(themefactory, includefactory, filesystem,
                pathresolver, backup, securitymanager, name, this, el, serial);
    }

}
