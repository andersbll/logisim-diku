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

// 128 x 128 pixel LCD display with 8bpp color (byte addressed)
class Video extends ManagedComponent {
    public static final ComponentFactory factory = new Factory();

    static final String BLINK_YES = "Blinking Dot";
    static final String BLINK_NO = "No Cursor";
    static final String[] BLINK_OPTIONS = { BLINK_YES, BLINK_NO };
    static final String RESET_ASYNC = "Asynchronous";
    static final String RESET_SYNC = "Synchronous";
    static final String[] RESET_OPTIONS = { RESET_ASYNC, RESET_SYNC };
	    
    public static final Attribute BLINK_OPTION = Attributes.forOption("cursor",
		    new StringGetter() { public String get() { return "Cursor"; } }, BLINK_OPTIONS);
    public static final Attribute RESET_OPTION = Attributes.forOption("reset",
		    new StringGetter() { public String get() { return "Reset Behavior"; } }, RESET_OPTIONS);

    private static final Attribute[] ATTRIBUTES = { BLINK_OPTION, RESET_OPTION };

    private static class Factory extends AbstractComponentFactory {
        private Factory() { }
        public String getName() { return "LCD Video"; }
        public String getDisplayName() { return "LCD Video"; }
        public AttributeSet createAttributeSet() {
            return AttributeSets.fixedSet(ATTRIBUTES, new Object[] { BLINK_OPTIONS[0], RESET_OPTIONS[0] });
        }
        public Component createComponent(Location loc, AttributeSet attrs) { return new Video(loc, attrs); }
        public Bounds getOffsetBounds(AttributeSet attrs) { return Bounds.create(-270, -140, 270, 270); }
        public void paintIcon(ComponentDrawContext context, int x, int y, AttributeSet attrs) {
			drawVideoIcon(context, x, y);
        }
    }

	static final BitWidth SEVEN = BitWidth.create(7); 
	static final BitWidth BPP = BitWidth.create(16); 

	static final int P_CLK = 0;
	static final int P_WE = 1;
	static final int P_X = 2;
	static final int P_Y = 3;
	static final int P_DATA = 4;
	static final int P_RST = 5;

    private Video(Location loc, AttributeSet attrs) {
        super(loc, attrs, 6);
        setEnd(P_CLK, getLocation().translate(-220, 130), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(P_WE, getLocation().translate(-200, 130), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(P_X, getLocation().translate(-140, 130), SEVEN, EndData.INPUT_ONLY);
        setEnd(P_Y, getLocation().translate(-130, 130), SEVEN, EndData.INPUT_ONLY);
        setEnd(P_DATA, getLocation().translate(-120, 130), BPP, EndData.INPUT_ONLY);
        setEnd(P_RST, getLocation().translate(-240, 130), BitWidth.ONE, EndData.INPUT_ONLY);
    }

    public ComponentFactory getFactory() { return factory; }

	Location loc(int pin) { return getEndLocation(pin); }
	Value val(CircuitState s, int pin) { return s.getValue(loc(pin)); }
	int addr(CircuitState s, int pin) { return val(s, pin).toIntValue(); }

    public void propagate(CircuitState circuitState) {
        State state = getState(circuitState);
		AttributeSet attrs = getAttributeSet();
		int x = addr(circuitState, P_X);
		int y = addr(circuitState, P_Y);
		int color = addr(circuitState, P_DATA);
		state.last_x = x;
		state.last_y = y;
		state.color = color;

		Object reset_option = attrs.getValue(RESET_OPTION);
		if (reset_option == null) reset_option = RESET_OPTIONS[0];

		if (state.tick(val(circuitState, P_CLK)) && val(circuitState, P_WE) == Value.TRUE) {
			Graphics g = state.img.getGraphics();
			g.setColor(new Color(state.img.getColorModel().getRGB(color)));
			g.fillRect(x*2, y*2, 2, 2);
			if (RESET_SYNC.equals(reset_option) && val(circuitState, P_RST) == Value.TRUE) {
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, 128*2, 128*2);
			}
		}

		if (!RESET_SYNC.equals(reset_option) && val(circuitState, P_RST) == Value.TRUE) {
			Graphics g = state.img.getGraphics();
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, 128*2, 128*2);
		}
	}

    public void draw(ComponentDrawContext context) {
        Location loc = getLocation();
		int size = getBounds().getWidth();
		State s = getState(context.getCircuitState());
		AttributeSet attrs = getAttributeSet();
		drawVideo(context, loc.getX(), loc.getY(), s, 
			attrs.getValue(BLINK_OPTION), attrs.getValue(RESET_OPTION));
	}

	static void drawVideoIcon(ComponentDrawContext context, int x, int y) {
		Graphics g = context.getGraphics();
		g.setColor(Color.BLACK);
		g.drawRoundRect(x+0,y+0,16-1,16-1,3,3);
		g.setColor(Color.BLUE);
		g.fillRect(x+3,y+3,10,10);
		g.setColor(Color.BLACK);
	}

	boolean blink() {
		long now = System.currentTimeMillis();
		return (now/1000 % 2 == 0);
	}


	void drawVideo(ComponentDrawContext context, int x, int y, State state, Object blink_option, Object reset_option) {
        Graphics g = context.getGraphics();

		x += -270;
		y += -140;

		g.drawRoundRect(x, y, 270-1, 270-1, 6, 6);
		for (int i = P_CLK+1; i <= P_RST; i++)
			context.drawPin(this, i);
		g.drawRect(x+6, y+6, 258-1, 258-1);
		context.drawClock(this, P_CLK, Direction.NORTH);
		g.drawImage(state.img, x+7, y+7, null);
		// draw a little cursor for sanity
		if (blink_option == null) blink_option = BLINK_OPTIONS[0];
		if (BLINK_YES.equals(blink_option) && blink()) {
			g.setColor(new Color(state.img.getColorModel().getRGB(state.color)));
			g.fillRect(x+7+state.last_x*2, y+7+state.last_y*2, 2, 2);
		}
    }

    private State getState(CircuitState circuitState) {
        State state = (State) circuitState.getData(this);
        if (state == null) {
            state = new State(new BufferedImage(256, 256, BufferedImage.TYPE_USHORT_555_RGB));
            circuitState.setData(this, state);
        }
        return state;
    }

	private class State implements ComponentState, Cloneable {
		public Value lastClock = null;
		public BufferedImage img;
		public int last_x, last_y, color;

		State(BufferedImage img) {
			this.img = img;
		}

		public Object clone() { try { return super.clone(); } catch(CloneNotSupportedException e) { return null; } }

		public boolean tick(Value clk) {
			boolean rising = (lastClock == null || (lastClock == Value.FALSE && clk == Value.TRUE));
			lastClock = clk;
			return rising;
		}
	}
}
