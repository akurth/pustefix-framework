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
package de.schlund.pfixcore.beans;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import de.schlund.pfixcore.beans.metadata.Bean;
import de.schlund.pfixcore.beans.metadata.Beans;
import de.schlund.pfixcore.beans.metadata.DefaultLocator;
import de.schlund.pfixcore.beans.metadata.Locator;
import de.schlund.pfixcore.oxm.bean.CovariantBeanB;
import junit.framework.TestCase;

public class BeanDescriptorTest extends TestCase {
    
    public void testPropertyDetection() throws InitException {

        URL url=getClass().getResource("beanmetadata.xml");
        if(url==null) {
            try {
                url=new URL("file:/src/test/java/de/schlund/pfixcore/beans/beanmetadata.xml");
            } catch(MalformedURLException x) {
                throw new RuntimeException(x);
            }
        }
        Locator locator=new DefaultLocator(url);
        BeanDescriptorFactory beanDescFactory=new BeanDescriptorFactory(locator);    
        
        //BeanA + BeanD
        
        Set<String> expProps=new HashSet<String>();
        expProps.add("foo");
        expProps.add("baz");
        expProps.add("mytest");
        
        //BeanA annotations
        BeanDescriptor beanDesc=new BeanDescriptor(BeanA.class);
        assertEquals(expProps,beanDesc.getReadableProperties());
        
        //BeanA metadata
        Beans beans=new Beans();
        Bean bean=new Bean(BeanA.class.getName());
        bean.excludeProperty("bar");
        bean.setPropertyAlias("test","mytest");
        beans.setBean(bean);
        beanDesc=new BeanDescriptor(BeanA.class,beans);
        assertEquals(expProps,beanDesc.getReadableProperties());
        
        //BeanA xml metadata
        beanDesc=beanDescFactory.getBeanDescriptor(BeanA.class);
        assertEquals(expProps,beanDesc.getReadableProperties());
        
        //BeanD annotations
        beanDesc=new BeanDescriptor(BeanD.class);
        assertEquals(expProps,beanDesc.getReadableProperties());
        
        //BeanD metadata
        beans=new Beans();
        bean=new Bean(BeanD.class.getName());
        bean.excludeByDefault();
        bean.includeProperty("foo");
        bean.includeProperty("baz");
        bean.includeProperty("test");
        bean.setPropertyAlias("test","mytest");
        beans.setBean(bean);
        beanDesc=new BeanDescriptor(BeanD.class,beans);
        assertEquals(expProps,beanDesc.getReadableProperties());
        
        //BeanD xml metadata
        beanDesc=beanDescFactory.getBeanDescriptor(BeanD.class);
        assertEquals(expProps,beanDesc.getReadableProperties());
        
        
        //BeanB + BeanE
        
        expProps=new HashSet<String>();
        expProps.add("foo");
        expProps.add("baz");
        expProps.add("mytest");
        expProps.add("my");
        
        //BeanB annotations
        beanDesc=new BeanDescriptor(BeanB.class);
        assertEquals(expProps,beanDesc.getReadableProperties());
        
        //BeanB metadata
        beans=new Beans();
        bean=new Bean(BeanB.class.getName());
        bean.excludeByDefault();
        bean.includeProperty("my");
        beans.setBean(bean);
        beanDesc=new BeanDescriptor(BeanB.class,beans);
        assertEquals(expProps,beanDesc.getReadableProperties());
        
        //BeanB xml metadata
        beanDesc=beanDescFactory.getBeanDescriptor(BeanB.class);
        assertEquals(expProps,beanDesc.getReadableProperties());
        
        //BeanE annotations
        beanDesc=new BeanDescriptor(BeanE.class);
        assertEquals(expProps,beanDesc.getReadableProperties());
        
        //BeanE metadata
        beans=new Beans();
        bean=new Bean(BeanE.class.getName());
        bean.excludeProperty("hey");
        bean.excludeProperty("ho");
        beans.setBean(bean);
        beanDesc=new BeanDescriptor(BeanE.class,beans);
        assertEquals(expProps,beanDesc.getReadableProperties());
        
        //BeanE xml metadata
        beanDesc=beanDescFactory.getBeanDescriptor(BeanE.class);
        assertEquals(expProps,beanDesc.getReadableProperties());
        
        //Test with beanmetadata from META-INF
        locator=new DefaultLocator();
        beanDescFactory=new BeanDescriptorFactory(locator);    
        
        //BeanC + BeanF
        
        expProps=new HashSet<String>();
        expProps.add("mybar");
        expProps.add("baz");
        expProps.add("mytest");
        expProps.add("my");
        expProps.add("hey");
        
        //BeanC annotations
        beanDesc=new BeanDescriptor(BeanC.class);
        assertEquals(expProps,beanDesc.getReadableProperties());
        
        //BeanC metadata
        beans=new Beans();
        bean=new Bean(BeanC.class.getName());
        bean.excludeProperty("foo");
        bean.setPropertyAlias("bar","mybar");
        beans.setBean(bean);
        beanDesc=new BeanDescriptor(BeanC.class,beans);
        assertEquals(expProps,beanDesc.getReadableProperties());
        
        //BeanC xml metadata
        beanDesc=beanDescFactory.getBeanDescriptor(BeanC.class);
        assertEquals(expProps,beanDesc.getReadableProperties());
        
        //BeanF annotations
        beanDesc=new BeanDescriptor(BeanF.class);
        assertEquals(expProps,beanDesc.getReadableProperties());
        
        //BeanF metadata
        beans=new Beans();
        bean=new Bean(BeanF.class.getName());
        bean.excludeByDefault();
        bean.includeProperty("hey");
        bean.includeProperty("bar");
        bean.setPropertyAlias("bar","mybar");
        beans.setBean(bean);
        beanDesc=new BeanDescriptor(BeanF.class,beans);
        assertEquals(expProps,beanDesc.getReadableProperties());
        
        //BeanF xml metadata
        beanDesc=beanDescFactory.getBeanDescriptor(BeanF.class);
        assertEquals(expProps,beanDesc.getReadableProperties());
        
        //Test method covariance
        CovariantBeanB cbean = new CovariantBeanB();
        cbean.setValue("foo");
        beanDesc=new BeanDescriptor(CovariantBeanB.class,beans);
        expProps=new HashSet<String>();
        expProps.add("value");
        assertEquals(expProps,beanDesc.getReadableProperties());
        
    }
    
    public static void main(String[] args) throws Exception {
        BeanDescriptorTest test=new BeanDescriptorTest();
        test.testPropertyDetection();
    }

}
