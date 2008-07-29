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
package org.pustefixframework.webservices.spring;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * @author mleidig
 *
 */
public class WebServiceBeanDefinitionParser extends AbstractSingleBeanDefinitionParser { 

   protected Class<?> getBeanClass(Element element) {
       return WebServiceRegistration.class;
   }

   protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder beanDefBuilder) {
    
       String serviceName = element.getAttribute("servicename");
       beanDefBuilder.addPropertyValue("serviceName", serviceName);
      
       String interfaceName = element.getAttribute("interface");
       beanDefBuilder.addPropertyValue("interface", interfaceName);
       
       String protocol = element.getAttribute("protocol");
       if(protocol!=null) beanDefBuilder.addPropertyValue("protocol", protocol);
       
       Object target = null;
       if (element.hasAttribute("ref")) { 
           target = new RuntimeBeanReference(element.getAttribute("ref"));
       }
           
       //Handle nested bean reference/definition
       NodeList nodes = element.getChildNodes();
       for (int i = 0; i < nodes.getLength(); i++) {
           Node node = nodes.item(i);
           if (node instanceof Element) {
               Element subElement = (Element)node;
               if (element.hasAttribute("ref")) {
                   parserContext.getReaderContext().error("Nested bean reference/definition isn't allowed because "
                           +"the webservice 'ref' attribute is already set to '"+element.getAttribute("ref")+"'.", element);
               }
               target = parserContext.getDelegate().parsePropertySubElement(subElement, beanDefBuilder.getBeanDefinition());    
           }
       }
      
       if (target instanceof RuntimeBeanReference) {
           beanDefBuilder.addPropertyValue("targetBeanName", ((RuntimeBeanReference) target).getBeanName());
       } else {
           beanDefBuilder.addPropertyValue("target", target);
       }
      
   }
   
}