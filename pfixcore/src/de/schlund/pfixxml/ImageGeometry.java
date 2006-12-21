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

package de.schlund.pfixxml;

import java.io.*;
import java.util.*;
import org.apache.log4j.*;

import de.schlund.pfixxml.resources.FileResource;
import de.schlund.pfixxml.resources.ResourceUtil;


/**
 * ImageGeometry.java
 *
 *
 * Created: Tue Apr 16 23:43:02 2002
 *
 * @author <a href="mailto:jtl@schlund.de">Jens Lautenbacher</a>
 *
 *
 */

public class ImageGeometry {
    private static Map      imageinfo = new HashMap();
    private static Logger   LOG       = Logger.getLogger(ImageGeometry.class); 
    
    public static int getHeight(String path) {
        ImageGeometryData data = getImageGeometryData(path);
        if (data == null) {
            return -1;
        } else {
            return data.getHeight();
        }
    }
    
    public static int getWidth(String path) {
        ImageGeometryData data = getImageGeometryData(path);
        if (data == null) {
            return -1;
        } else {
            return data.getWidth();
        }
    }

    public static String getType(String path) {
        ImageGeometryData data = getImageGeometryData(path);
        if (data == null) {
            return null;
        } else {
            return data.getType();
        }
    }
    
    public static String getStyleStringForImage(String path, String userStyle, String userWidth, String userHeight) {
        ImageGeometryData data = getImageGeometryData(path);
        int targetWidth, targetHeight;
        if (userWidth != null && userWidth.length() > 0) {
            targetWidth = Integer.parseInt(userWidth);
        } else {
            targetWidth = data.getWidth();
        }
        if (userHeight != null && userHeight.length() > 0) {
            targetHeight = Integer.parseInt(userHeight);
        } else {
            targetHeight = data.getHeight();
        }
        
        boolean haveWidth = false, haveHeight = false;
        
        if (userStyle == null) {
            userStyle = "";
        }
        StringBuffer genStyle = new StringBuffer(userStyle.trim());
        
        StringTokenizer st = new StringTokenizer(userStyle, ";");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            String propName = token.substring(0, token.indexOf(':'));
            propName = propName.trim().toLowerCase();
            if (propName.equals("width")) {
                haveWidth = true;
            } else if (propName.equals("height")) {
                haveHeight = true;
            }
        }
        
        if (!haveWidth && targetWidth != -1) {
            if (genStyle.length() > 0 && genStyle.charAt(genStyle.length()-1) != ';') {
                genStyle.append(';');
            }
            genStyle.append("width:");
            genStyle.append(targetWidth);
            genStyle.append("px;");
        }
        
        if (!haveHeight && targetHeight != -1) {
            if (genStyle.length() > 0 && genStyle.charAt(genStyle.length()-1) != ';') {
                genStyle.append(';');
            }
            genStyle.append("height:");
            genStyle.append(targetHeight);
            genStyle.append("px;");
        }
        
        return genStyle.toString();
    }

    private static ImageGeometryData getImageGeometryData(String path) {
        synchronized (imageinfo) {
            FileResource img = ResourceUtil.getFileResourceFromDocroot(path);
            if (img.exists() && img.canRead() && img.isFile()) {
                long              mtime = img.lastModified();
                ImageGeometryData tmp = (ImageGeometryData) imageinfo.get(path);
                if (tmp == null || mtime > tmp.lastModified()) {
                    // LOG.debug("Cache miss or outdated for: " + path);
                    try {
                        tmp = new ImageGeometryData(img);
                    } catch (IOException e) {
                        LOG.error("*** Couldn't get geometry for " + path, e);
                        return null;
                    }
                    if (!tmp.isOK()) {
                        LOG.error("*** Image data wasn't recognized for " + path);
                        return null;
                    }
                    imageinfo.put(path, tmp);
                } else {
                    // CAT.debug("Cache hit and uptodate for: " + path);
                }
                return tmp;
            }
            return null;
        }
    }
    
}// ImageGeometry
