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

package de.schlund.pfixcore.workflow.app;

import de.schlund.pfixcore.workflow.*;
import de.schlund.pfixxml.*;
import java.io.*;
import java.util.*;
import javax.servlet.http.*;

/**
 * FileDownloadDOState.java
 *
 * This State serves as a sample {@link DirectOutputState}. It let's the user
 * download a static file from the filesystem. The property pagerequest.PAGENAME.downloadfile
 * gives the filename of the file to be downloaded, the property pagerequest.PAGENAME.downloadmimetype
 * gives the contenttype that should be set in the Outputstream.
 *
 * Created: Wed Oct 10 09:50:19 2001
 *
 * @author <a href="mailto:jtl@schlund.de">Jens Lautenbacher</a>
 */
public class FileDownloadDOState implements DirectOutputState {
    private static String PROP_FILENAME = "downloadfile";
    private static String PROP_MIMETYPE = "downloadmimetype";
    
    /**
     * Describe <code>isAccessible</code> method here.
     * Called by the {@link ForeignContextServlet} to check if the {@link DirectOutputState} is
     * accessible.
     *
     * @param crm a <code>ContextResourceManager</code> value
     * @param props a <code>Properties</code> object giving the properties that are attached to the current page
     * name. 
     * @param preq <code>PfixServletRequest</code> holds the request data.
     * @return a <code>boolean</code> value
     * @exception Exception if an error occurs
     */
    public synchronized boolean isAccessible(ContextResourceManager crm, Properties props, PfixServletRequest preq) throws Exception {
        String filename = props.getProperty(PROP_FILENAME);
        if (filename == null || filename.equals("")) {
            throw new XMLException("*** Need property " + PROP_FILENAME + " ***");
        }
        String mimetype = props.getProperty(PROP_MIMETYPE);
        if (mimetype == null || mimetype.equals("")) {
            throw new XMLException("*** Need property " + PROP_MIMETYPE + " ***");
        }

        File file = PathFactory.getInstance().createPath(filename).resolve();

        if (file.exists() && file.canRead()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * The <code>handleRequest</code> method will simply write the given file (known from the Properties object)
     * to the OutputStream of the HttpServletResponse. It will set the ContentType according to the property
     * pagerequest.PAGENAME.downloadmimetype
     * 
     *
     * @param crm a <code>ContextResourceManager</code> value
     * @param props a <code>Properties</code> object giving the properties that are attached to the current page
     * name.
     * @param preq <code>PfixServletRequest</code> holds the request data.
     * @param res <code>HttpServletResponse</code> of the current request as given by the ServletContainer.
     * @exception Exception if an error occurs
     */
    public synchronized void handleRequest(ContextResourceManager crm, Properties props, PfixServletRequest preq, HttpServletResponse res) throws Exception {
        String filename = props.getProperty(PROP_FILENAME);
        String mimetype = props.getProperty(PROP_MIMETYPE);
        File   file     = PathFactory.getInstance().createPath(filename).resolve();
        
        res.setContentType(mimetype);
        FileInputStream  fin  = new FileInputStream(file);
        OutputStream     out  = res.getOutputStream();
        byte[]           buff = new byte[4096];
        int              num  = 0;
        while ((num = fin.read(buff)) != -1) {
            out.write(buff, 0, num);
        }
    }
}
