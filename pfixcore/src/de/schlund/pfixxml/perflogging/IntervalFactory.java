/*
 * Created on 08.06.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package de.schlund.pfixxml.perflogging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author jh
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class IntervalFactory {
    private static IntervalFactory instance = new IntervalFactory();
    private int interval_size = 20;
    private int increase_factor = 2;
    private HashMap interval_category_map;
    
    
    private IntervalFactory() {
        interval_category_map = new HashMap();
    }
    
    
    
    static IntervalFactory getInstance() {
        return instance;
    }
    
    List getIntervalForCategory(String category) {
        if(!interval_category_map.containsKey(category)) {
            List intervals = createInterval();
            interval_category_map.put(category, intervals);
        }
        return (List) interval_category_map.get(category);
    }
    
    HashMap getAllIntervals() {
        return interval_category_map;
    }
    
    private List createInterval() {
        List intervals = new ArrayList(interval_size); 
        intervals.add(0, new Interval(0, 1));
        intervals.add(1, new Interval(1, increase_factor));
        
        for(int i=2; i<interval_size -1; i++) {
            long pre_until = ((Interval) intervals.get(i-1)).getUntil(); 
            intervals.add(i,new Interval(
                     pre_until, pre_until * increase_factor)); 
        }
        
        intervals.add(interval_size -1, new Interval(
                ((Interval) intervals.get(interval_size -2)).getUntil(), Long.MAX_VALUE));
        
       /* for(int i=0; i<interval_size; i++) {
            System.out.print(i+":->"+intervals.get(i)+"|");
            System.out.println();
        }*/
        return intervals;
    }
   
}
