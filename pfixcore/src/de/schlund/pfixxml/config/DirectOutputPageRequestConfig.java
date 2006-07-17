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

import java.util.Enumeration;
import java.util.Properties;


/**
 * Stores configuration for a DirectOutputServlet PageRequest
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public class DirectOutputPageRequestConfig {

    private String pageName = null;

    private Class stateClass = null;
    
    private Properties properties = new Properties();

    public void setPageName(String page) {
        this.pageName = page;
    }

    public String getPageName() {
        return this.pageName;
    }

    public void setState(Class clazz) {
        this.stateClass = clazz;
    }

    public Class getState() {
        return this.stateClass;
    }
    
    public void setProperties(Properties props) {
        this.properties = new Properties();
        Enumeration e = props.propertyNames();
        while (e.hasMoreElements()) {
            String propname = (String) e.nextElement();
            this.properties.setProperty(propname, props.getProperty(propname));
        }
    }
    
    public Properties getProperties() {
        return this.properties;
    }
}
