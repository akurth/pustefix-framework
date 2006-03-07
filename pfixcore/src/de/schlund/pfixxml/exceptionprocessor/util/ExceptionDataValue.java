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

package de.schlund.pfixxml.exceptionprocessor.util;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;

/**
 * @author jh
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ExceptionDataValue implements Serializable {
	private Throwable throwable;
	private String scheme;
	private String servername;
	private int port;
	private String uri;
	private String sessionid;
	private String page;
	private String servlet;
	private String query;
	private HashMap requestParams;
	private ArrayList lastSteps;
	private HashMap sessionKeysAndValues;
	private Integer hashCodeForThrowable;
    
    private String text;
    private String subject;
    private Document xmlpresentation;
	
	/**
	 * @return Returns the sessionKeysAndValues.
	 */
	public HashMap getSessionKeysAndValues() {
		return sessionKeysAndValues;
	}
	/**
	 * @param sessionKeysAndValues The sessionKeysAndValues to set.
	 */
	public void setSessionKeysAndValues(HashMap sessionKeysAndValues) {
		this.sessionKeysAndValues = sessionKeysAndValues;
	}
	/**
	 * @return Returns the query.
	 */
	public String getQuery() {
		return query;
	}
	/**
	 * @param query The query to set.
	 */
	public void setQuery(String query) {
		this.query = query;
	}
	public void accept(ExceptionDataValueVisitor v) {
		v.visit(this);
	}
	
	
	/**
	 * @return Returns the page.
	 */
	public String getPage() {
		return page;
	}
	/**
	 * @param page The page to set.
	 */
	public void setPage(String page) {
		this.page = page;
	}
	/**
	 * @return Returns the servlet.
	 */
	public String getServlet() {
		return servlet;
	}
	/**
	 * @param servlet The servlet to set.
	 */
	public void setServlet(String servlet) {
		this.servlet = servlet;
	}
	
	
	public void setThrowable(Throwable th) {
		this.throwable = th;
		hashCodeForThrowable = null;
	}
	
	public Throwable getThrowable() {
		return this.throwable;
	}
	
	public int getHashcodeForThrowable() {
		if(hashCodeForThrowable == null) {
			StackTraceElement[] strace = throwable.getStackTrace();
			StringBuffer sb = new StringBuffer();
			for(int i=0; i<strace.length; i++) {
				sb.append(strace[i].toString());
			}
			return sb.toString().hashCode();
		} 
		return hashCodeForThrowable.intValue();
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("[Throwable="+printThrowable()+"][hashCodeForThrowable="+hashCodeForThrowable+"]");
		sb.append("[Scheme="+scheme+"]");
		sb.append("[Servername="+servername+"]");
		sb.append("[Port="+port+"]");
		sb.append("[Uri="+uri+"]");
		sb.append("[SessionId="+sessionid+"]");
		sb.append("[Servlet="+servlet+"]");
		sb.append("[Page="+page+"]");
		sb.append("[QueryString="+query+"]");
		sb.append("[RequestParams=[");
		if(requestParams == null) {
			sb.append("null]]");
		} else {
			for(Iterator iter = requestParams.keySet().iterator(); iter.hasNext();) {
				Object key = iter.next();
				Object value = requestParams.get(key);
				sb.append("["+key+"=>"+value+"]");
			}
			sb.append("]");
		}
		
		sb.append("[Last steps=[");
		if(lastSteps == null) {
			sb.append("null]]");
		} else {
			for(Iterator iter = lastSteps.iterator(); iter.hasNext(); ) {
				sb.append("["+iter.next()+"]");
			}
			sb.append("]");
		}
		
		sb.append("[Session keys and values=[");
		if(sessionKeysAndValues == null) {
			sb.append("null]]");
		} else {
			for(Iterator iter = sessionKeysAndValues.keySet().iterator(); iter.hasNext(); ) {
				Object key = iter.next();
				Object value = sessionKeysAndValues.get(key);
				sb.append("["+key+"=>"+value);
			}
			sb.append("\n");
		}
		
		return sb.toString();
	}
	
	private String printThrowable() {
		StringBuffer sb = new StringBuffer();
		sb.append("[Message="+throwable.getMessage()+"]");
		sb.append("[Stacktrace=");
		StackTraceElement[] st = throwable.getStackTrace();
		for(int i=0; i< st.length; i++) {
			sb.append("["+st[i].toString()+"]");
		}
		sb.append("]");
		return sb.toString();
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}
    
	/**
	 * @return Returns the port.
	 */
	public int getPort() {
		return port;
	}
	/**
	 * @param port The port to set.
	 */
	public void setPort(int port) {
		this.port = port;
	}
	/**
	 * @return Returns the scheme.
	 */
	public String getScheme() {
		return scheme;
	}
	/**
	 * @param scheme The scheme to set.
	 */
	public void setScheme(String scheme) {
		this.scheme = scheme;
	}
	/**
	 * @return Returns the servername.
	 */
	public String getServername() {
		return servername;
	}
	/**
	 * @param servername The servername to set.
	 */
	public void setServername(String servername) {
		this.servername = servername;
	}
	/**
	 * @return Returns the sessionid.
	 */
	public String getSessionid() {
		return sessionid;
	}
	/**
	 * @param sessionid The sessionid to set.
	 */
	public void setSessionid(String sessionid) {
		this.sessionid = sessionid;
	}
	/**
	 * @return Returns the uri.
	 */
	public String getUri() {
		return uri;
	}
	/**
	 * @param uri The uri to set.
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}
	/**
	 * @return Returns the sessionKeysNValues.
	 */
	public HashMap getRequestParams() {
		return requestParams;
	}
	/**
	 * @param sessionKeysNValues The sessionKeysNValues to set.
	 */
	public void setRequestParams(HashMap sessionKeysNValues) {
		this.requestParams = sessionKeysNValues;
	}
	/**
	 * @return Returns the lastSteps.
	 */
	public List getLastSteps() {
		return lastSteps;
	}
	/**
	 * @param lastSteps The lastSteps to set.
	 */
	public void setLastSteps(ArrayList lastSteps) {
		this.lastSteps = lastSteps;
	}
    
    public void setTextBodyRepresentation(String text) {
        this.text =text;
    }
    public void setTextSubjectRepresentation(String text) {
        this.subject = text;
    }
    
    public void setXMLRepresentation(Document doc) {
        this.xmlpresentation = doc;
    }
    
    public Document getXMLPresentation() {
        return this.xmlpresentation;
    }
    
    public String getTextSubject() {
        return this.subject;
    }
    
    public String getTextBody() {
        return this.text;
    }
}
