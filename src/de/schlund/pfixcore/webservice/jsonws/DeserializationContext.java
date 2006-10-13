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

public class DeserializationContext {

    DeserializerRegistry deserReg;
    
    public DeserializationContext(DeserializerRegistry deserReg) {
        this.deserReg=deserReg;
    }
   
    public boolean canDeserialize(Object jsonObj,Class targetClass) throws DeserializationException {
        Deserializer deser=deserReg.getDeserializer(targetClass);
        return deser.canDeserialize(this,jsonObj,targetClass);
    }
    
    public Object deserialize(Object jsonObj,Class targetClass) throws DeserializationException {
        if(jsonObj==null) return null;
        Deserializer deser=deserReg.getDeserializer(targetClass);
        return deser.deserialize(this,jsonObj,targetClass);
    }
    
}
