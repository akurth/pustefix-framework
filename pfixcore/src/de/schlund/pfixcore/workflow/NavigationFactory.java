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

package de.schlund.pfixcore.workflow;

import de.schlund.pfixxml.PathFactory;
import de.schlund.pfixxml.util.Path;
import java.io.*;
import java.util.*;
import org.apache.log4j.*;


/**
 * @author jtl
 *
 */

public class NavigationFactory {
    private static Logger            LOG      = Logger.getLogger(NavigationFactory.class);
    private static HashMap           navis    = new HashMap();
    private static NavigationFactory instance = new NavigationFactory();
    
    public static NavigationFactory getInstance() {
        return instance;
    }
            
    public synchronized Navigation getNavigation(String navifilename) throws Exception {
        Navigation navi = null;
        Path       navipath = PathFactory.getInstance().createPath(navifilename);
        File       navifile = navipath.resolve();
        
        navi = (Navigation) navis.get(navifilename);
        
        if (navi == null || navi.needsReload()) {
            LOG.warn("***** Creating Navigation object *******");
            navi     = new Navigation(navifile);
            navis.put(navifilename, navi);
        }
        
        return navi;
    }
}
