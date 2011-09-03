/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package dk.diku.logisim;

import java.awt.Color;
import java.awt.Graphics;
import java.io.File;
import java.util.WeakHashMap;

import com.cburch.hex.HexModel;
import com.cburch.hex.HexModelListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.MenuExtender;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;
//import com.cburch.logisim.util.SimpleStringGetter;
import com.cburch.logisim.util.StringUtil;

import com.cburch.logisim.std.memory.*;

abstract class Mem extends InstanceFactory {
    // Note: The code is meant to be able to handle up to 32-bit addresses, but it
    // hasn't been debugged thoroughly. There are two definite changes I would
    // make if I were to extend the address bits: First, there would need to be some
    // modification to the memory's graphical representation, because there isn't
    // room in the box to include such long memory addresses with the current font
    // size. And second, I'd alter the MemContents class's PAGE_SIZE_BITS constant
    // to 14 so that its "page table" isn't quite so big.
    public static final Attribute ADDR_ATTR = Attributes.forBitWidth(
            "addrWidth", new SimpleStringGetter("Address Bit Width"), 2, 24);
    /*public static final Attribute DATA_ATTR = Attributes.forBitWidth(
            "dataWidth", Strings.getter("ramDataWidthAttr")); */
    
    // port-related constants
    static final int DATA = 0;
    static final int ADDR = 1;
//    static final int CS = 2;
    static final int MEM_INPUTS = 2;

    // other constants
    static final int DELAY = 10;

    private WeakHashMap currentInstanceFiles; // mapping Instances to Files

    Mem(String name, StringGetter desc, int extraPorts) {
        super(name, desc);
        currentInstanceFiles = new WeakHashMap();
        setInstancePoker(MemPoker.class);

        setOffsetBounds(Bounds.create(-140, -80, 140, 140));
    }
    
    abstract void configurePorts(Instance instance);
    public abstract AttributeSet createAttributeSet();
    abstract MemState getState(InstanceState state);
    abstract MemState getState(Instance instance, CircuitState state);
    abstract HexFrame getHexFrame(Project proj, Instance instance, CircuitState state);
    public abstract void propagate(InstanceState state);

    protected void configureNewInstance(Instance instance) {
        configurePorts(instance);
    }
    
    void configureStandardPorts(Instance instance, Port[] ps) {
        ps[DATA] = new Port(   0,  0, Port.INOUT, 32 /*DATA_ATTR */);
        ps[ADDR] = new Port(-140,  0, Port.INPUT, ADDR_ATTR);
//        ps[CS]   = new Port( -90, 40, Port.INPUT, 4);
        ps[DATA].setToolTip(new SimpleStringGetter("Data: value loaded at address"));
        ps[ADDR].setToolTip(new SimpleStringGetter("Address: location accessed in memory"));
//        ps[CS].setToolTip(new SimpleStringGetter("Byte Selects: each 0 disables access to one byte of the addressed word."));
    }

    public void paintInstance(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        Bounds bds = painter.getBounds();

        // draw boundary
        painter.drawBounds();

        // draw contents
        if(painter.getShowState()) {
            MemState state = getState(painter);
            state.paint(painter.getGraphics(), bds.getX(), bds.getY());
        } else {
            BitWidth addr = (BitWidth) painter.getAttributeValue(ADDR_ATTR);
            int addrBits = addr.getWidth();
            int bytes = 1 << (addrBits+2); // count bytes, not words
            String label;
            /*if(this instanceof Rom) {
                if(addrBits >= 30) {
                    label = StringUtil.format(Strings.get("romGigabyteLabel"), ""
                            + (bytes >>> 30));
                } else if(addrBits >= 20) {
                    label = StringUtil.format(Strings.get("romMegabyteLabel"), ""
                            + (bytes >> 20));
                } else if(addrBits >= 10) {
                    label = StringUtil.format(Strings.get("romKilobyteLabel"), ""
                            + (bytes >> 10));
                } else {
                    label = StringUtil.format(Strings.get("romByteLabel"), ""
                            + bytes);
                }
            } else */ {
                if(addrBits >= 30) {
                    label = StringUtil.format("%sGB RAM", ""
                            + (bytes >>> 30));
                } else if(addrBits >= 20) {
                    label = StringUtil.format("%sMB RAM", ""
                            + (bytes >> 20));
                } else if(addrBits >= 10) {
                    label = StringUtil.format("%sKB RAM", ""
                            + (bytes >> 10));
                } else {
                    label = StringUtil.format("%sB RAM", ""
                            + bytes);
                }
            }
            GraphicsUtil.drawCenteredText(g, label, bds.getX() + bds.getWidth()
                    / 2, bds.getY() + bds.getHeight() / 2);
        }

        // draw input and output ports
        painter.drawPort(DATA, "D", Direction.WEST);
        painter.drawPort(ADDR, "A", Direction.EAST);
        g.setColor(Color.GRAY);
//        painter.drawPort(CS, "sel", Direction.SOUTH);
    }
    
    File getCurrentImage(Instance instance) {
        return (File) currentInstanceFiles.get(instance);
    }
    
    void setCurrentImage(Instance instance, File value) {
        currentInstanceFiles.put(instance, value);
    }

    protected Object getInstanceFeature(Instance instance, Object key) {
        if(key == MenuExtender.class) return new MemMenu(this, instance);
        return super.getInstanceFeature(instance, key);
    }
    
    static class MemListener implements HexModelListener {
        Instance instance;
        
        MemListener(Instance instance) { this.instance = instance; }
        
        public void metainfoChanged(HexModel source) { }

        public void bytesChanged(HexModel source, long start,
                long numBytes, int[] values) {
            instance.fireInvalidated();
        }
    }
}
