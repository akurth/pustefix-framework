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


/**
 * A ResdocFinalizer's job  is to inserts data of a handler into the {@link ResultDocument}. 
 * All classes which want to do this job have to implement thsi interface.
 * <br/>
 * Created: Fri Oct 12 21:55:52 2001
 *
 * @author <a href="mailto:jtl@schlund.de">Jens Lautenbacher</a>
 *
 *
 */

public interface ResdocFinalizer {
    void onWorkError(IWrapperContainer container) throws Exception;
    void onSuccess(IWrapperContainer container) throws Exception;
    void onRetrieveStatus(IWrapperContainer container) throws Exception;
}

// ResdocFinalizer
