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

package de.schlund.pfixcore.workflow;

import java.util.*;

import de.schlund.pfixcore.util.PropertiesUtils;
import org.apache.log4j.*;

/**
 * @author: jtl
 *
 *
 */

public class PageFlow {
    private String    flowname;
    private ArrayList allsteps = new ArrayList();
    private HashMap   stepmap  = new HashMap();
    
    private final static String PROPERTY_PREFIX   = PageFlowManager.PROP_PREFIX;
    private final static String FLAG_FINAL        = "FINAL";
    private static Category     LOG               = Category.getInstance(PageFlow.class.getName());
    private PageRequest         finalpage         = null;
    
    public PageFlow(Properties props, String name) {
        flowname       = name;
        Map     map    = PropertiesUtils.selectProperties(props, PROPERTY_PREFIX + "." + name);
        TreeMap sorted = new TreeMap();
        
        for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
            String  key      = (String) i.next();
            String  pagename = (String) map.get(key);
            Integer index;
            
            if (key.equals(FLAG_FINAL)) {
                finalpage = new PageRequest(pagename);
            } else {
                try {
                    index = new Integer(key);
                    sorted.put(index, pagename);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("**** The Pageflow [" + name + "] didn't specifiy a numerical index for page [" +
                                               pagename + "] ****\n" + e.getMessage());
                }
            }
        }

        for (Iterator i = sorted.values().iterator(); i.hasNext(); ) {
            String   pagename = (String) i.next();
            FlowStep step     = new FlowStep(new PageRequest(pagename), props, name);
            allsteps.add(step);
            stepmap.put(step.getPageRequest(), step);
        }
        
        if (LOG.isDebugEnabled()) {
            for (int i = 0; i < allsteps.size(); i++) {
                LOG.debug(">>> Workflow '" + name + "' Step #" + i + " " + allsteps.get(i));
            }
        }
    }

    public boolean containsPageRequest(PageRequest page) {
        return stepmap.keySet().contains(page);
    }

    /**
     * Return position of page in the PageFlow, starting with 0. Return -1 if
     * page isn't a member of the PageFlow.
     *
     * @param page a <code>PageRequest</code> value
     * @return an <code>int</code> value
     */
    public int getIndexOfPageRequest(PageRequest page) {
        FlowStep step = (FlowStep) stepmap.get(page);
        if (step != null) {
            return allsteps.indexOf(step);
        } else {
            return -1;
        }
    }

    public String getName() {
        return flowname;
    }

    public FlowStep[] getAllSteps() {
        return (FlowStep[]) allsteps.toArray(new FlowStep[] {});
    }
    
    public FlowStep getFlowStepForPage(PageRequest page) {
        return (FlowStep) stepmap.get(page);
    }

    public FlowStep getFirstStep() {
        return (FlowStep) allsteps.get(0);
    }
    
    public PageRequest getFinalPage() {
        return finalpage;
    }

    public String toString() {
        String ret = "";
        for (int i = 0; i < allsteps.size(); i++) {
            if (ret.length() > 0) {
                ret += ", ";
            } else {
                ret  = flowname + " = ";
            }
            ret += "[" + i + ": " + allsteps.get(i) + "]";
        }
        if (finalpage != null) {
            ret += " FINAL: " + finalpage.getName();
        }
        
        return ret;
    }

}
