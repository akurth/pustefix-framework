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

package de.schlund.pfixxml.exceptionprocessor.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

import org.pustefixframework.xslt.XSLSourceLocator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.SAXParseException;

import de.schlund.pfixxml.resources.Resource;
import de.schlund.pfixxml.resources.ResourceUtil;
import de.schlund.pfixxml.util.Xml;
import de.schlund.pfixxml.util.Xslt;
import de.schlund.pfixxml.util.XsltExtensionFunctionException;
import de.schlund.pfixxml.util.XsltMessageTempStore;


/**
 * @author jh
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class XMLCreatorVisitor implements ExceptionDataValueVisitor {

	private enum ErrorType { JAVA, XSLT, XSLT_EXT };
	
	private Document doc;
	/* (non-Javadoc)
	 * @see de.schlund.jmsexceptionhandler.rmiobj.ExceptionDataValueVisitor#visit(de.schlund.jmsexceptionhandler.rmiobj.ExceptionDataValue)
	 */
	public void visit(ExceptionDataValue data) {
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		dbfac.setNamespaceAware(false);
		dbfac.setValidating(false);
		try {
			doc = dbfac.newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return;
		}
		Element e = doc.createElement("error");
		ErrorType errorType = getErrorType(data.getThrowable());
		e.setAttribute("type", errorType.toString().toLowerCase());
		
		Element sess_info = doc.createElement("sessioninfo");
		Text sess_info_txt = doc.createTextNode(data.getSessionid());
		sess_info.appendChild(sess_info_txt);
		e.appendChild(sess_info);
		
		Element req_params = doc.createElement("requestparams");
		HashMap<String, String> params = data.getRequestParams();
		for(Iterator<String> iter = params.keySet().iterator(); iter.hasNext(); ) {
			String key = iter.next();
			String value = params.get(key);
			Element req_p = doc.createElement("param");
			req_p.setAttribute("key", key);
			Text req_p_txt = doc.createTextNode(value);
			req_p.appendChild(req_p_txt);
			req_params.appendChild(req_p);
		}
		e.appendChild(req_params);
		
		Element last_steps = doc.createElement("laststeps");
		for(Iterator<String> iter = data.getLastSteps().iterator(); iter.hasNext(); ) {
			Element step = doc.createElement("step");
			Text step_txt = doc.createTextNode(iter.next());
			step.appendChild(step_txt);
			last_steps.appendChild(step);
		}
		e.appendChild(last_steps);
		
		Element sess_keysnvals = doc.createElement("session_dump");
		HashMap<String, String> map = data.getSessionKeysAndValues();
		for(Iterator<String> iter = map.keySet().iterator(); iter.hasNext(); ) {
			String key = iter.next();
			String val = map.get(key);
			Element pair = doc.createElement("pair");
			pair.setAttribute("key", key);
			Text cd = doc.createTextNode(val); 
			pair.appendChild(cd);
			sess_keysnvals.appendChild(pair);
		}
		e.appendChild(sess_keysnvals);
		
		appendThrowable(e, data.getThrowable());
		
		doc.appendChild(e);
        data.setXMLRepresentation(doc);
	}

	private void appendThrowable(Element elem, Throwable throwable) {
	    if(throwable!=null) {
	        Element exElem = doc.createElement("exception");
	        exElem.setAttribute("type", throwable.getClass().getName());
	        exElem.setAttribute("msg", throwable.getMessage());
            //add toString output if Throwable overrides default implementation
            if(overridesToString(throwable)) {
                String str = throwable.toString();
                if(str == null) {
                    str = "-";
                } else {
                    str = str.trim();
                }
                exElem.setAttribute("string", str);
            }
	        if(throwable instanceof TransformerException) {
	            TransformerException te = (TransformerException)throwable;
	            Element xsltInfo = doc.createElement("xsltinfo");
                exElem.appendChild(xsltInfo);
	            SourceLocator locator = te.getLocator();
	            if(locator!=null) {
	                if(locator instanceof XSLSourceLocator) {
	                    Element xmlInfo = doc.createElement("xmlinfo");
                        xsltInfo.appendChild(xmlInfo);
                        addLocatorInfo(xmlInfo, ((XSLSourceLocator)locator).getXmlLocator());
	                }
	                addLocatorInfo(xsltInfo, locator);
	            }
	            String messages = XsltMessageTempStore.removeMessages(te);
	            if(messages != null) {
	            	Element msgElem = doc.createElement("messages");
	            	xsltInfo.appendChild(msgElem);
	            	msgElem.setTextContent(messages);
	            }
	        } else if (throwable instanceof SAXParseException) {
	            SAXParseException spe = (SAXParseException)throwable;
	            Element xsltInfo = doc.createElement("xsltinfo");
                exElem.appendChild(xsltInfo);
                xsltInfo.setAttribute("line", String.valueOf(spe.getLineNumber()));
                xsltInfo.setAttribute("column", String.valueOf(spe.getColumnNumber()));
                xsltInfo.setAttribute("publicId", spe.getPublicId());
                xsltInfo.setAttribute("systemId", spe.getSystemId());
                if(spe.getLineNumber() > -1) {
                    String systemId = spe.getSystemId();
                    if(systemId != null && systemId.matches("^\\w+:.*")) {
                        try {
                            URI uri = new URI(systemId);
                            Resource res = ResourceUtil.getResource(uri);
                            String context = cut(res, "utf-8", spe.getLineNumber(), spe.getColumnNumber(),10, 10, 160);
                            xsltInfo.setAttribute("context", context);
                        } catch(Exception x) {
                            //ignore
                        }
                    }
                }
	        }
	        
	        Element stackElem = doc.createElement("stacktrace");
	        StackTraceElement[] strace = throwable.getStackTrace();
	        for(int i=0; i<strace.length; i++) {
	            Element lineElem = doc.createElement("line");
	            Text lineText = doc.createTextNode(strace[i].toString());
	            lineElem.appendChild(lineText);
	            stackElem.appendChild(lineElem);
	        }
	        exElem.appendChild(stackElem);
	        elem.appendChild(exElem);
	        if(throwable.getCause()!=null) appendThrowable(exElem, throwable.getCause());
	    }
	}
	
	private void addLocatorInfo(Element info, SourceLocator locator) {
	    
	    if(locator != null) {
    	    info.setAttribute("line", String.valueOf(locator.getLineNumber()));
            info.setAttribute("column", String.valueOf(locator.getColumnNumber()));
            info.setAttribute("publicId", locator.getPublicId());
            info.setAttribute("systemId", locator.getSystemId());
            String systemId = locator.getSystemId();
            if(systemId != null && systemId.matches("^\\w+:.*")) {
                try {
                    URI uri = new URI(systemId);
                    Resource res = ResourceUtil.getResource(uri);
                    String context = cut(res, "utf-8", locator.getLineNumber(), locator.getColumnNumber(), 10, 10, 160);
                    info.setAttribute("context", context);
                } catch(Exception x) {
                    //ignore
                }
            }
	    }
	}
	
	private ErrorType getErrorType(Throwable throwable) {
		Throwable cause = throwable.getCause();
		if(cause != null) {
			ErrorType errorType = getErrorType(cause);
			if(errorType != ErrorType.JAVA) {
				if(errorType == ErrorType.XSLT && throwable instanceof XsltExtensionFunctionException && !hasLocationInfo(cause)) {
					return ErrorType.XSLT_EXT;
				}
				return errorType;
			}
		}
		if(throwable instanceof SAXParseException && stackTraceContains(throwable, Xml.class.getName())) {
			return ErrorType.XSLT;
		} else if(throwable instanceof XsltExtensionFunctionException) {
			return ErrorType.XSLT_EXT;
		} else if(throwable instanceof TransformerException && stackTraceContains(throwable, Xslt.class.getName()) 
				&& !throwable.getMessage().contains("Exception in extension function")) {
			return ErrorType.XSLT;
		}	
		return ErrorType.JAVA;
	}
	
	private boolean hasLocationInfo(Throwable throwable) {
		if(throwable instanceof TransformerException) {
			if(((TransformerException)throwable).getLocator() != null) {
				return true;
			}
		} else if(throwable instanceof SAXParseException) {
			if(((SAXParseException)throwable).getLineNumber() > -1) {
				return true;
			}
		}
		if(throwable.getCause() == null) {
			return false;
		} else {
			return hasLocationInfo(throwable.getCause());
		}
	}
	
	private boolean stackTraceContains(Throwable cause, String className) {
		StackTraceElement[] elements = cause.getStackTrace();
		for(StackTraceElement element: elements) {
			if(element.getClassName().equals(className)) {
				return true;
			}
		}
		return false;
	}
	
    private static String cut(Resource res, String encoding, int line, int col, int before, int after, int maxLineLen) throws IOException {
        InputStream in = res.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, encoding));
        StringBuilder cutStr = new StringBuilder();
        String str = null;
        int currentLine = 0;
        String lineFormatStr = "%s %" + (("" + col).length() + 1) + "s ";
        try {
            while ((str = reader.readLine()) != null) {
                currentLine++;
                if(currentLine < line) {
                    if(line - currentLine <= before) {
                        cutStr.append(String.format(lineFormatStr, " ", currentLine));
                        if(str.length() > maxLineLen) str = str.substring(0, maxLineLen) + " ...";
                        cutStr.append(str).append("\n");
                    }
                } else if(currentLine == line) {
                    cutStr.append(String.format(lineFormatStr, "X", currentLine));
                    if(str.length() > maxLineLen) {
                        int left, right;
                        if(col > 0) {
                            if(col + maxLineLen/2 < str.length()) {
                                left = Math.max(0, col - maxLineLen/2);
                                right = Math.min(str.length(), col + maxLineLen/2 + (maxLineLen/2 - (col - left)));
                            } else {
                                right = Math.min(str.length(), col + maxLineLen/2);
                                left = Math.max(0, col - maxLineLen/2 - (maxLineLen/2 - (right - col)));
                            }
                            int origLen = str.length();
                            str = str.substring(left, right);
                            if(left > 0) str = " ... " + str;
                            if(right < origLen) str = str + " ...";
                        } else {
                            str = str.substring(0, maxLineLen) + " ...";
                        }
                    }
                    cutStr.append(str).append("\n");
                } else {
                    if(currentLine - line <= after) {
                        cutStr.append(String.format(lineFormatStr, " ", currentLine));
                        if(str.length() > maxLineLen) str = str.substring(0, maxLineLen) + " ...";
                        cutStr.append(str).append("\n");
                    }
                }
            }
        } finally {
            in.close();
        }
        return cutStr.toString();
    }

    private boolean overridesToString(Throwable t) {
        try {
            Method meth = t.getClass().getMethod("toString", new Class<?>[0]);
            //check if returned toString method is not declared by Throwable
            return meth.getDeclaringClass() != Throwable.class;
        } catch (NoSuchMethodException e) {
            //can be ignored because there's Throwable toString as fallback
        }
        return false;
    }

}
