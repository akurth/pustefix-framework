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

package org.pustefixframework.webservices.jsonws.deserializers;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;

import org.pustefixframework.webservices.json.JSONObject;
import org.pustefixframework.webservices.jsonws.DeserializationContext;
import org.pustefixframework.webservices.jsonws.DeserializationException;
import org.pustefixframework.webservices.jsonws.Deserializer;


public class CalendarDeserializer extends Deserializer {
    
    @Override
    public boolean canDeserialize(DeserializationContext ctx, Object jsonValue, Type targetType) {
        Class<?> targetClass=(Class<?>)targetType;
        if( (jsonValue instanceof Calendar && (targetClass==Date.class || Calendar.class.isAssignableFrom(targetClass)))
            || (jsonValue instanceof JSONObject && ((JSONObject)jsonValue).hasMember("__time__")) ) {
            return true;
        }
        return false;
    }
    
    @Override
    public Object deserialize(DeserializationContext ctx,Object jsonValue,Type targetType) throws DeserializationException {
        Class<?> targetClass=(Class<?>)targetType;
        if(jsonValue instanceof Calendar) {
            if(targetClass==Date.class) {
                return ((Calendar)jsonValue).getTime();
            } 
            return jsonValue;
        } else if(jsonValue instanceof JSONObject && ((JSONObject)jsonValue).hasMember("__time__")) {
            long time = (Long)((JSONObject)jsonValue).getMember("__time__");
            if(targetClass == Date.class) {
                return new Date(time);
            } else {
                Calendar cal=Calendar.getInstance();
                cal.setTimeInMillis(time);
                return cal;
            }
        } else throw new DeserializationException("Wrong type: "+jsonValue.getClass().getName());
    }

}
