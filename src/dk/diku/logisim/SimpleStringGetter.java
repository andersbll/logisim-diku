/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package dk.diku.logisim;
import com.cburch.logisim.util.StringGetter;

public class SimpleStringGetter implements StringGetter {
    private String str;
    
    public SimpleStringGetter(String str) {
        this.str = str;
    }
    
    public String get() {
        return str;
    }
    
    public String toString() {
        return str;
    }
}
