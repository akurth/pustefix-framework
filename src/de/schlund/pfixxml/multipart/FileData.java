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

package de.schlund.pfixxml.multipart;

import java.io.File;
import java.util.Date;
import de.schlund.pfixxml.RequestParamType;

/**
 *
 *
 */

public class FileData extends PartData {

    private String transferEncoding = null;
    private String filename = null;
    private Date modificationDate = null;
    private Date readDate = null;
    private long size = 0;
    private String localFilename = null;
    private File localFile = null;

    /**
     * Constructor for FileData.
     */
    public FileData() {
        super();
        setType(RequestParamType.FILEDATA);
    }

        
    /**
     * Gets the filename.
     * @return Returns a String
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Sets the filename.
     * @param filename The filename to set
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * Gets the localFilename.
     * @return Returns a String
     */
    public String getLocalFilename() {
        return localFilename;
    }

    /**
     * Sets the localFilename.
     * @param localFilename The localFilename to set
     */
    public void setLocalFilename(String localFilename) {
        this.localFilename = localFilename;
    }

    /**
     * Gets the size.
     * @return Returns a long
     */
    public long getSize() {
        return size;
    }

    /**
     * Sets the size.
     * @param size The size to set
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Gets the transferEncoding.
     * @return Returns a String
     */
    public String getTransferEncoding() {
        return transferEncoding;
    }

    /**
     * Sets the transferEncoding.
     * @param transferEncoding The transferEncoding to set
     */
    public void setTransferEncoding(String transferEncoding) {
        this.transferEncoding = transferEncoding;
    }

    /**
     * Gets the modificationDate.
     * @return Returns a Date
     */
    public Date getModificationDate() {
        return modificationDate;
    }

    /**
     * Sets the modificationDate.
     * @param modificationDate The modificationDate to set
     */
    public void setModificationDate(Date modificationDate) {
        this.modificationDate = modificationDate;
    }

    /**
     * Gets the readDate.
     * @return Returns a Date
     */
    public Date getReadDate() {
        return readDate;
    }

    /**
     * Sets the readDate.
     * @param readDate The readDate to set
     */
    public void setReadDate(Date readDate) {
        this.readDate = readDate;
    }
    
    public void setValue(String val) {
        localFilename = val;
    }
    
    public String getValue() {
        return localFilename;
    }
    
    /**
     * Gets the localFile.
     * @return Returns a File
     */
    public File getLocalFile() {
        return localFile;
    }

    /**
     * Sets the localFile.
     * @param localFile The localFile to set
     */
    public void setLocalFile(File localFile) {
        this.localFile = localFile;
    }

    protected void finalize() throws Throwable {
        //delete associated file when object is no longer in use
        if(localFile!=null && localFile.exists()) localFile.delete();
    }
    
}
