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
package org.pustefixframework.container.spring.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

import org.pustefixframework.config.generic.PropertyFileReader;
import org.springframework.util.DefaultPropertiesPersister;

import com.marsching.flexiparse.parser.exception.ParserException;

/**
 * PropertyPersister implementation, which can read the Pustefix-XML-Property
 * format instead of the Java-Standard-XML-Property format.
 * 
 * @author mleidig
 *
 */
public class PustefixPropertiesPersister extends DefaultPropertiesPersister {

    @Override
    public void load(Properties props, InputStream in) throws IOException {
        super.load(props, in);
    }
    
    @Override
    public void load(Properties props, Reader reader) throws IOException {
        super.load(props, reader);
    }
    
    @Override
    public void loadFromXml(Properties properties, InputStream in) throws IOException { 
        try {
            PropertyFileReader.read(in, properties);
        } catch(ParserException x) {
            throw new IOException("Error reading XML properties: "+x.getMessage());
        }
    }
    
}