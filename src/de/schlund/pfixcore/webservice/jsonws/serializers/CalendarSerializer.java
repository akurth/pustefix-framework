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

package de.schlund.pfixcore.webservice.jsonws.serializers;

import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;

import de.schlund.pfixcore.webservice.jsonws.CustomJSONObject;
import de.schlund.pfixcore.webservice.jsonws.SerializationContext;
import de.schlund.pfixcore.webservice.jsonws.SerializationException;
import de.schlund.pfixcore.webservice.jsonws.Serializer;

public class CalendarSerializer extends Serializer {

    @Override
    public Object serialize(SerializationContext ctx, Object obj) throws SerializationException {
        if(obj instanceof Calendar) {
            String json="new Date("+((Calendar)obj).getTimeInMillis()+")";
            return new CustomJSONObject(json);
        } else throw new SerializationException("Can't serialize object of instance "+obj.getClass().getName());
    }
    
    @Override
    public void serialize(SerializationContext ctx, Object obj, Writer writer) throws SerializationException, IOException {
        if(obj instanceof Calendar) {
            writer.write("new Date("+((Calendar)obj).getTimeInMillis()+")");
        } else throw new SerializationException("Can't serialize object of instance "+obj.getClass().getName());
    }
    
}
