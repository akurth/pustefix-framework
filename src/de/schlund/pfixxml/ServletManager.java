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




import de.schlund.pfixxml.exceptionhandler.ExceptionHandler;
import de.schlund.pfixxml.exceptionprocessor.ExceptionConfig;
import de.schlund.pfixxml.exceptionprocessor.ExceptionProcessor;
import de.schlund.pfixxml.loader.AppLoader;
import de.schlund.pfixxml.serverutil.SessionAdmin;
import de.schlund.pfixxml.serverutil.SessionHelper;
import de.schlund.pfixxml.serverutil.SessionInfoStruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import org.apache.log4j.Category;

/**
 * ServletManager.java
 *
 *
 * Created: Wed May  8 16:39:06 2002
 *
 * @author <a href="mailto:jtl@schlund.de">Jens Lautenbacher</a>
 * @version $Id$
 */

public abstract class ServletManager extends HttpServlet {

    public  static final String STORED_REQUEST     = "__STORED_PFIXSERVLETREQUEST__";
    public  static final String SESSION_IS_SECURE  = "__SESSION_IS_SECURE__";
    public  static final String VISIT_ID           = "__VISIT_ID__";
    public  static final String SESSION_ID_URL     = "__SESSION_ID_URL__";
    public  static final String PARAM_FORCELOCAL   = "__forcelocal";
    public  static final String PROP_LOADINDEX     = "__PROPERTIES_LOAD_INDEX";
    public  static final String DEF_CONTENT_TYPE   = "text/html; charset=iso-8859-1";
    private static final String SECURE_SESS_COOKIE = "__PFIX_SECURE_SSL_SESS__";
    private static final String TEST_COOKIE        = "__PFIX_TEST__";
    private static final String SESSID_COOKIE      = "__PFIX_CURRENT_SESS__";
    public  static final String CHECK_FOR_RUNNING_SSL_SESSION = "__CHECK_FOR_RUNNING_SSL_SESSION__";
    private static final String PROP_EXCEPTION     = "exception";
    private static       String TIMESTAMP_ID       = "";
    private static       int    INC_ID             = 0;

    private SessionAdmin     sessionadmin     = SessionAdmin.getInstance();
    private Category         LOGGER_VISIT     = Category.getInstance("LOGGER_VISIT");
    private Category         CAT              = Category.getInstance(ServletManager.class);
    private ExceptionHandler xhandler         = ExceptionHandler.getInstance();
    private Map              exceptionConfigs = new Hashtable();
    private long             common_mtime     = 0;
    private long             servlet_mtime    = 0;
    private long             loadindex        = 0;
    private Properties       properties;
    private File             commonpropfile;
    private File             servletpropfile;

    protected Properties getProperties() {
        return properties;
    }

    protected boolean runningUnderSSL(HttpServletRequest req) {
        return req.getScheme().equals("https") && (isSslPort(req.getServerPort()));
    }

    protected boolean needsSSL(PfixServletRequest preq) throws ServletException {
        String needs_ssl = properties.getProperty("servlet.needsSSL");
        if (needs_ssl != null && (needs_ssl.equals("true") || needs_ssl.equals("yes") || needs_ssl.equals("1"))) {
            return true;
        } else {
            return false;
        }
    }

    abstract protected boolean needsSession();
    abstract protected boolean allowSessionCreate();

    protected void relocate(HttpServletResponse res, String reloc_url) {
        CAT.debug("\n\n        ======> relocating to " + reloc_url + "\n");
        res.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
        res.setHeader("Pragma", "no-cache");
        res.setHeader("Cache-Control", "no-cache, no-store, private, must-revalidate");
        res.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        res.setHeader("Location", reloc_url);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (CAT.isDebugEnabled()) {
            CAT.debug("\n ------------------- Start of new Request ---------------");
            CAT.debug("====> Scheme://Server:Port " + req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort());
            CAT.debug("====> URI:   " + req.getRequestURI());
            CAT.debug("====> Query: " + req.getQueryString());
            CAT.debug("----> needsSession=" + needsSession() + " allowSessionCreate=" + allowSessionCreate());
            CAT.debug("====> Sessions: " + SessionAdmin.getInstance().toString());

            Cookie[] cookies = req.getCookies();
            if (cookies != null) {
                for (int i = 0; i < cookies.length; i++) {
                    Cookie tmp = cookies[i];
                    CAT.debug(">>>>> Cookie: " + tmp.getName() + " -> " + tmp.getValue());
                }
            }
        }

        //if AppLoader is enabled and currently doing a reload, block request until reloading is finished
        AppLoader loader = AppLoader.getInstance();
        if (loader.isEnabled()) {
            while (loader.isLoading()) {
                try {
                    Thread.sleep(100);
                } catch(InterruptedException x) {
                    //
                }
            }
        }
        HttpSession session                  = null;
        boolean     has_session              = false;
        boolean     has_ssl_session_insecure = false;
        boolean     has_ssl_session_secure   = false;
        boolean     force_jump_back_to_ssl   = false;
        boolean     force_reuse_visit_id     = false;
        boolean     does_cookies             = doCookieTest(req, res);
        if (req.isRequestedSessionIdValid()) {
            session     = req.getSession(false);
            has_session = true;
            CAT.debug("*** Found valid session with ID " + session.getId());
            if (runningUnderSSL(req)) {
                CAT.debug("*** Found running under SSL");
                Boolean secure = (Boolean)session.getAttribute(SESSION_IS_SECURE);
                if (secure != null && secure.booleanValue()) {
                    CAT.debug("    ... and session is secure.");
                    has_ssl_session_secure = true;
                } else {
                    CAT.debug("    ... but session is insecure!");
                    has_ssl_session_insecure = true;
                }
            }
        } else if (req.getRequestedSessionId() != null) {
            CAT.debug("*** Found old and invalid session in request");
            // We have no valid session, but the request contained an invalid session id.
            // case a) This may be an invalid id because we invalidated the session when jumping
            // into the secure SSL session (see redirectToSecureSSLSession below). by using the back button
            // of the browser, the user may have come back to a (non-ssl) page (in his browser history) that contains
            // links with the old "parent" session id embedded. We need to check for this and create a
            // new session but reuse the visit id of the currently running SSL session.
            if (!runningUnderSSL(req) && SessionAdmin.getInstance().idWasParentSession(req.getRequestedSessionId())) {
                CAT.debug("    ... but this session was the parent of a currently running secure session.");
                // We'll try to get back there securely by first jumping back to a new (insecure) SSL session,
                // and after that the the jump to the secure SSL session will not create a new one, but reuse
                // the already running secure session instead (but only if a secure cookie can identify the request as
                // coming from the browser that made the initial jump http->https).
                if (does_cookies) {
                    force_jump_back_to_ssl = true;
                } else {
                    // OK, it seems as if we will not be able to identify the peer by comparing cookies.
                    // So the only thing we can do is to reuse the VISIT_ID.
                    force_reuse_visit_id = true;
                }
            } else {
                // Normally the balancer (or, more accurate: mod_jk) has a chance to choose the right server for a 
                // new session, but with a session id in the URL it wasn't able to. So we redirect to a "fresh" request 
                // without _any_ id, giving the balancer the possibility to choose a different server. (this can be 
                // overridden by supplying the parameter __forcelocal=1 to the request). All this makes only sense of 
                // course if we are running in a cluster of servers behind a balancer that chooses the right server
                // based on the session id included in the URL.  This redirect is important when switching from "A" to
                // "B" machines
                String forcelocal = req.getParameter(PARAM_FORCELOCAL);
                if (forcelocal != null && (forcelocal.equals("1") || forcelocal.equals("true") || forcelocal.equals("yes"))) {
                    CAT.debug("    ... but found __forcelocal parameter to be set.");
                } else {
                    CAT.debug("    ... and __forcelocal is NOT set.");
                    redirectToClearedRequest(req, res);
                    return;
                    // End of request cycle.
                }
            }
        }

        PfixServletRequest preq = null;
        if (has_session) {
            preq = (PfixServletRequest) session.getAttribute(STORED_REQUEST);
            if (preq != null) {
                CAT.debug("*** Found old PfixServletRequest object in session");
                session.removeAttribute(STORED_REQUEST);
                preq.updateRequest(req);
            }
        }
        if (preq == null) {
            CAT.debug("*** Creating PfixServletRequest object.");
            preq = new PfixServletRequest(req, properties);
        }

        tryReloadProperties(preq);

        // End of initialization. Now we handle all cases where we need to redirect.

        if (force_jump_back_to_ssl && allowSessionCreate()) {
            forceRedirectBackToInsecureSSL(preq, req, res);
            return;
            // End of request cycle.
        }
        if (force_reuse_visit_id && allowSessionCreate()) {
            forceNewSessionSameVisit(preq, req, res);
            return;
            // End of request cycle.
        }
        if (has_ssl_session_insecure) {
            redirectToSecureSSLSession(preq, req, res);
            return;
            // End of request cycle.
        }
        if (needsSession() && allowSessionCreate() && needsSSL(preq) && !has_ssl_session_secure) {
            redirectToInsecureSSLSession(preq, req, res);
            return;
            // End of request cycle.
        }
        if (!has_session && needsSession() && allowSessionCreate() && !needsSSL(preq)) {
            redirectToSession(preq, req, res);
            return;
            // End of request cycle.
        }
        if (!has_session && !needsSession() && needsSSL(preq) && !runningUnderSSL(req)) {
            redirectToSSL(req, res);
            return;
            // End of request cycle.
        }

        CAT.debug("*** >>> End of redirection management, handling request now.... <<< ***\n");


        //preq.initPerfLog();
        preq.startLogEntry();
        callProcess(preq, req, res);
        PerfEventType et = PerfEventType.XMLSERVER_CALLPROCESS;
        //et.setMessage(preq);
        preq.endLogEntry(et);
        //preq.endLogEntry("CALLPROCESS", 0);
        preq.printLog();
    }

    private void redirectToClearedRequest(HttpServletRequest req, HttpServletResponse res) {
        CAT.debug("===> Redirecting to cleared Request URL");
        String redirect_uri = SessionHelper.getClearedURL(req.getScheme(), req.getServerName(), req);
        relocate(res, redirect_uri);
    }

    private void redirectToSSL(HttpServletRequest req, HttpServletResponse res) {
        CAT.debug("===> Redirecting to session-less request URL under SSL");
        String redirect_uri = SessionHelper.getClearedURL("https", req.getServerName(), req);
        relocate(res, redirect_uri);
    }

    private void redirectToSecureSSLSession(PfixServletRequest preq, HttpServletRequest req, HttpServletResponse res) {
        HttpSession session  = req.getSession(false);
        String      visit_id = (String) session.getAttribute(VISIT_ID);
        String      parentid = (String) session.getAttribute(CHECK_FOR_RUNNING_SSL_SESSION);
        if (parentid != null && !parentid.equals("")) {
            CAT.debug("*** The current insecure SSL session says to check for a already running SSL session for reuse");
            HttpSession secure_session = SessionAdmin.getInstance().getChildSessionForParentId(parentid);
            if (secure_session != null) {
                String secure_id = secure_session.getId();
                CAT.debug("*** We have found a candidate: SessionId=" + secure_id + " now search for cookie...");
                // But we need to make sure that the current request comes
                // from the same user who created this secure session.
                // We do this by checking for a (secure) cookie with a corresponding session id.
                Cookie cookie = getSecureSessionCookie(req);
                if (cookie != null) {
                    CAT.debug("*** Found a matching cookie ...");
                    if (cookie.getValue().equals(secure_id)) {
                        CAT.debug("   ... and the value is correct!");
                        CAT.debug("==> Redirecting to the secure SSL URL with the already running secure session " + secure_id);
                        String redirect_uri = SessionHelper.encodeURL("https", req.getServerName(), req,  secure_id);
                        relocate(res, redirect_uri);
                        return;
                    } else {
                        CAT.debug("   ... but the value is WRONG!");
                        // throw new RuntimeException("Wrong Session-ID for running secure session from cookie.");
                        CAT.error("Wrong Session-ID for running secure session from cookie.");
                    }
                } else {
                    CAT.debug("*** NO matching SecureSessionCookie (not even a wrong one...)");
                }
            }
        }

        CAT.debug("*** Saving session data...");
        HashMap map = new HashMap();
        SessionHelper.saveSessionData(map, session);
        // Before we invalidate the current session we save the traillog
        SessionInfoStruct infostruct = SessionAdmin.getInstance().getInfo(session);
        LinkedList        traillog   = new LinkedList();
        String            old_id     = session.getId();
        if (infostruct != null) {
            traillog = SessionAdmin.getInstance().getInfo(session).getTraillog();
        } else {
            CAT.warn("*** Infostruct == NULL ***");
        }

        CAT.debug("*** Invalidation old session (Id: " + old_id + ")");
        session.invalidate();
        session = req.getSession(true);
        // First of all we put the old session id into the new session (__PARENT_SESSION_ID__)
        session.setAttribute(SessionAdmin.PARENT_SESS_ID, old_id);
        if (visit_id != null) {
            // Don't call this.registerSession(...) here. We don't want to log this as a different visit.
            // Now we register the new session with saved traillog
            SessionAdmin.getInstance().registerSession(session, traillog);
        } else {
            // Register a new session now.
            registerSession(req, session);
        }
        CAT.debug("*** Got new Session (Id: " + session.getId() + ")");
        CAT.debug("*** Copying data back to new session");
        SessionHelper.copySessionData(map, session);
        CAT.debug("*** Setting ContainerUtil.SESSION_ID_URL to " +  session.getAttribute(SessionHelper.SESSION_ID_URL));
        session.setAttribute(SessionHelper.SESSION_ID_URL, SessionHelper.getURLSessionId(req));
        CAT.debug("*** Setting SECURE flag");
        session.setAttribute(SESSION_IS_SECURE, Boolean.TRUE);
        session.setAttribute(STORED_REQUEST, preq);

        Cookie cookie = getSecureSessionCookie(req);
        if (cookie != null) {
            cookie.setMaxAge(0);
            res.addCookie(cookie);
        }
        cookie = new Cookie(SECURE_SESS_COOKIE, session.getId());
        cookie.setPath("/");
        cookie.setMaxAge(-1);
        cookie.setSecure(true);
        res.addCookie(cookie);

        CAT.debug("===> Redirecting to secure SSL URL with session (Id: " + session.getId() + ")");
        String redirect_uri = SessionHelper.encodeURL("https", req.getServerName(), req);
        relocate(res, redirect_uri);
    }

    private void redirectToInsecureSSLSession(PfixServletRequest preq, HttpServletRequest req, HttpServletResponse res) {
        boolean reuse_session = false;
        if (req.isRequestedSessionIdValid()) {
            reuse_session = true;
            CAT.debug("*** reusing existing session for jump http=>https");
        }
        HttpSession session = req.getSession(true);
        if (!reuse_session) {
            registerSession(req, session);
        }
        session.setAttribute(SessionHelper.SESSION_ID_URL,SessionHelper.getURLSessionId(req));
        CAT.debug("*** Setting INSECURE flag in session (Id: " + session.getId() + ")");
        session.setAttribute(SESSION_IS_SECURE, Boolean.FALSE);
        session.setAttribute(STORED_REQUEST, preq);
        CAT.debug("===> Redirecting to insecure SSL URL with session (Id: " + session.getId() + ")");
        String redirect_uri = SessionHelper.encodeURL("https", req.getServerName(), req);
        relocate(res, redirect_uri);
    }

    private void forceRedirectBackToInsecureSSL(PfixServletRequest preq, HttpServletRequest req, HttpServletResponse res) {
        // When we come here, we KNOW that there's a secure SSL session already running, so this session here is
        // only used for the jump to SSL so we can get the cookie to check the identity of the caller.
        String      parentid      = req.getRequestedSessionId();
        HttpSession session       = req.getSession(true);
        // registerSession(req, session);
        session.setAttribute(SessionHelper.SESSION_ID_URL, SessionHelper.getURLSessionId(req));
        session.setAttribute(CHECK_FOR_RUNNING_SSL_SESSION, parentid);
        CAT.debug("*** Setting INSECURE flag in session (Id: " + session.getId() + ")");
        session.setAttribute(SESSION_IS_SECURE, Boolean.FALSE);
        session.setAttribute(STORED_REQUEST, preq);
        CAT.debug("===> Redirecting to SSL URL with session (Id: " + session.getId() + ")");
        String redirect_uri = SessionHelper.encodeURL("https", req.getServerName(), req);
        relocate(res, redirect_uri);
    }

    private void forceNewSessionSameVisit(PfixServletRequest preq, HttpServletRequest req, HttpServletResponse res) {
        // When we come here, we KNOW that there's a secure SSL session already running, but unfortunately
        // it seems that the browser doesn't send cookies. So we will not be able to know for sure that the request comes
        // from the legitimate user. The only thing we can do is to copy the VISIT_ID, which helps to keep the
        // statistic clean :-)
        String      parentid      = req.getRequestedSessionId();
        HttpSession child         = SessionAdmin.getInstance().getChildSessionForParentId(parentid);
        String      curr_visit_id = (String) child.getAttribute(VISIT_ID);
        HttpSession session       = req.getSession(true);
        LinkedList  traillog      = SessionAdmin.getInstance().getInfo(child).getTraillog();
        session.setAttribute(SessionHelper.SESSION_ID_URL, SessionHelper.getURLSessionId(req));
        session.setAttribute(VISIT_ID, curr_visit_id);
        SessionAdmin.getInstance().registerSession(session, traillog);
        CAT.debug("===> Redirecting with session (Id: " + session.getId() + ") using OLD VISIT_ID: " + curr_visit_id);
        session.setAttribute(STORED_REQUEST, preq);
        String redirect_uri = SessionHelper.encodeURL(req.getScheme(), req.getServerName(), req);
        relocate(res, redirect_uri);
    }

    private void redirectToSession(PfixServletRequest preq, HttpServletRequest req, HttpServletResponse res) {
        HttpSession session = req.getSession(true);
        session.setAttribute(SessionHelper.SESSION_ID_URL, SessionHelper.getURLSessionId(req));
        registerSession(req, session);
        CAT.debug("===> Redirecting to URL with session (Id: " + session.getId() + ")");
        session.setAttribute(STORED_REQUEST, preq);
        String redirect_uri = SessionHelper.encodeURL(req.getScheme(), req.getServerName(), req);
        relocate(res, redirect_uri);
    }

    private boolean doCookieTest(HttpServletRequest req, HttpServletResponse res) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            return true;
        } else {
            Cookie probe = new Cookie(TEST_COOKIE, "TRUE");
            probe.setPath("/");
            res.addCookie(probe);
            return false;
        }
    }

    private Cookie getSecureSessionCookie(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        Cookie   tmp;
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                tmp = cookies[i];
                if (tmp.getName().equals(SECURE_SESS_COOKIE))
                    return tmp;
            }
        }
        return null;
    }

    private void registerSession(HttpServletRequest req, HttpSession session) {
        if (session != null) {
            synchronized (TIMESTAMP_ID) {
                SimpleDateFormat sdf       = new SimpleDateFormat("yyyyMMddHHmmss");
                String           timestamp = sdf.format(new Date());
                NumberFormat     nf        = NumberFormat.getInstance();
                nf.setMinimumIntegerDigits(3);

                if (timestamp.equals(TIMESTAMP_ID)) {
                    INC_ID++;
                } else {
                    TIMESTAMP_ID = timestamp;
                    INC_ID       = 0;
                }
                if (INC_ID >= 1000) {
                    CAT.warn("*** More than 999 connects/sec! ***");
                }
                String sessid = session.getId();
                String mach   = "";
                if (sessid.lastIndexOf(".") > 0) {
                    mach = sessid.substring(sessid.lastIndexOf("."));
                }
                session.setAttribute(VISIT_ID, TIMESTAMP_ID + "-" + nf.format(INC_ID) + mach);
            }
            StringBuffer logbuff = new StringBuffer();
            logbuff.append(session.getAttribute(VISIT_ID) + "|" + session.getId() + "|");
            logbuff.append(req.getServerName() + "|" + req.getRemoteAddr() + "|" + req.getHeader("user-agent") + "|");
            if (req.getHeader("referer") != null) {
                logbuff.append(req.getHeader("referer"));
            }
            logbuff.append("|");
            if (req.getHeader("accept-language") != null) {
                logbuff.append(req.getHeader("accept-language"));
            }
            LOGGER_VISIT.warn(logbuff.toString());
            SessionAdmin.getInstance().registerSession(session);
        }
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        properties = new Properties(System.getProperties());

        ServletContext ctx=config.getServletContext();
        CAT.debug("*** Servlet container is '"+ctx.getServerInfo()+"'");
        int major=ctx.getMajorVersion();
        int minor=ctx.getMinorVersion();
        if((major==2 && minor>=3)||(major>2)) {
            CAT.warn("*** Servlet container with support for Servlet API "+major+"."+minor+" detected");
        } else {
            throw new ServletException("*** Can't detect servlet container with support for Servlet API 2.3 or higher");
        }

        String commonpropfilename = config.getInitParameter("servlet.commonpropfile");
        if (commonpropfilename != null) {
            commonpropfile = new File(commonpropfilename);
            common_mtime = loadPropertyfile(properties, commonpropfile);
        }

        String servletpropfilename = config.getInitParameter("servlet.propfile");
        if (servletpropfilename != null) {
            servletpropfile = new File(servletpropfilename);
            servlet_mtime   = loadPropertyfile(properties, servletpropfile);
        }
        loadindex = 0;
        properties.setProperty(PROP_LOADINDEX, "" + loadindex);

        initExceptionConfigs();
    }

    protected boolean tryReloadProperties(PfixServletRequest preq) throws ServletException {
        if ((commonpropfile  != null && commonpropfile.lastModified()  > common_mtime) ||
            (servletpropfile != null && servletpropfile.lastModified() > servlet_mtime)) {
            loadindex++;

            CAT.warn("\n\n##############################\n" +
                     "#### Reloading properties ####\n" +
                     "##############################\n");
            properties.clear();
            
            if (commonpropfile != null) {
                common_mtime = loadPropertyfile(properties, commonpropfile);
            }
            if (servletpropfile != null) {
                servlet_mtime = loadPropertyfile(properties, servletpropfile);
            }
            properties.setProperty(PROP_LOADINDEX, "" + loadindex);
            return true;
        } else {
            return false;
        }

    }

    private long loadPropertyfile(Properties props, File propfile) throws ServletException {
        long mtime;
        try {
            mtime = propfile.lastModified();
            props.load(new FileInputStream(propfile));
        } catch (FileNotFoundException e) {
            throw new ServletException("*** [" + propfile.getName() + "] Not found: " + e.toString());
        } catch (IOException e) {
            throw new ServletException("*** [" + propfile.getName() + "] IO-error: " + e.toString());
        }
        return mtime;
    }

    private void callProcess(PfixServletRequest preq, HttpServletRequest req,
                             HttpServletResponse res) throws ServletException, IOException {
        try {
            res.setContentType(DEF_CONTENT_TYPE);
            process(preq, res);
        } catch (Throwable e) {
        	CAT.error("Exception in process", e);
        	ExceptionConfig exconf = getExceptionConfigForThrowable(e);
        	
        	if(exconf != null && exconf.getProcessor()!= null) { 
        		if ( preq.getLastException() != null ) {
                    return;
        		}
        		ExceptionProcessor eproc = exconf.getProcessor();
        		eproc.processException(e, exconf, preq,
                        getServletConfig().getServletContext(),
                        req, res, properties);

        	} else {
        		// This is the default case when no
        		// exceptionprocessors are defined.
        		xhandler.handle(e, preq, properties);
        	}
            throw new ServletException("callProcess failed", e);
        }
    }

    /**
     * 
     * @return null if no processor is responsible for the passed throwable
     * @throws ServletException
     * @throws ClassNotFoundException
     */
    private ExceptionConfig getExceptionConfigForThrowable(Throwable th) throws ServletException {
        for(Iterator iter = exceptionConfigs.keySet().iterator(); iter.hasNext(); ) {
            String key = (String) iter.next();
            //TODO: avoid frequent creation of temporary class objects 
            Class clazz;
            try {
                clazz = Class.forName(key);
            } catch (ClassNotFoundException e) {
                CAT.error(e);
                throw new ServletException("Unable to instantiate class: "+key, e);
            }
            if(clazz.isInstance(th)) {
                // bingo
                return (ExceptionConfig) exceptionConfigs.get(key);
            }
        }    
        return null;
    }
    
    /**
     *
     */
    /*private void processException(ExceptionProcessor proc, PfixServletRequest preq, HttpServletRequest req,
                                  HttpServletResponse res, Throwable exception)
                           throws ServletException, IOException {
        // make sure, that if the current request is already the result of a
        // forwarded request, that no more forwarding takes place in order
        // to prevent an infinite loop

        if ( preq.getLastException() != null )
            return;

        ExceptionConfig conf = 
        proc.processException(exception, exConfig, preq,
                                       getServletConfig().getServletContext(),
                                       req, res);
        
    }*/

    /**
     * This method uses all properties prefixed with 'exception' to build ExceptionConfig
     * objects, which are then stored in the exConfig-Map, keyed by the type attribute
     * of the ExceptionConfig
     * @exception ServletException if the exception configuration defined in the properties
     * is somehow invalid
     */
    private void initExceptionConfigs() throws ServletException {
        Map tmpExConf = new HashMap();
        int len = PROP_EXCEPTION.length();

    	Enumeration props = properties.propertyNames();
    	while( props.hasMoreElements()) {
        	String propName = (String) props.nextElement();

        	if ( propName.startsWith(PROP_EXCEPTION) ) {
              String propValue = properties.getProperty(propName);

              StringTokenizer tokenizer = new StringTokenizer(propName, ".");
              if ( tokenizer.countTokens() < 3 )
                  throw new ServletException("Exception configuration has wrong format: "+ propName);

              tokenizer.nextToken();
              String exConfName = tokenizer.nextToken();
              ExceptionConfig exConf = (ExceptionConfig) tmpExConf.get(exConfName);

              CAT.debug("Property found for exception processing: "+propName +"="+propValue);

              if ( exConf == null ) {
                  exConf = new ExceptionConfig();
                  tmpExConf.put(exConfName, exConf);
              }

              try {
                  String attrName = tokenizer.nextToken();
                  CAT.debug(attrName);
                  if ( "type".equals(attrName) ) {
                      exConf.setType(propValue);
                  } else if ( "forward".equals(attrName) ) {
                      exConf.setForward( Boolean.valueOf(propValue).booleanValue() );
                  } else if ( "page".equals(attrName) ) {
                      exConf.setPage(propValue);
                  } else if ( "processor".equals(attrName) ) {
                      Class procClass = Class.forName(propValue);
                      ExceptionProcessor exProc = (ExceptionProcessor) procClass.newInstance();
                      exConf.setProcessor(exProc);
                  }
              } catch (ClassCastException ex) {
                  throw new ServletException("INVALID CONF: Class "+propValue+" is not an instance of 'ExceptionProcessor'");
              } catch (IllegalAccessException ex) {
                  throw new ServletException("INVALID CONF: Can't create instance of class "+propName, ex);
              } catch (SecurityException ex) {
                  throw new ServletException("INVALID CONF: Can't create instance of class "+propName, ex);
              } catch (ClassNotFoundException ex) {
                  throw new ServletException("INVALID CONF: Can't create instance of class "+propName, ex);
              } catch (InstantiationException ex) {
                  throw new ServletException("INVALID CONF: Can't create instance of class "+propName, ex);
              }
        	}
    	}

        CAT.debug("Finished reading properties for Exception configuration! \n"+tmpExConf);

        exceptionConfigs.clear();

        // validate the ExceptionConfig-instances and save them, keyed by their type-attribute
        for(Iterator values = tmpExConf.values().iterator(); values.hasNext();) {
            ExceptionConfig exConfig = (ExceptionConfig) values.next();
            if ( exConfig.validate() == false )
                throw new ServletException("INVALID ExceptionConfig: \n"+ exConfig);
            else
                exceptionConfigs.put(exConfig.getType(), exConfig);
        }

        if ( CAT.isDebugEnabled() ) {
            CAT.debug("Complete ExceptionConfig is:");
            CAT.debug("\n"+ exceptionConfigs);
        }
    }


    protected abstract void process(PfixServletRequest preq, HttpServletResponse res) throws Exception;

    //--

    // TODO: replace constants - ask tomcat 
    public static final int HTTP_PORT = 80;
    public static final int APACHE_SSL_PORT = 443;
    public static final int TOMCAT_SSL_PORT = 8443;
    
    public static boolean isDefault(int port) {
        return port == HTTP_PORT || port == APACHE_SSL_PORT;
    }

    public static boolean isSslPort(int port) {
        return port == APACHE_SSL_PORT || port == TOMCAT_SSL_PORT;
    }
    
}// ServletManager
