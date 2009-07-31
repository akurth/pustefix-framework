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

import org.pustefixframework.config.application.XMLGeneratorInfo;
import org.pustefixframework.config.contextxmlservice.parser.internal.ContextConfigImpl;
import org.pustefixframework.config.contextxmlservice.parser.internal.PustefixContextXMLRequestHandlerConfigImpl;
import org.pustefixframework.config.generic.ParsingUtils;
import org.w3c.dom.Element;

import com.marsching.flexiparse.parser.HandlerContext;
import com.marsching.flexiparse.parser.ParsingHandler;
import com.marsching.flexiparse.parser.exception.ParserException;


/**
 * 
 * @author mleidig
 *
 */
public class ContextParsingHandler implements ParsingHandler {

    public void handleNode(HandlerContext context) throws ParserException {
        
        Element element = (Element)context.getNode();
        ParsingUtils.checkAttributes(element, null, new String[] {"defaultpage", "synchronized"});
        
        XMLGeneratorInfo info = ParsingUtils.getSingleTopObject(XMLGeneratorInfo.class, context);
        PustefixContextXMLRequestHandlerConfigImpl config = ParsingUtils.getSingleTopObject(PustefixContextXMLRequestHandlerConfigImpl.class, context);
        
        ContextConfigImpl ctxConfig = new ContextConfigImpl();
        // Navigation is stored in depend.xml
        ctxConfig.setNavigationFile(info.getConfigurationFile());
        ctxConfig.setDefaultState(config.getDefaultStaticState());
        
        String defaultPage = element.getAttribute("defaultpage").trim();
        if(defaultPage.length()>0) ctxConfig.setDefaultPage(defaultPage);
        
        String syncStr = element.getAttribute("synchronized");
        if (syncStr != null) {
            ctxConfig.setSynchronized(Boolean.parseBoolean(syncStr));
        } else {
            ctxConfig.setSynchronized(true);
        }
        config.setContextConfig(ctxConfig);
        context.getObjectTreeElement().addObject(ctxConfig);
    }

}
