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

import org.apache.log4j.Logger;
import org.pustefixframework.config.contextxmlservice.StateConfig;
import org.pustefixframework.web.mvc.internal.ControllerStateAdapter;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

import de.schlund.pfixcore.util.StateUtil;
import de.schlund.pfixxml.PfixServletRequest;
import de.schlund.pfixxml.ResultDocument;

/**
 * @author jtl
 */

public abstract class StateImpl implements ConfigurableState {
  
    protected final Logger CAT = Logger.getLogger(this.getClass());

    public  static final String PROP_INSERTCR = "insertcr";
    
    protected StateConfig config;
    protected ControllerStateAdapter adapter;
    
    public void setConfig(StateConfig config) {
        this.config = config;
    }
    
    public StateConfig getConfig() {
        return this.config;
    }
    
    public void setAdapter(ControllerStateAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * @see de.schlund.pfixcore.util.StateUtil#isPageFlowRunning(Context)
     */
    public final boolean isPageFlowRunning(Context context) {
        return StateUtil.isPageFlowRunning(context);
    }

    /**
     * @see de.schlund.pfixcore.util.StateUtil#isDirectTrigger(Context, PfixServletRequest)
     */
    public final boolean isDirectTrigger(Context context, PfixServletRequest preq) {
        return StateUtil.isDirectTrigger(context, preq);
    }
    

    /**
     * @see de.schlund.pfixcore.util.StateUtil#isSubmitTrigger(Context, PfixServletRequest)
     */    
    public final boolean isSubmitTrigger(Context context, PfixServletRequest preq) {
        return StateUtil.isSubmitTrigger(context, preq);
    }


    /**
     * @see de.schlund.pfixcore.util.StateUtil#isSubmitAuthTrigger(Context, PfixServletRequest)
     */    
    public final boolean isSubmitAuthTrigger(Context context, PfixServletRequest preq) {
        return StateUtil.isSubmitAuthTrigger(context, preq);
    }
    

    /**
     * @see de.schlund.pfixcore.util.StateUtil#createDefaultResultDocument(Context)
     */
    protected ResultDocument createDefaultResultDocument(Context context) throws Exception {
        return StateUtil.createDefaultResultDocument(context, getConfig());
    }
    
    
    /**
     * @see de.schlund.pfixcore.util.StateUtil#renderContextResources(Context, ResultDocument)
     */
    protected void renderContextResources(Context context, ResultDocument resdoc) throws Exception {
        StateUtil.renderContextResources(context, resdoc, getConfig());
    }

    protected ModelAndView processMVC(Context context, PfixServletRequest preq) throws Exception {
        if(adapter != null) {
            String pageName;
            if(context.getCurrentPageRequest() != null) {
                pageName = context.getCurrentPageRequest().getRootName();
            } else {
                pageName = preq.getPageName();
            }
            ModelAndView modelAndView = adapter.tryHandle(preq, this, pageName);
            if(modelAndView != null && modelAndView.getViewName() != null
                    && modelAndView.getViewName().startsWith("redirect:")) {
                context.prohibitContinue();
            }
            return modelAndView;
        }
        return null;
    }
    
    protected void renderMVC(ResultDocument resdoc, ModelAndView modelAndView) throws Exception {
        if(modelAndView != null) {
            ModelMap modelMap = modelAndView.getModelMap();
            for(String key: modelMap.keySet()) {
                Object value = modelMap.get(key);
                if(value instanceof BindingResult) {
                    //TODO: add serializer
                } else {
                    ResultDocument.addObject(resdoc.getRootElement(), key, modelMap.get(key));
                }
            }
        }
    }

    /**
     * @see de.schlund.pfixcore.util.StateUtil#addResponseHeadersAndType(Context, ResultDocument)
     */
    protected void addResponseHeadersAndType(Context context, ResultDocument resdoc) {
        StateUtil.addResponseHeadersAndType(context, resdoc, getConfig());
    }


    /**
     * This default implementation returns <code>true</code>. You may want to override this.
     * 
     * @param context The Context of the current session/user.
     * 
     * @param preq The current PfixServletRequest object, representing the request
     * being processed 
     *
     * @exception Exception if anything goes wrong in the process.
     */    
    public boolean isAccessible(Context context, PfixServletRequest preq) throws Exception {
        return true;
    }

    /**
     * This default implementation returns <code>true</code>. You may want to override this.
     * 
     * @param context The Context of the current session/user.
     * 
     * @param preq The current PfixServletRequest object, representing the request
     * being processed 
     *
     * @exception Exception if anything goes wrong in the process.
     */    
    public boolean needsData(Context context, PfixServletRequest preq) throws Exception {
        return true;
    }
    
    /**
     * You need to implement the state logic (aka business logic) in this method.
     *
     * @param context The Context of the current session/user.
     * 
     * @param preq The current PfixServletRequest object, representing the request
     * being processed 
     *
     * @exception Exception if anything goes wrong in the process.
     */
    public abstract ResultDocument getDocument(Context context, PfixServletRequest preq) throws Exception;

}
