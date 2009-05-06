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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

/**
 * Stores configuration for a PageRequest
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public class PageRequestConfig implements SSLOption, Cloneable {
    
    public enum Policy {
        ANY, ALL, NONE
    }

    private String pageName = null;
    private String copyFromPage = null;
    private boolean storeXML = true;
    private boolean ssl = false;
    private Class stateClass = null;
    private Class defaultStaticStateClass = null;
    private Class defaultIWrapperStateClass = null;
    private Class finalizer = null;
    private String authPrefix = null;
    private Class authClass = null;
    private LinkedHashMap<String, Class> auxwrappers = new LinkedHashMap<String, Class>();
    private LinkedHashMap<String, IWrapperConfig> iwrappers = new LinkedHashMap<String, IWrapperConfig>();
    private LinkedHashMap<String, Class> resources = new LinkedHashMap<String, Class>();
    private Properties props = new Properties();
    private Policy policy = Policy.ANY;
    
    public void setPageName(String page) {
        this.pageName = page;
    }
    
    public String getPageName() {
        return this.pageName;
    }
    
    public void setStoreXML(boolean store) {
        this.storeXML = store;
    }
    
    public boolean isStoreXML() {
        return this.storeXML;
    }
    
    public void setSSL(boolean forceSSL) {
        this.ssl = forceSSL;
    }
    
    public boolean isSSL() {
        return this.ssl;
    }
    
    public void setState(Class clazz) {
        this.stateClass = clazz;
    }
    
    public Class getState() {
        if (this.stateClass != null) {
            return this.stateClass;
        } else {
            if (this.iwrappers.size() > 0) {
                return this.defaultIWrapperStateClass;
            } else {
                return this.defaultStaticStateClass;
            }
        }
    }
    
    public void setCopyFromPage(String page) {
        this.copyFromPage = page;
    }
    
    public String getCopyFromPage() {
        return this.copyFromPage;
    }
    
    public boolean isCopy() {
        return (this.copyFromPage != null);
    }
    
    public void setDefaultStaticState(Class clazz) {
        this.defaultStaticStateClass = clazz;
    }
    
    public void setDefaultIHandlerState(Class clazz) {
        this.defaultIWrapperStateClass = clazz;
    }
    
    public void setIWrapperPolicy(Policy policy) {
        this.policy = policy;
    }
    
    public Policy getIWrapperPolicy() {
        return this.policy;
    }
    
    public void setFinalizer(Class clazz) {
        this.finalizer = clazz;
    }
    
    public Class getFinalizer() {
        return this.finalizer;
    }
    
    public void addIWrapper(IWrapperConfig config) {
        this.iwrappers.put(config.getPrefix(), config);
    }
    
    public IWrapperConfig[] getIWrappers() {
        ArrayList<IWrapperConfig> list = new ArrayList<IWrapperConfig>();
        for (Iterator i = this.iwrappers.entrySet().iterator(); i.hasNext();) {
            Entry entry = (Entry) i.next();
            list.add((IWrapperConfig) entry.getValue());
        }
        return list.toArray(new IWrapperConfig[list.size()]);
    }
    
    public void addAuxWrapper(String prefix, Class clazz) {
        this.auxwrappers.put(prefix, clazz);
    }
    
    public Map<String, Class> getAuxWrappers() {
        return this.auxwrappers;
    }
    
    public void addContextResource(String prefix, Class clazz) {
        this.resources.put(prefix, clazz);
    }
    
    public Map<String, Class> getContextResources() {
        return this.resources;
    }
    
    public void setProperty(String key, String value) {
        this.props.setProperty(key, value);
    }
    
    public void setProperties(Properties props) {
        this.props = new Properties();
        Enumeration e = props.propertyNames();
        while (e.hasMoreElements()) {
            String propname = (String) e.nextElement();
            this.props.setProperty(propname, props.getProperty(propname));
        }
    }
    
    public Properties getProperties() {
        return this.props;
    }
    
    public void addAuthWrapper(String prefix, Class clazz) {
        this.authPrefix = prefix;
        this.authClass = clazz;
        if (this.stateClass == null) {
            // Create class object at runtime due to build dependency problems
            try {
                this.setState(Class.forName("de.schlund.pfixcore.workflow.app.DefaultAuthIWrapperState"));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class de.schlund.pfixcore.workflow.app.DefaultAuthIWrapperState could not be loaded!", e);
            }
        }
    }
    
    public String getAuthWrapperPrefix() {
        return this.authPrefix;
    }
    
    public Class getAuthWrapperClass() {
        return this.authClass;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
 }