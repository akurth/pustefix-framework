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
import org.xml.sax.SAXException;

import de.schlund.pfixcore.editor2.core.exception.EditorInitializationException;
import de.schlund.pfixxml.util.XPath;

public class ConfigurationServiceImpl implements ConfigurationService {
    private HashMap map;
    
    public ConfigurationServiceImpl(FileSystemService filesystem,
            PathResolverService pathresolver, String projectsFile) throws FileNotFoundException, SAXException, IOException, ParserConfigurationException, EditorInitializationException {
        Document doc = filesystem.readXMLDocumentFromFile(new File(pathresolver.resolve(projectsFile)));
        List nlist;
        this.map = new HashMap();
        try {
            nlist = XPath.select(doc.getParentNode(), "common/namespaces/namespace-declaration");
        } catch (TransformerException e) {
            // Should never happen
            String err = "XPath error!";
            Logger.getLogger(this.getClass()).error(err, e);
            throw new RuntimeException(err, e);
        }
        for (Iterator i = nlist.iterator(); i.hasNext();) {
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
    }

    public Map getPrefixToNamespaceMappings() {
        return new HashMap(this.map);
    }

}
