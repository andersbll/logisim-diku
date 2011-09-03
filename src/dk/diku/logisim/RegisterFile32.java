package dk.diku.logisim;

import java.awt.Font;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.ComponentState;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.comp.ManagedComponent;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.tools.AbstractCaret;
import com.cburch.logisim.tools.Caret;
import com.cburch.logisim.tools.Pokable;
import com.cburch.logisim.instance.StdAttr;

/** Represents a N-bit M-way dual port register file.
 * Draws heavily from com.cburch.incr.Counter example.
 */
class RegisterFile32 extends ManagedComponent {

    public static final ComponentFactory factory = new Factory();
    private static final Attribute[] ATTRIBUTES = { StdAttr.TRIGGER };
   
    // storage capacity	
    static final BitWidth WIDTH = BitWidth.create(32);
    static final BitWidth DEPTH = BitWidth.create(5);
	static final int NUM_REGISTERS = 32; // DEPTH=4

	static final int BOX_HEIGHT = 10;
	static final int BOX_WIDTH = 50;
	static final int COL_WIDTH = BOX_WIDTH + 15;
	static final int BOX_SEP = 10;

	// size
	static final int CHIP_WIDTH = 160;
	static final int CHIP_DEPTH = 180;
	//int digits = (WIDTH.getWidth() + 3) / 4;
	//int wid = 7 * digits + 6;

    private static class Factory extends AbstractComponentFactory {
        private Factory() { }
        public String getName() { return "RegisterFile"; }
        public String getDisplayName() { return "Registers"; }
        public AttributeSet createAttributeSet() {
            return AttributeSets.fixedSet(ATTRIBUTES, new Object[] { StdAttr.TRIG_RISING });
        }
        public Component createComponent(Location loc, AttributeSet attrs) { return new RegisterFile32(loc, attrs); }
        public Bounds getOffsetBounds(AttributeSet arg0) { return Bounds.create(-1*CHIP_WIDTH, -1*CHIP_DEPTH/2, CHIP_WIDTH, CHIP_DEPTH); }
    }
    
    private class MyListener implements AttributeListener, Pokable {
        public void attributeListChanged(AttributeEvent e) { }
        public void attributeValueChanged(AttributeEvent e) { }
        public Caret getPokeCaret(ComponentUserEvent event) {
			return new PokeCaret(event.getCircuitState());
		}
    }
    
    private class PokeCaret extends AbstractCaret {
        CircuitState circuitState;
		int idx, idx2;

        PokeCaret(CircuitState circuitState) {
            this.circuitState = circuitState;
			this.idx = 0;
            setBounds(RegisterFile32.this.getBounds());
        }

        public void draw(Graphics g) {
			if (idx < 1) return;
            Bounds bds = RegisterFile32.this.getBounds();
			drawBox(g, bds, Color.RED, idx);
		}

        public void keyTyped(KeyEvent e) {
            // convert it to a hex digit; if it isn't a hex digit, abort.
			// todo: if user clicked up or down, change idx
            State state = RegisterFile32.this.getState(circuitState);
			if (idx < 1) return;
            int nue = Character.digit(e.getKeyChar(), 16);
            if (nue < 0) return;
			int old = (state.R[idx].isFullyDefined() ? state.R[idx].toIntValue() : 0);

			// getMask had a bug, and isn't needed
            Value val = Value.createKnown(WIDTH, ((old<<4) | nue) /* & WIDTH.getMask() */);
            
            state.R[idx] = val;
			// check if need to propagate to P_RDATA1 or P_RDATA2
			int a1 = RegisterFile32.this.addr(circuitState, P_RADDR1);
			if (a1 == idx) 
				circuitState.setValue(RegisterFile32.this.loc(P_RDATA1), val, RegisterFile32.this, 1);
			int a2 = RegisterFile32.this.addr(circuitState, P_RADDR2);
			if (a2 == idx) 
				circuitState.setValue(RegisterFile32.this.loc(P_RDATA2), val, RegisterFile32.this, 1);
        }

        public void stopEditing() { }
        public void cancelEditing() { }
		
		public void mousePressed(MouseEvent e) {
            idx2 = getRIndex(e.getX(), e.getY());
        }
        public void mouseDragged(MouseEvent e) { }
        public void mouseReleased(MouseEvent e) {
            int idx3 = getRIndex(e.getX(), e.getY());
            if(idx3 < 1 || idx2 != idx3) {
                idx = 0;
                return;
            }
            idx = idx3;
		}

        public void keyPressed(KeyEvent e) {
			if (idx < 0) return;
			switch (e.getKeyCode()) {
				case KeyEvent.VK_DOWN:
				case KeyEvent.VK_KP_DOWN:
					if (idx < NUM_REGISTERS - 1) idx++;
					break;
				case KeyEvent.VK_RIGHT:
				case KeyEvent.VK_KP_RIGHT:
					if (0 < idx && idx < NUM_REGISTERS/2) idx += NUM_REGISTERS/2;
					break;
				case KeyEvent.VK_UP:
				case KeyEvent.VK_KP_UP:
					if (idx > 1) idx--;
					break;
				case KeyEvent.VK_LEFT:
				case KeyEvent.VK_KP_LEFT:
					if (idx > NUM_REGISTERS/2) idx -= NUM_REGISTERS/2;
					break;
			}
		}

		private int getRIndex(int x, int y) {
            Bounds bds = RegisterFile32.this.getBounds();
			x -= bds.getX();
			y -= bds.getY();
			if (x < boxX(0) - 1 || x > boxX(NUM_REGISTERS-1) + BOX_WIDTH + 1)
				return -1;
			if (x > boxX(0) + BOX_WIDTH + 1 && x < boxX(NUM_REGISTERS-1) - 1)
				return -1;
			if (y <  boxY(0) || y >  boxY(NUM_REGISTERS-1) + BOX_HEIGHT) return -1;
			int i = y / BOX_HEIGHT;
			if (x > CHIP_WIDTH/2) i += NUM_REGISTERS/2;
			if (i < 0 || i >= NUM_REGISTERS) return -1;
			return i;
		}

    }

    private MyListener myListener = new MyListener();

	static final int P_WDATA = 0;
	static final int P_RDATA1 = 1;
	static final int P_RDATA2 = 2;
	static final int P_WE = 3;
	static final int P_CLK = 4;
	static final int P_WADDR = 5;
	static final int P_RADDR1 = 6;
	static final int P_RADDR2 = 7;
	static final int P_CLEAR = 8;
	static final int NUM_PINS = 9;
    
    private RegisterFile32(Location loc, AttributeSet attrs) {
        super(loc, attrs, NUM_PINS);
		int left = -1*CHIP_WIDTH;
		int right = 0;
		int top = -1*CHIP_DEPTH/2;
		int bottom = CHIP_DEPTH/2;
	// following must be in numerical order of pins
        setEnd(P_WDATA, loc.translate(left, -10), WIDTH, EndData.INPUT_ONLY);
        setEnd(P_RDATA1,loc.translate(right, top+40), WIDTH, EndData.OUTPUT_ONLY);
        setEnd(P_RDATA2,loc.translate(right, bottom-40), WIDTH, EndData.OUTPUT_ONLY);
        setEnd(P_WE,    loc.translate(left+CHIP_WIDTH/2-40, bottom), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(P_CLK,   loc.translate(left, bottom-10), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(P_WADDR, loc.translate(left+CHIP_WIDTH/2-10, bottom), DEPTH, EndData.INPUT_ONLY);
        setEnd(P_RADDR1,loc.translate(left+CHIP_WIDTH/2+20, bottom), DEPTH, EndData.INPUT_ONLY);
        setEnd(P_RADDR2,loc.translate(left+CHIP_WIDTH/2+40, bottom), DEPTH, EndData.INPUT_ONLY);
        setEnd(P_CLEAR, loc.translate(left+CHIP_WIDTH/2+70, bottom), BitWidth.ONE, EndData.INPUT_ONLY);

        attrs.addAttributeListener(myListener);
    }
    
    public Object getFeature(Object key) { return (key == Pokable.class) ? myListener : super.getFeature(key); }
    public ComponentFactory getFactory() { return factory; }

	static final Value zero = Value.createKnown(WIDTH, 0);
	static final Value xxxx = Value.createError(WIDTH);
	static final Value zzzz = Value.createUnknown(WIDTH);

	private class State implements ComponentState, Cloneable {
		public Value lastClock = null;
		public Value R[] = new Value[NUM_REGISTERS];

		public State() { R[0] = zero; reset(zero); }
		public void reset(Value val) {
			for (int i = 1; i < NUM_REGISTERS; i++)
				R[i] = val;
		}

		public Object clone() { try { return super.clone(); } catch(CloneNotSupportedException e) { return null; } }

		public boolean updateClock(Value newClock, Object trigger) {
		    Value oldClock = lastClock;
		    lastClock = newClock;
		    if(trigger == null || trigger == StdAttr.TRIG_RISING) {
			return oldClock == Value.FALSE && newClock == Value.TRUE;
		    } else if(trigger == StdAttr.TRIG_FALLING) {
			return oldClock == Value.TRUE && newClock == Value.FALSE;
		    } else if(trigger == StdAttr.TRIG_HIGH) {
			return newClock == Value.TRUE;
		    } else if(trigger == StdAttr.TRIG_LOW) {
			return newClock == Value.FALSE;
		    } else {
			return oldClock == Value.FALSE && newClock == Value.TRUE;
		    }
		}
	}

	Location loc(int pin) { return getEndLocation(pin); }
	Value val(CircuitState s, int pin) { return s.getValue(loc(pin)); }
	int addr(CircuitState s, int pin) { return val(s, pin).toIntValue(); }

    public void propagate(CircuitState circuitState) {
        State state = getState(circuitState);
	Object triggerType = getAttributeSet().getValue(StdAttr.TRIGGER);

		if (state.updateClock(val(circuitState, P_CLK), triggerType) && val(circuitState, P_WE) == Value.TRUE) {
			int a = addr(circuitState, P_WADDR);
			Value v = val(circuitState, P_WDATA);
			if (a < 0) state.reset(zzzz); // clobber all
			else if (a == 0) { /* skip */ }
			else if (a < NUM_REGISTERS) state.R[a] = v;
			else
				throw new IllegalArgumentException("Write address invalid: Please email kwalsh@cs and tell him!");
		}
		int clear = addr(circuitState, P_CLEAR);
		if(clear>0) {
			state.reset(zero);
		}
		int a1 = addr(circuitState, P_RADDR1);
		int a2 = addr(circuitState, P_RADDR2);
		if (a1 >= NUM_REGISTERS || a2 >= NUM_REGISTERS)
			throw new IllegalArgumentException("Read address invalid: Please email kwalsh@cs and tell him!");
		Value v1 = (a1 < 0 ? zzzz : (a1 < NUM_REGISTERS ? state.R[a1] : xxxx));
		Value v2 = (a2 < 0 ? zzzz : (a2 < NUM_REGISTERS ? state.R[a2] : xxxx));
		circuitState.setValue(loc(P_RDATA1), v1, this, 9);
		circuitState.setValue(loc(P_RDATA2), v2, this, 9);
	}

    private State getState(CircuitState circuitState) {
        State state = (State) circuitState.getData(this);
        if (state == null) {
            state = new State();
            circuitState.setData(this, state);
        }
        return state;
    }

	public int boxX(int i) {
		if (i < NUM_REGISTERS / 2)
			return CHIP_WIDTH / 2 - BOX_SEP / 2 - BOX_WIDTH;
		else
			return CHIP_WIDTH / 2 + BOX_SEP / 2 + COL_WIDTH - BOX_WIDTH;
	}
	
	public int boxY(int i) {
		i = i % (NUM_REGISTERS / 2);
		return i*BOX_HEIGHT+2;
	}

	public void drawBox(Graphics g, Bounds bds, Color color, int i) {
		g.setColor(color);
		g.drawRect(bds.getX() + boxX(i), bds.getY() + boxY(i), BOX_WIDTH, BOX_HEIGHT);
		g.setColor(Color.BLACK);
	}

    public void draw(ComponentDrawContext context) {
        context.drawRectangle(this);
        context.drawClock(this, P_CLK, Direction.EAST);
        context.drawPin(this, P_WADDR);
        context.drawPin(this, P_WDATA);
        context.drawPin(this, P_WE);
        context.drawPin(this, P_RADDR1);
        context.drawPin(this, P_RDATA1);
        context.drawPin(this, P_RADDR2);
        context.drawPin(this, P_RDATA2);
        context.drawPin(this, P_CLEAR);

		Graphics g = context.getGraphics();
		Bounds bds = getBounds();

		Font font = g.getFont().deriveFont(9f);;

		// draw some pin labels
		int left = bds.getX();
		int right = bds.getX()+CHIP_WIDTH;
		int top = bds.getY();
		int bottom =  bds.getY()+CHIP_DEPTH;
		GraphicsUtil.drawText(g, font, "W", left+2, top+CHIP_DEPTH/2-10,
				GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		GraphicsUtil.drawText(g, font, "A", right-2, top+40,
				GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);
		GraphicsUtil.drawText(g, font, "B", right-2, bottom-40,
				GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);
        context.getGraphics().setColor(Color.GRAY);
		GraphicsUtil.drawText(g, "WE", left+CHIP_WIDTH/2-40, bottom-1,
				GraphicsUtil.H_CENTER, GraphicsUtil.V_BOTTOM);
		GraphicsUtil.drawText(g, "rW", left+CHIP_WIDTH/2-10, bottom-1,
				GraphicsUtil.H_CENTER, GraphicsUtil.V_BOTTOM);
		GraphicsUtil.drawText(g, "rA", left+CHIP_WIDTH/2+20, bottom-1,
				GraphicsUtil.H_CENTER, GraphicsUtil.V_BOTTOM);
		GraphicsUtil.drawText(g, "rB", left+CHIP_WIDTH/2+40, bottom-1,
				GraphicsUtil.H_CENTER, GraphicsUtil.V_BOTTOM);
		GraphicsUtil.drawText(g, "clr", left+CHIP_WIDTH/2+70, bottom-1,
				GraphicsUtil.H_CENTER, GraphicsUtil.V_BOTTOM);
        context.getGraphics().setColor(Color.BLACK);

		// draw some rectangles 
		for (int i = 0; i < NUM_REGISTERS; i++) 
			drawBox(g, bds, Color.GRAY, i);

		// draw register labels
		for (int i = 0; i < NUM_REGISTERS; i++) {
			GraphicsUtil.drawText(g, font, "$"+i,
					bds.getX() + boxX(i) - 1,
					bds.getY() + boxY(i) + (BOX_HEIGHT-1)/2,
					GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);
		}
        
        if (!context.getShowState()) return;

		// draw state
		g.setColor(Color.LIGHT_GRAY);
		g.fillRect(bds.getX() + boxX(0)+1, bds.getY() + boxY(0)+1, BOX_WIDTH-1, BOX_HEIGHT-1);
		g.setColor(Color.BLACK);
		State state = getState(context.getCircuitState());
		for (int i = 0; i < NUM_REGISTERS; i++) {
			int v = state.R[i].toIntValue();
			String s = (state.R[i].isFullyDefined() ? StringUtil.toHexString(WIDTH.getWidth(), v) : "?");
			GraphicsUtil.drawText(g, font, s, 
					bds.getX() + boxX(i) + BOX_WIDTH/2,
					bds.getY() + boxY(i) + (BOX_HEIGHT-1)/2,
					GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER);
		}
    }

}
