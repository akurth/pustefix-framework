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

import de.schlund.pfixcore.workflow.*;
import de.schlund.pfixcore.workflow.Navigation.*;
import de.schlund.pfixxml.*;
import de.schlund.pfixxml.serverutil.*;
import de.schlund.pfixxml.targets.*;
import de.schlund.pfixcore.util.*;
import java.util.*;
import javax.servlet.http.*;
import org.apache.log4j.*;
import org.w3c.dom.*;


/**
 * This class is the corner piece of our workflow concept.
 * Here we decide which {@link de.schlund.pfixcore.workflow.State State} handles the
 * request. This decision is based on the notion of workflows (aka
 * {@link de.schlund.pfixcore.workflow.PageFlow PageFlow}s).
 * 
 *
 * @author jtl
 * @version 2.0
 *
 *
 */

public class Context implements AppContext {
    private final static Category LOG               = Category.getInstance(Context.class.getName());
    private final static String   NOSTORE           = "nostore";
    private final static String   DEFPROP           = "context.defaultpageflow";
    private final static String   NAVPROP           = "xmlserver.depend.xml";
    private final static String   PROP_NAVI_AUTOINV = "navigation.autoinvalidate"; 
    private final static String   PROP_NEEDS_SSL    = "needsSSL"; 
    private final static String   WATCHMODE         = "context.adminmode.watch";
    private final static String   ADMINPAGE         = "context.adminmode.page";
    private final static String   ADMINMODE         = "context.adminmode";
    private final static String   AUTH_PROP         = "authcontext.authpage";

    // from constructor
    private String     name;
    private Properties properties;
    
    // shared between all instances that have the same properties
    private PageFlowManager       pageflowmanager;
    private PageRequestProperties preqprops;
    private PageMap               pagemap;
    
    // new instance for every Context
    private ContextResourceManager rmanager;
    private Navigation             navigation = null;
    private PageRequest            authpage   = null;
    
    // values read from properties
    private boolean     autoinvalidate_navi = true;
    private boolean     in_adminmode        = false;
    private PageRequest admin_pagereq;

    // the request state
    protected PfixServletRequest currentpreq;
    protected PageRequest        currentpagerequest;
    // protected PageRequest        initialpagerequest;
    protected PageFlow           currentpageflow;
    
    private HashMap navigation_visible = null;
    private String  visit_id           = null;
    private boolean needs_update;



    /**
     * <code>init</code> sets up the Context for operation.
     *
     * @param properties a <code>Properties</code> value
     * @exception Exception if an error occurs
     */
    public void init(Properties properties, String name) throws Exception {
        this.properties = properties;
        this.name       = name;
        rmanager        = new ContextResourceManager();
        rmanager.init(this);
        reset();
    }

    public void reset() {
        needs_update = true;
        invalidateNavigation();
    }
    
    /**
     * <code>handleRequest</code> is the entry point where the Context is called
     * from outside to handle the supplied request.
     *
     * @param req a <code>HttpServletRequest</code> value
     * @return a <code>SPDocument</code> value
     * @exception Exception if an error occurs
     */
    public synchronized SPDocument handleRequest(PfixServletRequest preq) throws Exception {
        currentpreq = preq;
        if (needs_update) {
            do_update();
        }
        
        SPDocument  spdoc;
        PageRequest prevpage = currentpagerequest;
        PageFlow    prevflow = currentpageflow;
        
        if (visit_id == null) 
            visit_id = (String) currentpreq.getSession(false).getValue(ServletManager.VISIT_ID);

        if (in_adminmode) {
            ResultDocument resdoc;
            if (checkIsAccessible(admin_pagereq, PageRequestStatus.UNDEF)) {
                currentpagerequest = admin_pagereq;
                resdoc             = documentFromCurrentStep();
                currentpagerequest = prevpage;
            } else {
                throw new XMLException("*** admin mode requested but admin page " + admin_pagereq + " is inaccessible ***");
            }
            spdoc = resdoc.getSPDocument();
            spdoc.setPagename(admin_pagereq.getName());
            return spdoc;
        }
        
    	trySettingPageRequestAndFlow();
        spdoc = documentFromFlow();

        if (spdoc != null && spdoc.getPagename() == null) {
            spdoc.setPagename(currentpagerequest.getName());
        }

        if (spdoc != null && currentpageflow != null) {
            spdoc.setProperty("pageflow", currentpageflow.getName());
        }

        if (spdoc.getResponseError() != 0) {
            currentpagerequest = prevpage;
            currentpageflow    = prevflow;
            return spdoc;
        }
        
        if (navigation != null && spdoc != null) {
            addNavigation(navigation, spdoc);
        }
        
        if (pageIsSidestepPage(currentpagerequest)) {
            LOG.info("* [" + currentpagerequest + "] is sidestep: Restoring to [" +
                     prevpage + "] in flow [" + prevflow.getName() + "]");
            currentpagerequest = prevpage;
            currentpageflow    = prevflow;
            spdoc.setNostore(true);
        }

        LOG.debug("\n");
        return spdoc;
    }

    /**
     * <code>getContextResourceManager</code> returns the ContextResourceManager defined in init(Properties properties).
     *
     * @return a <code>ContextResourceManager</code> value
     */
    public ContextResourceManager getContextResourceManager() {
        return rmanager;
    }

    /**
     * <code>getProperties</code> returns the Properties supplied in init(Properties properties).
     *
     * @return a <code>Properties</code> value
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Describe <code>getPropertiesForCurrentPageRequest</code> method here.
     * This returnes the all Properties which match the current PageRequest <b>without</b> the common prefix.
     * E.g. the page is named "foo", then all properties starting with "pagerequest.foo.*" will be returned after
     * stripping the "pagerequest.foo." prefix.
     *
     * @return a <code>Properties</code> value
     */
    public Properties getPropertiesForCurrentPageRequest() {
        return preqprops.getPropertiesForPageRequest(currentpagerequest);
    }

    /**
     * <code>getCurrentPageRequest</code> gets the currently active PageRequest.
     *
     * @return a <code>PageRequest</code> value
     */
    public PageRequest getCurrentPageRequest() {
        return currentpagerequest;
    }

    /**
     * <code>flowIsRunning</code> can be called from inside a {@link de.schlund.pfixcore.workflow.State State}
     * It returned true if the Context is currently running one of the defined pageflows.
     *
     * @return a <code>boolean</code> value
     */
    public boolean flowIsRunning() {
        if (currentpagerequest.getStatus() == PageRequestStatus.WORKFLOW) {
            return true;
        } else {
            return false;
        }
    }

    public void invalidateNavigation() {
        navigation_visible = new HashMap();
    }
    
    /**
     * <code>getCurrentSessionId</code> returns the visit_id.
     *
     * @return <code>String</code> visit_id
     */
    public String getVisitId() throws RuntimeException {
        if ( visit_id != null) {
            return visit_id;
        } else {
            throw new RuntimeException("visit_id not set, but asked for!!!!");
        }
    }

    public boolean flowBeforeNeedsData() throws Exception {
        // This is needed when the navigation is built! we don't want to step through the pages of a different workflow every time.
        if (!currentpageflow.containsPageRequest(currentpagerequest)) {
            throw new RuntimeException("*** current pageflow " + currentpageflow.getName() + " does not contain current pagerequest " + currentpagerequest);
        }
        PageRequest   current  = currentpagerequest;
        PageRequest[] workflow = currentpageflow.getAllSteps();
        
        for (int i = 0; i < workflow.length; i++) {
            PageRequest page = workflow[i];
            if (page.equals(current)) {
                return false;
            }
            if (checkNeedsData(page, current.getStatus())) {
                return true;
            }
        }
        return false;
    }

    /**
     * <code>finalPageIsRunning</code> can be called from inside a {@link de.schlund.pfixcore.workflow.State State}
     * It returned true if the Context is currently running a FINAL page of a defined workflow.
     *
     * @return a <code>boolean</code> value
     */
    public boolean finalPageIsRunning() {
        if (currentpagerequest.getStatus() == PageRequestStatus.FINAL) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isCurrentPageRequestInCurrentFlow() {
        if (currentpageflow.containsPageRequest(currentpagerequest)) {
            return true;
        } else {
            return false;
        }
    }
    
    public boolean currentPageNeedsSSL(PfixServletRequest preq) throws Exception {
        PageRequest page = new PageRequest(preq); 
        if (page.isEmpty() && currentpagerequest != null) {
            page = currentpagerequest;
        }
        if (!page.isEmpty()) {
            Properties props = preqprops.getPropertiesForPageRequest(page);
            if (props != null) {
                String     needssl = props.getProperty(PROP_NEEDS_SSL);
                if (needssl != null && needssl.equals("true")) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public SPDocument checkAuthorization() throws Exception {
        if (authpage != null) {
            ResultDocument resdoc = null;
            LOG.debug("===> [" + authpage + "]: Checking authorisation");
            if (!checkIsAccessible(authpage, PageRequestStatus.AUTH)) {
                throw new XMLException("*** Authorisation page [" + authpage + "] is not accessible! ***");
            }
            if (checkNeedsData(authpage, PageRequestStatus.AUTH)) {
                LOG.debug("===> [" + authpage + "]: Need authorisation data");
                PageRequest saved  = currentpagerequest;
                currentpagerequest = authpage;
                resdoc             = documentFromCurrentStep();
                currentpagerequest = saved;
                if (resdoc.wantsContinue()) {
                    LOG.debug("===> [" + authpage + "]: Authorisation granted");
                } else {
                    LOG.debug("===> [" + authpage + "]: Authorisation failed");
                }
            } else {
                LOG.debug("===> [" + authpage + "]: Already authorised");
            }
            if (resdoc != null && !resdoc.wantsContinue()) {
                // getting a document here means we need to show the authpage
                if (resdoc.getSPDocument() == null) {
                    throw new XMLException("*** FATAL: " + authpage + " returns a 'null' SPDocument! ***");
                }
                resdoc.getSPDocument().setPagename(authpage.getName());
                return resdoc.getSPDocument();
            }
        }
        return null;
    }

    private void do_update() throws Exception {
    	// get PropertyObjects from PropertyObjectManager
    	PropertyObjectManager pom = PropertyObjectManager.getInstance();
        
        pageflowmanager = (PageFlowManager) pom.getPropertyObject(properties,"de.schlund.pfixcore.workflow.PageFlowManager");
        preqprops       = (PageRequestProperties) pom.getPropertyObject(properties,"de.schlund.pfixcore.workflow.PageRequestProperties");
        pagemap         = (PageMap) pom.getPropertyObject(properties,"de.schlund.pfixcore.workflow.PageMap");
        
        // The navigation is possibly shared across more than one context, i.e. more than one properties object.
        // So we can't let it be handled by the PropertyObjectManager.
        if (properties.getProperty(NAVPROP) != null) {
            navigation = NavigationFactory.getInstance().getNavigation(properties.getProperty(NAVPROP));
        }

        currentpageflow    = pageflowmanager.getPageFlowByName(properties.getProperty(DEFPROP));
        currentpagerequest = currentpageflow.getFirstStep();

        checkForAuthenticationMode();
        checkForAdminMode();
        checkForNavigationReuse();
        
        needs_update = false;
    }

    private boolean pageIsSidestepPage(PageRequest page) {
        Properties props  = preqprops.getPropertiesForPageRequest(page);
        if (props != null) {
            String nostore = props.getProperty(NOSTORE);
            if (nostore != null && nostore.toLowerCase().equals("true")) {
                LOG.info("*** Found sidestep page: " + page);
                return true;
            }
        } else {
            LOG.error("*** Got NULL properties for page " + page);
        }
        return false;
    }
    
    private void checkForAuthenticationMode() {
        String authpagename = properties.getProperty(AUTH_PROP);
        if (authpagename != null) {
            authpage = new PageRequest(authpagename);
        } else {
            authpage = null;
        }
    }
    
    private void checkForNavigationReuse() {
        String navi_autoinv = properties.getProperty(PROP_NAVI_AUTOINV);
        if (navi_autoinv != null && navi_autoinv.equals("false")) {
            autoinvalidate_navi = false;
            LOG.info("CAUTION: Setting autoinvalidate of navigation to FALSE!");
            LOG.info("CAUTION: You need to call context.invalidateNavigation() to update the navigation.");
        } else {
            autoinvalidate_navi = true;
        }
    }
    
    private void checkForAdminMode() {
        admin_pagereq = null;
        in_adminmode  = false;

        String watchprop = properties.getProperty(WATCHMODE);
        if (watchprop != null && !watchprop.equals("")) {
            String adminprop = properties.getProperty(ADMINMODE + "." + watchprop + ".status");
            String adminpage = properties.getProperty(ADMINPAGE);
            if (adminpage != null && !adminpage.equals("") && adminprop != null && adminprop.equals("on")) {
                LOG.debug("*** setting Adminmode for : " + watchprop + " ***");
                admin_pagereq = new PageRequest(adminpage);
                in_adminmode  = true;
            }
        }
    }

    private boolean checkNeedsData(PageRequest page, PageRequestStatus status) throws Exception {
        PageRequest saved  = currentpagerequest;
        currentpagerequest = page;
        State       state  = pagemap.getState(page);
        if (state == null) {
            throw new XMLException ("*** Can't get a state to check needsData() for page " + page.getName() + " ***");
        }
        page.setStatus(status);
        currentpreq.startLogEntry();
        boolean retval     = state.needsData(this, currentpreq);
        currentpreq.endLogEntry("NEEDS_DATA (" + page + ")", 10);
        currentpagerequest = saved;
        return retval;
    }

    private boolean checkIsAccessible(PageRequest page, PageRequestStatus status) throws Exception {
        PageRequest saved = currentpagerequest;
        currentpagerequest = page;
        State state = pagemap.getState(page);
        if (state == null) {
            throw new XMLException ("* Can't get a state to check isAccessible() for page " + page.getName());
        }
        page.setStatus(status);
        currentpreq.startLogEntry();
        boolean retval = state.isAccessible(this, currentpreq);
        currentpreq.endLogEntry("IS_ACCESSIBLE (" + page + ")", 10);
        currentpagerequest = saved;
        return retval;
    }

    private SPDocument documentFromFlow() throws Exception {
        SPDocument     document = null;
        PageRequest[]  workflow;
        ResultDocument resdoc;

        // First, check for possibly needed authorization
        document = checkAuthorization();
        if (document != null) {
            return document;
        }
        
        // Now we need to make sure that the current page is accessible, and take the right measures if not.
        if (!checkIsAccessible(currentpagerequest, PageRequestStatus.DIRECT)) {
            LOG.warn("[" + currentpagerequest + "]: not accessible! Trying first page of default flow.");
            currentpageflow     = pageflowmanager.getPageFlowByName(properties.getProperty(DEFPROP));
            PageRequest defpage = currentpageflow.getFirstStep();
            currentpagerequest  = defpage;
            if (!checkIsAccessible(defpage, PageRequestStatus.DIRECT)) {
                throw new XMLException("Even first page [" + defpage + "] of default flow was not accessible! Bailing out.");
            }
        }
        
        resdoc = documentFromCurrentStep();
        if (!resdoc.wantsContinue()) {
            LOG.debug("* [" + currentpagerequest + "] returned document to show, skipping workflow.");
            document = resdoc.getSPDocument();
        } else if (currentpageflow != null) {
            LOG.debug("* [" + currentpagerequest + "] signalled success, starting workflow process");
            // We need to re-check the authorisation because the just handled submit could have changed the authorisation status.
            document = checkAuthorization();
            if (document != null) {
                return document;
            }
            workflow                  = currentpageflow.getAllSteps();
            PageRequest saved         = currentpagerequest;
            boolean     after_current = false;
            for (int i = 0; i < workflow.length; i++) {
                PageRequest page = workflow[i];
                if (page.equals(saved)) {
                    LOG.debug("* Skipping step [" + page + "] in workflow (been there already...)");
                    after_current = true;
                } else if (!checkIsAccessible(page, PageRequestStatus.WORKFLOW)) {
                    LOG.debug("* Skipping step [" + page + "] in workflow (state is not accessible...)");
                    break;
                } else {
                    LOG.debug("* Workflow is at step " + i + ": [" + page + "]");
                    boolean needsdata;
                    if (after_current && currentpageflow.pageIsClearingPoint(page)) {
                        LOG.debug("=> [" + page + "]: Workflow wants to stop, getting document now.");
                        currentpagerequest = page;
                        page.setStatus(PageRequestStatus.WORKFLOW);
                        resdoc             = documentFromCurrentStep();
                        document           = resdoc.getSPDocument();
                        if (document == null) {
                            throw new XMLException("*** FATAL: [" + page + "] returns a 'null' SPDocument! ***");
                        }
                        LOG.debug("* [" + page + "] returned document => show it.");
                        break;
                    } else if (checkNeedsData(page, PageRequestStatus.WORKFLOW)) {
                        LOG.debug("=> [" + page + "]: needsData() returned TRUE, leaving workflow and getting document now.");
                        currentpagerequest = page;
                        page.setStatus(PageRequestStatus.WORKFLOW);
                        resdoc             = documentFromCurrentStep();
                        document           = resdoc.getSPDocument();
                        if (document == null) {
                            throw new XMLException("*** FATAL: [" + page + "] returns a 'null' SPDocument! ***");
                        }
                        LOG.debug("* [" + page + "] returned document => show it.");
                        break;
                    } else {
                        LOG.debug("=> [" + page + "]: Workflow doesn't want to stop and needsData() returned FALSE");
                        LOG.debug("=> [" + page + "]: going to next step in workflow.");
                    }
                }
            }
            if (document == null) {
                PageRequest finalpage = currentpageflow.getFinalPage();
                if (finalpage == null) {
                    throw new XMLException("*** Reached end of Workflow '" + currentpageflow.getName() + "' " +
                                           "with neither getting a non-null SPDocument or having a FINAL page defined ***");
                } else if (!checkIsAccessible(finalpage, PageRequestStatus.FINAL)) {
                    throw new XMLException("*** Reached end of Workflow '" + currentpageflow.getName() + "' " +
                                           "but FINAL page [" + finalpage + "] is inaccessible ***");
                } else {
                    currentpagerequest = finalpage;
                    currentpageflow    = pageflowmanager.pageFlowToPageRequest(currentpageflow, finalpage);
                    finalpage.setStatus(PageRequestStatus.FINAL);
                    resdoc             = documentFromCurrentStep();
                    document           = resdoc.getSPDocument();
                    if (document == null) {
                        throw new XMLException("*** FATAL: " + finalpage + " returns a 'null' SPDocument! ***");
                    }
                }
            }
        } else {
            throw new XMLException("*** ERROR! *** current Stateflow == null!");
        }
        return document;
    }

    /**
     * <code>documentFromCurrentStep</code> handles how to get a SPDocument from the state
     * that is associated (via the properties) to the current PageRequest.
     *
     * @param req a <code>HttpServletRequest</code> value
     * @param skip_on_inaccessible a <code>boolean</code> value
     * @return a <code>SPDocument</code> value
     * @exception Exception if an error occurs
     */
    private ResultDocument documentFromCurrentStep() throws Exception {
        State state = pagemap.getState(currentpagerequest);
        if (state == null) {
            LOG.warn("* Can't get a handling state for page " + currentpagerequest);
            ResultDocument resdoc = new ResultDocument();
            SPDocument     spdoc  = resdoc.getSPDocument();
            spdoc.setResponseError(HttpServletResponse.SC_NOT_FOUND);
            return resdoc;
        }
        
        LOG.debug("** [" + currentpagerequest + "]: associated state: " + state.getClass().getName());
        LOG.debug("=> [" + currentpagerequest + "]: Calling getDocument()");
        return state.getDocument(this, currentpreq);
    }

    private void trySettingPageRequestAndFlow() {
        PageRequest page = new PageRequest(currentpreq); 
        if (!page.isEmpty() && (authpage == null || !page.equals(authpage))) {
            page.setStatus(PageRequestStatus.DIRECT);
            currentpagerequest = page;
            // initialpagerequest = page;
            currentpageflow    = pageflowmanager.pageFlowToPageRequest(currentpageflow, page, currentpreq);
            LOG.debug("* Setting currentpagerequest to [" + page + "]");
            LOG.debug("* Setting currentpageflow to [" + currentpageflow.getName() + "]");
        } else {
            page = currentpagerequest;
            // initialpagerequest = page;
            if (page != null) {
                page.setStatus(PageRequestStatus.DIRECT);
                LOG.debug("* Reusing page [" + page + "]");
                LOG.debug("* Reusing flow [" + currentpageflow.getName() + "]");
            } else {
                throw new RuntimeException("Don't have a current page to use as output target");
            }
        }
    }

    private void addNavigation(Navigation navi, SPDocument spdoc) throws Exception {
        long     start   = System.currentTimeMillis();
        Document doc     = spdoc.getDocument();
        Element  element = doc.createElement("navigation");
        doc.getDocumentElement().appendChild(element);
        
        StringBuffer debug_buffer = new StringBuffer();
        StringBuffer warn_buffer  = new StringBuffer();
        
        if (autoinvalidate_navi) {
            LOG.debug("=> Add new navigation.");
            currentpreq.startLogEntry();
            recursePages(navi.getNavigationElements(), element, doc, null, warn_buffer, debug_buffer);
            currentpreq.endLogEntry("CREATE_NAVI_COMPLETE", 25);
        } else {
            if (navigation_visible != null) {
                LOG.debug("=> Reuse old navigation.");
            } else {
                LOG.debug("=> Add new navigation (has been invalidated).");
            }
            currentpreq.startLogEntry();
            recursePages(navi.getNavigationElements(), element, doc, navigation_visible, warn_buffer, debug_buffer);
            currentpreq.endLogEntry("CREATE_NAVI_REUSE", 2);
        }
    }

    private void recursePages(NavigationElement[] pages, Element parent,  Document doc,
                              HashMap vis_map, StringBuffer warn_buffer, StringBuffer debug_buffer) throws Exception {
        for (int i = 0; i < pages.length; i++) {
            NavigationElement page     = pages[i];
            String            name     = page.getName();
            PageRequest       pagereq  = new PageRequest(name);
            Element           pageelem = doc.createElement("page");
            
            parent.appendChild(pageelem);
            pageelem.setAttribute("name", name);
            pageelem.setAttribute("handler", page.getHandler());

            Integer page_vis = null;
            if (vis_map != null) {
                page_vis = (Integer) vis_map.get(page);
            }

            if (page_vis != null) {
                pageelem.setAttribute("visible", "" + page_vis.intValue());
            } else {
                if (preqprops.pageRequestIsDefined(pagereq)) {
                    if (checkIsAccessible(pagereq,PageRequestStatus.NAVIGATION)) {
                        pageelem.setAttribute("visible", "1");
                        if (vis_map != null) {
                            vis_map.put(page, new Integer(1));
                        }
                    } else {
                        pageelem.setAttribute("visible", "0");
                        if (vis_map != null) {
                            vis_map.put(page, new Integer(0));
                        }
                    }
                } else {
                    pageelem.setAttribute("visible", "-1");
                    if (vis_map != null) {
                        vis_map.put(page, new Integer(-1));
                    }
                }
                if (page.hasChildren()) {
                    recursePages(page.getChildren(), pageelem, doc, vis_map, warn_buffer, debug_buffer);
                }
            }
        }
    }
    
    /**
     * <code>toString</code> tries to give a detailed printed representation of the Context.
     * WARNING: this may be very long!
     *
     * @return a <code>String</code> value
     */
    public String toString() {
        StringBuffer contextbuf = new StringBuffer("\n");
	
        contextbuf.append("     workflow:      " + currentpageflow  + "\n");
        contextbuf.append("     PageRequest:   " + currentpagerequest + "\n"); 
        if (currentpagerequest != null) { 
            contextbuf.append("       -> State: " + pagemap.getState(currentpagerequest) + "\n");
            contextbuf.append("       -> Status: " + currentpagerequest.getStatus() + "\n");
        }
        contextbuf.append("     >>>> ContextResourcen <<<<\n");
        for (Iterator i = rmanager.getResourceIterator(); i.hasNext(); ) {
            ContextResource res = (ContextResource) i.next();
            contextbuf.append("         " + res.getClass().getName() + ": ");
            contextbuf.append(res.toString() + "\n");
        }
        
        return contextbuf.toString();
    }

    public String getName() {
        return name;
    }

    public void startLogEntry() {
        currentpreq.startLogEntry();
    }
    
    public void endLogEntry(String info, long min) {
        currentpreq.endLogEntry(info, min);
    }

    ///**
    // * <code>flowIsStopped</code> can be called from inside a {@link de.schlund.pfixcore.workflow.State State}
    // * It returned true if the Context is forced to stop at a page of the running workflow
    // * (this happens when the pageflow returns true on calling flow.getStopAtFirstAfterCurrent().
    // * The pageflow should not advance past the first accessible page AFTER the current page, in other words:
    // * the first accessible page after the current page should work exactly the same as if directly
    // * calling it).
    // *
    // * @return a <code>boolean</code> value
    // */
    // public boolean flowIsStopped() {
    //     if (getCurrentPageRequest().getStatus() == PageRequestStatus.WORKFLOW_STOP) {
    //         return true;
    //     } else {
    //         return false;
    //     }
    // }

    ///**
    // * <code>getPageFlowManager</code> returns the PageFlowManager defined in init(Properties properties)
    // *
    // * @return a <code>PageFlowManager</code> value
    // */
    //protected PageFlowManager getPageFlowManager() {
    //    return pageflowmanager;
    //}

    ///**
    // * <code>getPageMap</code> returns the PageMap defined in init(Properties properties)
    // *
    // * @return a <code>PageMap</code> value
    // */
    //protected PageMap getPageMap() {
    //    return pagemap;
    //}
    
    ///**
    // * <code>setCurrentPageFlow</code> sets the currently active PageFlow.
    // *
    // * @param flow a <code>PageFlow</code> value
    // */
    //protected void setCurrentPageFlow(PageFlow flow) {
    //    currentpageflow = flow;
    //}

    ///**
    // * <code>setCurrentPageRequest</code> sets the currently active PageRequest.
    // *
    // * @param page a <code>PageRequest</code> value
    // */
    //protected void setCurrentPageRequest(PageRequest page) {
    //    currentpagerequest = page;
    //}

    ///**
    // * <code>getInitialPageRequest</code> returns the PageRequest that was called requested initially
    // * for this request cycle. The current pagerequest as returned by getCurrentPageRequest() may be differnt for example
    // * while querying all pages for the navigation or during a pageflow run when one page of the flow after the other is
    // * queried if it want's to show itself.
    // *
    // * @return a <code>PageRequest</code> value
    // */
    //public PageRequest getInitialPageRequest() {
    //    return initialpagerequest;
    //}

    ///**
    // * <code>getCurrentPageFlow</code> returnes the currently active PageFlow.
    // *
    // * @return a <code>PageFlow</code> value
    // */
    //public PageFlow getCurrentPageFlow() {
    //    return currentpageflow;
    //}

}
