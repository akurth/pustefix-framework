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

package de.schlund.pfixcore.generator.casters;
import de.schlund.pfixcore.generator.*;
import de.schlund.pfixxml.*;
import de.schlund.util.statuscodes.*;
import java.text.*;
import java.util.*;

/**
 * ToDate.java
 *
 *
 * Created: Thu Aug 16 15:34:25 2001
 *
 * @author <a href="mailto:jtl@schlund.de">Jens Lautenbacher</a>
 *
 *
 */

public class ToDate extends SimpleCheck implements IWrapperParamCaster {
    private Date[]           value  = null;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
    private StatusCode       scode;
    
    public ToDate() {
        scode = StatusCodeFactory.getInstance().getStatusCode("pfixcore.generator.caster.ERR_TO_DATE");
    }
    
    public void put_scode_casterror(String fqscode) {
        scode = StatusCodeFactory.getInstance().getStatusCode(fqscode);
    }

    public void put_format(String fmtstr) {
        format = new SimpleDateFormat(fmtstr);
    }
    
    public Object[] getValue() {
        return value;
    }

    public void castValue(RequestParam[] param) {
        reset();
        format.setLenient(false);
        Date      val;
        ArrayList dates = new ArrayList();
        for (int i = 0; i < param.length; i++) {
            try {
                val = format.parse(param[i].getValue());
                dates.add(val);
            } catch (ParseException e) {
                val = null;
                addSCode(scode);
                break;
            }
        }
        if (!errorHappened()) {
            value = (Date[]) dates.toArray(new Date[] {});
        }
    }

}// ToDate
