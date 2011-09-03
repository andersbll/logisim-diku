/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package dk.diku.logisim;


import com.cburch.logisim.std.memory.*;

import java.util.Arrays;

class MemContentsSub {
    private MemContentsSub() { }
    
    static ContentsInterface createContents(int size, int bits) {
        if(bits <= 8)       return new ByteContents(size);
        else if(bits <= 16) return new ShortContents(size);
        else                return new IntContents(size);
    }
    
    static abstract class ContentsInterface implements Cloneable {
        public Object clone() {
            try {
                return super.clone();
            } catch(CloneNotSupportedException e) {
                return this;
            }
        }
        abstract int getLength();
        abstract int get(int addr);
        abstract void set(int addr, int value);
        abstract void clear();
        abstract void load(int start, int[] values, int mask);

        boolean matches(int[] values, int start, int mask) {
            for(int i = 0; i < values.length; i++) {
                if(get(start + i) != (values[i] & mask)) return false;
            }
            return true;
        }
        
        int[] get(int start, int len) {
            int[] ret = new int[len];
            for(int i = 0; i < ret.length; i++) ret[i] = get(start + i);
            return ret;
        }
        
        boolean isClear() {
            for(int i = 0, n = getLength(); i < n; i++) {
                if(get(i) != 0) return false;
            }
            return true;
        }
    }
    
    private static class ByteContents extends ContentsInterface {
        private byte[] data;
    
        public ByteContents(int size) {
            data = new byte[size];
        }
        
        public Object clone() {
            ByteContents ret = (ByteContents) super.clone();
            ret.data = new byte[this.data.length];
            System.arraycopy(this.data, 0, ret.data, 0, this.data.length);
            return ret;
        }
        
        //
        // methods for accessing data within memory
        //
        int getLength() {
            return data.length;
        }
        
        int get(int addr) {
            return addr >= 0 && addr < data.length ? data[addr] : 0;
        }
        
        void set(int addr, int value) {
            if(addr >= 0 && addr < data.length) {
                byte oldValue = data[addr];
                if(value != oldValue) {
                    data[addr] = (byte) value;
                }
            }
        }
    
        void clear() {
            Arrays.fill(data, (byte) 0);
        }
    
        void load(int start, int[] values, int mask) {
            int n = Math.min(values.length, data.length - start);
            for(int i = 0; i < n; i++) {
                data[start + i] = (byte) (values[i] & mask);
            }
        }
    }

    private static class ShortContents extends ContentsInterface {
        private short[] data;
    
        public ShortContents(int size) {
            data = new short[size];
        }
        
        public Object clone() {
            ShortContents ret = (ShortContents) super.clone();
            ret.data = new short[this.data.length];
            System.arraycopy(this.data, 0, ret.data, 0, this.data.length);
            return ret;
        }
        
        //
        // methods for accessing data within memory
        //
        int getLength() {
            return data.length;
        }
        
        int get(int addr) {
            return addr >= 0 && addr < data.length ? data[addr] : 0;
        }
        
        void set(int addr, int value) {
            if(addr >= 0 && addr < data.length) {
                short oldValue = data[addr];
                if(value != oldValue) {
                    data[addr] = (short) value;
                }
            }
        }
    
        void clear() {
            Arrays.fill(data, (short) 0);
        }
    
        void load(int start, int[] values, int mask) {
            int n = Math.min(values.length, data.length - start);
            for(int i = 0; i < n; i++) {
                data[start + i] = (short) (values[i] & mask);
            }
        }
    }
    
    private static class IntContents extends ContentsInterface {
        private int[] data;
    
        public IntContents(int size) {
            data = new int[size];
        }
        
        public Object clone() {
            IntContents ret = (IntContents) super.clone();
            ret.data = new int[this.data.length];
            System.arraycopy(this.data, 0, ret.data, 0, this.data.length);
            return ret;
        }
        
        //
        // methods for accessing data within memory
        //
        int getLength() {
            return data.length;
        }
        
        int get(int addr) {
            return addr >= 0 && addr < data.length ? data[addr] : 0;
        }
        
        void set(int addr, int value) {
            if(addr >= 0 && addr < data.length) {
                int oldValue = data[addr];
                if(value != oldValue) {
                    data[addr] = value;
                }
            }
        }
    
        void clear() {
            Arrays.fill(data, 0);
        }
    
        void load(int start, int[] values, int mask) {
            int n = Math.min(values.length, data.length - start);
            for(int i = 0; i < n; i++) {
                data[i] = values[i] & mask;
            }
        }
    }
}
