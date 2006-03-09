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

package de.schlund.pfixxml.config;

import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;

public class SSLRule extends CheckedRule {

    public void begin(String namespace, String name, Attributes attributes) throws Exception {
        check(namespace, name, attributes);
        String force = attributes.getValue("force");
        if (force == null) {
            throw new Exception("Attribute \"force\" is mandatory!");
        }
        Object obj = this.getDigester().peek();
        if (obj instanceof SSLOption) {
            SSLOption opt = (SSLOption) obj;
            opt.setSSL(Boolean.parseBoolean(force));
        } else {
            throw new RuntimeException("Object on stack does not implement SSLOption!");
        }
    }

    protected Map<String, Boolean> wantsAttributes() {
        HashMap<String, Boolean> atts = new HashMap<String, Boolean>();
        atts.put("force", true);
        return atts;
    }
}
