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
package de.schlund.pfixcore.generator.postchecks;

import java.util.StringTokenizer;

import org.pustefixframework.generated.CoreStatusCodes;

import de.schlund.pfixcore.generator.IWrapperParamPostCheck;
import de.schlund.pfixcore.generator.SimpleCheck;
import de.schlund.util.statuscodes.StatusCode;
import de.schlund.util.statuscodes.StatusCodeHelper;

public class IntegerRange extends SimpleCheck implements IWrapperParamPostCheck {

    private int        lower;
    private int        upper;
    private StatusCode scode_small;
    private StatusCode scode_big;
    
    
    public IntegerRange() {
        scode_small = CoreStatusCodes.POSTCHECK_INTEGER_TOO_SMALL;
        scode_big   = CoreStatusCodes.POSTCHECK_INTEGER_TOO_BIG;
    }

    public void setScodeTooSmall(String fqscode) {
        scode_small = StatusCodeHelper.getStatusCodeByName(fqscode);
    }

    public void setScodeTooBig(String fqscode) {
        scode_big = StatusCodeHelper.getStatusCodeByName(fqscode);
    }

    public void setRange(String param) {
        param = param.trim();
        
        StringTokenizer tok = new StringTokenizer(param, " :", false);
        if (tok.countTokens() == 2) {
            Integer thelower = Integer.valueOf(tok.nextToken());
            Integer theupper = Integer.valueOf(tok.nextToken());
            lower = thelower.intValue();
            upper = theupper.intValue();
        } else {
            throw new RuntimeException("Range spec '" + param + "' isn't correct");
        }
    }

    public void check(Object[] obj) {
        reset();
        for (int i = 0; i < obj.length; i++) {
            int value = ((Integer) obj[i]).intValue();
            if (lower > value) {
                addSCode(scode_small);
                break;
            } else if (upper < value) {
                addSCode(scode_big);
                break;
            }
        }
    }

}
