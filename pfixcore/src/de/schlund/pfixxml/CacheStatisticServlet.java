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


import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;

import de.schlund.pfixxml.targets.cachestat.SPCacheStatistic;
import de.schlund.pfixxml.testenv.XMLSerializeUtil;

/**
 * @author Joerg Haecker <haecker@schlund.de>
 *  
 */
public class CacheStatisticServlet extends HttpServlet {

    private static int OUTPUTXML=0;
    private static int OUTPUTTEXT=1;
    
    
    /**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        int outmode = OUTPUTXML;
        
        String outputmode_param = req.getParameter("mode");
      
        if(outputmode_param != null) {
            if(outputmode_param.equals("text")) {
                outmode = OUTPUTTEXT;
            }
        }
       
        String output;
        
        if(outmode == OUTPUTXML) {
            Document doc;
            try {
                doc = SPCacheStatistic.getInstance().getCacheStatisticAsXML();
            } catch (ParserConfigurationException e) {
                throw new ServletException(e);
            }
            res.setContentType("text/xml");
            output = XMLSerializeUtil.getInstance().serializeToString(doc);
        } else if(outmode == OUTPUTTEXT) {
            output = SPCacheStatistic.getInstance().getCacheStatisticAsString(); 
            res.setContentType("text/plain");
        } else {
            throw new ServletException("No outputmode set.");
        }
        ServletOutputStream ostream = res.getOutputStream();
        ostream.println(output);  
     
    }

    
    
    
    /**
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }


}
