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

package de.schlund.pfixcore.example;
import java.util.HashMap;

import org.apache.log4j.Category;
import org.w3c.dom.Element;

import de.schlund.pfixcore.workflow.Context;
import de.schlund.pfixcore.workflow.ContextResource;
import de.schlund.pfixxml.ResultDocument;
import de.schlund.pfixcore.util.PropertiesUtils;
/**
 * ContextTShirt.java
 *
 *
 * Created: Thu Oct 18 19:24:37 2001
 *
 * @author <a href="mailto:jtl@schlund.de">Jens Lautenbacher</a>
 *
 *
 */

public class ContextTShirtImpl implements ContextResource, ContextTShirt{
    private String     size     = null;
    private Integer    color    = null;
    private Integer[]  features = null;
    protected Context  context  = null;
    private Category CAT = Category.getInstance(this.getClass().getName());
    
    public void init(Context context) {
        this.context = context;
    }
    
    public void reset() {
        size     = null;
        color    = null;
        features = null;
    }

    public Integer getColor() { return color; }

    public Integer[] getFeature() { return features; }

    public String getSize() { return size; }

    public void setColor(Integer color) { this.color = color; }
    
    public void setSize(String size) { this.size = size; }

    public void setFeature(Integer[] features) { this.features = features; }

    public boolean needsData() {
        if (size == null || color == null) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        CAT.debug("Doing ContextTShirt...");
        return "[Size: " + size + "][Color: " + color + "]";
    }

    public void insertStatus(ResultDocument resdoc, Element tshirt) {
	tshirt.setAttribute("size", getSize());
        tshirt.setAttribute("color", "" + getColor());
        
        Integer[] tshirtfeatures = getFeature();
        HashMap   featmap        = PropertiesUtils.selectProperties(context.getProperties(), "contexttshirt.feature");
        if (tshirtfeatures != null) {
            for (int i = 0; i < tshirtfeatures.length; i++) {
                Integer feat = tshirtfeatures[i];
                ResultDocument.addTextChild(tshirt, "feature", (String) featmap.get(feat.toString()));
            }
        }
    }
    
}// ContextTShirt
