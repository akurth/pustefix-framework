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

import de.schlund.pfixxml.*;
import de.schlund.pfixxml.serverutil.*;
import java.lang.reflect.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.log4j.*;
import org.w3c.dom.*;

/**
 * @author jtl
 *
 */

public class ContextXMLServer extends AbstractXMLServer {
    private              Category CAT            = Category.getInstance(ContextXMLServer.class.getName());
    private final static String   CONTEXT_SUFFIX = "__CONTEXT__";
    private final static String   CONTEXT_CLASS  = "context.class";
    
    private WeakHashMap contextMap=new WeakHashMap();
    private String contextClassName;
    
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        contextClassName=getProperties().getProperty(CONTEXT_CLASS);
    }

    protected boolean needsSession() {
        return true;
    }
    
    protected boolean allowSessionCreate() {
        return true;
    }

    protected boolean tryReloadProperties(PfixServletRequest preq) throws ServletException {
        if (super.tryReloadProperties(preq)) {
            //Reset PropertyObjects
            PropertyObjectManager.getInstance().resetPropertyObjects(getProperties());
            //Reset Contexts
            synchronized (contextMap) {
                //Set name of Context class, compare with old name
                String oldClassName=contextClassName;
                contextClassName=getProperties().getProperty(CONTEXT_CLASS);
                if(contextClassName.equals(oldClassName)) {
                    //Iterate over Contexts and reset them
                    Iterator it=contextMap.keySet().iterator();
                    while(it.hasNext()) {
                        try {
                            AppContext appCon=(AppContext)it.next();
                            appCon.reset();
                        } catch(Exception e) {
                            throw new ServletException("Error while resetting context.");
                        }
                    }
                } else {
                    //Remove deprecated Contexts from contextMap
                    contextMap.clear();
                }
            } 
            return true;
        } else {
            return false;
        }
    }
    
    public SPDocument getDom(PfixServletRequest preq) throws Exception {
        AppContext context = getContext(preq);
        SPDocument spdoc   = context.handleRequest(preq);
        return spdoc;
    }

    private AppContext getContext(PfixServletRequest preq) throws Exception {
        String        contextname = makeContextName();
        HttpSession   session     = preq.getSession(false);
        ContainerUtil conutil     = getContainerUtil();
        if (session == null) {
            throw new XMLException("No valid session found! Aborting...");
        }
        AppContext context = (AppContext) conutil.getSessionValue(session, contextname);
        //Create new context and add it to contextMap, if context is null or contextClass has changed
        if ((context==null) || (!contextClassName.equals(context.getClass().getName()))) {
            context = createContext();
            conutil.setSessionValue(session, contextname, context);
            synchronized (contextMap) {
                contextMap.put(context,null);
            }
        }
        return context;
    }
    
    private String makeContextName() {
        return servletname + CONTEXT_SUFFIX;
    }
    
    private AppContext createContext() throws Exception {
        String contextclass = getProperties().getProperty(CONTEXT_CLASS);
        if (contextclass == null) {
            throw (new XMLException("Need name for context class from context.class property"));
        }
        AppContext context = (AppContext) Class.forName(contextclass).newInstance();
        context.init(getProperties(), getContainerUtil(), makeContextName());
        return context;
    }
}
