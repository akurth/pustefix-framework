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


import java.util.HashMap;

import org.apache.log4j.Logger;

import de.schlund.pfixxml.ConfigurableObject;
import de.schlund.pfixxml.Variant;
import de.schlund.pfixxml.config.ContextConfig;

/**
 * VariantManager.java
 *
 *
 * Created: Sun Oct  7 13:28:11 2001
 *
 * @author <a href="mailto:jtl@schlund.de">Jens Lautenbacher</a>
 *
 */

public class VariantManager implements ConfigurableObject {
    private ContextConfig           contextConfig;
    private HashMap<String, String> variantpagecache = new HashMap<String, String>();
    private Logger                  LOG              = Logger.getLogger(this.getClass());
    
    public void init(Object confObj) throws Exception {
        contextConfig = (ContextConfig) confObj;
    }

    public String getVariantMatchingPageRequestName(String name, Variant variant) {
        String variant_id = variant.getVariantId();
        String fullname   = (String) variantpagecache.get(variant_id + "@" + name);
        
        if (fullname == null) {
            synchronized(variantpagecache) {
                fullname = (String) variantpagecache.get(variant_id + "@" + name);
                if (fullname == null) {
                    // CAT.debug("------ Cache miss " + variant_id + "@" + name);
                    String[] variant_arr = variant.getVariantFallbackArray();
                    for (int i = 0; i < variant_arr.length; i++) {
                        String tmp = name + "::" + variant_arr[i];
                        if (contextConfig.getPageRequestConfig(tmp) != null) {
                            LOG.debug("=== Found PR for '" + tmp + "' ===");
                            fullname = tmp;
                            break;
                        } else {
                            LOG.debug("=== PR NOT FOUND for '" + tmp + "' ===");
                        }
                    }
                    if (fullname == null) {
                        fullname = name;
                    }
                    variantpagecache.put(variant_id + "@" + name, fullname);
                }
            } // Yes, DCL doesn't work, but if we hit the race, it doesn't matter here.
        }
        return fullname;
    }
    
} // VariantManager (was: PageRequestProperties)
