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

import de.schlund.pfixcore.util.PropertiesUtils;
import de.schlund.pfixxml.loader.*;
import de.schlund.util.FactoryInit;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Category;
import org.apache.log4j.spi.ThrowableInformation;
import org.apache.log4j.xml.DOMConfigurator;


/**
 * This Servlet is just there to have it's init method called on startup of the VM.
 * It starts all VM-global factories by calling their 'init' method from 
 * the {@link FactoryInit} interface. These factories are located by
 * analyzing the "servlet.propfile" parameter which points to a file where
 * all factories are listed.
 */
public class FactoryInitServlet extends HttpServlet implements Reloader {

    //~ Instance/static variables ..................................................................

    private Object         LOCK       = new Object();
    private Category       CAT        = Category.getInstance(FactoryInitServlet.class.getName());
    private static boolean configured = false;
    private ArrayList      factories;

    //~ Methods ....................................................................................

    /**
     * Handle the HTTP-Post method. 
     * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest, HttpServletResponse)
     * @throws ServletException on all
     */
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        doGet(req, res);
    }

    /**
     * Handle the HTTP-Get method 
     * @see javax.servlet.http.HttpServlet#doGet(HttpServletRequest, HttpServletResponse)
     * @throws ServletException on call
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        throw new ServletException("This servlet can't be called interactively");
    }

    /**
     * Initialize this servlet. Also call the 'init' method of all classes
     * listed in the configuration. These classes must implement
     * the FactoryInit interface.
     * @param config the servlet configuration 
     * @see javax.servlet.Servlet#init(ServletConfig)
     * @throws ServletException on errors
     */
    public void init(ServletConfig Config) throws ServletException {
        super.init(Config);
        Properties properties = new Properties(System.getProperties());
        String     confname   = Config.getInitParameter("servlet.propfile");
        if (confname != null) {
            try {
                properties.load(new FileInputStream(confname));
            } catch (FileNotFoundException e) {
                throw new ServletException("*** [" + confname + "] Not found: " + e.toString());
            }
             catch (IOException e) {
                throw new ServletException("*** [" + confname + "] IO-error: " + e.toString());
            }
        }
        synchronized (LOCK) {
            if (!configured) {
                if (properties != null) {
                    String log4jconfig = properties.getProperty("pustefix.log4j.config");
                    if (log4jconfig == null & log4jconfig.equals("")) {
                        throw new ServletException("*** FATAL: Need the pustefix.log4j.config property... ***");
                    }
                    DOMConfigurator.configure(log4jconfig);
                }
                CAT.debug(">>>> LOG4J Init OK <<<<");
                HashMap to_init = PropertiesUtils.selectProperties(properties, "factory.initialize");
                if (to_init != null) {
                    // sort key to initialize the factories in defined order
                    TreeSet keyset = new TreeSet(to_init.keySet());
                    for (Iterator i = keyset.iterator(); i.hasNext();) {
                        String key       = (String) i.next();
                        String the_class = (String) to_init.get(key);
                        try {
                            CAT.debug(">>>> Init key: [" + key + "] class: [" + the_class + "] <<<<");
                            AppLoader appLoader = AppLoader.getInstance();
                            long      start     = 0;
                            long      stop      = 0;
                            if (appLoader.isEnabled() && appLoader.isIncludedClass(the_class)) {
                                Class       clazz   = appLoader.loadClass(the_class);
                                FactoryInit factory = (FactoryInit) clazz.getMethod("getInstance",null).invoke(null,null);
                                CAT.debug("     Object ID: " + factory);
                                start               = System.currentTimeMillis();
                                factory.init(properties);
                                stop                = System.currentTimeMillis();
                                CAT.debug("Init of " + factory + " took " + (stop - start) + " ms");
                                if (factories == null) {
                                    factories = new ArrayList();
                                }
                                factories.add(factory);
                            } else {
                                FactoryInit factory = (FactoryInit) Class.forName(the_class).getMethod("getInstance", null).invoke(null, null);
                                CAT.debug("     Object ID: " + factory);
                                start               = System.currentTimeMillis();
                                factory.init(properties);
                                stop                = System.currentTimeMillis();
                                CAT.debug("Init of " + factory + " took " + (stop - start) + " ms");
                            }
                        } catch (Exception e) {
                            CAT.error(e.toString());
                            ThrowableInformation info     = new ThrowableInformation(e);
                            String[]             trace    = info.getThrowableStrRep();
                            StringBuffer         strerror = new StringBuffer();
                            for (int ii = 0; ii < trace.length; ii++) {
                                strerror.append("->" + trace[ii] + "\n");
                            }
                            CAT.error(strerror.toString());
                            throw new ServletException(e.toString());
                        }
                    }
                }
            }
            configured = true;
            CAT.debug("***** INIT of FactoryInitServlet done *****");
            
            AppLoader appLoader = AppLoader.getInstance();
            if (appLoader.isEnabled()) appLoader.addReloader(this);
        }
    }
    
    public static boolean isConfigured() {
    	return configured;
    }
    
    
    public void reload() {
        if (factories!=null) {
            ArrayList newFacs = new ArrayList();
            Iterator  it      = factories.iterator();
            while(it.hasNext()) {
                FactoryInit fac       = (FactoryInit) it.next();
                String      className = fac.getClass().getName();
                FactoryInit facNew    = (FactoryInit) StateTransfer.getInstance().transfer(fac);
                newFacs.add(facNew);
            }
            factories = newFacs;
        }
     }
}
