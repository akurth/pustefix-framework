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


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Category;
import org.xml.sax.SAXException;

import de.schlund.pfixxml.config.ServletManagerConfig;
import de.schlund.pfixxml.config.XMLPropertiesUtil;
import de.schlund.pfixxml.exceptionhandler.ExceptionHandler;
import de.schlund.pfixxml.exceptionprocessor.ExceptionConfig;
import de.schlund.pfixxml.exceptionprocessor.ExceptionProcessor;
import de.schlund.pfixxml.loader.AppLoader;
import de.schlund.pfixxml.perflogging.PerfEvent;
import de.schlund.pfixxml.perflogging.PerfEventType;
import de.schlund.pfixxml.serverutil.SessionAdmin;
import de.schlund.pfixxml.serverutil.SessionHelper;
import de.schlund.pfixxml.serverutil.SessionInfoStruct;
import de.schlund.pfixxml.util.MD5Utils;

/**
 * ServletManager.java
 *
 *
 * Created: Wed May  8 16:39:06 2002
 *
 * @author <a href="mailto:jtl@schlund.de">Jens Lautenbacher</a>
 */

public abstract class ServletManager extends HttpServlet {
    public    static final String VISIT_ID                      = "__VISIT_ID__";
    public    static final String PROP_LOADINDEX                = "__PROPERTIES_LOAD_INDEX";
    private   static final String STORED_REQUEST                = "__STORED_PFIXSERVLETREQUEST__";
    private   static final String SESSION_ID_URL                = "__SESSION_ID_URL__";
    private   static final String SECURE_SESS_COOKIE            = "__PFIX_SEC_";
    private   static final String TEST_COOKIE                   = "__PFIX_TST_";
    private   static final String SESSION_COOKIES_MARKER        = "__COOKIES_USED_DURING_SESSION__";
    private   static final String NO_COOKIES_OVERRIDE           = "__NO_COOKIES_OVERRIDE__";
    private   static final String RAND_SESS_COOKIE_VALUE        = "__RAND_SESS_COOKIE_VALUE__";
    private   static final String CHECK_FOR_RUNNING_SSL_SESSION = "__CHECK_FOR_RUNNING_SSL_SESSION__";
    private   static final String PARAM_FORCELOCAL              = "__forcelocal";
    public    static final String PROP_COOKIE_SEC_NOT_ENFORCED  = "servletmanager.cookie_security_not_enforced";
    private   static final String PROP_EXCEPTION                = "exception";
    protected static final String DEF_CONTENT_TYPE              = "text/html";
    private   static final String DEFAULT_ENCODING              = "UTF-8";
    private   static final String SERVLET_ENCODING              = "servlet.encoding";
    private   static       String TIMESTAMP_ID                  = "";
    private   static       int    INC_ID                        = 0;

    //FIXME NS should be removed, is just here for legacy reasons, the real thing is in SessionAdmin
    public  static final String SESSION_IS_SECURE             = "__SESSION_IS_SECURE__";

    private boolean          cookie_security_not_enforced = false;
    private SessionAdmin     sessionadmin                 = SessionAdmin.getInstance();
    private Category         LOGGER_VISIT                 = Category.getInstance("LOGGER_VISIT");
    private Category         CAT                          = Category.getInstance(ServletManager.class);
    private ExceptionHandler xhandler                     = ExceptionHandler.getInstance();
    private Map              exceptionConfigs             = new Hashtable();
    private long             common_mtime                 = 0;
    private long             servlet_mtime                = 0;
    private long             loadindex                    = 0;
    private File             commonpropfile;
    private File             servletpropfile;
    private String           servletEncoding;
    private AtomicInteger    configLoadIndex              = new AtomicInteger(0);
    
    protected abstract ServletManagerConfig getServletManagerConfig();
    protected abstract void reloadServletConfig(File configFile, Properties globalProperties) throws ServletException;

//     protected Properties getProperties() {
//         return this.properties;
//     }
    
    protected boolean runningUnderSSL(HttpServletRequest req) {
        return req.getScheme().equals("https") && (isSslPort(req.getServerPort()));
    }

    protected boolean needsSSL(PfixServletRequest preq) throws ServletException {
        return this.getServletManagerConfig().isSSL();
    }

    abstract protected boolean needsSession();
    abstract protected boolean allowSessionCreate();

    public static String getServerName(HttpServletRequest req) {
        String forward = req.getHeader("X-Forwarded-Server");
        if (forward != null && !forward.equals("")) {
            return forward;
        } else {
            return req.getServerName();
        }
    }
    
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
        req.setCharacterEncoding(servletEncoding);
        res.setCharacterEncoding(servletEncoding);
        if (CAT.isDebugEnabled()) {
            CAT.debug("\n ------------------- Start of new Request ---------------");
            CAT.debug("====> Scheme://Server:Port " + req.getScheme() + "://" + getServerName(req) + ":" + req.getServerPort());
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

            CAT.debug("\n");
            Enumeration headers = req.getHeaderNames();
            while (headers.hasMoreElements()) {
                String header    = (String) headers.nextElement();
                String headerval = req.getHeader(header);
                CAT.debug("+++ Header: " + header + " -> " + headerval);
            }

        }

        //if AppLoader is enabled and currently doing a reload, block request until reloading is finished
        AppLoader loader = AppLoader.getInstance();
        if (loader.isEnabled()) {
            while (loader.isLoading()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException x) {
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
        String      mark_session_as_no_cookies = null;
        boolean     does_cookies               = false;
        
        if (req.isRequestedSessionIdValid()) {
            session        = req.getSession(false);
            does_cookies   = doCookieTest(req, res, session);
            has_session    = true;
            Boolean secure = (Boolean) session.getAttribute(SessionAdmin.SESSION_IS_SECURE);
            CAT.debug("*** Found valid session with ID " + session.getId());
            // Much of the advanced security depends on having cookies enabled.  We need to make
            // sure that this isn't defeated by just disabling cookies.  So we mark every session
            // whenever the client has cookies enabled, and don't allow further uses of this session
            // without cookies. BUT: having a valid session that has the attribute
            // __NO_COOKIES_OVERRIDE__ set, will be considered as not doing cookies at all. See
            // below where mark_session_as_no_cookies is set for the reason behind this.
            Boolean refuse_cookies = (Boolean) session.getAttribute(NO_COOKIES_OVERRIDE);
            if (refuse_cookies != null && refuse_cookies.booleanValue()) {
                CAT.debug("*** Session REFUSES to use cookies! ***");
                does_cookies = false;
            }
            if (!does_cookies) {
                CAT.debug("*** Client doesn't use cookies...");
                // We still need to check if the session itself thinks differently -
                // this happens e.g. when cookies are disabled in the middle of the session.
                Boolean need_cookies = (Boolean) session.getAttribute(SESSION_COOKIES_MARKER);
                if (need_cookies != null && need_cookies.booleanValue()) {
                    if (cookie_security_not_enforced) {
                        CAT.debug("    ... during the session cookies were ENABLED, " +
                                  "but will continue because of cookie_security_not_enforced " + session.getId());
                    } else {
                        CAT.debug("    ... but during the session cookies were already ENABLED: " +
                                  "Will invalidate the session " + session.getId());
                        session.invalidate();
                        has_session = false;
                    }
                } else {
                    CAT.debug("    ... and during the session cookies were DISABLED, too: Let's hope everything is OK...");
                }
            } else {
                CAT.debug("*** Client uses cookies.");
            }
            if (has_session) {
                if (runningUnderSSL(req)) {
                    CAT.debug("*** Found running under SSL");
                    if (secure != null && secure.booleanValue()) {
                        CAT.debug("    ... and session is secure.");
                        if (does_cookies) {
                            CAT.debug("*** Client does cookies: Double checking SSL cookie for session ID");
                            String sec_testid = (String) session.getAttribute(SECURE_SESS_COOKIE + MD5Utils.hex_md5(session.getId()));
                            CAT.debug("*** Session expects to see the cookie value " + sec_testid);
                            Cookie cookie = getSecureSessionCookie(req, session.getId());
                            if (cookie != null) {
                                CAT.debug("*** Found a matching cookie ...");
                                if (cookie.getValue().equals(sec_testid)) {
                                    CAT.debug("   ... and the value is correct! (" + cookie.getValue() + ")");
                                    has_ssl_session_secure = true;
                                } else {
                                    CAT.debug("   ... but the value is WRONG!");
                                    CAT.error("*** Wrong Session-ID for running secure session from cookie. " +
                                              "IP:" + req.getRemoteAddr() + " Cookie: " + cookie.getValue() + " SessID: " + session.getId());
                                    session.invalidate();
                                    has_session = false;
                                }
                            } else {
                                CAT.debug("*** Found NO matching cookie at all. ***");
                                CAT.error("*** Got NO secure Session-ID from cookie, but client does cookies: " +
                                          "IP:" + req.getRemoteAddr() + " SessID: " + session.getId());
                                // Most time when this happens, we are not under attack, but a
                                // stupid behaviour (bug?)  of IE or opera strikes us bad: With
                                // these two browsers, if we accept the __PFIX_TST_* cookie, but
                                // then deny the __PFIX_SEC_* cookie AND also deny for all further
                                // cookies from the domain, the stupid browser will still continue
                                // to send the __PFIX_TST_* cookie, so we will continue to come
                                // into this branch over and over again....  So we try to mark the
                                // now created session to decide in the following requests that this
                                // session does NOT use cookies at all, despite what ever the
                                // __PFIX_TST_* cookie says.
                                mark_session_as_no_cookies = (String) session.getAttribute(VISIT_ID);
                                session.invalidate();
                                has_session = false;
                            }
                        } else {
                            // We don't do cookies, so we simply have to believe it...
                            has_ssl_session_secure = true;
                        }
                    } else {
                        CAT.debug("    ... but session is insecure!");
                        has_ssl_session_insecure = true;
                    }
                } else if (secure != null && secure.booleanValue()) {
                    CAT.debug("*** Found secure session but NOT running under SSL => Destroying session.");
                    session.invalidate();
                    has_session = false;
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
                HttpSession secure_session = SessionAdmin.getInstance().getChildSessionForParentId(req.getRequestedSessionId());
                if (secure_session != null) {
                    does_cookies = doCookieTest(req, res, secure_session);
                }
                // We'll try to get back there securely by first jumping back to a new (insecure) SSL session,
                // and after that the the jump to the secure SSL session will not create a new one, but reuse
                // the already running secure session instead (but only if a secure cookie can identify the request as
                // coming from the browser that made the initial jump http->https).
                if (does_cookies) {
                    CAT.debug("    ... client handles cookies, so we'll check if we can reuse the parent session.");
                    force_jump_back_to_ssl = true;
                } else {
                    // OK, it seems as if we will not be able to identify the peer by comparing cookies.
                    // So the only thing we can do is to reuse the VISIT_ID.
                    CAT.debug("    ... but can't reuse the secure session because the client doesn't handle cookies.");
                    force_reuse_visit_id = true;
                }
            } else {
                // Normally the balancer (or, more accurate: mod_jk) has a chance to choose the right server for a 
                // new session, but with a session id in the URL it wasn't able to. So we redirect to a "fresh" request 
                // without _any_ id, giving the balancer the possibility to choose a different server. (this can be 
                // overridden by supplying the parameter __forcelocal=1 to the request). All this makes only sense of 
                // course if we are running in a cluster of servers behind a balancer that chooses the right server
                // based on the session id included in the URL.
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
            preq = new PfixServletRequest(req, this.getServletManagerConfig().getProperties());
        }

        FactoryInitServlet.tryReloadLog4j();
        tryReloadProperties(preq);

        // End of initialization. Now we handle all cases where we need to redirect.

        if (force_jump_back_to_ssl && allowSessionCreate()) {
            CAT.debug("=> I");
            forceRedirectBackToInsecureSSL(preq, req, res);
            return;
            // End of request cycle.
        }
        if (force_reuse_visit_id && allowSessionCreate()) {
            CAT.debug("=> II");
            forceNewSessionSameVisit(preq, req, res);
            return;
            // End of request cycle.
        }
        if (has_ssl_session_insecure) {
            CAT.debug("=> III");
            redirectToSecureSSLSession(preq, req, res);
            return;
            // End of request cycle.
        }
        if (needsSession() && allowSessionCreate() && needsSSL(preq) && !has_ssl_session_secure) {
            CAT.debug("=> IV");
            redirectToInsecureSSLSession(preq, req, res, mark_session_as_no_cookies);
            return;
            // End of request cycle.
        }
        if (!has_session && needsSession() && allowSessionCreate() && !needsSSL(preq)) {
            CAT.debug("=> V");
            redirectToSession(preq, req, res, mark_session_as_no_cookies);
            return;
            // End of request cycle.
        }
        if (!has_session && !needsSession() && needsSSL(preq) && !runningUnderSSL(req)) {
            CAT.debug("=> VI");
            redirectToSSL(req, res);
            return;
            // End of request cycle.
        }

        CAT.debug("*** >>> End of redirection management, handling request now.... <<< ***\n");

        PerfEvent pe = new PerfEvent(PerfEventType.XMLSERVER_CALLPROCESS, req.getRequestURI());
        pe.start();
        callProcess(preq, req, res);
        pe.save();
    }

    private void redirectToClearedRequest(HttpServletRequest req, HttpServletResponse res) {
        CAT.debug("===> Redirecting to cleared Request URL");
        String redirect_uri = SessionHelper.getClearedURL(req.getScheme(), getServerName(req), req);
        relocate(res, redirect_uri);
    }

    private void redirectToSSL(HttpServletRequest req, HttpServletResponse res) {
        CAT.debug("===> Redirecting to session-less request URL under SSL");
        String redirect_uri = SessionHelper.getClearedURL("https", getServerName(req), req);
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
                String secure_id  = secure_session.getId();
                String sec_testid = (String) secure_session.getAttribute(SECURE_SESS_COOKIE + MD5Utils.hex_md5(secure_id));
                CAT.debug("*** We have found a candidate: SessionId=" + secure_id + " now search for cookie...");
                CAT.debug("*** Session expects to see the cookie value " + sec_testid);
                // But we need to make sure that the current request comes
                // from the same user who created this secure session.
                // We do this by checking for a (secure) cookie with a corresponding session id.
                Cookie cookie = getSecureSessionCookie(req, secure_id);
                if (cookie != null) {
                    CAT.debug("*** Found a matching cookie ...");
                    if (cookie.getValue().equals(sec_testid)) {
                        CAT.debug("   ... and the value is correct! (" + cookie.getValue() + ")");
                        CAT.debug("==> Redirecting to the secure SSL URL with the already running secure session " + secure_id);
                        String redirect_uri = SessionHelper.encodeURL("https", getServerName(req), req,  secure_id);
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
            SessionAdmin.getInstance().registerSession(session, traillog, infostruct.getData().getServerName(),
                                                       infostruct.getData().getRemoteAddr());
        } else {
            // Register a new session now.
            registerSession(req, session);
        }
        CAT.debug("*** Got new Session (Id: " + session.getId() + ")");
        CAT.debug("*** Copying data back to new session");
        SessionHelper.copySessionData(map, session);
        session.setAttribute(SessionHelper.SESSION_ID_URL, SessionHelper.getURLSessionId(req));
        CAT.debug("*** Setting " + SessionHelper.SESSION_ID_URL + "  to " + session.getAttribute(SessionHelper.SESSION_ID_URL));
        CAT.debug("*** Setting SECURE flag");
        session.setAttribute(SessionAdmin.SESSION_IS_SECURE, Boolean.TRUE);
        session.setAttribute(STORED_REQUEST, preq);

        Cookie cookie = getSecureSessionCookie(req, session.getId());
        if (cookie != null) {
            cookie.setMaxAge(0);
            res.addCookie(cookie);
        }
        String sec_testid = req.getRemoteAddr() + ":" + Long.toHexString((long) (Math.random() * Long.MAX_VALUE));
        CAT.debug("*** Secure Test-ID used in session and cookie: " + sec_testid);
        String sec_cookie = MD5Utils.hex_md5(session.getId());
        session.setAttribute(SECURE_SESS_COOKIE + sec_cookie, sec_testid);
        cookie = new Cookie(SECURE_SESS_COOKIE + sec_cookie, sec_testid);
        cookie.setPath("/");
        cookie.setMaxAge(-1);
        cookie.setSecure(true);
        res.addCookie(cookie);

        // Make sure a test cookie is created for the new session if needed
        createTestCookie(req, res);

        CAT.debug("===> Redirecting to secure SSL URL with session (Id: " + session.getId() + ")");
        String redirect_uri = SessionHelper.encodeURL("https", getServerName(req), req);
        relocate(res, redirect_uri);
    }

    private void redirectToInsecureSSLSession(PfixServletRequest preq, HttpServletRequest req, HttpServletResponse res, String msanc) {
        boolean reuse_session = false;
        if (req.isRequestedSessionIdValid()) {
            reuse_session = true;
            CAT.debug("*** reusing existing session for jump http=>https");
        }
        HttpSession session = req.getSession(true);
        if (!reuse_session) {
            if (msanc == null) {
                registerSession(req, session);
            } else {
                session.setAttribute(VISIT_ID, msanc);
                session.setAttribute(NO_COOKIES_OVERRIDE, Boolean.TRUE);
                SessionAdmin.getInstance().registerSession(session, getServerName(req), req.getRemoteAddr());
            }
        }

        session.setAttribute(SessionHelper.SESSION_ID_URL,SessionHelper.getURLSessionId(req));
        CAT.debug("*** Setting INSECURE flag in session (Id: " + session.getId() + ")");
        session.setAttribute(SessionAdmin.SESSION_IS_SECURE, Boolean.FALSE);
        session.setAttribute(STORED_REQUEST, preq);

        // Make sure a test cookie is created if needed
        createTestCookie(req, res);

        CAT.debug("===> Redirecting to insecure SSL URL with session (Id: " + session.getId() + ")");
        String redirect_uri = SessionHelper.encodeURL("https", getServerName(req), req);
        relocate(res, redirect_uri);
    }

    private void forceRedirectBackToInsecureSSL(PfixServletRequest preq, HttpServletRequest req, HttpServletResponse res) {
        // When we come here, we KNOW that there's a secure SSL session already running, so this session here is
        // only used for the jump to SSL so we can get the cookie to check the identity of the caller.
        String      parentid      = req.getRequestedSessionId();
        HttpSession session       = req.getSession(true);
        session.setAttribute(SessionHelper.SESSION_ID_URL, SessionHelper.getURLSessionId(req));
        session.setAttribute(CHECK_FOR_RUNNING_SSL_SESSION, parentid);
        CAT.debug("*** Setting INSECURE flag in session (Id: " + session.getId() + ")");
        session.setAttribute(SessionAdmin.SESSION_IS_SECURE, Boolean.FALSE);
        session.setAttribute(STORED_REQUEST, preq);

        HttpSession child    = SessionAdmin.getInstance().getChildSessionForParentId(parentid);
        String      testrand = (String) child.getAttribute(RAND_SESS_COOKIE_VALUE);
        if (testrand == null || testrand.equals("")) {
            // Make sure a test cookie is created
            createTestCookie(req, res);
        } else {
            session.setAttribute(RAND_SESS_COOKIE_VALUE, testrand);
        }

        CAT.debug("===> Redirecting to SSL URL with session (Id: " + session.getId() + ")");
        String redirect_uri = SessionHelper.encodeURL("https", getServerName(req), req);
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

        String      testrand = (String) child.getAttribute(RAND_SESS_COOKIE_VALUE);
        if (testrand == null || testrand.equals("")) {
            // Make sure a test cookie is created
            createTestCookie(req, res);
        } else {
            session.setAttribute(RAND_SESS_COOKIE_VALUE, testrand);
        }
        
        LinkedList  traillog      = SessionAdmin.getInstance().getInfo(child).getTraillog();
        session.setAttribute(SessionHelper.SESSION_ID_URL, SessionHelper.getURLSessionId(req));
        session.setAttribute(VISIT_ID, curr_visit_id);
        SessionAdmin.getInstance().registerSession(session, traillog, getServerName(req), req.getRemoteAddr());
        CAT.debug("===> Redirecting with session (Id: " + session.getId() + ") using OLD VISIT_ID: " + curr_visit_id);
        session.setAttribute(STORED_REQUEST, preq);
        String redirect_uri = SessionHelper.encodeURL(req.getScheme(), getServerName(req), req);
        relocate(res, redirect_uri);
    }

    private void redirectToSession(PfixServletRequest preq, HttpServletRequest req, HttpServletResponse res, String msanc) {
        HttpSession session = req.getSession(true);
        session.setAttribute(SessionHelper.SESSION_ID_URL, SessionHelper.getURLSessionId(req));
        if (msanc == null) {
            registerSession(req, session);
        } else {
            session.setAttribute(VISIT_ID, msanc);
            session.setAttribute(NO_COOKIES_OVERRIDE, Boolean.TRUE);
            SessionAdmin.getInstance().registerSession(session, getServerName(req), req.getRemoteAddr());
        }

        // Make sure a test cookie is created if needed.
        createTestCookie(req, res);

        CAT.debug("===> Redirecting to URL with session (Id: " + session.getId() + ")");
        session.setAttribute(STORED_REQUEST, preq);
        String redirect_uri = SessionHelper.encodeURL(req.getScheme(), getServerName(req), req);
        relocate(res, redirect_uri);
    }

    private boolean doCookieTest(HttpServletRequest req, HttpServletResponse res, HttpSession sess) {
        if (sess == null) {
            sess = req.getSession(false);
        }
        // If in this session the client has been found to do cookies already, don't check the test
        // cookie value again.  We still have to check if there are any cookies at all (a
        // test cookie should be there, but maybe the wrong value because another session is
        // opened in parallel), because we need to guard against clients which supply cookies over
        // the whole redirect chain, but don't supply cookies on the following request, and we want
        // to correctly react on people who turn off cookies during the session.
        if (sess != null) {
            CAT.debug("*** Testing for marked session...");
            Cookie[] cookies            = req.getCookies();
            boolean  sessionusescookies = false;
            Boolean  doescookies        = (Boolean) sess.getAttribute(SESSION_COOKIES_MARKER);
            if (doescookies != null && doescookies.booleanValue()) {
                sessionusescookies = true;
                CAT.debug("    ...session is already marked as using cookies, looking for ANY test cookie...");
            } else {
                CAT.debug("    ...session is NOT already marked as using cookies!");
            }
            
            String rand = (String) sess.getAttribute(RAND_SESS_COOKIE_VALUE);
            if (rand != null) {
                CAT.debug("*** Testing for cookie " + TEST_COOKIE + "...");
                if (cookies != null) {
                    for (int i = 0; i < cookies.length ; i++) {
                        Cookie cookie = cookies[i];
                        if (cookie.getName().equals(TEST_COOKIE)) {
                            if (sessionusescookies) {
                                // No need to check the value...
                                CAT.debug("    ... found it, no need to check the value (because session is marked).");
                                return true;
                            } else {
                                CAT.debug("    ... found it, checking value " + rand);
                                if (cookie.getValue().equals(rand)) {
                                    CAT.debug("    ... value matches! Marking session...");
                                    sess.setAttribute(SESSION_COOKIES_MARKER, Boolean.TRUE);
                                    return true;
                                } else {
                                    CAT.debug("    ... value is WRONG.");
                                }
                            }
                            break;
                        }
                    }
                    CAT.debug("*** Client sends cookies, but not our test cookie! ***");
                }
            }
        }
        return false;
    }

    private boolean createTestCookie(HttpServletRequest req, HttpServletResponse res) {
        HttpSession sess = req.getSession(false);
        String      rand = null;
        if (sess != null) {
            rand = (String) sess.getAttribute(RAND_SESS_COOKIE_VALUE);
            if (rand != null) {
                CAT.debug("*** Already found a test cookie name in session: " + rand);
            } else {
                rand = Long.toHexString((long) (Math.random() * Long.MAX_VALUE));
                CAT.debug("*** Creating a random test cookie name: " + rand);
            }
            Cookie newprobe = new Cookie(TEST_COOKIE, rand);
            if (req.getContextPath().length() > 0) {
                newprobe.setPath(req.getContextPath());
            } else {
                newprobe.setPath("/");
            }
            res.addCookie(newprobe);
            sess.setAttribute(RAND_SESS_COOKIE_VALUE, rand);
            return true;
        }
        return false;
    }
    
    private Cookie getSecureSessionCookie(HttpServletRequest req, String sessionid) {
        Cookie[] cookies = req.getCookies();
        Cookie   tmp;
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                tmp = cookies[i];
                if (tmp.getName().equals(SECURE_SESS_COOKIE + MD5Utils.hex_md5(sessionid)))
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
            logbuff.append(getServerName(req) + "|" + req.getRemoteAddr() + "|" + req.getHeader("user-agent") + "|");
            if (req.getHeader("referer") != null) {
                logbuff.append(req.getHeader("referer"));
            }
            logbuff.append("|");
            if (req.getHeader("accept-language") != null) {
                logbuff.append(req.getHeader("accept-language"));
            }
            LOGGER_VISIT.warn(logbuff.toString());
            SessionAdmin.getInstance().registerSession(session, getServerName(req), req.getRemoteAddr());
        }
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        ServletContext ctx   = config.getServletContext();
        CAT.debug("*** Servlet container is '" + ctx.getServerInfo() + "'");
        int            major = ctx.getMajorVersion();
        int            minor = ctx.getMinorVersion();
        if ((major == 2 && minor >= 3) || (major > 2)) {
            CAT.warn("*** Servlet container with support for Servlet API " + major + "." + minor + " detected");
        } else {
            throw new ServletException("*** Can't detect servlet container with support for Servlet API 2.3 or higher");
        }
        
        Properties properties = new Properties(System.getProperties());
        
        String commonpropfilename = config.getInitParameter("servlet.commonpropfile");
        if (commonpropfilename != null) {
            if (!commonpropfilename.startsWith("/")) {
                commonpropfile = PathFactory.getInstance().createPath(commonpropfilename).resolve();
            } else {
                commonpropfile = new File(commonpropfilename);
            }
            // Load on first request
            common_mtime = loadPropertyfile(properties, commonpropfile);
        }
        
        String servletpropfilename = config.getInitParameter("servlet.propfile");
        if (servletpropfilename != null) {
            if (!servletpropfilename.startsWith("/")) {
                servletpropfile = PathFactory.getInstance().createPath(servletpropfilename).resolve();
            } else {
                servletpropfile = new File(servletpropfilename);
            }
        }
        
        // Make sure configuration is available
        this.reloadServletConfig(servletpropfile, properties);
        
        initCookieSec();
        initExceptionConfigs();
        initServletEncoding();
    }

    protected boolean tryReloadProperties(PfixServletRequest preq) throws ServletException {
        if ((commonpropfile  != null && commonpropfile.lastModified()  > common_mtime) ||
            (servletpropfile != null && servletpropfile.lastModified() > servlet_mtime)) {
            
            int currLoadIndex = configLoadIndex.incrementAndGet();
            
            CAT.warn("\n\n##############################\n" +
                     "#### Reloading properties ####\n" +
                     "##############################\n");
            Properties properties = new Properties(System.getProperties());
                        
            if (commonpropfile != null) {
                common_mtime = loadPropertyfile(properties, commonpropfile);
            }
            servlet_mtime = servletpropfile.lastModified();
            this.reloadServletConfig(servletpropfile, properties);
            this.getServletManagerConfig().getProperties().setProperty(PROP_LOADINDEX, String.valueOf(currLoadIndex));
            
            initCookieSec();
            return true;
        } else {
            return false;
        }
        
    }

    /**
     * This is only for broken clients who suddenly stop supplying cookies in the middle of a
     * session.  Windows XP internet configuration wizard (which speaks http) seems to be an example of a
     * client that remembers the cookies over the first request/relocate/relocate cycle, but never sends a
     * cookie again on the next request.
     * You are strongly advised to NOT set the corresponding property to true, unless you deal with broken software.
     */
    private void initCookieSec() {
        String csec = this.getServletManagerConfig().getProperties().getProperty(PROP_COOKIE_SEC_NOT_ENFORCED);
        if (csec != null && csec.equals("true")) {
            cookie_security_not_enforced = true;
        } else {
            cookie_security_not_enforced = false;
        }
    }
   
    private long loadPropertyfile(Properties props, File propfile) throws ServletException {
        long mtime;
        try {
            mtime = propfile.lastModified();
            XMLPropertiesUtil.loadPropertiesFromXMLFile(propfile, props);
        } catch (FileNotFoundException e) {
            throw new ServletException("*** [" + propfile.getName() + "] Not found: " + e.toString());
        } catch (IOException e) {
            throw new ServletException("*** [" + propfile.getName() + "] IO-error: " + e.toString());
        } catch (SAXException e) {
            throw new ServletException("*** [" + propfile.getName() + "] Parsing-error: " + e.toString());
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
                                       req, res, this.getServletManagerConfig().getProperties());
                
            } else {
                // This is the default case when no
                // exceptionprocessors are defined.
                xhandler.handle(e, preq, this.getServletManagerConfig().getProperties());
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
            if (clazz.isInstance(th)) {
                // bingo
                return (ExceptionConfig) exceptionConfigs.get(key);
            }
        }    
        return null;
    }
    
    /**
     * This method uses all properties prefixed with 'exception' to build ExceptionConfig
     * objects, which are then stored in the exConfig-Map, keyed by the type attribute
     * of the ExceptionConfig.
     * Syntax of key: "exception" '.' JAVATYPE '.' ("forward"|"page"|"processor")
     * @exception ServletException if the exception configuration defined in the properties
     * is somehow invalid
     */
    private void initExceptionConfigs() throws ServletException {
        Map tmpExConf = new HashMap();
        int len = PROP_EXCEPTION.length();
        
        Properties properties = this.getServletManagerConfig().getProperties();
    	Enumeration props = properties.propertyNames();
    	while (props.hasMoreElements()) {
            String propName = (String) props.nextElement();
            
            if (propName.startsWith(PROP_EXCEPTION)) {
                String          propValue = properties.getProperty(propName);

                String type = null;
                String attrName = null;
                IndexOutOfBoundsException ioobex = null;
                try {
                    int dot1 = propName.indexOf('.');
                    int dot2 = propName.lastIndexOf('.');
                    type = propName.substring(dot1+1, dot2);
                    attrName = propName.substring(dot2+1);
                } catch (IndexOutOfBoundsException e) {
                    // logging is done below
                    ioobex = e;
                }
                if ( type == null || attrName == null || "".equals(type) || "".equals(attrName) ) {
                    CAT.debug("Could not parse property key \""+propName+"\" into three non-empty parts separated by '.'", ioobex);
                }
                
                ExceptionConfig exConf     = (ExceptionConfig) tmpExConf.get(type);

                CAT.debug("Property found for exception processing: " + propName + "=" + propValue);

                if (exConf == null) {
                    exConf = new ExceptionConfig();
                    exConf.setType(type);
                    tmpExConf.put(type, exConf);
                }

                try {
                    if ("forward".equals(attrName) ) {
                        exConf.setForward( Boolean.valueOf(propValue).booleanValue() );
                    } else if ("page".equals(attrName) ) {
                        exConf.setPage(propValue);
                    } else if ("processor".equals(attrName) ) {
                        Class procClass = Class.forName(propValue);
                        ExceptionProcessor exProc = (ExceptionProcessor) procClass.newInstance();
                        exConf.setProcessor(exProc);
                    }
                } catch (ClassCastException ex) {
                    throw new ServletException("INVALID CONF: Class "+propValue + " is not an instance of 'ExceptionProcessor'");
                } catch (IllegalAccessException ex) {
                    throw new ServletException("INVALID CONF: Can't create instance of class " + propName, ex);
                } catch (SecurityException ex) {
                    throw new ServletException("INVALID CONF: Can't create instance of class " + propName, ex);
                } catch (ClassNotFoundException ex) {
                    throw new ServletException("INVALID CONF: Can't create instance of class " + propName, ex);
                } catch (InstantiationException ex) {
                    throw new ServletException("INVALID CONF: Can't create instance of class " + propName, ex);
                }
            }
    	}
        
        CAT.debug("Finished reading properties for Exception configuration! \n" + tmpExConf);

        exceptionConfigs.clear();

        // validate the ExceptionConfig-instances and save them, keyed by their type-attribute
        for (Iterator values = tmpExConf.values().iterator(); values.hasNext();) {
            ExceptionConfig exConfig = (ExceptionConfig) values.next();
            if (exConfig.validate() == false)
                throw new ServletException("INVALID ExceptionConfig: \n"+ exConfig);
            else
                exceptionConfigs.put(exConfig.getType(), exConfig);
        }
        
        if (CAT.isDebugEnabled()) {
            CAT.debug("Complete ExceptionConfig is:");
            CAT.debug("\n"+ exceptionConfigs);
        }
    }

    /**
     * Sets the servlet's encoding, which is used as character encoding for decoding/encoding 
     * requests/responses. Be aware that this setting only applies to the appropriate Readers, 
     * Writers and body request parameters. It has no effect on the byte streams. The URI
     * encoding (which is set on Tomcat connector level and can't be changed here) is set always
     * be the same as the body encoding.
     */
    private void initServletEncoding() {
        //Try to get servlet encoding from properties:
        String encoding = this.getServletManagerConfig().getProperties().getProperty(SERVLET_ENCODING);
        if (encoding == null || encoding.trim().equals("")) CAT.warn("No servlet encoding property set");
        else if(!Charset.isSupported(encoding)) CAT.error("Servlet encoding '"+encoding+"' is not supported.");
        else servletEncoding = encoding;
        
        //Try to get servlet encoding from init parameters:
        if (servletEncoding == null) {
            encoding = getServletConfig().getInitParameter(SERVLET_ENCODING);
            if(encoding == null || encoding.trim().equals("")) CAT.warn("No servlet encoding init parameter set");
            else if (!Charset.isSupported(encoding)) CAT.error("Servlet encoding '"+encoding+"' is not supported.");
            else servletEncoding=encoding;
        }
        //Use default servlet encoding:
        if (servletEncoding == null) {
            servletEncoding = DEFAULT_ENCODING;
            CAT.warn("Using default servlet encoding: " + DEFAULT_ENCODING);
        }
        
        CAT.debug("Servlet encoding was set to '" + servletEncoding + "'.");  
    }
    
    
    protected abstract void process(PfixServletRequest preq, HttpServletResponse res) throws Exception;

    // TODO: replace constants - ask tomcat 
    public static final int HTTP_PORT       = 80;
    public static final int APACHE_SSL_PORT = 443;
    public static final int TOMCAT_SSL_PORT = 8443;
    
    public static boolean isDefault(int port) {
        return port == HTTP_PORT || port == APACHE_SSL_PORT;
    }

    public static boolean isSslPort(int port) {
        return port == APACHE_SSL_PORT || port == TOMCAT_SSL_PORT;
    }
    
}// ServletManager
