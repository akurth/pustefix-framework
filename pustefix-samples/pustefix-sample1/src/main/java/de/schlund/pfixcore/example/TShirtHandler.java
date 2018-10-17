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

package de.schlund.pfixcore.example;

import org.pustefixframework.web.mvc.InputHandler;
import org.springframework.beans.factory.annotation.Autowired;

import de.schlund.pfixcore.example.iwrapper.TShirt;

/**
 * TShirtHandler.java
 *
 *
 * Created: Thu Oct 18 18:53:20 2001
 *
 * @author <a href="mailto:jtl@schlund.de">Jens Lautenbacher</a>
 *
 *
 */

public class TShirtHandler implements InputHandler<TShirt> {

    private ContextTShirt cts;
    
    public void handleSubmittedData(TShirt tshirt) {
        Integer       color   = tshirt.getColor();
        String        size    = tshirt.getSize();
        Integer[]     feature = tshirt.getFeature();

        if (size.equals("L") && color.equals(Integer.valueOf(2))) {
            // The combination size "L" and color No. "2" is considered invalid (maybe out of stock) 
            tshirt.addSCodeSize(StatusCodeLib.TSHIRT_SIZECOLOR_OUTOF_STOCK, new String[]{"L", "2"}, "note");
            return;
        }

        // Everything was ok, store it.
        cts.setSize(size);
        cts.setColor(color);
        if (feature != null) {
            cts.setFeature(feature);
        } else {
            cts.setFeature(new Integer[]{Integer.valueOf(-1)});
            // This is needed so we produce some output at all on retrieveCurrentStatus when
            // the user decided to NOT check any checkbox in the UI (this makes defaults work)
        }
        
    }
    
    public void retrieveCurrentStatus(TShirt tshirt) {
        if (!cts.needsData()) {
            tshirt.setColor(cts.getColor());
            tshirt.setSize(cts.getSize());
            tshirt.setFeature(cts.getFeature());
        }
    }
    
    public boolean needsData() {
        return cts.needsData();
    }
    
    public boolean prerequisitesMet() {
        return true;
    }

    public boolean isActive() {
        return true;
    }
    
    @Autowired
    public void setContextTShirt(ContextTShirt cts) {
        this.cts = cts;
    }

}
