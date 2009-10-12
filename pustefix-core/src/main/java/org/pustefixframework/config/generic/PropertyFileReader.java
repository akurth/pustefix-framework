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

package org.pustefixframework.config.generic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.pustefixframework.config.customization.PropertiesBasedCustomizationInfo;
import org.pustefixframework.config.customization.RuntimeProperties;
import org.pustefixframework.resource.InputStreamResource;
import org.xml.sax.InputSource;

import com.marsching.flexiparse.parser.OSGiAwareParser;
import com.marsching.flexiparse.parser.exception.ParserException;


/**
 * Helper class providing methods to read in Properties from customizable
 * Pustefix XML property files 
 * 
 * @author mleidig
 *
 */
public class PropertyFileReader {
    
    public static void read(File file, BundleContext bundleContext, Properties properties) throws ParserException, FileNotFoundException {
        InputSource in = new InputSource(new FileInputStream(file));
        in.setSystemId(file.toURI().toASCIIString());
        read(in, bundleContext, properties);
    }
    
    public static void read(InputStreamResource resource, BundleContext bundleContext, Properties properties) throws ParserException, IOException {
        InputSource in = new InputSource(resource.getInputStream());
        in.setSystemId(resource.getURI().toASCIIString());
        read(in, bundleContext, properties);
    }
    
    public static void read(InputStream in, BundleContext bundleContext, Properties properties) throws ParserException { 
        read(new InputSource(in), bundleContext, properties);
    }
    
    public static void read(InputSource in, BundleContext bundleContext, Properties properties) throws ParserException {
        
        PropertiesBasedCustomizationInfo customizationInfo = new PropertiesBasedCustomizationInfo(RuntimeProperties.getProperties());
        OSGiAwareParser parser = new OSGiAwareParser(bundleContext, "META-INF/org/pustefixframework/config/generic/properties-config.xml");
        
        parser.parse(in, customizationInfo, properties);

        //TODO: alternative error check and support of empty property files 
        if(properties.size()==0) throw new ParserException("No properties found. Check if your property file "+
                "is valid and uses the correct XML namespace.");
    }
    
}
