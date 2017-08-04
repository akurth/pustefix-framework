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
package de.schlund.pfixxml.resources;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.pustefixframework.live.LiveResolver;

import de.schlund.pfixcore.exception.PustefixRuntimeException;
import de.schlund.pfixcore.util.ModuleDescriptor;
import de.schlund.pfixcore.util.ModuleInfo;
import de.schlund.pfixxml.config.EnvironmentProperties;

/**
 * 
 * @author mleidig@schlund.de
 *
 */
public class ModuleResourceProvider implements ResourceProvider {

    private final static Logger LOG = LoggerFactory.getLogger(ModuleResourceProvider.class);
    
    private final static String MODULE_SCHEME = "module";
    
    private String[] supportedSchemes = {MODULE_SCHEME};
    
    public String[] getSupportedSchemes() {
        return supportedSchemes;
    }
    
    public Resource getResource(URI uri) throws ResourceProviderException {
        if (uri.getScheme() == null)
            throw new ResourceProviderException("Missing URI scheme: " + uri);
        if (!uri.getScheme().equals(MODULE_SCHEME))
            throw new ResourceProviderException("URI scheme not supported: " + uri);
        String module = uri.getAuthority();
        if (module == null || module.equals(""))
            throw new ResourceProviderException("Missing module name: " + uri);
        ModuleDescriptor desc = ModuleInfo.getInstance().getModuleDescriptor(module);
        if (desc != null) {
            URL url = desc.getURL().getProtocol().equals("jar") ? getJarURL(desc.getURL()) : getFileUrl(desc.getURL());
            // Ensure module resources are read from classpath in production environment
            boolean checkLive = !EnvironmentProperties.getProperties().getProperty("mode").equals("prod");
            if (checkLive) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Getting live resource for " + uri);
                }
                try {
                    URL resolvedUrl = LiveResolver.getInstance().resolveLiveModuleRoot(url, uri.getPath());
                    if (resolvedUrl != null) {
                        // jar or file?
                        if (resolvedUrl.getProtocol().equals("jar")) {
                            return new ModuleResource(uri, url, desc.getResourcePath());
                        } else {
                            return new ModuleSourceResource(uri, new File(resolvedUrl.getFile()), desc.getResourcePath());
                        }
                    } else if(url.getProtocol().equals("file")){
                        return new ModuleSourceResource(uri, new File(url.getFile()), desc.getResourcePath());
                    }
                } catch (Exception e) {
                    throw new PustefixRuntimeException(e);
                }
            }
            return new ModuleResource(uri, url, desc.getResourcePath());
        }
        return new ModuleResource(uri);
    }
    
    private static URL getFileUrl(URL url) {
        if (!url.getProtocol().equals("file"))
            throw new PustefixRuntimeException("Invalid protocol: " + url);
        String urlStr = url.toString();
        int ind = urlStr.indexOf("META-INF");
        if (ind > -1) {
            urlStr = urlStr.substring(0, ind);
        } else
            throw new PustefixRuntimeException("Unexpected module descriptor URL: " + url);
        try {
            return new URL(urlStr);
        } catch (MalformedURLException x) {
            throw new PustefixRuntimeException("Invalid module URL: " + urlStr);
        }
    }

    private static URL getJarURL(URL url) {
        if(!url.getProtocol().equals("jar")) throw new PustefixRuntimeException("Invalid protocol: "+url);
        String urlStr = url.toString();
        int ind = urlStr.indexOf('!');
        if(ind > -1 && urlStr.length() > ind + 1)  {
            urlStr = urlStr.substring(0, ind+2);
        } else throw new PustefixRuntimeException("Unexpected module descriptor URL: "+url);
        try {
            return new URL(urlStr);
        } catch(MalformedURLException x) {
            throw new PustefixRuntimeException("Invalid module URL: "+urlStr);
        }
    }
    
}
