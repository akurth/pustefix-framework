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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.pustefixframework.config.contextxmlservice.ServletManagerConfig;
import org.pustefixframework.config.project.SessionTimeoutInfo;
import org.pustefixframework.container.spring.beans.TenantScope;
import org.pustefixframework.container.spring.http.UriProvidingHttpRequestHandler;
import org.pustefixframework.util.LogUtils;
import org.pustefixframework.util.NetUtils;
import org.pustefixframework.util.net.IPRangeMatcher;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.context.ServletContextAware;

import de.schlund.pfixcore.workflow.PageMap;
import de.schlund.pfixcore.workflow.PageProvider;
import de.schlund.pfixcore.workflow.SiteMap;
import de.schlund.pfixcore.workflow.SiteMap.PageLookupResult;
import de.schlund.pfixxml.LanguageInfo;
import de.schlund.pfixxml.PfixServletRequest;
import de.schlund.pfixxml.Tenant;
import de.schlund.pfixxml.TenantInfo;
import de.schlund.pfixxml.config.EnvironmentProperties;
import de.schlund.pfixxml.exceptionprocessor.ExceptionConfig;
import de.schlund.pfixxml.exceptionprocessor.ExceptionProcessingConfiguration;
import de.schlund.pfixxml.exceptionprocessor.ExceptionProcessor;
import de.schlund.pfixxml.serverutil.SessionAdmin;

/**
 * ServletManager.java
 *
 *
 * Created: Wed May  8 16:39:06 2002
 *
 * @author <a href="mailto:jtl@schlund.de">Jens Lautenbacher</a>
 */

public abstract class AbstractPustefixRequestHandler implements PageProvider, SessionTrackingStrategyContext, UriProvidingHttpRequestHandler, ServletContextAware, InitializingBean {

    protected Logger LOGGER_SESSION = LoggerFactory.getLogger("LOGGER_SESSION");
    
    public static final String DEFAULT_SESSION_COOKIE_NAME = "JSESSIONID";
    
    public static final String           VISIT_ID                      = "__VISIT_ID__";
    public static final String           PROP_LOADINDEX                = "__PROPERTIES_LOAD_INDEX";
    
    public static final String           PROP_COOKIE_SEC_NOT_ENFORCED  = "servletmanager.cookie_security_not_enforced";
    public static final String           PROP_P3PHEADER                = "servletmanager.p3p";
    public static final String           PROP_SSL_REDIRECT_PORT        = "pfixcore.ssl_redirect_port.for.";
    public static final String           PROP_NONSSL_REDIRECT_PORT     = "pfixcore.nonssl_redirect_port.for.";
    protected static final String        DEF_CONTENT_TYPE              = "text/html";
    private static final String          DEFAULT_ENCODING              = "UTF-8";
    private static final String          SERVLET_ENCODING              = "servlet.encoding";
    
    public static final String SESSION_ATTR_COOKIE_SESSION = "__PFX_SESSION_FROM_COOKIE__";
    private static final String SESSION_ATTR_REQUEST_COUNT = "__PFX_REQUEST_COUNT__";
    private static final String SESSION_ATTR_ORIGINAL_TIMEOUT = "__PFX_SESSION_ORIGINAL_TIMEOUT__";
    private static final String SESSION_ATTR_USER_AGENT = "__PFX_USER_AGENT__";
    private static final String SESSION_ATTR_REMOTE_IP = "__PFX_REMOTE_IP__";
    
    public static final String REQUEST_ATTR_LANGUAGE = "__PFX_LANGUAGE__";
    public static final String REQUEST_ATTR_PAGE_ALTERNATIVE = "__PFX_PAGE_ALTERNATIVE__";
    public static final String REQUEST_ATTR_PAGE_ADDITIONAL_PATH = "__PFX_PAGE_ADDITIONAL_PATH__";
    public static final String REQUEST_ATTR_PAGEFLOW = "__PFX_PAGEFLOW__";
    public static final String REQUEST_ATTR_PAGEGROUP = "__PFX_PAGEGROUP__";
    public static final String REQUEST_ATTR_INVALIDATE_SESSION_AFTER_COMPLETION = "__PFX_INVALIDATE_SESSION_AFTER_COMPLETION__";
    public static final String REQUEST_ATTR_CLIENT_ABORTED = "__PFX_CLIENT_ABORTED__";
    public static final String REQUEST_ATTR_REQUEST_TYPE = "__PFX_REQUEST_TYPE__";
    
    public static enum RequestType { PAGE, RENDER, DIRECT };
    
    private static final IPRangeMatcher privateIPRange = new IPRangeMatcher("10.0.0.0/8", "169.254.0.0/16", 
            "172.16.0.0/12", "192.168.0.0/16", "fc00::/7");
    
    private int INC_ID = 0;
    private String TIMESTAMP_ID = "";
    
    public static final Logger LOGGER_VISIT = LoggerFactory.getLogger("LOGGER_VISIT");
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPustefixRequestHandler.class);
    private String                       servletEncoding;
    private ServletContext servletContext;
    protected String handlerURI;
    private SessionAdmin sessionAdmin;
    private ExceptionProcessingConfiguration exceptionProcessingConfig;
    protected SessionTrackingStrategy sessionTrackingStrategy;
    private BotSessionTrackingStrategy botSessionTrackingStrategy;
    private SessionTimeoutInfo sessionTimeoutInfo;
    protected TenantInfo tenantInfo;
    protected LanguageInfo languageInfo;
    protected SiteMap siteMap;
    protected PageMap pageMap;
    
    public abstract ServletManagerConfig getServletManagerConfig();

    public boolean needsSSL(PfixServletRequest preq) throws ServletException {
        return this.getServletManagerConfig().isSSL();
    }

    public abstract boolean needsSession();

    public abstract boolean allowSessionCreate();
    
    protected int validateRequest(HttpServletRequest req) {
        return 0;
    }

    public static String getSessionCookieName(HttpServletRequest req) {
        SessionCookieConfig cookieConfig = req.getServletContext().getSessionCookieConfig();
        if(cookieConfig != null) {
            if(cookieConfig.getName() != null) {
                return cookieConfig.getName();
            }
        }
        return DEFAULT_SESSION_COOKIE_NAME;
    }
    
    public static String getServerName(HttpServletRequest req) {
        String forward = req.getHeader("X-Forwarded-Server");
        if (forward != null && !forward.equals("")) {
            return forward;
        } else {
            return req.getServerName();
        }
    }
    
    public static String getRemoteAddr(HttpServletRequest req) {
        String remoteIp = req.getRemoteAddr();
        String forward = req.getHeader("X-Forwarded-For");
        if (forward != null && !forward.equals("")) {
            if(privateIPRange.matches(remoteIp)) {
                String[] ips = forward.split(",");
                for(int i=ips.length - 1; i >= 0; i--) {
                    String ip = ips[i].trim();
                    if(ip.length() > 0) {
                        if(NetUtils.checkIP(ip) && !privateIPRange.matches(ip)) {
                            remoteIp = ip;
                            break;
                        }
                    }
                }
            }
        }
        return remoteIp;
    }
    
    public static int getSSLRedirectPort(int port, Properties props) {
        String redirectPort = (String)props.get(PROP_SSL_REDIRECT_PORT + String.valueOf(port));
        if(redirectPort == null) {
            //try to get SSL redirect port from Tomcat MBean
            try {
                MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
                ObjectName objNamePattern = new ObjectName("*:type=Connector,port=" + port + ",*");
                Set<ObjectName> objNames= mbeanServer.queryNames(objNamePattern, null);
                if(objNames.size() == 1) {
                    ObjectName objName = objNames.iterator().next();
                    Integer targetPort = (Integer)mbeanServer.getAttribute(objName, "redirectPort");
                    redirectPort = String.valueOf(targetPort);
                    props.setProperty(PROP_SSL_REDIRECT_PORT + String.valueOf(port), redirectPort);
                }
            } catch(JMException x) {
                LOG.warn("Error getting redirect port from Tomcat connector", x);
            }
            //if not found use default port
            if(redirectPort == null) {
                redirectPort = "443";
                props.put(PROP_SSL_REDIRECT_PORT + String.valueOf(port), redirectPort);
            }
        }
        return Integer.valueOf(redirectPort);
    }
    
    public static int getNonSSLRedirectPort(int port, Properties props) {
        String redirectPort = props.getProperty(AbstractPustefixRequestHandler.PROP_NONSSL_REDIRECT_PORT + String.valueOf(port));
        if(redirectPort == null) {
            Enumeration<?> propNames = props.propertyNames();
            String mappedPort = null;
            while(propNames.hasMoreElements() && mappedPort == null) {
                String propName = (String)propNames.nextElement();
                if(propName.startsWith(AbstractPustefixRequestHandler.PROP_SSL_REDIRECT_PORT)) {
                    int ind = propName.lastIndexOf('.');
                    if(ind > -1) {
                        String portKey = propName.substring(ind + 1);
                        String portVal = props.getProperty(propName);
                        if(portVal.equals(String.valueOf(port))) {
                            redirectPort = portKey;
                            props.put(PROP_NONSSL_REDIRECT_PORT + String.valueOf(port), redirectPort);
                        }
                    }
                }
            }
            //if not found use default port
            if(redirectPort == null) {
                redirectPort = "80";
                props.put(PROP_NONSSL_REDIRECT_PORT + String.valueOf(port), redirectPort);
            }
        }
        return Integer.valueOf(redirectPort);
    }

    public static boolean checkClientIdentity(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if(session != null) {
            String storedIp = (String)session.getAttribute(SESSION_ATTR_REMOTE_IP);
            if(storedIp != null) {
                String ip = AbstractPustefixRequestHandler.getRemoteAddr(req);
                if(!ip.equals(storedIp)) {
                    LOG.warn("Differing client IP: " + ip + " " + storedIp);
                    return false;
                }
            }
            String storedUserAgent = (String)session.getAttribute(SESSION_ATTR_USER_AGENT);
            if(storedUserAgent != null) {
                String userAgent = req.getHeader("User-Agent");
                if(userAgent == null) userAgent = "-";
                if(!userAgent.equals(storedUserAgent)) {
                    LOG.warn("Differing client useragent: " + userAgent + " " + storedUserAgent);
                    return false;
                }
            }
        }
        return true;
    }
    
    public static void storeClientIdentity(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if(session != null) {
            String ip = AbstractPustefixRequestHandler.getRemoteAddr(req);
            session.setAttribute(SESSION_ATTR_REMOTE_IP, ip);
            String userAgent = req.getHeader("User-Agent");
            if(userAgent == null) {
                userAgent = "-";
            }
            session.setAttribute(SESSION_ATTR_USER_AGENT, userAgent);
        }
    }
    
    public void setHandlerURI(String uri) {
        this.handlerURI = uri;
    }
    
    public String[] getRegisteredURIs() {
        if(handlerURI != null) return new String[] { handlerURI };
        return new String[0];
    }

    public void handleRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        req.setCharacterEncoding(servletEncoding);
        res.setCharacterEncoding(servletEncoding);
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("\n ------------------- Start of new Request ---------------");
            LOG.debug("====> Scheme://Server:Port " + req.getScheme() + "://" + getServerName(req) + ":" + req.getServerPort());
            LOG.debug("====> URI:   " + req.getRequestURI());
            LOG.debug("====> Query: " + req.getQueryString());
            LOG.debug("----> needsSession=" + needsSession() + " allowSessionCreate=" + allowSessionCreate());
            LOG.debug("====> Sessions: " + sessionAdmin.toString());
            LOG.debug("\n");

            Enumeration<?> headers = req.getHeaderNames();

            while (headers.hasMoreElements()) {
                String header = (String) headers.nextElement();
                String headerval = req.getHeader(header);
                LOG.debug("+++ Header: " + header + " -> " + headerval);
            }

        }

        int httpStatus = validateRequest(req);
        if(validateRequest(req) >= 400) {
            res.sendError(httpStatus);
            if(LOG.isInfoEnabled()) LOG.info("Rejecting invalid request to path (" + httpStatus + "): " + req.getPathInfo());
            return;
        }
        
        String tenantParam = req.getParameter("__tenant");
        if(tenantParam != null && !"prod".equals(EnvironmentProperties.getProperties().getProperty("mode"))) {
            Tenant tenant = tenantInfo.getTenant(tenantParam);
            if(tenant != null) {
                res.addCookie(new Cookie(TenantScope.REQUEST_ATTRIBUTE_TENANT, tenant.getName()));
            }
            HttpSession session = req.getSession(false);
            if(session != null) {
                session.invalidate();
            }
            res.sendRedirect(req.getRequestURL().toString());
        }

        // Set P3P-Header if needed to make sure it is 
        // set for every response (even redirects).
        String p3pHeader = getServletManagerConfig().getProperties().getProperty(PROP_P3PHEADER);
        if (p3pHeader != null && p3pHeader.length() > 0) {
            res.addHeader("P3P", p3pHeader);
        }
        
        if(BotDetector.isBot(req)) {
            botSessionTrackingStrategy.handleRequest(req, res);
        } else {
            sessionTrackingStrategy.handleRequest(req, res);
        }
            
    }
    
    public void afterPropertiesSet() throws Exception {
        init();
    }
    
    public void init() throws ServletException {
        ServletContext ctx = getServletContext();
        LOG.debug("*** Servlet container is '" + ctx.getServerInfo() + "'");
        int major = ctx.getMajorVersion();
        int minor = ctx.getMinorVersion();
        if ((major == 2 && minor >= 3) || (major > 2)) {
            LOG.info("*** Servlet container with support for Servlet API " + major + "." + minor + " detected");
        } else {
            throw new ServletException("*** Can't detect servlet container with support for Servlet API 2.3 or higher");
        }
        
        initServletEncoding();

        if(sessionTrackingStrategy == null) {
            Set<SessionTrackingMode> modes = ctx.getEffectiveSessionTrackingModes();
            if(modes.contains(SessionTrackingMode.COOKIE)) {
                if(modes.contains(SessionTrackingMode.URL)) {
                    sessionTrackingStrategy = new CookieSessionTrackingStrategy();
                } else {
                    sessionTrackingStrategy = new CookieOnlySessionTrackingStrategy();
                }
            } else if(modes.contains(SessionTrackingMode.URL)) {
                sessionTrackingStrategy = new URLRewriteSessionTrackingStrategy();
            }
        }
        sessionTrackingStrategy.init(this);
        botSessionTrackingStrategy = new BotSessionTrackingStrategy();
        botSessionTrackingStrategy.init(this);
    }


    public void callProcess(PfixServletRequest preq, HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        
    	//trigger initialization of page alternative name if not already done
    	preq.getPageName();
    	
    	HttpSession session = req.getSession(false);
        if(sessionTimeoutInfo != null) {
            if(session != null) {
                Integer count = (Integer)session.getAttribute(SESSION_ATTR_REQUEST_COUNT);
                if(count == null) {
                    count = 1;
                    session.setAttribute(SESSION_ATTR_ORIGINAL_TIMEOUT, session.getMaxInactiveInterval());
                    session.setMaxInactiveInterval(sessionTimeoutInfo.getInitialTimeout());
                } else {
                    if(count == sessionTimeoutInfo.getRequestLimit()) {
                        Integer origTimeout = (Integer)session.getAttribute(SESSION_ATTR_ORIGINAL_TIMEOUT);
                        if(origTimeout != null) {
                            session.setMaxInactiveInterval(origTimeout);
                        }
                    }
                    count++;
                }
                session.setAttribute(SESSION_ATTR_REQUEST_COUNT, count);
            }
        }
        
        try {
            res.setContentType(DEF_CONTENT_TYPE);
            if(needsSession() && session != null) {
				ReadWriteLock lock = (ReadWriteLock)session.getAttribute(SessionUtils.SESSION_ATTR_LOCK);
				if(lock != null) {
					Lock readLock = lock.readLock();
					readLock.lock();
					try {
						process(preq, res);
						return;
					} finally {
						readLock.unlock();
					}
				}
            }
            process(preq, res);
        } catch (Throwable e) {
            if((e instanceof IOException) &&
                    (e.getClass().getSimpleName().equals("ClientAbortException") || 
                            e.getClass().getName().equals("org.mortbay.jetty.EofException"))) {
                LOG.warn("Client aborted request.");
                req.setAttribute(REQUEST_ATTR_CLIENT_ABORTED, true);
            } else {
                //Check if exception occurred while having a session which wasn't created by Pustefix,
                //i.e. the session was created after the Pustefix session timed out and the request thread
                //tried to access request/session-scoped beans, which let's Spring create a new one.
                //If no response was written we invalidate the illegal session and make a temporary
                //redirect to negotiate a new Pustefix session.
                session = req.getSession(false);
                if(session != null) {
                    String visitId = (String)session.getAttribute(VISIT_ID);
                    if(visitId == null && !res.isCommitted()) {
                        LOG.warn("Error occurred while using non-Pustefix session '" + session.getId() + 
                                "' -> invalidate it and redirect for new session negotiation", e);
                        session.invalidate();
                        res.sendRedirect(req.getRequestURL().toString());
                        return;
                    }
                } 
                //Check if IllegalStateException thrown because of accessing already invalidated session.
                //If no response was written yet make a temporary redirect to get a new Pustefix session.
                if(needsSession() && session == null && e instanceof IllegalStateException) {
                    if(!res.isCommitted()) {
                        LOG.warn("Error occurred while accessing already invalidated session " +
                                "-> redirect to new Pustefix session", e);
                        res.sendRedirect(req.getRequestURL().toString());
                        return;
                    }
                }
                LOG.error("Exception in process", e);
                ExceptionConfig exconf = exceptionProcessingConfig.getExceptionConfigForThrowable(e.getClass());
                if(exconf != null && exconf.getProcessor()!= null) { 
                    if ( preq.getLastException() == null ) {  
                        ExceptionProcessor eproc = exconf.getProcessor();
                        eproc.processException(e, exconf, preq,
                                           getServletContext(),
                                           req, res, this.getServletManagerConfig().getProperties());
                    }
                } 
                if(!res.isCommitted()) throw new ServletException("Exception in process.",e);
            }
        } finally {
            try {
                if (session != null && (session.getAttribute(REQUEST_ATTR_INVALIDATE_SESSION_AFTER_COMPLETION) != null)) {
                    LOGGER_SESSION.info("Invalidate session VII: " + session.getId());
                    session.invalidate();
                }
            } catch(IllegalStateException x) {
                //can be ignored, because session has been already invalidated meanwhile
            }
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
        if (encoding == null || encoding.trim().equals(""))
            LOG.info("No servlet encoding property set");
        else if (!Charset.isSupported(encoding))
            LOG.error("Servlet encoding '" + encoding + "' is not supported.");
        else
            servletEncoding = encoding;

        //Try to get servlet encoding from init parameters:
        if (servletEncoding == null) {
            encoding = getServletEncoding();
            if (encoding == null || encoding.trim().equals(""))
                LOG.info("No servlet encoding init parameter set");
            else if (!Charset.isSupported(encoding))
                LOG.error("Servlet encoding '" + encoding + "' is not supported.");
            else
                servletEncoding = encoding;
        }
        //Use default servlet encoding:
        if (servletEncoding == null) {
            servletEncoding = DEFAULT_ENCODING;
            LOG.info("Using default servlet encoding: " + DEFAULT_ENCODING);
        }

        LOG.debug("Servlet encoding was set to '" + servletEncoding + "'.");
    }

    protected static List<Pattern> getBotPatterns() {
        List<Pattern> patterns = new ArrayList<Pattern>();
        try {
            Enumeration<URL> urls = AbstractPustefixRequestHandler.class.getClassLoader().getResources("META-INF/org/pustefixframework/http/bot-user-agents.txt");
            while(urls.hasMoreElements()) {
                URL url = urls.nextElement();
                InputStream in = url.openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, "utf8"));
                String line;
                while((line = reader.readLine()) != null) {
                    line = line.trim();
                    if(!line.startsWith("#")) {
                        Pattern pattern = Pattern.compile(line);
                        patterns.add(pattern);
                    }
                }
                in.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading bot user-agent configuration", e);
        }
        return patterns;
    }
    
    protected abstract void process(PfixServletRequest preq, HttpServletResponse res) throws Exception;

    public static final int HTTP_PORT  = 80;
    public static final int HTTPS_PORT = 443;

    public static boolean isDefault(String scheme, int port) {
        if (scheme.equals("http") && port == HTTP_PORT) {
            return true;
        } else if (scheme.equals("https") && port == HTTPS_PORT) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Can be overridden by a subclass in order to disable the check
     * whether a session id provided by a request is valid.
     * 
     * @return <code>true</code> if and only if the request handler should
     * check whether the session id is valid for every request
     */
    public boolean wantsCheckSessionIdValid() {
        return true;
    }
    
    public void registerSession(HttpServletRequest req, HttpSession session) {
        if (session != null) {
            synchronized (TIMESTAMP_ID) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                String timestamp = sdf.format(new Date());
                NumberFormat nf = NumberFormat.getInstance();
                nf.setMinimumIntegerDigits(3);

                if (timestamp.equals(TIMESTAMP_ID)) {
                    INC_ID++;
                } else {
                    TIMESTAMP_ID = timestamp;
                    INC_ID = 0;
                }
                if (INC_ID >= 1000) {
                    LOG.warn("*** More than 999 connects/sec! ***");
                }
                String sessid = session.getId();
                String mach = "";
                if (sessid.lastIndexOf(".") > 0) {
                    mach = sessid.substring(sessid.lastIndexOf("."));
                }
                session.setAttribute(VISIT_ID, TIMESTAMP_ID + "-" + nf.format(INC_ID) + mach);
            }
            session.setAttribute(SessionUtils.SESSION_ATTR_LOCK, new ReentrantReadWriteLock());
            StringBuffer logbuff = new StringBuffer();
            logbuff.append(session.getAttribute(VISIT_ID) + "|" + session.getId() + "|");
            logbuff.append(LogUtils.makeLogSafe(getServerName(req)) + "|" + LogUtils.makeLogSafe(getRemoteAddr(req)) + "|");
            logbuff.append(LogUtils.makeLogSafe(req.getHeader("user-agent")) + "|");
            if (req.getHeader("referer") != null) {
                logbuff.append(LogUtils.makeLogSafe(req.getHeader("referer")));
            }
            logbuff.append("|");
            if (req.getHeader("accept-language") != null) {
                logbuff.append(LogUtils.makeLogSafe(req.getHeader("accept-language")));
            }
            LOGGER_VISIT.warn(logbuff.toString());
            getSessionAdmin().registerSession(session, getServerName(req), req.getRemoteAddr());
        }
    }

    public static void relocate(HttpServletResponse res, String reloc_url) {
        relocate(res, HttpServletResponse.SC_MOVED_TEMPORARILY, reloc_url);
    }
    
    public static void relocate(HttpServletResponse res, int type, String reloc_url) {
        LOG.debug("\n\n        ======> relocating to " + reloc_url + "\n");
        res.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
        res.setHeader("Pragma", "no-cache");
        res.setHeader("Cache-Control", "no-cache, no-store, private, must-revalidate");
        res.setStatus(type);
        res.setHeader("Location", reloc_url);
    }
    
    @Override
    public String[] getRegisteredPages() {
        return new String[0];
    }
    
    public String getPageName(final String pageAlias, final HttpServletRequest request) {
        
        String pageName = pageAlias;
        
        String prefix;
        int ind = pageName.indexOf('/');
        if(ind > -1) {
            prefix = pageName.substring(0, ind);
        } else {
            prefix = pageName;
        }
        
        //check if pageAlias has language prefix
        Tenant tenant = (Tenant)request.getAttribute(TenantScope.REQUEST_ATTRIBUTE_TENANT);
        if((tenant != null && tenant.useLangPrefix() && tenant.getSupportedLanguageByCode(prefix) != null) ||
            (tenant == null && languageInfo.getSupportedLanguageByCode(prefix) != null)) {
            if(ind > -1) {
                //remove language prefix
                pageName = pageName.substring(ind + 1);
            } else {
                //default page
                return null;
            }
        }    
        
        pageName = resolvePrefix(pageName, request);
        if(pageName == null) {
            return null;
        }
        
        //check page alias
        PageLookupResult res = null;
        String lang = (String)request.getAttribute(REQUEST_ATTR_LANGUAGE);
        if(tenant != null && !tenant.useLangPrefix() && tenant.getSupportedLanguages().size() > 1) {
            res = siteMap.getPageName(pageName, lang, tenant.getSupportedLanguages());
        } else {
            res = siteMap.getPageName(pageName, lang);
        }
        
        if(pageName.startsWith(res.getAliasPageName()) && pageName.length() > res.getAliasPageName().length()) {
            String additionalPath = pageName.substring(pageName.indexOf(res.getAliasPageName()) + res.getAliasPageName().length());
            request.setAttribute(REQUEST_ATTR_PAGE_ADDITIONAL_PATH, additionalPath);
        }
        if(res.getPageAlternativeKey() != null) {
            request.setAttribute(REQUEST_ATTR_PAGE_ALTERNATIVE, res.getPageAlternativeKey());
        }
        if(res.getPageGroup() != null) {
            request.setAttribute(REQUEST_ATTR_PAGEGROUP, res.getPageGroup());
        }
        ind = res.getPageName().indexOf('/');
        if(ind > -1) {
            return res.getPageName().substring(0, ind);
        } else {
            return res.getPageName();
        }
    }

    protected String resolvePrefix(final String pageAlias, final HttpServletRequest request) {
        return pageAlias;
    }
    
    public void setServletEncoding(String encoding) {
        this.servletEncoding = encoding;
    }
    
    public String getServletEncoding() {
        return servletEncoding;
    }
    
    public void setServletContext(ServletContext context) {
        this.servletContext = context;
    }
    
    public ServletContext getServletContext() {
        return this.servletContext;
    }
    
    public void setSessionAdmin(SessionAdmin sessionAdmin) {
        this.sessionAdmin = sessionAdmin;
    }
    
    public SessionAdmin getSessionAdmin() {
        return sessionAdmin;
    }
    
    public void setSessionTrackingStrategy(SessionTrackingStrategy strategy) {
        this.sessionTrackingStrategy = strategy;
    }
    
    public void setSessionTimeoutInfo(SessionTimeoutInfo sessionTimeoutInfo) {
        this.sessionTimeoutInfo = sessionTimeoutInfo;
    }
    
    public void setExceptionProcessingConfiguration(ExceptionProcessingConfiguration exceptionProcessingConfig) {
        this.exceptionProcessingConfig = exceptionProcessingConfig;
    }
    
    public void setTenantInfo(TenantInfo tenantInfo) {
        this.tenantInfo = tenantInfo;
    }

    public void setLanguageInfo(LanguageInfo languageInfo) {
        this.languageInfo = languageInfo;
    }
    
    public void setSiteMap(SiteMap siteMap) {
        this.siteMap = siteMap;
    }
    
    public void setPageMap(PageMap pageMap) {
        this.pageMap = pageMap;
    }
}
