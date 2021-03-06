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

package org.pustefixframework.config.contextxmlservice.parser;

import org.pustefixframework.config.contextxmlservice.PageRequestConfig;
import org.pustefixframework.config.contextxmlservice.parser.internal.IWrapperConfigImpl;
import org.pustefixframework.config.contextxmlservice.parser.internal.StateConfigImpl;
import org.pustefixframework.config.generic.ParsingUtils;
import org.w3c.dom.Element;

import com.marsching.flexiparse.parser.HandlerContext;
import com.marsching.flexiparse.parser.ParsingHandler;
import com.marsching.flexiparse.parser.exception.ParserException;

import de.schlund.pfixcore.generator.IWrapper;

/**
 * 
 * @author mleidig
 *
 */
public class PageRequestInputWrapperParsingHandler implements ParsingHandler {

    public void handleNode(HandlerContext context) throws ParserException {
       
        Element element = (Element)context.getNode();
        ParsingUtils.checkAttributes(element, new String[] {"class", "prefix"}, new String[] {"checkactive", "activeignore", "logging", "tenant"});
         
        StateConfigImpl stateConfig = ParsingUtils.getFirstTopObject(StateConfigImpl.class, context, true);
        PageRequestConfig pageConfig = ParsingUtils.getFirstTopObject(PageRequestConfig.class, context, true);
        IWrapperConfigImpl wrapperConfig = new IWrapperConfigImpl();
        
        String prefix = element.getAttribute("prefix").trim();
        wrapperConfig.setPrefix(prefix);
        
        String className = element.getAttribute("class").trim();
        Class<?> wrapperClass;
        try {
            wrapperClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new ParserException("Could not load wrapper class \"" + className + "\"!", e);
        }
        if (!IWrapper.class.isAssignableFrom(wrapperClass)) {
            throw new ParserException("Input wrapper class " + wrapperClass + " on page " + pageConfig.getPageName() + " does not implement " + IWrapper.class + " interface!");
        }
        wrapperConfig.setWrapperClass(wrapperClass.asSubclass(IWrapper.class));
        
        String scope = element.getAttribute("scope").trim();
        if (scope.length() > 0) {
            wrapperConfig.setScope(scope);
        }
        
        String activeignore = element.getAttribute("activeignore").trim();
        if (activeignore.length()>0) {
            wrapperConfig.setCheckActive(!Boolean.parseBoolean(activeignore));
        } else {
            wrapperConfig.setCheckActive(true);
        }
        
        String checkactive = element.getAttribute("checkactive").trim();
        if (checkactive.length()>0) {
            wrapperConfig.setCheckActive(Boolean.parseBoolean(checkactive));
        } else {
            wrapperConfig.setCheckActive(true);
        }
        
        String dologging = element.getAttribute("logging").trim();
        if (dologging.length()>0) {
            wrapperConfig.setLogging(Boolean.parseBoolean(dologging));
        } else {
            wrapperConfig.setLogging(false);
        }
        
        String tenant = element.getAttribute("tenant").trim();
        if(tenant.length() > 0) {
            wrapperConfig.setTenant(tenant);
        }
        
        stateConfig.addIWrapper(wrapperConfig);
        
    }

}
