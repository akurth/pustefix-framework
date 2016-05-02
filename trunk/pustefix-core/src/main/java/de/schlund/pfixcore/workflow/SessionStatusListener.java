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

package de.schlund.pfixcore.workflow;

/**
 * Listener that can be registered with a {@link Context} in order to be informed
 * when the status of the underlying session changes.
 * 
 * @deprecated Will be removed, because since Servlet API 2.3 you can use {@link javax.servlet.http.HttpSessionListener}
 */
@Deprecated
public interface SessionStatusListener {
    /**
     * Triggered when the status of a session changes (e.g. when the session is 
     * invalidated) 
     * 
     * @param ev event containing more information about the status change
     */
    void sessionStatusChanged(SessionStatusEvent ev);
}
