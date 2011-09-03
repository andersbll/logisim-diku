/* Adapted by kwalsh from Logisim's standard RAM, which is... */
/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package dk.diku.logisim;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.GraphicsUtil;
//import com.cburch.logisim.util.IntegerFactory;
//import com.cburch.logisim.util.SimpleStringGetter;

import com.cburch.logisim.std.memory.*;

public class Ram extends Mem {
    /*static final Object BUS_COMBINED
        = new AttributeOption("combined", Strings.getter("ramBusSynchCombined"));
    static final Object BUS_ASYNCH
        = new AttributeOption("asynch", Strings.getter("ramBusAsynchCombined"));
    static final Object BUS_SEPARATE
        = new AttributeOption("separate", Strings.getter("ramBusSeparate"));

    static final Attribute ATTR_BUS = Attributes.forOption("bus",
            Strings.getter("ramBusAttr"),
            new Object[] { BUS_COMBINED, BUS_ASYNCH, BUS_SEPARATE });
	*/

    private static Attribute[] ATTRIBUTES = {
        Mem.ADDR_ATTR /*, Mem.DATA_ATTR, ATTR_BUS */
    };
    private static Object[] DEFAULTS = {
        BitWidth.create(20) /*, BitWidth.create(8), BUS_COMBINED */
    };
    
    private static final int OE  = MEM_INPUTS + 0;
    private static final int CLR = MEM_INPUTS + 1;
    private static final int CLK = MEM_INPUTS + 2;
    private static final int WE  = MEM_INPUTS + 3;
    private static final int DIN = MEM_INPUTS + 4;

    private static Object[][] logOptions = new Object[9][];

    public Ram() {
        super("MIPS RAM", new SimpleStringGetter("Data memory"), 3);
        setInstanceLogger(Logger.class);
    }
    
    protected void configureNewInstance(Instance instance) {
        super.configureNewInstance(instance);
        instance.addAttributeListener();
    }
    
    protected void instanceAttributeChanged(Instance instance, Attribute attr) {
        super.instanceAttributeChanged(instance, attr);
        configurePorts(instance);
    }
    
    void configurePorts(Instance instance) {
        //Object bus = instance.getAttributeValue(ATTR_BUS);
        //if(bus == null) bus = BUS_COMBINED;
        boolean asynch = false; // bus == null ? false : bus.equals(BUS_ASYNCH);
        boolean separate = true; // bus == null ? false : bus.equals(BUS_SEPARATE);

        int portCount = MEM_INPUTS;
        if(asynch) portCount += 2;
        else if(separate) portCount += 5;
        else portCount += 3;
        Port[] ps = new Port[portCount];

        configureStandardPorts(instance, ps);
        ps[OE]  = new Port(-100, 60, Port.INPUT, 1);
        ps[OE].setToolTip(new SimpleStringGetter("Load: if 1 load memory to output"));
        ps[CLR] = new Port(-20, 60, Port.INPUT, 1);
        ps[CLR].setToolTip(new SimpleStringGetter("Clear: 1 resets contents to zero asynchronously"));
        if(!asynch) {
            ps[CLK] = new Port(-70, 60, Port.INPUT, 1);
            ps[CLK].setToolTip(new SimpleStringGetter("Clock: memory value updates on rise from 0 to 1"));
        }
        if(separate) {
            ps[WE] = new Port(-120, 60, Port.INPUT, 1);
            ps[WE].setToolTip(new SimpleStringGetter("Store: if 1 store input to memory"));
            ps[DIN] = new Port(-140, 20, Port.INPUT, 32 /*DATA_ATTR*/);
            ps[DIN].setToolTip(new SimpleStringGetter("Input: value stored at address"));
        } else {
            ps[DATA].setToolTip(new SimpleStringGetter("Data: value loaded or stored at address"));
        }
        instance.setPorts(ps);
    }

    public AttributeSet createAttributeSet() {
        return AttributeSets.fixedSet(ATTRIBUTES, DEFAULTS);
    }

    MemState getState(InstanceState state) {
        BitWidth addrBits = (BitWidth) state.getAttributeValue(ADDR_ATTR);
        //BitWidth dataBits = (BitWidth) state.getAttributeValue(DATA_ATTR);

        RamState myState = (RamState) state.getData();
        if(myState == null) {
            MemContents contents = MemContents.create(addrBits.getWidth(), 32 /*dataBits.getWidth()*/);
            Instance instance = state.getInstance();
            myState = new RamState(instance, contents, new MemListener(instance));
            state.setData(myState);
        } else {
            myState.setRam(state.getInstance());
        }
        return myState;
    }

    MemState getState(Instance instance, CircuitState state) {
        BitWidth addrBits = (BitWidth) instance.getAttributeValue(ADDR_ATTR);
        //BitWidth dataBits = (BitWidth) instance.getAttributeValue(DATA_ATTR);

        RamState myState = (RamState) instance.getData(state);
        if(myState == null) {
            MemContents contents = MemContents.create(addrBits.getWidth(), 32 /*dataBits.getWidth()*/);
            myState = new RamState(instance, contents, new MemListener(instance));
            instance.setData(state, myState);
        } else {
            myState.setRam(instance);
        }
        return myState;
    }

    HexFrame getHexFrame(Project proj, Instance instance, CircuitState circState) {
        RamState state = (RamState) getState(instance, circState);
        return state.getHexFrame(proj);
    }

    static final Value[] vmask = new Value[] {
	/*0:xxxxxxxx*/ Value.createUnknown(BitWidth.create(32)),
	/*1:xxxxxx00*/ Value.createKnown(BitWidth.create(8), 0).extendWidth(32, Value.UNKNOWN),
	/*2:xxxx00xx*/ Value.createUnknown(BitWidth.create(8)).extendWidth(16, Value.FALSE).extendWidth(32, Value.UNKNOWN),
	/*3:xxxx0000*/ Value.createKnown(BitWidth.create(16), 0).extendWidth(32, Value.UNKNOWN),
	/*4:xx00xxxx*/ Value.createUnknown(BitWidth.create(16)).extendWidth(24, Value.FALSE).extendWidth(32, Value.UNKNOWN),
	/*5:xx00xx00*/ Value.createKnown(BitWidth.create(8), 0).extendWidth(16, Value.UNKNOWN).extendWidth(24, Value.FALSE).extendWidth(32, Value.UNKNOWN),
	/*6:xx0000xx*/ Value.createUnknown(BitWidth.create(8)).extendWidth(24, Value.FALSE).extendWidth(32, Value.UNKNOWN),
	/*7:xx000000*/ Value.createKnown(BitWidth.create(24), 0).extendWidth(32, Value.UNKNOWN),
	/*8:00xxxxxx*/ Value.createUnknown(BitWidth.create(24)).extendWidth(32, Value.FALSE),
	/*9:00xxxx00*/ Value.createKnown(BitWidth.create(8), 0).extendWidth(24, Value.UNKNOWN).extendWidth(32, Value.FALSE),
	/*a:00xx00xx*/ Value.createUnknown(BitWidth.create(8)).extendWidth(16, Value.FALSE).extendWidth(24, Value.UNKNOWN).extendWidth(32, Value.FALSE),
	/*b:00xx0000*/ Value.createKnown(BitWidth.create(16), 0).extendWidth(24, Value.UNKNOWN).extendWidth(32, Value.FALSE),
	/*c:0000xxxx*/ Value.createUnknown(BitWidth.create(16)).extendWidth(32, Value.FALSE),
	/*d:0000xx00*/ Value.createKnown(BitWidth.create(8), 0).extendWidth(16, Value.UNKNOWN).extendWidth(32, Value.FALSE),
	/*e:000000xx*/ Value.createUnknown(BitWidth.create(8)).extendWidth(32, Value.FALSE),
	/*f:00000000*/ Value.createKnown(BitWidth.create(32), 0)
    };

    public void propagate(InstanceState state) {
        RamState myState = (RamState) getState(state);
        BitWidth dataBits = BitWidth.create(32); // (BitWidth) state.getAttributeValue(DATA_ATTR);
        //Object busVal = state.getAttributeValue(ATTR_BUS);
        boolean asynch = false; //busVal == null ? false : busVal.equals(BUS_ASYNCH);
        boolean separate = true; //busVal == null ? false : busVal.equals(BUS_SEPARATE);

        Value addrValue = state.getPort(ADDR);
//        Value maskValue = state.getPort(CS);
        boolean triggered = asynch || myState.setClock(state.getPort(CLK), StdAttr.TRIG_RISING);
        boolean outputEnabled = state.getPort(OE) != Value.FALSE; //XXX should be == Value.TRUE
        boolean writeEnabled = state.getPort(WE) == Value.TRUE;
        boolean shouldClear = state.getPort(CLR) == Value.TRUE;

        if(shouldClear) {
            myState.getContents().clear();
        }

//	int mask = 0, bmask = 0;
////	if (maskValue.get(0) != Value.FALSE) { mask |= 0x1<<0; bmask |= 0xff<<0; }
////	if (maskValue.get(1) != Value.FALSE) { mask |= 0x1<<1; bmask |= 0xff<<8; }
////	if (maskValue.get(2) != Value.FALSE) { mask |= 0x1<<2; bmask |= 0xff<<16; }
////	if (maskValue.get(3) != Value.FALSE) { mask |= 0x1<<3; bmask |= 0xff<<24; }
//	mask |= 0x1<<0; bmask |= 0xff<<0;
//	mask |= 0x1<<1; bmask |= 0xff<<8;
//	mask |= 0x1<<2; bmask |= 0xff<<16;
//	mask |= 0x1<<3; bmask |= 0xff<<24;
	int mask = 0xf, bmask = 0xffffffff;

        if (mask == 0) {
            myState.setCurrent(-1, 0);
            state.setPort(DATA, Value.createUnknown(dataBits), DELAY);
            return;
        }

        int addr = addrValue.toIntValue()>>2;
        if(!addrValue.isFullyDefined() || addr < 0)
            return;
        if(addr != myState.getCurrent() && (outputEnabled || writeEnabled)) {
            myState.setCurrent(addr, mask);
            myState.scrollToShow(addr);
        } else if (mask != myState.getCurrentMask()) {
            myState.setCurrent(addr, mask);
	}

        if(!shouldClear && triggered) {
            boolean shouldStore;
            if(separate) {
                shouldStore = state.getPort(WE) != Value.FALSE;
            } else {
                shouldStore = !outputEnabled;
            }
            if(shouldStore) {
                Value dataValue = state.getPort(separate ? DIN : DATA);
		int newVal = dataValue.toIntValue();
		int oldVal = myState.getContents().get(addr);
		newVal = (newVal & bmask) | (oldVal & ~bmask);
                myState.getContents().set(addr, newVal);
            }
        }

        if(outputEnabled) {
            int val = myState.getContents().get(addr);
	    Value v = vmask[mask].xor(Value.createKnown(BitWidth.create(32), val));
	    state.setPort(DATA, v, DELAY);
        } else {
            state.setPort(DATA, vmask[0], DELAY);
        }
    }

    public void paintInstance(InstancePainter painter) {
        super.paintInstance(painter);
        //Object busVal = painter.getAttributeValue(ATTR_BUS);
        boolean asynch = false; // busVal == null ? false : busVal.equals(BUS_ASYNCH);
        boolean separate = true; // busVal == null ? false : busVal.equals(BUS_SEPARATE);
        
        if(!asynch) painter.drawClock(CLK, Direction.NORTH);
        painter.drawPort(OE, "ld", Direction.SOUTH);
        painter.drawPort(CLR, "clr", Direction.SOUTH);

        if(separate) {
            painter.drawPort(WE, "str", Direction.SOUTH);
            painter.getGraphics().setColor(Color.BLACK);
            painter.drawPort(DIN, "D", Direction.EAST);
        }
    }

    public void paintIcon(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        Font old = g.getFont();
        g.setFont(old.deriveFont(9.0f));
        GraphicsUtil.drawCenteredText(g, "RAM", 10, 9);
        g.setFont(old);
        g.drawRect(0, 4, 19, 12);
        for(int dx = 2; dx < 20; dx += 5) {
            g.drawLine(dx,  2, dx,  4);
            g.drawLine(dx, 16, dx, 18);
        }
    }

    private static class RamState extends MemState
            implements InstanceData, AttributeListener {
        private Instance parent;
        private MemListener listener;
        private HexFrame hexFrame = null;
        private ClockState clockState;

        RamState(Instance parent, MemContents contents, MemListener listener) {
            super(contents);
            this.parent = parent;
            this.listener = listener;
            this.clockState = new ClockState();
            if(parent != null) parent.getAttributeSet().addAttributeListener(this);
            contents.addHexModelListener(listener);
        }
        
        void setRam(Instance value) {
            if(parent == value) return;
            if(parent != null) parent.getAttributeSet().removeAttributeListener(this);
            parent = value;
            if(value != null) value.getAttributeSet().addAttributeListener(this);
        }
        
        public Object clone() {
            RamState ret = (RamState) super.clone();
            ret.parent = null;
            ret.clockState = (ClockState) this.clockState.clone();
            ret.getContents().addHexModelListener(listener);
            return ret;
        }
        
        // Retrieves a HexFrame for editing within a separate window
        public HexFrame getHexFrame(Project proj) {
            if(hexFrame == null) {
                hexFrame = new HexFrame(proj, getContents());
                hexFrame.addWindowListener(new WindowAdapter() {
                    public void windowClosed(WindowEvent e) {
                        hexFrame = null;
                    }
                });
            }
            return hexFrame;
        }
        
        //
        // methods for accessing the write-enable data
        //
        public boolean setClock(Value newClock, Object trigger) {
            return clockState.updateClock(newClock, trigger);
        }

        public void attributeListChanged(AttributeEvent e) { }

        public void attributeValueChanged(AttributeEvent e) {
            AttributeSet attrs = e.getSource();
            BitWidth addrBits = (BitWidth) attrs.getValue(Mem.ADDR_ATTR);
            //BitWidth dataBits = (BitWidth) attrs.getValue(Mem.DATA_ATTR);
            getContents().setDimensions(addrBits.getWidth(), 32 /*dataBits.getWidth()*/);
        }
    }
    
    public static class Logger extends InstanceLogger {
        public Object[] getLogOptions(InstanceState state) {
            int addrBits = ((BitWidth) state.getAttributeValue(ADDR_ATTR)).getWidth();
            if(addrBits >= logOptions.length) addrBits = logOptions.length - 1;
            synchronized(logOptions) {
                Object[] ret = logOptions[addrBits];
                if(ret == null) {
                    ret = new Object[1 << addrBits];
                    logOptions[addrBits] = ret;
                    for(int i = 0; i < ret.length; i++) {
                        ret[i] = IntegerFactory.create(i);
                    }
                }
                return ret;
            }
        }

        public String getLogName(InstanceState state, Object option) {
            if(option instanceof Integer) {
                String disp = "MIPSRAM";
                Location loc = state.getInstance().getLocation();
                return disp + loc + "[" + option + "]";
            } else {
                return null;
            }
        }

        public Value getLogValue(InstanceState state, Object option) {
            if(option instanceof Integer) {
                MemState s = (MemState) state.getData();
                int addr = ((Integer) option).intValue();
                return Value.createKnown(BitWidth.create(s.getDataBits()),
                        s.getContents().get(addr));
            } else {
                return Value.NIL;
            }
        }
    }
}
