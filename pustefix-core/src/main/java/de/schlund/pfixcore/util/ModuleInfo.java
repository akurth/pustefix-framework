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
package de.schlund.pfixcore.util;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import de.schlund.pfixcore.exception.PustefixRuntimeException;
import de.schlund.pfixxml.resources.ModuleFilter;

public class ModuleInfo {
    
	private final static Logger LOG = Logger.getLogger(ModuleInfo.class);
	
    private final static String MODULE_DESCRIPTOR_LOCATION = "META-INF/pustefix-module.xml";
    
    private static ModuleInfo instance = new ModuleInfo();
    
    private SortedMap<String,ModuleDescriptor> moduleDescMap = new TreeMap<String,ModuleDescriptor>();
    
    private List<String> commonDefaultSearchModules = new ArrayList<String>();
    private Map<String, List<String>> appVariantToDefaultSearchModules = new HashMap<String, List<String>>();
    
    public static ModuleInfo getInstance() {
        return instance;
    }
    
    public ModuleInfo() {
        try {
            read();
            LOG.info(toString());
        } catch(Exception x) {
            throw new PustefixRuntimeException(x);
        }
    }
    
    public ModuleInfo(List<URL> urls){
        try {
            read(urls);
            LOG.info(toString());
        } catch(Exception x) {
            throw new PustefixRuntimeException(x);
        }
    }
    
    public void read() throws Exception {
        Enumeration<URL> urls = getClass().getClassLoader().getResources(MODULE_DESCRIPTOR_LOCATION);
        while(urls.hasMoreElements()) {
            URL url = urls.nextElement();
            read(url);
        }
    }
    
    public void read(List<URL> urls) throws Exception {
        for(URL url:urls) {
           read(url);
        }
    }
    
    private void read(URL url) throws Exception {
        ModuleDescriptor moduleDesc = ModuleDescriptor.read(url);
        if(moduleDescMap.containsKey(moduleDesc.getName())) {
            throw new Exception("Found multiple modules named '"+moduleDesc.getName()+"' ("+
                    moduleDesc.getURL() + " - " + moduleDescMap.get(moduleDesc.getName()).getURL() + ")");
        }
        moduleDescMap.put(moduleDesc.getName(), moduleDesc);
    }
    
    public ModuleDescriptor getModuleDescriptor(String moduleName) {
        return moduleDescMap.get(moduleName);
    }
    
    public Set<String> getModules() {
        return moduleDescMap.keySet();
    }
    
    public List<String> getOverridingModules(String moduleName, ModuleFilter filter, String resourcePath) {
        List<String> modules = new ArrayList<String>();
        getOverridingModules(moduleName, filter, resourcePath, modules);
        return modules;
    }
    
    private void getOverridingModules(String moduleName, ModuleFilter filter, String resourcePath, List<String> modules) {
        for(ModuleDescriptor moduleDesc:moduleDescMap.values()) {
            System.out.println("FILTER: "+filter);
            if((filter == null || filter.accept(moduleDesc)) && moduleDesc.overridesResource(moduleName, resourcePath)) {
                if(!modules.contains(moduleDesc.getName())) {
                    modules.add(0, moduleDesc.getName());
                    getOverridingModules(moduleDesc.getName(), filter, resourcePath, modules);
                }
            }
        }
    }
    
    public void addDefaultSearchModule(String moduleName) {
        if(!moduleDescMap.containsKey(moduleName)) 
            throw new RuntimeException("Default-search module '" + moduleName + "' doesn't exist.");
        commonDefaultSearchModules.add(moduleName);
        for(String appVariant: appVariantToDefaultSearchModules.keySet()) {
            List<String> defaultSearchModules = appVariantToDefaultSearchModules.get(appVariant);
            defaultSearchModules.add(moduleName);
        }
    }
    
    public void addDefaultSearchModule(String appVariant, String moduleName) {
        if(!moduleDescMap.containsKey(moduleName)) 
            throw new RuntimeException("Default-search module '" + moduleName + "' doesn't exist.");
        List<String> defaultSearchModules = appVariantToDefaultSearchModules.get(appVariant);
        if(defaultSearchModules == null) {
            defaultSearchModules = new ArrayList<String>();
            appVariantToDefaultSearchModules.put(appVariant, defaultSearchModules);
            defaultSearchModules.add(moduleName);
            defaultSearchModules.addAll(commonDefaultSearchModules);
        } else {
            defaultSearchModules.add(moduleName);
        }
    }
    
    public List<String> getDefaultSearchModules() {
        return commonDefaultSearchModules;
    }
    
    public List<String> getDefaultSearchModules(String appVariant) {
        return appVariantToDefaultSearchModules.get(appVariant);
    }
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("Module information - ");
    	int no = moduleDescMap.values().size();
    	sb.append("Detected " + no + " module" + (no==1?"":"s") + " [");
    	for(ModuleDescriptor moduleDesc:moduleDescMap.values()) {
    		sb.append(moduleDesc.getName() + " ");
    	}
    	if(no>0) sb.deleteCharAt(sb.length()-1);
    	sb.append("]");
    	return sb.toString();
    }

}
