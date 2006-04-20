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

package de.schlund.pfixcore.workflow.app;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import de.schlund.pfixcore.generator.IHandler;
import de.schlund.pfixcore.generator.IWrapper;
import de.schlund.pfixcore.generator.IWrapperParam;
import de.schlund.pfixcore.generator.RequestData;
import de.schlund.pfixcore.generator.StatusCodeInfo;
import de.schlund.pfixcore.workflow.Context;
import de.schlund.pfixcore.workflow.StateImpl;
import de.schlund.pfixxml.AbstractXMLServer;
import de.schlund.pfixxml.PfixServletRequest;
import de.schlund.pfixxml.RequestParam;
import de.schlund.pfixxml.ResultDocument;
import de.schlund.pfixxml.XMLException;
import de.schlund.pfixxml.config.PageRequestConfig;
import de.schlund.pfixxml.loader.AppLoader;

/**
 * DefaultAuthIWrapperState.java
 *
 *
 * Created: Wed Dec 07 19:23:49 2001
 *
 * @author <a href="mailto:jtl@schlund.de">Jens Lautenbacher</a>
 * @version $Id$
 *
 *
 */

public class DefaultAuthIWrapperState extends StateImpl {
    /**
     * This returns alwys true. I don't know why you'd want to overwrite
     * this with a different behaviour.
     *
     * @param context a <code>Context</code> value
     * @param req a <code>HttpServletRequest</code> value
     * @return a <code>boolean</code> value
     * @exception Exception if an error occurs
     * @see de.schlund.pfixcore.workflow.State#isAccessible(Context, PfixServletRequest) 
     */
    public boolean isAccessible(Context context, PfixServletRequest preq) throws Exception {
        return true;
    }

    public boolean needsData(Context context, PfixServletRequest preq) throws Exception {
        IWrapper userwrapper = getAuthWrapper(context, false);
        IHandler userhandler = userwrapper.gimmeIHandler();
        
        if (userhandler.needsData(context)) {
            return true;
        } else {
            RequestData rdata = new RequestDataImpl(context, preq);
            ArrayList   aux   = getAuxWrapper(context);
            auxLoadData(aux, context, rdata);
            return false;
        }
    }

    private ArrayList getAuxWrapper(Context context) throws Exception {
        ArrayList  aux       = new ArrayList();
        PageRequestConfig config = context.getConfigForCurrentPageRequest();
        Map<String, Class> auxwrp = config.getAuxWrappers();
        AppLoader  appLoader = AppLoader.getInstance();
        
        if (auxwrp != null) {
            for (Iterator i = auxwrp.keySet().iterator(); i.hasNext(); ) {
                String prefix = (String) i.next(); 
                String iface  = auxwrp.get(prefix).getName();
                if (iface.equals("")) {
                    throw new XMLException("FATAL: No interface for prefix " + prefix);
                }
                Class auxwrapper = null;
                if (appLoader.isEnabled()) {
                    auxwrapper = appLoader.loadClass(iface);
                } else {
                    auxwrapper = Class.forName(iface);
                }
                IWrapper wrapper = (IWrapper) auxwrapper.newInstance();
                wrapper.init(prefix);
                aux.add(wrapper);
            }
        }
        return aux;
    }

    private IWrapper getAuthWrapper(Context context, boolean do_init) throws Exception {
        String     pagename  = context.getCurrentPageRequest().getName();
        PageRequestConfig config = context.getConfigForCurrentPageRequest();
        AppLoader  appLoader = AppLoader.getInstance();
        
        String authprefix = config.getAuthWrapperPrefix();
        String authwrapper = config.getAuthWrapperClass().getName();
        
        if (authprefix == null || authwrapper == null) {
            String msg;
            if (authprefix == null) {
                msg = "authprefix == null (" + pagename + ")";
            } else {
                msg = "authwrapper == null  (" + pagename + ")";
            }
            throw new XMLException("FATAL: Need exactly one interface definition for authpage! " + msg);
        }

        if (authwrapper.equals("")) {
            throw new XMLException("*** No interface for prefix " + authprefix);
        }

        CAT.debug("===> authorisation handler: " + authprefix + " => " + authwrapper);
        Class     thewrapper = null;
        
        if (appLoader.isEnabled()) {
            thewrapper = appLoader.loadClass(authwrapper);
        } else {
            thewrapper = Class.forName(authwrapper);
        }
        IWrapper user = (IWrapper) thewrapper.newInstance();
        if (do_init) {
            user.init(authprefix);
        }
        return user;
    }
    
    /**
     * In this method, we decide if the Session is authenticated or not. If the session is not
     * authenticated, we want to return a page to the user to query for authentication information.
     *
     * @param context a <code>Context</code> value
     * @param req a <code>HttpServletRequest</code> value
     * @return a <code>SPDocument</code> value
     * @exception Exception if an error occurs
     * @see de.schlund.pfixcore.workflow.State#getDocument(Context, PfixServletRequest) 
     */
    public ResultDocument getDocument(Context context, PfixServletRequest preq) throws Exception {
        IWrapper       user        = getAuthWrapper(context, true);
        IHandler       userhandler = user.gimmeIHandler();
        Properties     properties  = context.getProperties();
        ResultDocument resdoc      = super.createDefaultResultDocument(context);
        RequestData    rdata       = new RequestDataImpl(context, preq);
        ArrayList      aux         = getAuxWrapper(context);

        // Two cases: we are actively submitting data, or not.
        // If we are, we always try to authenticate against supplied user data.
        if (isSubmitAuthTrigger(context, preq)) {
            CAT.debug("====> Handling AUTHDATA SUBMIT");
            user.load(rdata);
            if (user.errorHappened()) { // during loading of the wrapper...
                userhandler.retrieveCurrentStatus(context, user);
                CAT.debug("====> Error during loading of wrapper data");
                // Try loading the aux interfaces, just to echo the stringvals.
                // so no error handling needs to take place.
                auxEchoData(aux, rdata, resdoc);
                userInsertErrors(properties, user, resdoc);
                context.prohibitContinue();
            } else {
                CAT.debug("====> Calling handleSubmittedData on " + userhandler.getClass().getName());
                userhandler.handleSubmittedData(context, user);
                if (user.errorHappened()) { // during trying to authenticate
                    CAT.debug("====> Error during submit handling");
                    userhandler.retrieveCurrentStatus(context, user);
                    // Try loading the aux interfaces, just to echo the stringvals.
                    // so no error handling needs to take place.
                    auxEchoData(aux, rdata, resdoc);
                    userInsertErrors(properties, user, resdoc);
                    context.prohibitContinue();
                } else {
                    // Try loading the aux interfaces, and call
                    // their handlers if no error happened.
                    auxLoadData(aux, context, rdata);
                }
            }
        } else { // No data is actually submitted
            userhandler.retrieveCurrentStatus(context, user);
            // Try loading the aux interfaces, just to echo the stringvals.
            // so no error handling needs to take place.
            auxEchoData(aux, rdata, resdoc);
            userInsertErrors(properties, user, resdoc);
            context.prohibitContinue();
        }
        // Make sure that the next selected page will really build up a new navigation structure.
        context.invalidateNavigation();
        return resdoc;
    }

    private void userInsertErrors(Properties props, IWrapper user, ResultDocument resdoc) {
        IWrapperParam[] pinfos = user.gimmeAllParams();
        String          prefix = user.gimmePrefix();
        for (int i = 0; i < pinfos.length; i++) {
            IWrapperParam pinfo  = pinfos[i];
            StatusCodeInfo[]  scodeinfos = pinfo.getStatusCodeInfos();
            String            name   = pinfo.getName();
            String[]          value  = pinfo.getStringValue();
            if (value != null) {
                for (int j = 0; j < value.length; j++) {
                    resdoc.addValue(prefix + "." + name, value[j]);
                }
            }
            if (scodeinfos != null) {
                for (int j = 0; j < scodeinfos.length; j++) {
                    StatusCodeInfo sci = scodeinfos[j];
                    resdoc.addStatusCode(props, sci.getStatusCode(), sci.getArgs(), sci.getLevel(), prefix + "." + name);
                }
            }
        }
    }

    private void auxEchoData(ArrayList aux, RequestData rdata, ResultDocument resdoc) throws Exception {
        for (Iterator i = aux.iterator(); i.hasNext(); ) {
            IWrapper tmp = (IWrapper) i.next();
            tmp.load(rdata);
            IWrapperParam[] params = tmp.gimmeAllParams();
            String          prefix = tmp.gimmePrefix();
            for (int j = 0; j < params.length; j++) {
                IWrapperParam par  = params[j];
                String[]          sarr = par.getStringValue();
                String            val  = "";
                if (sarr != null && sarr.length > 0) {
                    val = sarr[0];
                }
                // put the stringvals into hidden variables,
                // so the next submit will supply them again.
                resdoc.addHiddenValue(prefix + "." + par.getName(), val);
            }
        }
        for (Iterator i = rdata.getParameterNames(); i.hasNext(); ) {
            String name = (String) i.next();
            if (name.equals(AbstractXMLServer.PARAM_ANCHOR)) {
                RequestParam[] vals = rdata.getParameters(name);
                for (int j = 0; j < vals.length; j++) {
                    resdoc.addHiddenValue(name, vals[j].getValue());
                }
            }
        }
    }
    
    private void auxLoadData(ArrayList aux, Context context, RequestData rdata) throws Exception {
        for (Iterator i = aux.iterator(); i.hasNext(); ) {
            IWrapper tmp = (IWrapper) i.next();
            IHandler hnd = tmp.gimmeIHandler();
            tmp.load(rdata);
            if (!tmp.errorHappened()) {
                hnd.handleSubmittedData(context, tmp);
            }
            // No error handling here!! if it didn't work --- bad luck.
            // We still return null, as the authentication is OK
        }
    }
}
