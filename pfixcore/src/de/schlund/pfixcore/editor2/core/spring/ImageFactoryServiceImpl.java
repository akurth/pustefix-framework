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

import java.util.HashMap;
import de.schlund.pfixcore.editor2.core.dom.Image;
import de.schlund.pfixcore.editor2.core.spring.internal.ImageImpl;

/**
 * Implementation of ImageFactoryService
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public class ImageFactoryServiceImpl implements ImageFactoryService {

    private VariantFactoryService variantfactory;

    private ProjectFactoryService projectfactory;

    private HashMap cache;

    private PathResolverService pathresolver;

    private FileSystemService filesystem;

    private SecurityManagerService securitymanager;

    public void setFileSystemService(FileSystemService filesystem) {
        this.filesystem = filesystem;
    }

    public void setPathResolverService(PathResolverService pathresolver) {
        this.pathresolver = pathresolver;
    }

    public void setSecurityManagerService(SecurityManagerService securitymanager) {
        this.securitymanager = securitymanager;
    }

    public void setVariantFactoryService(VariantFactoryService variantfactory) {
        this.variantfactory = variantfactory;
    }

    public void setProjectFactoryService(ProjectFactoryService projectfactory) {
        this.projectfactory = projectfactory;
    }

    public ImageFactoryServiceImpl() {
        this.cache = new HashMap();
    }

    public Image getImage(String path) {
        synchronized (this.cache) {
            if (!this.cache.containsKey(path)) {
                Image image = new ImageImpl(this.variantfactory,
                        this.projectfactory, this.pathresolver,
                        this.filesystem, this.securitymanager, path);
                this.cache.put(path, image);
            }
            return (Image) this.cache.get(path);
        }

    }

}
