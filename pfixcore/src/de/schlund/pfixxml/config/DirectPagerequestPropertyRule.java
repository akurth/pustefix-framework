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
import org.xml.sax.SAXException;

public class DirectPagerequestPropertyRule extends CheckedRule {

    private DirectOutputServletConfig config;
    private String propName;
    private String propValue;

    public DirectPagerequestPropertyRule(DirectOutputServletConfig config) {
        this.config = config;
    }

    public void begin(String namespace, String name, Attributes attributes) throws Exception {
        check(namespace, name, attributes);
        String propName = attributes.getValue("name");
        if (propName == null) {
            throw new SAXException("Mandatory attribute \"name\" is missing!");
        }
        this.propName = propName;
        this.propValue = "";
    }

    public void end(String namespace, String name) throws Exception {
        DirectOutputPageRequestConfig reqConfig = (DirectOutputPageRequestConfig) this.getDigester().peek();
        reqConfig.getProperties().setProperty(propName, XMLPropertiesUtil.unesacpePropertyValue(propValue.trim()));
    }

    public void body(String namespace, String name, String text) throws Exception {
        this.propValue += text;
    }
    
    protected Map<String, Boolean> wantsAttributes() {
        HashMap<String, Boolean> atts = new HashMap<String, Boolean>();
        atts.put("name", true);
        return atts;
    }
}
