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

package de.schlund.pfixcore.generator;
import java.util.*;
import org.apache.log4j.*;
import de.schlund.util.*;
import de.schlund.pfixxml.loader.*;

/**
 * IHandlerFactory.java
 *
 *
 * Created: Sat Oct 13 00:07:21 2001
 *
 * @author <a href="mailto:jtl@schlund.de">Jens Lautenbacher</a>
 *
 *
 */

public class IHandlerFactory implements Reloader {
    private static HashMap         knownhandlers    = new HashMap();
    private static HashMap         wrapper2handlers = new HashMap();
    private static Category        LOG              = Category.getInstance(IHandlerFactory.class.getName());
    private static IHandlerFactory instance         = new IHandlerFactory();

    private IHandlerFactory() {
        // do nothing.
        AppLoader appLoader=AppLoader.getInstance();
        if(appLoader.isEnabled()) {
            appLoader.addReloader(this);
        }
    }
    
    /**
     * <code>getInstance</code> returns the single Instance of a IHandlerFactory.
     *
     * @return an <code>IHandlerFactory</code> value
     */
    public static IHandlerFactory getInstance() {
        return instance;
    }
    
    /**
     * <code>getIHandler</code> returns the IHandler for the given classname.
     * (IHandlers are singletons).
     *
     * @param classname a <code>String</code> value
     * @return a <code>IHandler</code> value
     */
    public IHandler getIHandler(String classname) {
        synchronized (knownhandlers) {
            IHandler retval = (IHandler) knownhandlers.get(classname); 
            if (retval == null) {
                try {
                    AppLoader appLoader=AppLoader.getInstance();
                    if(appLoader.isEnabled()) {
                        retval=(IHandler)appLoader.loadClass(classname).newInstance();
                    } else {
                        Class stateclass = Class.forName(classname);
                        retval = (IHandler) stateclass.newInstance();
                    }
                } catch (InstantiationException e) {
                    LOG.error("unable to instantiate class [" + classname + "]", e);
                } catch (IllegalAccessException e) {
                    LOG.error("unable access class [" + classname + "]", e);
                } catch (ClassNotFoundException e) {
                    LOG.error("unable to find class [" + classname + "]", e);
                } catch (ClassCastException e) {
                    LOG.error("class [" + classname + "] does not implement the interface IHandler", e);
                }
                knownhandlers.put(classname, retval);
            }
            // LOG.debug("Retval is: " + retval);
            return retval;
        }
    }

    public IHandler getIHandlerForWrapperClass(String classname) {
        synchronized (wrapper2handlers) {
            IHandler retval = (IHandler) wrapper2handlers.get(classname); 
            if (retval == null) {
                try {
                    AppLoader appLoader=AppLoader.getInstance();
                    if(appLoader.isEnabled()) {
                        IWrapper wrapper=(IWrapper)appLoader.loadClass(classname).newInstance();
                        retval=wrapper.gimmeIHandler();
                    } else {
                        Class    stateclass = Class.forName(classname);
                        IWrapper wrapper    = (IWrapper) stateclass.newInstance();
                        retval              = wrapper.gimmeIHandler();
                    }
                } catch (InstantiationException e) {
                    LOG.error("unable to instantiate class [" + classname + "]", e);
                } catch (IllegalAccessException e) {
                    LOG.error("unable access class [" + classname + "]", e);
                } catch (ClassNotFoundException e) {
                    LOG.error("unable to find class [" + classname + "]", e);
                } catch (ClassCastException e) {
                    LOG.error("class [" + classname + "] does not implement the interface IWrapper", e);
                }
                wrapper2handlers.put(classname, retval);
            }
            return retval;
        }
    }
    
    
    public void reload() {
        HashMap knownNew=new HashMap();
        Iterator it=knownhandlers.keySet().iterator();
        while(it.hasNext()) {
            String str=(String)it.next();
            IHandler ihOld=(IHandler)knownhandlers.get(str);
            IHandler ihNew=(IHandler)StateTransfer.getInstance().transfer(ihOld);
            knownNew.put(str,ihNew);
        }
        knownhandlers=knownNew;
        HashMap wrapperNew=new HashMap();
        it=wrapper2handlers.keySet().iterator();
        while(it.hasNext()) {
            String str=(String)it.next();
            IHandler ihOld=(IHandler)wrapper2handlers.get(str);
            IHandler ihNew=(IHandler)StateTransfer.getInstance().transfer(ihOld);
            wrapperNew.put(str,ihNew);   
        }
        wrapper2handlers=wrapperNew;
    }
    
    
}// IHandlerFactory
