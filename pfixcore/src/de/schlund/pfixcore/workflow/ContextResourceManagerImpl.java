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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import de.schlund.pfixcore.exception.PustefixApplicationException;
import de.schlund.pfixcore.exception.PustefixCoreException;
import de.schlund.pfixcore.exception.PustefixRuntimeException;
import de.schlund.pfixxml.config.ContextResourceConfig;
import de.schlund.pfixxml.config.ContextConfig;

/**
 * Implements the ability to store objects implementing a number of interfaces extending
 * the ContextResource interface
 *
 * @author jtl, thomas
 *
 */

public class ContextResourceManagerImpl implements ContextResourceManager {
    private final static Logger LOG = Logger.getLogger(ContextResourceManagerImpl.class);
    private HashMap<String, Object>  resources = new HashMap<String, Object>();
    
    /**
     * Instanciates the objects and registers the interfaces which
     * should be used from each.
     *
     * In general such an object implements a number of interfaces extending
     * the ContextResource interface. You are able to specify the classes
     * you want to instanciate and to specify which interfaces you want to
     * use from such an object.
     * 
     * The configuration is done by passing properties, each object you want
     * to use must be specified in a single property.
     * <br>
     * The name of this property consists of the prefix
     * <code>context.resource.[A_NUMBER].</code> followed by the
     * full qualified classname of the class.
     * <br>
     * The value of the property specifies the interfaces you want to use
     * from this object. All interfaces are declared by the full qualified
     * classname of the interface and separated by a comma. 
     * <br>
     * An wrong example:<br>
     * <code>context.rescoure.1.de.foo.FooImpl             = Foo</code><br>
     * <code>context.rescoure.2.de.foo.FooAndBarAndBazImpl = Foo,Bar,Baz</code>
     *
     * This example, written as above, would be invalid as no two ContextRessources
     * are allowed to act as an implementation for the same interface(Foo in this case).
     * Note that the classes may implement the same interface, they are just not allowed to act as
     * an implementation for the same interface in a ContextRessource declaration.
     *
     * The correct example could be:<br>
     * <code>context.rescoure.1.de.foo.FooImpl             = Foo</code><br>
     * <code>context.rescoure.2.de.foo.FooAndBarAndBazImpl = Bar,Baz</code>
     *
     * which is correct without any change in the code of the implementing classes.
     * @throws PustefixApplicationException 
     * @throws PustefixCoreException 
     *
     */
    
     public void init(Context context, ContextConfig config) throws PustefixApplicationException, PustefixCoreException {
        LOG.debug("initialize ContextResources...");
        
        Collection<ContextResource> resourcesToInitialize = new ArrayList<ContextResource>();
        Map<String, ContextResource> resourceClassToInstance = new HashMap<String, ContextResource>();
        
        for (ContextResourceConfig resourceConfig : config.getContextResourceConfigs()) {
            ContextResource cr = null;
            String classname = resourceConfig.getContextResourceClass().getName();
            try {
                LOG.debug("Creating object with name [" + classname + "]");
                cr = (ContextResource) resourceConfig.getContextResourceClass().newInstance();
            } catch (InstantiationException e) {
                throw new PustefixRuntimeException("Exception while creating object " + classname + ":" + e);
            } catch (IllegalAccessException e) {
                throw new PustefixRuntimeException("Exception while creating object " + classname + ":" + e);
            }
            
            resourcesToInitialize.add(cr);
            resourceClassToInstance.put(cr.getClass().getName(), cr);
        }
        
        Map<Class, ? extends ContextResourceConfig> interfaces = config.getInterfaceToContextResourceMap();
        for (Class clazz : interfaces.keySet()) {
            String interfacename = clazz.getName();
            String resourceclass = interfaces.get(clazz).getContextResourceClass().getName();
            ContextResource cr = resourceClassToInstance.get(resourceclass);
            checkInterface(cr, interfacename);
            LOG.debug("* Registering [" + cr.getClass().getName() + "] for interface [" + interfacename + "]");
            resources.put(interfacename, cr);
        }
        
        for (Iterator i = resourcesToInitialize.iterator(); i.hasNext();) {
            ContextResource resource = (ContextResource) i.next();
            try {
                resource.init(context);
            } catch (Exception e) {
                throw new PustefixApplicationException("Exception while initializing context resource " + resource.getClass(), e);
            }
        }
        
    }

    /* (non-Javadoc)
     * @see de.schlund.pfixcore.workflow.ContextResourceManager#getResource(java.lang.String)
     */
    public Object getResource(String name) {
        return resources.get(name);
    }

    /* (non-Javadoc)
     * @see de.schlund.pfixcore.workflow.ContextResourceManager#getResource(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    public <T> T getResource(Class<T> clazz) {
        return (T) resources.get(clazz.getName());
    }
    
    private void checkInterface(Object obj, String interfacename) throws PustefixCoreException {
        Class wantedinterface = null;
        
        // Get the class of the requested interface and get all
        // implemented interfaces of the object
        try {
            wantedinterface = Class.forName(interfacename) ;
        } catch (ClassNotFoundException e) {
            throw new PustefixRuntimeException("Got ClassNotFoundException for classname " +  interfacename +
                                       "while checking for interface");
        }
	
        LOG.debug("Check if requested interface [" + interfacename + 
                  "] is implemented by [" + obj.getClass().getName() + "]");
        
        // Check for all implemented interfaces, if it equals the interface that
        // we want, than break.
        
        if (wantedinterface.isInstance(obj)) {
            LOG.debug("Got requested interface " + interfacename);
        } else {
            // Uh, the requested interface is not implemented by the
            // object, that's not nice!
            throw new PustefixCoreException("The class [" + obj.getClass().getName() +
                                       "] doesn't implemented requested interface " +
                                       interfacename);
        }
        
        // Now check if the interface is already registered...
        if (resources.containsKey(interfacename)) {
            throw new PustefixCoreException("Interface [" + interfacename +
                                       "] already registered for instance of [" +
                                       resources.get(interfacename).getClass().getName() + "]");
        }
    }
    
    /* (non-Javadoc)
     * @see de.schlund.pfixcore.workflow.ContextResourceManager#getResourceIterator()
     */
    public Iterator<Object> getResourceIterator() {
        return  resources.values().iterator();
    }
    
}