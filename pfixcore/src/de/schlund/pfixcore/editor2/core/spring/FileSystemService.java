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
 */

package de.schlund.pfixcore.editor2.core.spring;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import de.schlund.pfixcore.editor2.core.exception.EditorIOException;

/**
 * Provides methods to access files on the filesystem.
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public interface FileSystemService {
    /**
     * Returns lock object for the specified file. Code that does any operations
     * one file should synchronize on the lock object to make sure no other
     * threads is attempting to access the file concurrently.
     * 
     * @param file
     *            File get lock object for
     */
    public Object getLock(File file);

    /**
     * Returns a DOM document which represents the XML content of the file.
     * 
     * @param file
     *            File to read
     * @return DOM Document with the content of the file
     * @throws IOExceptionParserConfigurationException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     * @t
     * @throws ParserConfigurationException
     *             hrows FileNotFoundExceptio, n
     */
    public Document readXMLDocumentFromFile(File file)
            throws FileNotFoundException, SAXException, IOException,
            ParserConfigurationException, IOException,
            ParserConfigurationException;

    /**
     * Stores DOM document to file. If file is not yet existing, it is created,
     * otherwise it is overwritten.
     * 
     * @param file
     *            File to write
     * @param document
     *            DOM document containing all data that will be written to the
     *            file.
     * @throws IOException
     */
    public void storeXMLDocumentToFile(File file, Document document)
            throws IOException;

    /**
     * Creates a directory on the filesystem
     * 
     * @param directory
     *            Path to the directory to create
     * @param makeParentDirectories
     *            Set to <code>true</code> to create parent directories, if
     *            necessary
     * @throws EditorIOException
     */
    public void makeDirectory(File directory, boolean makeParentDirectories)
            throws EditorIOException;

    /**
     * Copies a file
     * 
     * @param source
     *            File to read from
     * @param target
     *            File to write in
     * @throws EditorIOException
     *             If an I/O errors occurs during the copying process
     */
    public void copy(File source, File target) throws EditorIOException;
}
