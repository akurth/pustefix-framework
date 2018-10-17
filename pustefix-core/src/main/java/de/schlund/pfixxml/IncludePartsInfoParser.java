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

package de.schlund.pfixxml;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import de.schlund.pfixxml.resources.Resource;
import de.schlund.pfixxml.util.Xml;

public class IncludePartsInfoParser {
 
    public static IncludePartsInfo parse(Resource resource) throws IncludePartsInfoParsingException {
        if(resource.exists()) {
            InputSource in = new InputSource();
            try {
                in.setByteStream(resource.getInputStream());
                in.setSystemId(resource.toURI().toASCIIString());
                IncludePartsInfo info = parse(in);
                info.setLastMod(resource.lastModified());
                return info;
            } catch(IOException x) {
                throw new IncludePartsInfoParsingException(resource.toURI().toString(), x);
            }
        } else {
            return null;
        }
    }
    
    public static IncludePartsInfo parse(InputSource source) throws IncludePartsInfoParsingException {
        Handler handler;
        try {
            XMLReader xr = Xml.createXMLReader();
            handler = new Handler();
            xr.setContentHandler(handler);
            xr.setErrorHandler(handler);
            xr.parse(source);
        } catch(IOException x) {
            throw new IncludePartsInfoParsingException(source.getSystemId(), x);
        } catch(SAXException x) {
            throw new IncludePartsInfoParsingException(source.getSystemId(), x);
        }
        return new IncludePartsInfo(handler.getParts());
    }
    
    static class Handler extends DefaultHandler {
        
        private int level;
        private boolean isIncludeParts;
        
        private IncludePartInfo currentInfo;
        
        private Map<String, IncludePartInfo> includePartInfos = new LinkedHashMap<String, IncludePartInfo>();
        
        public Map<String, IncludePartInfo> getParts() {
            return includePartInfos;
        }
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            level++;    
            if(level == 1) {
                if(localName.equals("include_parts")) {
                    isIncludeParts = true;
                }
            } else if(isIncludeParts) {
                if(level == 2) {
                    if(localName.equals("part")) {
                        String partName = attributes.getValue("name");
                        if(partName != null) {
                            partName = partName.trim();
                            boolean render = false;
                            String val = attributes.getValue("render");
                            if(val != null) {
                                render = Boolean.parseBoolean(val);
                            }
                            String[] variants = null;
                            val = attributes.getValue("render-variants");
                            if(val != null) {
                                val = val.trim();
                                variants = val.split("\\s+");
                            }
                            Set<String> variantSet = new HashSet<String>();
                            if(variants != null) {
                                for(String variant: variants) variantSet.add(variant);
                            }
                            String contentType = null;
                            contentType = attributes.getValue("content-type");
                            if(contentType != null) {
                                contentType = contentType.trim();
                            }
                            boolean contextual = false;
                            val = attributes.getValue("contextual");
                            if(val != null) {
                                contextual = Boolean.parseBoolean(val);
                            }
                            currentInfo = new IncludePartInfo(partName, render, variantSet, contentType, contextual);
                            includePartInfos.put(partName, currentInfo);
                        }
                    }
                } else if(level == 3) {
                    if(localName.equals("theme")) {
                        String themeName = attributes.getValue("name");
                        if(themeName != null) {
                            themeName = themeName.trim();
                            currentInfo.addTheme(themeName);
                        }
                    }
                }
            }
        }
        
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            level--;
        }
        
    }
    
}
