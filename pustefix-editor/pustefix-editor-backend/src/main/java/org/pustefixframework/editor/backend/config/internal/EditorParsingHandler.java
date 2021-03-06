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

package org.pustefixframework.editor.backend.config.internal;

import java.util.NoSuchElementException;
import java.util.Properties;

import org.pustefixframework.config.customization.CustomizationAwareParsingHandler;
import org.pustefixframework.config.generic.ParsingUtils;
import org.pustefixframework.editor.backend.config.EditorProjectInfo;
import org.pustefixframework.editor.backend.remote.HTTPAuthenticationHandlerInterceptor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.xml.sax.InputSource;

import com.marsching.flexiparse.parser.HandlerContext;
import com.marsching.flexiparse.parser.exception.ParserException;

import de.schlund.pfixxml.config.EnvironmentProperties;


public class EditorParsingHandler extends CustomizationAwareParsingHandler {
    
    @Override
    protected void handleNodeIfActive(HandlerContext context) throws ParserException {
        EditorConfig editorConfig;
        BeanDefinitionRegistry beanRegistry = ParsingUtils.getSingleTopObject(BeanDefinitionRegistry.class, context);
        try {
            editorConfig = context.getObjectTreeElement().getObjectsOfTypeFromSubTree(EditorConfig.class).iterator().next();
        } catch (NoSuchElementException e) {
            // No editor configuration in this configuration file
            editorConfig = new EditorConfig();
            editorConfig.setEnabled(false);
        }
        overrideByEnvironment(editorConfig);
        if (!editorConfig.isEnabled()) {
            return;
        }
        if (editorConfig.getLocation() == null) {
            throw new ParserException("Editor is enabled, but location is not set!");
        }
        if (editorConfig.getSecret() == null) {
            throw new ParserException("Editor is enabled, but secret is not set!");
        }
        
        EditorProjectInfo projectInfo = ParsingUtils.getSingleSubObjectFromRoot(EditorProjectInfo.class, context);
        
        BeanNameGenerator beanNameGenerator = new DefaultBeanNameGenerator();
        BeanDefinitionBuilder beanBuilder = BeanDefinitionBuilder.genericBeanDefinition(HTTPAuthenticationHandlerInterceptor.class);
        beanBuilder.addPropertyValue("secret", editorConfig.getSecret());
        BeanDefinition beanDefinition = beanBuilder.getBeanDefinition();
        beanRegistry.registerBeanDefinition(beanNameGenerator.generateBeanName(beanDefinition, beanRegistry), beanDefinition);
        
        beanBuilder = BeanDefinitionBuilder.genericBeanDefinition(EditorProjectInfo.class);
        beanBuilder.addPropertyValue("name", projectInfo.getName());
        beanBuilder.addPropertyValue("description", projectInfo.getDescription());
        beanBuilder.addPropertyValue("includePartsEditableByDefault", editorConfig.isIncludePartsEditableByDefault());
        beanBuilder.addPropertyValue("enableTargetUpdateService", editorConfig.isEnableTargetUpdateService());
        beanDefinition = beanBuilder.getBeanDefinition();
        beanRegistry.registerBeanDefinition(EditorProjectInfo.class.getName(), beanDefinition);
        
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanRegistry);
        reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        reader.loadBeanDefinitions(new InputSource(this.getClass().getResourceAsStream("editor-spring.xml")));
    }
    
    private void overrideByEnvironment(EditorConfig editorConfig) {
        Properties envProps = EnvironmentProperties.getProperties();
        String val = envProps.getProperty("editor.enabled");
        if(val != null) {
            boolean enabled = Boolean.parseBoolean(val);
            editorConfig.setEnabled(enabled);
        }
        val = envProps.getProperty("editor.location");
        if(val != null) {
            editorConfig.setLocation(val);
        }
        val = envProps.getProperty("editor.secret");
        if(val != null) {
            editorConfig.setSecret(val);
        }
    }
    
}
