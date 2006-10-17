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

package de.schlund.pfixcore.webservice.jsonws;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import de.schlund.pfixcore.webservice.jsonws.serializers.ArraySerializer;

public class SerializationContext {

    boolean doClassHinting;
    SerializerRegistry serReg;
    
    public SerializationContext(SerializerRegistry serReg,boolean doClassHinting) {
        this.serReg=serReg;
        this.doClassHinting=doClassHinting;
    }
    
    public Object serialize(Object obj) throws SerializationException {
        Class clazz=obj.getClass();
        Serializer ser=null;
        if(clazz.isArray()||List.class.isAssignableFrom(clazz)) ser=new ArraySerializer(); 
        else ser=serReg.getSerializer(obj);
        return ser.serialize(this,obj);
    }
    
    public void serialize(Object obj,Writer writer) throws SerializationException,IOException {
        Class clazz=obj.getClass();
        Serializer ser=null;
        if(clazz.isArray()||List.class.isAssignableFrom(clazz)) ser=new ArraySerializer(); 
        else ser=serReg.getSerializer(obj);
        ser.serialize(this,obj,writer);
    }
    
    public boolean doClassHinting() {
        return doClassHinting;
    }
    
    public String getClassHintPropertyName() {
        return "javaClass";
    }
    
}
