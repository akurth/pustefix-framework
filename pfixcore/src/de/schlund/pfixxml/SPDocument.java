/* This file is part of PFIXCORE.
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

import de.schlund.pfixxml.serverutil.*;
import de.schlund.pfixxml.util.Xml;
import java.util.*;

import javax.servlet.http.*;

import org.w3c.dom.*;


/**
 *
 *
 */
public class SPDocument implements NoCopySessionData {

    //~ Instance/static variables ..................................................................

    private Document   document;
    private Properties properties;
    private boolean    nostore     = false;
    private String     pagename    = null;
    private String     xslkey      = null;
    private long       timestamp   = -1;
    private int        error       = 0;
    private String     errortext   = null;
    private String     contenttype = null;
    private HashMap    header      = new HashMap();
    private ArrayList  cookies     = new ArrayList();

    //~ Methods ....................................................................................

    // Pagename is the preferred way to specify the target
    public void setPagename(String pagename) {
        this.pagename = pagename;
    }

    public String getPagename() {
        return pagename;
    }

    public void setNostore(boolean nostore) {
        this.nostore = nostore;
    }

    public boolean getNostore() {
        return nostore;
    }
    
    public void setResponseContentType(String type) {
        contenttype = type;
    }

    public String getResponseContentType() {
        return contenttype;
    }

    public void setResponseErrorText(String err) {
        errortext = err;
    }

    public String getResponseErrorText() {
        return errortext;
    }

    public void setResponseError(int err) {
        error = err;
    }

    public int getResponseError() {
        return error;
    }

    public void addResponseHeader(String key, String val) {
        header.put(key, val);
    }

    public HashMap getResponseHeader() {
        return header;
    }

    public void storeFrameAnchors(Map anchors) {
        if (document == null) {
            throw new RuntimeException("*** Can't store anchors into a null Document ***");
        }
        Element root = document.getDocumentElement();
        for (Iterator i = anchors.keySet().iterator(); i.hasNext();) {
            String  frame  = (String) i.next();
            String  anchor = (String) anchors.get(frame);
            Element elem   = document.createElement("frameanchor");
            elem.setAttribute("frame", frame);
            elem.setAttribute("anchor", anchor);
            root.appendChild(elem);
        }
    }

    /**
     * Describe <code>setTimestamp</code> method here.
     *
     * @param stamp a <code>long</code> value
     */
    public void setTimestamp(long stamp) {
        timestamp = stamp;
    }

    /**
     * Describe <code>getTimestamp</code> method here.
     *
     * @return a <code>long</code> value
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Describe <code>addCookie</code> method here.
     *
     * @param cookie a <code>Cookie</code> value
     */
    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    /**
     * Describe <code>getCookies</code> method here.
     *
     * @return an <code>ArrayList</code> value
     */
    public ArrayList getCookies() {
        return cookies;
    }

    /**
     * Describe <code>setCookies</code> method here.
     *
     * @param newcookies an <code>ArrayList</code> value
     */
    public void setCookies(ArrayList newcookies) {
        cookies = newcookies;
    }

    /**
     * Describe <code>getXSLKey</code> method here.
     *
     * @return a <code>String</code> value
     */
    public String getXSLKey() {
        return xslkey;
    }
    
    /**
     * Insert the method's description here.
     * Creation date: (05/10/00 19:46:18)
     * @return org.w3c.dom.Document
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Insert the method's description here.
     * Creation date: (05/10/00 19:46:18)
     * @return java.util.Properties
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Insert the method's description here.
     * Creation date: (05/10/00 19:46:18)
     * @param newDocument org.w3c.dom.Document
     */
    public void setDocument(Document newDocument) {
        document = newDocument;
    }

    /**
     * Insert the method's description here.
     * Creation date: (05/10/00 19:46:18)
     * @param newProperties java.util.Properties
     */
    public void setProperties(Properties newProperties) {
        properties = newProperties;
    }

    public void setProperty(String key, String value) {
        if (properties == null) {
            properties = new Properties();
        }
        properties.setProperty(key, value);
    }

    /**
     * Describe <code>setXSLKey</code> method here.
     *
     * @param xslkey a <code>String</code> value
     */
    public void setXSLKey(String xslkey) {
        this.xslkey = xslkey;
    }

    /**
     * Describe <code>toString</code> method here.
     *
     * @return a <code>String</code> value
     */
    public String toString() {
        Document tmp = document;
        StringBuffer sw = new StringBuffer();
        sw.append("\n");
        if (tmp == null) {
            sw.append("null\n");
        } else {
            sw.append("[class: " + tmp.getClass().getName() + "]\n");
            sw.append(Xml.serialize(tmp, true, true));
        }
        return sw.toString();
    }
}
