/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package dk.diku.logisim;
import com.cburch.logisim.util.*;

public class IntegerFactory {
    public static final Integer ZERO = create(0);
    public static final Integer ONE = create(1);

    private static Integer[] prefabs = null;
    private static final Cache cache = new Cache(5);

    private IntegerFactory() { }

    public static Integer create(int val) {
        if(prefabs == null) {
            prefabs = new Integer[101];
            for(int i = 0; i < prefabs.length; i++) {
                prefabs[i] = new Integer(i);
            }
        }
        if(val >= 0 && val < prefabs.length) {
            return prefabs[val];
        } else {
            Object cached = cache.get(val);
            if(cached != null) {
                Integer i = (Integer) cached;
                if(i.intValue() == val) return i;
            }
            Integer ret = new Integer(val);
            cache.put(val, ret);
            return ret;
        }
    }

    public static Integer create(String str) {
        return create(Integer.parseInt(str));
    }
}
