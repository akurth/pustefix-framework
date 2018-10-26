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
package de.schlund.pfixcore.oxm.impl;

import org.pustefixframework.util.BytecodeAPIUtils;

import de.schlund.pfixcore.beans.BeanDescriptorFactory;
import de.schlund.pfixcore.beans.InitException;
import de.schlund.pfixcore.beans.metadata.DefaultLocator;
import de.schlund.pfixcore.oxm.Marshaller;

public class MarshallerFactory {

    private static Marshaller defaultMarshaller;
    private static Marshaller jaxbMarshaller;

    static {
        BeanDescriptorFactory factory;
        try {
            factory = new BeanDescriptorFactory(new DefaultLocator());
        } catch (InitException e) {
            throw new RuntimeException("Error initializing bean descriptors", e);
        }
        SerializerRegistry registry = new SerializerRegistry(factory);
        defaultMarshaller = new MarshallerImpl(registry);
        try {
            Class.forName("javax.xml.bind.JAXBContext");
            jaxbMarshaller = new de.schlund.pfixcore.oxm.impl.JAXBMarshaller();
        } catch(ClassNotFoundException x) {
            //JAXB not in classpath, always use default Marshaller
        }
    }
    
    public static Marshaller getMarshaller(Object object) {
        Marshaller marshaller;
        if(jaxbMarshaller != null && jaxbMarshaller.isSupported(getObjectClass(object))) {
            marshaller = jaxbMarshaller;
        } else {
            marshaller = defaultMarshaller;
        }
        return marshaller;
    }

    private static Class<?> getObjectClass(Object object) {
        Class<?> objectClass = object.getClass();
        if(BytecodeAPIUtils.isProxy(objectClass)) {
            objectClass = objectClass.getSuperclass();
        }
        return objectClass;
    }

}
