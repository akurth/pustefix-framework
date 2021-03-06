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

package org.pustefixframework.container.spring.beans;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.pustefixframework.container.annotations.ImplementedBy;
import org.pustefixframework.container.annotations.Inject;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * Utility for checking the beans in a {@link DefaultListableBeanFactory} 
 * for autowire annotations. Iterates over all {@link BeanDefinition}s in
 * the bean factory and checks for types with {@link Inject} annotations.
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public class AnnotationBeanDefinitionPostProcessor implements BeanFactoryPostProcessor {
    
    private Map<String, String> scopedProxyMap = new HashMap<String, String>(); 
    
    private Set<Class<?>> notAnnotatedClasses = new HashSet<Class<?>>();
    private Map<Class<?>,RuntimeBeanReference> beanRefCache = new HashMap<Class<?>,RuntimeBeanReference>();
    
    /**
     * Iterates over all {@link BeanDefinition}s in the <code>beanFactory</code>
     * and looks for setters with the {@link Inject} annotation. If such an
     * annotation is found on a method, the requested type is determined by
     * inspecting the method's paramter. If the bean definition does not yet
     * have the corresponding property set, it is set to a reference to a
     * bean matching the requested type. If no such bean is present, a new bean
     * for the requested type is created (if possible). If the requested type
     * is an interface the {@link ImplementedBy} annotation on this interface
     * is used to determine the type of the bean.
     * Automatically creating new beans can only work if the passed BeanFactory
     * implements the {@link BeanDefinitionRegistry} interface, otherwise this
     * method will throw a {@link BeanFactoryImplNotSupportedException}.
     * 
     * @param beanFactory bean factory containing the bean definitions to check
     */
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        prepareScopedProxyMap(beanFactory);
        String[] beanNames = beanFactory.getBeanDefinitionNames();
        for (int i = 0; i < beanNames.length; i++) {
            String beanName = beanNames[i];
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            processBeanDefinition(beanName, beanDefinition, beanFactory);
        }
    }
    
    private void prepareScopedProxyMap(ConfigurableListableBeanFactory beanFactory) {
        this.scopedProxyMap.clear();
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            if (!beanDefinition.isAbstract() && beanDefinition.getBeanClassName() != null && beanDefinition.getBeanClassName().equals("org.springframework.aop.scope.ScopedProxyFactoryBean")) {
                PropertyValue value = beanDefinition.getPropertyValues().getPropertyValue("targetBeanName");
                if (value != null) {
                    scopedProxyMap.put((String) value.getValue(), beanName);
                }
            }
        }
    }

    /**
     * Processes a bean definition looking for {@link Inject} annotations
     * in the bean class.
     * 
     * @param beanName name of the bean to process
     * @param beanDefinition definition for the bean
     * @param beanFactory bean factory that contains the bean
     * @throws BeanInitializationException if annotation is used on an 
     * invalid method
     */
    private void processBeanDefinition(String beanName, BeanDefinition beanDefinition, ConfigurableListableBeanFactory beanFactory) {
        if(beanDefinition.isAbstract() || beanDefinition.getBeanClassName() == null) return;
        ClassLoader beanClassLoader = getClassLoader(beanFactory);
        Class<?> beanClass;
        try {
            beanClass = beanClassLoader.loadClass(beanDefinition.getBeanClassName());
        } catch (ClassNotFoundException e) {
            throw new BeanDefinitionValidationException("Class \"" + beanDefinition.getBeanClassName() + "\" specified for bean \"" + beanName + "\" could not be loaded.");
        }
        if(notAnnotatedClasses.contains(beanClass)) return;
        
        Method[] methods = beanClass.getMethods();
        boolean hasInject = false;
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String methodName = method.getName();
            Inject annotation = method.getAnnotation(Inject.class);
            if (annotation != null) {
            	hasInject = true;
                String propertyName = null;
                
                if (methodName.startsWith("set") && method.getParameterTypes().length == 1) {
                    if (methodName.length() == 3) {
                        propertyName = "";
                    } else {
                        propertyName = methodName.substring(3);
                        if (! (propertyName.length() > 1 && Character.isUpperCase(propertyName.charAt(0)) 
                                && Character.isUpperCase(propertyName.charAt(1)))) { 
                            propertyName = Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
                        }
                    }
                } else {
                    throw new BeanInitializationException("\"" + methodName + "\" is not a vaild setter method in bean \"" + beanName + "\" (" + beanDefinition.getBeanClassName() + ").");
                }
                
                Class<?> wantedType = method.getParameterTypes()[0];
                if (beanDefinition.getPropertyValues().getPropertyValue(propertyName) == null) {
                    try {
                        beanDefinition.getPropertyValues().addPropertyValue(propertyName, findOrCreateBeanDefinition(wantedType, beanFactory));
                    } catch (RuntimeException e) {
                        throw new BeanInitializationException("Error while processing Inject annotation on method \"" + methodName + "\" of bean \"" + beanName + "\" (" + beanDefinition.getBeanClassName() + "): " + e.getMessage(), e);
                    }
                }

            }
        }
        if(!hasInject) {
        	notAnnotatedClasses.add(beanClass);
        }
    }
    
    /**
     * Checks wether the bean name represents a bean that was automatically
     * created by this tool.
     * 
     * @param beanName name of the bean to check
     * @return <code>true</code> if the bean was created by this tool,
     * <code>false</code> otherwise
     */
    private boolean isAutoBeanName(String beanName) {
        return beanName.startsWith(this.getClass().getName() + "#");
    }
    
    /**
     * Creates a reference to a bean matching the requested type. Creates a 
     * bean definition if no matching bean is found in the supplied 
     * <code>beanFactory</code>.
     * 
     * @param wantedType type to find bean for
     * @param beanFactory bean factory to search
     * @return Reference to a matching bean
     * @throws BeanDefinitionValidationException if the class of a bean definition
     * could not be loaded
     * @throws BeanInitializationException if no matching and unambiguous bean
     * could be found or created
     */
    private RuntimeBeanReference findOrCreateBeanDefinition(Class<?> wantedType, ConfigurableListableBeanFactory beanFactory) {
        
    	RuntimeBeanReference beanRef = beanRefCache.get(wantedType);
        if(beanRef != null) return beanRef;
        
    	String matchingBeanName = null;
        ClassLoader beanClassLoader = getClassLoader(beanFactory);
        
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            // Ignore beans that were added using this processor,
            // otherwise the wiring process could be hard to predict.
            if (isAutoBeanName(beanName)) {
                continue;
            }
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            if(beanDefinition.isAbstract() || beanDefinition.getBeanClassName() == null) continue;
            Class<?> beanClass;
            try {
                beanClass = beanClassLoader.loadClass(beanDefinition.getBeanClassName());
            } catch (ClassNotFoundException e) {
                throw new BeanDefinitionValidationException("Type \"" + beanDefinition.getBeanClassName() + "\" could not be loaded.");
            }
            if (wantedType.isAssignableFrom(beanClass)) {
                if (matchingBeanName == null) {
                    matchingBeanName = beanName;
                    // Look for an AOP proxy
                    if (scopedProxyMap.containsKey(matchingBeanName)) {
                        matchingBeanName = scopedProxyMap.get(matchingBeanName);
                    }
                } else {
                    // There is more than one matching definition,
                    // which is an error condition.
                    throw new BeanInitializationException("There is more than one bean matching the type \"" + wantedType.getName() + "\" wanted for automatic injection.");
                }
            }
        }
        
        if (matchingBeanName == null) {
            throw new BeanInitializationException("No Spring bean of type '" + wantedType.getName() + "' found for injection.");
            //Disabled automatic creation of Spring bean definitions as first step to
            //getting rid of the @Inject annotation at all in the future
        }
        
        beanRef = new RuntimeBeanReference(matchingBeanName);
        beanRefCache.put(wantedType, beanRef);
        return beanRef;
    }
    
    /**
     * Returns the bean class loader of the supplied <code>beanFactory</code> 
     * or the current thread's context class loader if the bean class loader 
     * is not set. 
     * 
     * @param beanFactory bean factory to get class loader from
     * @return class loader to load bean classes with
     */
    private ClassLoader getClassLoader(ConfigurableListableBeanFactory beanFactory) {
        ClassLoader loader = beanFactory.getBeanClassLoader();
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }
        return loader;
    }
}
