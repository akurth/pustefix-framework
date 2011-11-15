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
package de.schlund.pfixcore.oxm.impl.serializers;

import de.schlund.pfixcore.oxm.impl.ComplexTypeSerializer;
import de.schlund.pfixcore.oxm.impl.SerializationContext;
import de.schlund.pfixcore.oxm.impl.SerializationException;
import de.schlund.pfixcore.oxm.impl.XMLWriter;

/**
 * CDataSerializer
 * 
 * Writes a string as character data section
 * to the document.
 *  
 * @author  Stephan Schmidt <schst@stubbles.net>
 */
public class CDataSerializer implements ComplexTypeSerializer {

	public CDataSerializer() {
	}

	public void serialize(Object obj, SerializationContext context, XMLWriter writer) throws SerializationException {
        if (!(obj instanceof String)) {
            throw new SerializationException("Type not supported: "+obj.getClass().getName());
        }
        String cdata = (String)obj;
        writer.writeCDataSection(cdata);
    }
}