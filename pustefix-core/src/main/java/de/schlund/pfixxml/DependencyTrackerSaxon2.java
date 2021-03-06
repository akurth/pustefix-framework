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

package de.schlund.pfixxml;

import net.sf.saxon.expr.XPathContext;
import de.schlund.pfixxml.resources.Resource;
import de.schlund.pfixxml.targets.TargetGenerator;
import de.schlund.pfixxml.targets.VirtualTarget;
import de.schlund.pfixxml.util.XsltContext;
import de.schlund.pfixxml.util.xsltimpl.XsltContextSaxon2;

/**
 * @author mleidig@schlund.de
 */
public class DependencyTrackerSaxon2 {

    public static String logImage(XPathContext context,Resource path,String parent_part_in,String parent_theme_in,
            TargetGenerator targetGen,String targetKey,String type) throws Exception {
        XsltContext xsltContext=new XsltContextSaxon2(context);
        return DependencyTracker.logImage(xsltContext,path,parent_part_in,parent_theme_in,targetGen,targetKey,type);
    }
    
    public static void logTyped(String type,Resource path,String part,String theme,
            Resource parent_path,String parent_part,String parent_theme,VirtualTarget target) {
        DependencyTracker.logTyped(type,path,part,theme,parent_path,parent_part,parent_theme,target);
    }
    
}
