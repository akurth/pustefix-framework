package org.pustefixframework.util;

import java.lang.reflect.Method;

/**
 * Bytecode manipulation API utilities.
 * 
 * @author mleidig@schlund.de
 *
 */
public class BytecodeAPIUtils {

    private final static String PROXYCHECK_CLASS_CGLIB = "net.sf.cglib.proxy.Enhancer";
    private final static String PROXYCHECK_METHOD_CGLIB = "isEnhanced";
    
    private final static String PROXYCHECK_CLASS_JAVASSIST = "javassist.util.proxy.ProxyFactory";
    private final static String PROXYCHECK_METHOD_JAVASSIST = "isProxyClass";
    
    private static Method cglibProxyCheckMethod;
    private static Method javassistProxyCheckMethod;
    
    static {
        try {
            Class<?> clazz = Class.forName(PROXYCHECK_CLASS_CGLIB);
            cglibProxyCheckMethod = clazz.getMethod(PROXYCHECK_METHOD_CGLIB, new Class<?>[] {Class.class});
        } catch(ClassNotFoundException x) {
            //ignore
        } catch(NoSuchMethodException x) {
            //ignore
        }
        try {
            Class<?> clazz = Class.forName(PROXYCHECK_CLASS_JAVASSIST);
            javassistProxyCheckMethod = clazz.getMethod(PROXYCHECK_METHOD_JAVASSIST, new Class<?>[] {Class.class});
        } catch(ClassNotFoundException x) {
            //ignore
        } catch(NoSuchMethodException x) {
            //ignore
        }
    }

    /**
     * Checks if class is proxied, i.e. if it's a derived proxy class
     * generated by cglib or javassist
     * 
     * @param clazz class to check
     * @return true if class is generated proxy class
     */
    public static boolean isProxy(Class<?> clazz) {
        if(cglibProxyCheckMethod != null) {
            try {
                boolean ret = (Boolean)cglibProxyCheckMethod.invoke(null, clazz);
                if(ret) {
                    return true;
                }
            } catch(Exception x) {
                //ignore
            }
        }
        if(javassistProxyCheckMethod != null) {
            try {
                boolean ret = (Boolean)javassistProxyCheckMethod.invoke(null, clazz);
                if(ret) {
                    return true;
                }
            } catch(Exception x) {
                //ignore
            }
        }
        return false;
    }
    
}
