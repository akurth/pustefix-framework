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
 */

package de.schlund.pfixcore.editor2.core.spring;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import de.schlund.pfixcore.editor2.core.exception.EditorInitializationException;
import de.schlund.pfixxml.util.XPath;

public class ConfigurationServiceImpl implements ConfigurationService {
    private HashMap<String, String> map;
    private PathResolverService pathresolver;
    private FileSystemService filesystem;
    private String projectsFile;
    private boolean includePartsEditableByDefault = true;
    
    public void setPathResolverService(PathResolverService pathresolver) {
        this.pathresolver = pathresolver;
    }
    
    public void setFileSystemService(FileSystemService filesystem) {
        this.filesystem = filesystem;
    }
    
    public void setProjectsFilePath(String path) {
        this.projectsFile = path;
    }
    
    public void init() throws FileNotFoundException, SAXException, IOException, ParserConfigurationException, EditorInitializationException {
        Document doc = filesystem.readXMLDocumentFromFile(new File(pathresolver.resolve(projectsFile)));
        List<Node> nlist;
        this.map = new HashMap<String, String>();
        try {
            nlist = XPath.select(doc.getDocumentElement(), "common/namespaces/namespace-declaration");
        } catch (TransformerException e) {
            // Should never happen
            String err = "XPath error!";
            Logger.getLogger(this.getClass()).error(err, e);
            throw new RuntimeException(err, e);
        }
        for (Iterator<Node> i = nlist.iterator(); i.hasNext();) {
            Element node = (Element) i.next();
            if (!node.hasAttribute("prefix")) {
                String err = "Mandatory attribute prefix is missing for tag namespace-declaration!";
                Logger.getLogger(this.getClass()).error(err);
                throw new EditorInitializationException(err);
            }
            if (!node.hasAttribute("url")) {
                String err = "Mandatory attribute url is missing for tag namespace-declaration!";
                Logger.getLogger(this.getClass()).error(err);
                throw new EditorInitializationException(err);
            }
            String prefix = node.getAttribute("prefix");
            String url = node.getAttribute("url");
            this.map.put(prefix, url);
        }

        try {
            nlist = XPath.select(doc.getDocumentElement(), "common/editor/include-parts-editable-by-default");
        } catch (TransformerException e) {
            // Should never happen
            String err = "XPath error!";
            Logger.getLogger(this.getClass()).error(err, e);
            throw new RuntimeException(err, e);
        }
        if (nlist.size() > 0) {
            Element includePartsEditableByDefaultElement = (Element) nlist.get(0);
            if (includePartsEditableByDefaultElement != null) {
                this.includePartsEditableByDefault  = Boolean.parseBoolean(includePartsEditableByDefaultElement.getTextContent());
            }
        }
    }

    public Map<String, String> getPrefixToNamespaceMappings() {
        return new HashMap<String, String>(this.map);
    }

    public boolean isIncludePartsEditableByDefault() {
        return includePartsEditableByDefault;
    }

}