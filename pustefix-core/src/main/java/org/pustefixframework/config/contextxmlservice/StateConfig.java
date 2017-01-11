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

package org.pustefixframework.config.contextxmlservice;

import java.util.Map;
import java.util.Properties;

import de.schlund.pfixcore.workflow.ConfigurableState;
import de.schlund.pfixxml.Tenant;

public interface StateConfig {

    /**
     * Enum type for activity handling policy. The policy will not be applied
     * on handlers with checkactive set to true.
     */
    public enum Policy {
        /**
         * Signal isActive() for the state if any handler is active.
         */
        ANY,
        /**
         * Signal isActive() for the state only if all handlers are active.
         */
        ALL,
        /**
         * Signal isActive() even if none of the handlers is active.
         */
        NONE
    }

    /**
     * Returns the policy for the <code>isActive()</code> method.
     * 
     * @return policy for isActive() check
     */
    StateConfig.Policy getIWrapperPolicy();

    /**
     * Returns the list of IWrappers for this page. IWrappers are used
     * for input handling. The map returned has the form prefix => IWrapperConfig.
     * 
     * @return list of IWrappers
     */
    Map<String, ? extends IWrapperConfig> getIWrappers(Tenant tenant);

    /**
     * Returns context resources defined for this page. The map has the form
     * tagname => context resource object. Each context resource specified here
     * will be included in the result XML tree.
     * 
     * @return mapping of tagname to context resource class
     */
    Map<String, ?> getContextResources();
    
    boolean isLazyContextResource(String prefix);

    /**
     * Returns properties defined for this state.
     * 
     * @return properties for this state
     */
    Properties getProperties();

    boolean requiresToken();
    
    /**
     * Returns the class that is used to construct the state that
     * serves this page.
     * 
     * @return class of the state for this page
     */
    Class<? extends ConfigurableState> getState();
    
    /**
     * Specifies whether this state configuration should be ignored and
     * a bean that is defined elsewhere should be used. 
     * 
     * @return <code>true</code> if and only if this configuration is to
     *  be ignored and an external bean should be used
     */
    boolean isExternalBean();
    
    /**
     * Specifies the scope in which the state object is to be created.
     * All values that are understood by the used container are valid.
     * 
     * @return scope of the state object
     */
    String getScope();
    
    public Map<String, ? extends ProcessActionStateConfig> getProcessActions();
    
    /**
     * Return if handlers should be executed before MVC RequestMapping method.
     * Default is false, i.e. RequestMappings are processed first.
     *
     * @return true if handlers are executed before MVC method.
     */
    boolean preMVC();

}