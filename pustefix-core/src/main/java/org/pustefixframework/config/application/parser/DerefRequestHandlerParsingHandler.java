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
package org.pustefixframework.config.application.parser;

import java.util.Properties;

import org.pustefixframework.config.Constants;
import org.pustefixframework.config.contextxmlservice.ServletManagerConfig;
import org.pustefixframework.http.dereferer.DerefRequestHandler;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

import com.marsching.flexiparse.parser.HandlerContext;
import com.marsching.flexiparse.parser.ParsingHandler;
import com.marsching.flexiparse.parser.exception.ParserException;

import de.schlund.pfixxml.serverutil.SessionAdmin;

public class DerefRequestHandlerParsingHandler implements ParsingHandler {
    
    private BeanDefinitionBuilder beanBuilder;
    
    public void handleNode(HandlerContext context) throws ParserException {
        
        Element root = (Element) context.getNode();
        
        if(root.getLocalName().equals("application")) {
            
            final Properties properties = new Properties(System.getProperties());
            ServletManagerConfig config = new ServletManagerConfig() {

                public Properties getProperties() {
                    return properties;
                }

                public boolean isSSL() {
                    return false;
                }

                public boolean needsReload() {
                    return false;
                }
                
            };
            
            beanBuilder = BeanDefinitionBuilder.genericBeanDefinition(DerefRequestHandler.class);
            beanBuilder.setScope("singleton");
            beanBuilder.setInitMethodName("init");
            beanBuilder.addPropertyValue("handlerURI", "/xml/deref/**");
            beanBuilder.addPropertyValue("validTime", 1000 * 60 * 60);
            beanBuilder.addPropertyValue("mustSign", true);
            beanBuilder.addPropertyValue("configuration", config);
            beanBuilder.addPropertyValue("sessionAdmin", new RuntimeBeanReference(SessionAdmin.class.getName()));
            BeanDefinition beanDefinition = beanBuilder.getBeanDefinition();
            BeanDefinitionHolder beanHolder = new BeanDefinitionHolder(beanDefinition, DerefRequestHandler.class.getName());
            context.getObjectTreeElement().addObject(beanHolder);
            
        } else if(root.getLocalName().equals("deref-service")) {
        
            Element serviceElement = (Element) context.getNode();
            
            String path="/xml/deref";
            Element element = (Element) serviceElement.getElementsByTagNameNS(Constants.NS_APPLICATION, "path").item(0);
            if (element != null) path = element.getTextContent().trim();
            
            long validTime = 1000 * 60 * 60;
            element = (Element) serviceElement.getElementsByTagNameNS(Constants.NS_APPLICATION, "validtime").item(0);
            if (element != null) validTime = Long.parseLong(element.getTextContent().trim()) * 1000;
            
            boolean mustSign = true;
            element = (Element) serviceElement.getElementsByTagNameNS(Constants.NS_APPLICATION, "mustsign").item(0);
            if (element != null) mustSign = Boolean.parseBoolean(element.getTextContent().trim());
            
            beanBuilder.addPropertyValue("handlerURI", path + "/**");
            beanBuilder.addPropertyValue("validTime", validTime);
            beanBuilder.addPropertyValue("mustSign", mustSign);
        }
    }

}