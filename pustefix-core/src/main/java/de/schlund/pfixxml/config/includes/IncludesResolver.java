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

package de.schlund.pfixxml.config.includes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.marsching.flexiparse.util.DOMBasedNamespaceContext;

import de.schlund.pfixcore.util.ModuleInfo;
import de.schlund.pfixxml.resources.Resource;
import de.schlund.pfixxml.resources.ResourceUtil;
import de.schlund.pfixxml.util.Generics;
import de.schlund.pfixxml.util.XPath;
import de.schlund.pfixxml.util.Xml;

public class IncludesResolver {
    
    private final static String CONFIG_EXCLUDE_FILE = "/META-INF/config-excludes.txt";
    
    private List<FileIncludeEventListener> listeners = new ArrayList<FileIncludeEventListener>();

    private String namespace;
    
    private String includeTag;
    
    private final static String TAGNAME = "config-include";
    
    private final static String CONFIG_FRAGMENTS_NS = "http://www.pustefix-framework.org/2008/namespace/config-fragments";
    
    private final static String CONFIG_FRAGMENTS_ROOT_TAG = "config-fragments";
    
    private ThreadLocal<Set<String>> includesList = new ThreadLocal<Set<String>>();

    public IncludesResolver(String namespace) {
        this(namespace, TAGNAME);
    }
    
    public IncludesResolver(String namespace, String includeTag) {
        this.namespace = namespace;
        this.includeTag = includeTag;
    }

    public void registerListener(FileIncludeEventListener listener) {
        listeners.add(listener);
    }

    public void resolveIncludes(Document doc) throws SAXException {
        List<Element> nodes;
        try {
            nodes = Generics.convertList(XPath.select(doc, "//*[local-name()='" + includeTag + "']"));
        } catch (TransformerException e) {
            throw new RuntimeException("Unexpected XPath error!");
        }
        for (Element elem : nodes) {
            if ((namespace == null && elem.getNamespaceURI() != null) || (namespace != null && !namespace.equals(elem.getNamespaceURI()))) {
                continue;
            }

            String xpath = null, refid = null, section = null;
            if (elem.hasAttribute("xpath")) {
                    xpath = elem.getAttribute("xpath");
            }
            if (elem.hasAttribute("refid")) {
                refid = elem.getAttribute("refid");
            }
            if (elem.hasAttribute("section")) {
                section = elem.getAttribute("section");
            }
            
            if (xpath != null) {
                if (refid != null || section != null) {
                    throw new SAXException("Only one of the \"xpath\", \"refid\" or \"section\" attributes may be supplied to the include tag!");
                }
                // Just use the supplied XPath expression
            } else if (refid != null) {
                if (section != null || xpath != null) {
                    throw new SAXException("Only one of the \"xpath\", \"refid\" or \"section\" attributes may be supplied to the include tag!");
                }
                xpath = "/*[local-name() = '" + CONFIG_FRAGMENTS_ROOT_TAG + "' and namespace-uri()='" + CONFIG_FRAGMENTS_NS + "']/*[@id='" + refid + "']/node()";
            } else if (section != null) {
                if (xpath != null || refid != null) {
                    throw new SAXException("Only one of the \"xpath\", \"refid\" or \"section\" attributes may be supplied to the include tag!");
                }
                if (!checkSectionType(section)) {
                    throw new SAXException("\"" + section + "\" is not a valid include section type!");
                }
                xpath = "/*[local-name()='" + CONFIG_FRAGMENTS_ROOT_TAG + "' and namespace-uri()='" + CONFIG_FRAGMENTS_NS + "']/*[local-name()='" + section + "' and namespace-uri()='" + CONFIG_FRAGMENTS_NS + "']/node()";
            } else {
                throw new SAXException("One of the \"xpath\", \"refid\" or \"section\" attributes must be set for the include tag!");
            }

            String module = elem.getAttribute("module");
            if(module.equals("")) module = null;
            
            Set<String> excludedModules = new HashSet<String>();
            URL configExcludeFileURL = getClass().getResource(CONFIG_EXCLUDE_FILE);
            if(configExcludeFileURL != null) {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(configExcludeFileURL.openStream(), "UTF-8"));
                    String line;
                    while((line = reader.readLine()) != null) {
                        excludedModules.add(line);
                    }
                    reader.close();
                } catch(IOException x) {
                    throw new SAXException("Error while reading " + CONFIG_EXCLUDE_FILE, x);
                }
            }
            
            String filepath = elem.getAttribute("file");
            if (filepath == null) {
                throw new SAXException("The attribute \"file\" must be set for the include tag!");
            }
            List<String> includePaths = new ArrayList<String>();
            boolean wildcardInclude = false;
            if(module != null) {
                if(filepath.startsWith("/")) filepath = filepath.substring(1);
                if(module.contains("*")) {
                    Pattern modulePattern = Pattern.compile(module.replaceAll("\\*", ".*"));
                    Set<String> moduleNames = ModuleInfo.getInstance().getModules();
                    for(String moduleName: moduleNames) {
                        if(modulePattern.matcher(moduleName).matches() && !excludedModules.contains(moduleName)) {
                            includePaths.add("module://" + moduleName + "/" + filepath);
                        }
                    }
                    wildcardInclude = true;
                } else {
                    filepath = "module://" + module + "/" + filepath;
                    includePaths.add(filepath);
                }
            } else {
                includePaths.add(filepath);
            }
            
            for(String includePath: includePaths) {
                // Look if the same include has been performed ealier in the recursion
                // If yes, we have a cyclic dependency
                Set<String> list = includesList.get();
                if (list == null) {
                    list = new HashSet<String>();
                    includesList.set(list);
                }
                if (list.contains(includePath + "#" + xpath)) {
                    throw new SAXException("Cyclic dependency in include detected: " + includePath.toString());
                }
                
                Resource includeFile = ResourceUtil.getResource(includePath);
                if(includeFile.exists()) {
                    
                    Document includeDocument;
                    try {
                        includeDocument = Xml.parseMutable(includeFile);
                    } catch (IOException e) {
                        throw new SAXException("I/O exception on included file " + includeFile.toString(), e);
                    }
                    
                    if (!CONFIG_FRAGMENTS_NS.equals(includeDocument.getDocumentElement().getNamespaceURI()) || !CONFIG_FRAGMENTS_ROOT_TAG.equals(includeDocument.getDocumentElement().getLocalName())) {
                        throw new SAXException("File " + includePath + " seems not to be a valid configuration fragments file!");
                    }
        
                    String tupel = includePath + "#" + xpath;
                    list.add(tupel);
                    try {
                        resolveIncludes(includeDocument);
                    } finally {
                        list.remove(tupel);
                    }
                    
                    NodeList includeNodes;
                    try {
                        javax.xml.xpath.XPath xpathProc = XPathFactory.newInstance().newXPath();
                        xpathProc.setNamespaceContext(new DOMBasedNamespaceContext(elem));
                        includeNodes = (NodeList) xpathProc.evaluate(xpath, includeDocument, XPathConstants.NODESET);
                    } catch (XPathExpressionException e) {
                        throw new SAXException("XPath expression invalid: " + xpath, e);
                    }
                    
                    for (int i=0; i < includeNodes.getLength(); i++) {
                        Node node = includeNodes.item(i);
                        Node newNode = doc.importNode(node, true);
                        String definingModule = null;
                        URI uri = includeFile.toURI();
                        if("module".equals(uri.getScheme())) {
                            definingModule = uri.getAuthority();
                        }
                        setModuleUserData(newNode, definingModule);
                        elem.getParentNode().insertBefore(newNode, elem);
                    }
                    
                    // Trigger event
                    FileIncludeEvent ev = new FileIncludeEvent(this, includeFile);
                    for (FileIncludeEventListener listener : listeners) {
                        listener.fileIncluded(ev);
                    }
                } else if(!wildcardInclude) {
                    throw new SAXException("Included config fragment file '" + includeFile.toString() +"' doesn't exist.");
                }
            }
            elem.getParentNode().removeChild(elem);
        }
    }

    private void setModuleUserData(Node root, String module) {
        if(root.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element)root;
            if(element.getUserData("module") == null) {
                element.setUserData("module", module, null);
                NodeList nodes = element.getChildNodes();
                for(int i=0; i<nodes.getLength(); i++) {
                    Node node = nodes.item(i);
                    if(node.getNodeType() == Node.ELEMENT_NODE) {
                        setModuleUserData((Element)node, module == null ? "webapp" : module);
                    }
                }
            }
        }
    }
    
    private boolean checkSectionType(String section) {
        if (section.equals("targets") || section.equals("sitemap") || section.equals("pageflows") || 
            section.equals("pagerequests") || section.equals("properties") || section.equals("interceptors") || 
            section.equals("scriptedflows") || section.equals("roles") || section.equals("authconstraints") || 
            section.equals("conditions") || section.equals("resources") || section.equals("directoutputpagerequests") ||
            section.equals("tenants") || section.equals("preserve-params")) {
            return true;
        } else {
            return false;
        }
    }

}
