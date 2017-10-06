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
package org.pustefixframework.web.mvc.internal;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import de.schlund.pfixcore.workflow.State;

/**
 * Extends RequestMappingHandlerMapping to support Pustefix states
 * as controllers without needing an according type-level annotation.
 */
public class StateRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

	private ConcurrentHashMap<Class<?>, Boolean> mappedClassCache = new ConcurrentHashMap<Class<?>, Boolean>();

	@Override
	protected boolean isHandler(Class<?> beanType) {
		return State.class.isAssignableFrom(beanType) && hasRequestMapping(beanType);
	}

	/**
     * Check if class contains request mapping methods.
     */
    public boolean hasRequestMapping(Class<?> clazz) {
        Boolean hasMapping = mappedClassCache.get(clazz);
        if(hasMapping == null) {
            Method[] methods = clazz.getMethods();
            for(Method method: methods) {
                RequestMapping mapping = method.getAnnotation(RequestMapping.class);
                if(mapping != null) {
                    hasMapping = true;
                    break;
                }
            }
            if(hasMapping == null) {
                hasMapping = false;
            }
            mappedClassCache.putIfAbsent(clazz, hasMapping);
        }
        return hasMapping;
    }

}
