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

import de.schlund.pfixcore.editor2.core.dom.IncludeFile;
import de.schlund.pfixcore.editor2.core.dom.IncludePart;
import de.schlund.pfixcore.editor2.core.dom.IncludePartThemeVariant;
import de.schlund.pfixcore.editor2.core.dom.Theme;
import de.schlund.pfixcore.editor2.core.exception.EditorParsingException;

/**
 * Service providing methods to retrieve IncludeParts
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public interface IncludeFactoryService {
    /**
     * Creates an IncludeFile object using the specified filename
     * 
     * @param filename Relative path to the file
     * @return IncludeFile object corresponding to the filename
     * @throws EditorParsingException If an error occurs during parsing of the IncludeFile
     */
    IncludeFile getIncludeFile(String filename) throws EditorParsingException;
    
    /**
     * Creates an IncludePartThemeVariant object which is a child of the specified IncludePart and uses the specified Theme.
     * 
     * @param theme Theme to use
     * @param part IncludePart the created variant belongs to
     * @return New IncludePartThemeVariant for the given parameters
     */
    IncludePartThemeVariant getIncludePartThemeVariant(Theme theme, IncludePart part);
    
    /**
     * Refreshes a cached IncludeFile. This can be used to make a "passive" (only virtual, not existing on FS) IncludeFile "active" after the corresponding file has been created.
     * 
     * @param filename Relative path to the IncludeFile
     * @throws EditorParsingException 
     */
    void refreshIncludeFile(String filename) throws EditorParsingException;
}
