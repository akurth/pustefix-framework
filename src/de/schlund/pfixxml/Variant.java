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

package de.schlund.pfixxml;

import java.util.StringTokenizer;
import java.util.ArrayList;

/**
 * Describe class Variant here.
 *
 *
 * Created: Sun Apr 10 17:41:28 2005
 *
 * @author <a href="mailto:jtl@schlund.de">Jens Lautenbacher</a>
 * @version 1.0
 */
public class Variant {

    String variant;
    String[] variant_arr;
        
    public Variant(String var) {
        variant = var;
        if (variant == null || variant.equals("")) {
            variant_arr = null;
        } else {
            StringTokenizer tokenizer = new StringTokenizer(variant, ":");
            ArrayList       arrlist   = new ArrayList();
            String          fallback  = "";
            while (tokenizer.hasMoreElements()) {
                String tok = tokenizer.nextToken();
                if (!fallback.equals("")) {
                    fallback += ":";
                }
                fallback += tok;
                arrlist.add(0,fallback);
            }
            variant_arr = (String[]) arrlist.toArray(new String[]{});
        }
    }

    public String getVariantId() {
        return variant;
    }

    public String[] getVariantFallbackArray() {
        return variant_arr;
    }
}
