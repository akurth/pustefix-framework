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

package org.pustefixframework.http;

import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.pustefixframework.config.contextxmlservice.AbstractPustefixRequestHandlerConfig;
import org.pustefixframework.config.directoutputservice.DirectOutputPageRequestConfig;
import org.pustefixframework.config.directoutputservice.DirectOutputServiceConfig;

import de.schlund.pfixcore.auth.AuthConstraint;
import de.schlund.pfixcore.workflow.ContextImpl;
import de.schlund.pfixcore.workflow.ContextResourceManager;
import de.schlund.pfixcore.workflow.DirectOutputState;
import de.schlund.pfixcore.workflow.PageRequest;
import de.schlund.pfixxml.PfixServletRequest;
import de.schlund.pfixxml.resources.FileResource;

/**
 * The <code>DirectOutputServlet</code> is a servlet that hijacks the {@link de.schlund.pfixcore.workflow.Context} of a
 * comapnion ContextXMLServlet that runs in the same servlet session.
 * It has no Context of it's own, but rather makes all the work itself, as
 * there is no PageFlow handling involved.
 * Instead of {@link de.schlund.pfixcore.workflow.State}s (as the Context of a ContextXMLServlet
 * does) this kind of Servlet uses so called {@link
 * DirectOutputStates}. These can write their output directly to the HttpServletResponse
 * object. So there is also no XML/XSLT transformation involved.
 * You can use this construct whenever you need to make things like
 * images, pdfs available for download and you can not simply write out a static file.
 * Consider e.g. a autogenerated pdf that needs information that is saved in some ContextResource.
 * A DirectOutputState can do this, as it has the possibility to work with the Context of a
 * foreign ContextXMLServlet servlet, getting all the information it needs from there, generating the
 * pdf and streaming it directly to the OutputStream of the HttpServletResponse.
 *
 * If the foreign Context is of type AuthContext, the servlet
 * additionally checks, if the Context is authenticated before trying to call any DirectOutputState.
 *
 * The servlet gets the foreign context by using the mandatory property
 * <code>foreigncontextservlet.foreignservletname</code>.
 * The value must be the servlet name of the ContextXMLServlet whose Context you want to use.
 *  
 * @author <a href="mailto:jtl@schlund.de">Jens Lautenbacher</a>
 * @version $Id: DirectOutputServlet.java 3515 2008-04-28 22:11:18Z jenstl $
 */
public class PustefixContextDirectOutputRequestHandler extends AbstractPustefixRequestHandler {
    private Logger                    LOG       = Logger.getLogger(this.getClass());
    private DirectOutputServiceConfig config;
    private Map<String, DirectOutputState> stateMap;
    
    private ContextImpl context;
    
    /**
     * The usual <code>needsSession</code> method. Is set to return
     * true, as any other value wouldn't make sense (You need to get a
     * Context from a running session after all)
     *
     * @return a <code>boolean</code> value
     */
    @Override
    protected final boolean needsSession() {
        return true;
    }
   
    /**
     * <code>allowSessionCreate</code> returns false. If the session is being created
     * here, it will not have a saved Context anyway, so this makes no sense. 
     *
     * @return a <code>boolean</code> value
     */
    @Override
    protected final boolean allowSessionCreate() {
        return false;
    }

    /**
     * <code>process</code> first tries to get the requested
     * Context. if the Context is of type AuthContext, it checks the
     * Authorization of the context first.  After that, it asks the
     * {@link de.schlund.pfixcore.workflow.DirectOutputPageMap} for a
     * {@link de.schlund.pfixcore.workflow.DirectOutputState}
     * that matches the current PageRequest (NOTE: this is NOT the
     * pagerequest that is returned from the foreign Context as the
     * current PageRequest!). The accessibility of the
     * DirectOutputState is checked, then the handleRequest(Context,
     * Properties, PfixServletRequest, HttpServletResponse) method of
     * the DirectOutputState is called. NOTE: The properties parameter
     * are the properties matching the current PageRequest. Again this
     * is not what the foreign context would return!
     *
     * @param preq a <code>PfixServletRequest</code> value
     * @param res a <code>HttpServletResponse</code> value
     * @exception Exception if an error occurs
     */
    @Override
    protected void process(PfixServletRequest preq, HttpServletResponse res) throws Exception {
         HttpSession   session = preq.getSession(false);
         if (session == null) {
             //throw new RuntimeException("*** didn't get Session from request. ***");
             LOG.error("*** didn't get Session from request. Stop processing. ***");
             res.sendError(HttpServletResponse.SC_FORBIDDEN, "No session supplied");
             return;
         }
         
         // Make sure the context is initialized and deinitialized this thread
         context.prepareForRequest();
         try {
             if (config.isSynchronized()) {
                 synchronized (context) {
                     doProcess(preq, res, context);
                 }
             } else {
                 doProcess(preq, res, context);
             }
         } finally {
             context.cleanupAfterRequest();
         }
    }
    
    protected void doProcess(PfixServletRequest preq, HttpServletResponse res, ContextImpl context) throws Exception {
         ContextResourceManager crm = context.getContextResourceManager();
         
         String pagename = preq.getPageName();
         if (pagename == null || pagename.length() == 0) {
             LOG.error("*** got request without page name ***");
             res.sendError(HttpServletResponse.SC_NOT_FOUND, "Must specify page name");
             return;
         }
         
         //Find authconstraint using the following search order:
         //   - authconstraint referenced by directoutputpagerequest
         //   - authconstraint referenced by directoutputserver
         //   - default authconstraint from context configuration
         AuthConstraint authConst = null;
         String authRef = null;
         DirectOutputPageRequestConfig pageConfig = config.getPageRequest(pagename);
         if (pageConfig != null) authRef = pageConfig.getAuthConstraintRef();
         if (authRef == null) authRef = config.getAuthConstraintRef();
         if (authRef != null) {
             authConst = context.getContextConfig().getAuthConstraint(authRef);
             if (authConst == null) throw new RuntimeException("AuthConstraint not found: " + authRef);
         }
         if (authConst == null) authConst = context.getContextConfig().getDefaultAuthConstraint();
         if (authConst != null) {
             if (!authConst.isAuthorized(context)) {
                 LOG.info("Got request without authorization");
                 res.sendError(HttpServletResponse.SC_FORBIDDEN, "Must authenticate first");
                 return;
             }
         }
         
         PageRequest       page  = new PageRequest(pagename);
         DirectOutputState state = stateMap.get(page.getName());
         if (state != null) {
             Properties props   = config.getPageRequest(page.getName()).getProperties();
             boolean    allowed = state.isAccessible(crm, props, preq);
             if (allowed) {
                 try {
                     state.handleRequest(crm, props, preq, res);
                 } catch (Exception exep) {
                     if (!exep.getClass().getName().equals("org.apache.catalina.connector.ClientAbortException")) {
                         throw exep;
                     }
                 }
             } else {
                 throw new RuntimeException("*** Called DirectOutputState " + state.getClass().getName() +
                                            " for page " + page.getName() + " without being accessible ***");  
             }
         } else {
             LOG.error("*** No DirectOutputState for page " + page.getName() + " ***");
             res.sendError(HttpServletResponse.SC_NOT_FOUND, "Page " + page.getName() + " not found");
             return;
         }
    }
    
    @Override
    protected AbstractPustefixRequestHandlerConfig getServletManagerConfig() {
        return this.config;
    }

    protected void reloadServletConfig(FileResource configFile, Properties globalProperties) throws ServletException {
        // Do nothing, configuration is injected
    }
    
    public void setContext(ContextImpl context) {
        this.context = context;
    }
    
    public void setStateMap(Map<String, DirectOutputState> stateMap) {
        this.stateMap = stateMap;
    }
    
    public void setConfiguration(DirectOutputServiceConfig config) {
        this.config = config;
    }
    
}
