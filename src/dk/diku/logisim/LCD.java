package dk.diku.logisim;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Graphics;
import javax.swing.Icon;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentState;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.comp.ManagedComponent;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.StringGetter;

// 32 x 1 character LCD display, byte addressed, ascii data
class LCD extends ManagedComponent {
    public static final ComponentFactory factory = new Factory();

    private static class Factory extends AbstractComponentFactory {
        private Factory() { }
        public String getName() { return "LCD Banner"; }
        public String getDisplayName() { return "LCD Banner"; }
        public Component createComponent(Location loc, AttributeSet attrs) { return new LCD(loc, attrs); }
        public Bounds getOffsetBounds(AttributeSet attrs) { return Bounds.create(-330, -20, 330, 30); }
        public void paintIcon(ComponentDrawContext context, int x, int y, AttributeSet attrs) {
			drawLCDIcon(context, x, y);
        }
    }

	static final BitWidth FIVE = BitWidth.create(5); 
	static final BitWidth EIGHT = BitWidth.create(8); 

	static final int P_CLK = 0;
	static final int P_WE = 1;
	static final int P_A = 2;
	static final int P_D = 3;
	static final int P_RST = 4;

    private LCD(Location loc, AttributeSet attrs) {
        super(loc, attrs, 5);
        setEnd(P_CLK, getLocation().translate(-220, 10), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(P_WE, getLocation().translate(-200, 10), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(P_A, getLocation().translate(-140, 10), FIVE, EndData.INPUT_ONLY);
        setEnd(P_D, getLocation().translate(-120, 10), EIGHT, EndData.INPUT_ONLY);
        setEnd(P_RST, getLocation().translate(-240, 10), BitWidth.ONE, EndData.INPUT_ONLY);
    }

    public ComponentFactory getFactory() { return factory; }

	Location loc(int pin) { return getEndLocation(pin); }
	Value val(CircuitState s, int pin) { return s.getValue(loc(pin)); }
	int addr(CircuitState s, int pin) { return val(s, pin).toIntValue(); }

    public void propagate(CircuitState circuitState) {
        State state = getState(circuitState);
		int a = addr(circuitState, P_A);
		if (a >= 0) state.last_a = a;

		if (state.tick(val(circuitState, P_CLK)) && val(circuitState, P_WE) == Value.TRUE) {
			int c = addr(circuitState, P_D);
			if (c >= 0x21 && c <= 0x7f)
				state.c[a % state.c.length] = (byte)c;
			else
				state.c[a % state.c.length] = (byte)0x20;
		}
		if (val(circuitState, P_RST) == Value.TRUE) {
			for (int i = 0; i < state.c.length; i++) {
				state.c[i] = 0x20; // space
			}
		}
	}

    public void draw(ComponentDrawContext context) {
        Location loc = getLocation();
		int size = getBounds().getWidth();
		State s = getState(context.getCircuitState());
		drawLCD(context, loc.getX(), loc.getY(), s);
	}

	static void drawLCDIcon(ComponentDrawContext context, int x, int y) {
		Graphics g = context.getGraphics();
		g.setColor(Color.BLACK);
		g.drawRect(x+0,y+2,16-1,10-1);
		g.setColor(Color.LIGHT_GRAY);
		g.fillRect(x+3,y+5,10,5);
		g.setColor(Color.BLACK);
	}


	void drawLCD(ComponentDrawContext context, int x, int y, State state) {
        Graphics g = context.getGraphics();

		x += -330;
		y += -20;

		g.drawRect(x, y, 330-1, 30-1);
		context.drawClock(this, P_CLK, Direction.NORTH);
		for (int i = P_CLK+1; i <= P_RST; i++)
			context.drawPin(this, i);

		g.setColor(Color.LIGHT_GRAY);
		g.fillRect(x+3, y+2, 330-6, 30-8);

		g.setColor(Color.DARK_GRAY);
		g.drawRect(x+5+state.last_a*10, y+3, 10, 19);
		g.setColor(Color.BLACK);
		String s;
		try {
			s = new String(state.c, "UTF-8");
		} catch (Exception e) {
			s = "ENCODING ERROR";
		}
		int n = state.c.length;
		if (n > s.length()) n = s.length();
		for (int i = 0; i < n; i++) {
			g.drawString(s.substring(i, i+1), x+6+i*10, y+18);
		}
    }

    private State getState(CircuitState circuitState) {
        State state = (State) circuitState.getData(this);
        if (state == null) {
            state = new State(32);
            circuitState.setData(this, state);
        }
        return state;
    }

	private class State implements ComponentState, Cloneable {
		public Value lastClock = null;
		public byte c[];
		public int last_a;

		State(int len) {
			c = new byte[len];
			for (int i = 0; i < c.length; i++) c[i] = 0x20; // space
		}

		public Object clone() { try { return super.clone(); } catch(CloneNotSupportedException e) { return null; } }

		public boolean tick(Value clk) {
			boolean rising = (lastClock == null || (lastClock == Value.FALSE && clk == Value.TRUE));
			lastClock = clk;
			return rising;
		}
	}
}
