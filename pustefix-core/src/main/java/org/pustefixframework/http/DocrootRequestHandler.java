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

package org.pustefixframework.http;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.pustefixframework.container.spring.http.UriProvidingHttpRequestHandler;
import org.springframework.web.context.ServletContextAware;

import de.schlund.pfixxml.resources.FileResource;
import de.schlund.pfixxml.resources.Resource;
import de.schlund.pfixxml.resources.ResourceUtil;
import de.schlund.pfixxml.util.MD5Utils;

/**
 * This servlet serves the static files from the docroot.   
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public class DocrootRequestHandler implements UriProvidingHttpRequestHandler, ServletContextAware {
    
    private Logger LOG = Logger.getLogger(DocrootRequestHandler.class);
    
    private String base;

    private String defaultpath;
    
    private List<String> passthroughPaths;
    
    private ServletContext servletContext;

    public ServletContext getServletContext() {
        return servletContext;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void setDefaultPath(String defaultpath) {
        this.defaultpath = defaultpath;
    }

    public void setPassthroughPaths(List<String> passthroughPaths) {
        this.passthroughPaths = passthroughPaths;
    }
    
    public void setBase(String path) {
        this.base = path;
    }

    public void handleRequest(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String path = req.getPathInfo();
      
        boolean warOnly = getServletContext().getRealPath("/") == null ? true : false;

        // Handle default (root) request
        if (this.defaultpath != null
                && (path == null || path.length() == 0 || path.equals("/"))) {
            res.sendRedirect(req.getContextPath() + this.defaultpath);
            return;
        }

        // Avoid path traversal and access to config or source files
        if (path.contains("..") || path.startsWith("/WEB-INF")) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, path);
            return;
        }

        // Directory listing is not allowed
        if (path.endsWith("/")) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, path);
            return;
        }

        InputStream in = null;
        long contentLength = -1;
        long lastModified = -1;
        
        try {

            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            
            if (!warOnly) {
                
                if (passthroughPaths != null) {
                    for (String prefix : this.passthroughPaths) {
                        if (path.startsWith(prefix)) {
                            Resource resource = ResourceUtil.getFileResourceFromDocroot(path);
                            if(resource.exists()) {
                                contentLength = resource.length();
                                lastModified = resource.lastModified();
                                in = resource.getInputStream();
                                break;
                            }
                        }
                    }
                }
                
                if (in == null) {
                    FileResource baseResource = ResourceUtil.getFileResource(base);
                    FileResource resource = ResourceUtil.getFileResource(baseResource, path);
                    contentLength = resource.length();
                    lastModified = resource.lastModified();
                    in = resource.getInputStream();
                }
                
            } else {
                
                if (passthroughPaths != null) {
                    for (String prefix : this.passthroughPaths) {
                        if (path.startsWith(prefix)) {
                            // Use getResource() to make sure we can
                            // access the file even in packed WAR mode
                            URL url = getServletContext().getResource("/"+path);
                            if(url != null) {
                                URLConnection con = url.openConnection();
                                lastModified = con.getLastModified();
                                contentLength = con.getContentLength();
                                in = url.openStream();
                                break;
                            }
                        }
                    }
                }
                
            }
            
        } catch(IOException x) {
            LOG.warn("Resource can't be read: " + path, x);
            //send 'not found' below
        }
            
        if(in == null) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, path);
            if(LOG.isDebugEnabled()) {
                LOG.debug("Resource doesn't exist -> send 'not found': " + path);
            }
            return;
        }
            
        String reqETag = req.getHeader("If-None-Match");
        if(reqETag != null) {
            String etag = createETag(path, contentLength, lastModified);
            if(etag.equals(reqETag)) {
                res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                res.flushBuffer();
                if(LOG.isDebugEnabled()) {
                    LOG.debug("ETag didn't change -> send 'not modified' for resource: " + path);
                }
                return;
            }
        }
            
        long reqMod = req.getDateHeader("If-Modified-Since");
        if(reqMod != -1) {
            if(lastModified < reqMod + 1000) {
                res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                res.flushBuffer();
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Modification time didn't change -> send 'not modified' for resource: " + path);
                }
                return;
            }
        }

        String type = getServletContext().getMimeType(path);
        if (type == null) {
            type = "application/octet-stream";
        }
        res.setContentType(type);
        if(contentLength > -1 && contentLength < Integer.MAX_VALUE) {
            res.setContentLength((int)contentLength);
        }
        if(lastModified > -1) {
            res.setDateHeader("Last-Modified", lastModified);
        }
                
        String etag = MD5Utils.hex_md5(path+contentLength+lastModified);
        res.setHeader("ETag", etag);
            
        res.setHeader("Cache-Control", "max-age=3600");
            
        OutputStream out = new BufferedOutputStream(res.getOutputStream());

        int bytes_read;
        byte[] buffer = new byte[8];
        while ((bytes_read = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytes_read);
        }
        out.flush();
        in.close();
        out.close();

    }

    
    public String[] getRegisteredURIs() {
        return new String[] {"/**", "/xml/**"};
    }
    
    
    private String createETag(String path, long length, long modtime) {
        return MD5Utils.hex_md5(path + length + modtime);
    }

}