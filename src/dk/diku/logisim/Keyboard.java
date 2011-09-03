package dk.diku.logisim;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Graphics;
import javax.swing.Icon;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentState;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.comp.ManagedComponent;
import com.cburch.logisim.comp.ComponentUserEvent;
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
import com.cburch.logisim.tools.Pokable;
import com.cburch.logisim.tools.AbstractCaret;
import com.cburch.logisim.tools.Caret;

// keyboard input buffer, queue based with ascii data
class Keyboard extends ManagedComponent {
    public static final ComponentFactory factory = new Factory();

    private static class Factory extends AbstractComponentFactory {
        private Factory() { }
        public String getName() { return "Keyboard Queue"; }
        public String getDisplayName() { return "Keyboard Queue"; }
        public Component createComponent(Location loc, AttributeSet attrs) { return new Keyboard(loc, attrs); }
        public Bounds getOffsetBounds(AttributeSet attrs) { return Bounds.create(-330, -20, 330, 30); }
        public void paintIcon(ComponentDrawContext context, int x, int y, AttributeSet attrs) {
			drawKeyboardIcon(context, x, y);
        }
    }

	static final BitWidth FIVE = BitWidth.create(5); 
	static final BitWidth EIGHT = BitWidth.create(8); 

	static final int P_CLK = 0;
	static final int P_RE = 1;
	static final int P_D = 2;
	static final int P_RST = 3;

    private Keyboard(Location loc, AttributeSet attrs) {
        super(loc, attrs, 4);
        setEnd(P_CLK, getLocation().translate(-220, 10), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(P_RE, getLocation().translate(-200, 10), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(P_D, getLocation().translate(-120, 10), EIGHT, EndData.INPUT_ONLY);
        setEnd(P_RST, getLocation().translate(-240, 10), BitWidth.ONE, EndData.INPUT_ONLY);
    }

    public ComponentFactory getFactory() { return factory; }
    public Object getFeature(Object key) { return (key == Pokable.class) ? myListener : super.getFeature(key); }

	Location loc(int pin) { return getEndLocation(pin); }
	Value val(CircuitState s, int pin) { return s.getValue(loc(pin)); }
	int addr(CircuitState s, int pin) { return val(s, pin).toIntValue(); }

    public void propagate(CircuitState circuitState) {
        State state = getState(circuitState);

		if (state.tick(val(circuitState, P_CLK)) && val(circuitState, P_RE) == Value.TRUE) {
			synchronized(state) {
				if (state.buf.length() > 0) state.buf = state.buf.substring(1);
				if (state.editpos > 0) state.editpos--;
			}
		}
		if (val(circuitState, P_RST) == Value.TRUE) {
			synchronized(state) {
				state.buf = "";
				if (state.editpos > 0) state.editpos = 0;
			}
		}
		byte c;
		synchronized(state) {
			c = (state.buf.length() > 0 ? state.buf.getBytes()[0] : 0);
		}

		circuitState.setValue(loc(P_D), Value.createKnown(EIGHT, (int)c & 0xff), this, 1);
	}

    public void draw(ComponentDrawContext context) {
        Location loc = getLocation();
		int size = getBounds().getWidth();
		State s = getState(context.getCircuitState());
		drawKeyboard(context, loc.getX(), loc.getY(), s);
	}

	static void drawKeyboardIcon(ComponentDrawContext context, int x, int y) {
		Graphics g = context.getGraphics();
		g.setColor(Color.BLACK);
		g.drawRect(x+0,y+2,16-1,10-1);
		g.setColor(Color.LIGHT_GRAY);
		g.fillRect(x+3,y+5,10,5);
		g.setColor(Color.BLACK);
	}

	void drawKeyboard(ComponentDrawContext context, int x, int y, State state) {
        Graphics g = context.getGraphics();

		x += -330;
		y += -20;

		g.drawRect(x, y, 330-1, 30-1);
		context.drawClock(this, P_CLK, Direction.NORTH);
		for (int i = P_CLK+1; i <= P_RST; i++)
			context.drawPin(this, i);

		drawString(g, x, y, state);
	}
	void drawString(Graphics g, int x, int y, State state) {
		g.setColor(Color.LIGHT_GRAY);
		String s;
		int editpos;
		synchronized(state) {
			s = state.buf;
			editpos = state.editpos;
		}

		if (editpos > 31) {
			// show editpos-31 .. editpos, last of which may be blank
			s = "\u2026" + s.substring(editpos-30);
			editpos = 31;
		}

		g.setColor(Color.WHITE);
		g.fillRect(x+3, y+2, 330-6, 30-8);
		g.setColor(Color.LIGHT_GRAY);
		if (s.length() >= 32)
			g.fillRect(x+3, y+2, 330-6, 30-8);
		else if (s.length() > 0)
			g.fillRect(x+3, y+2, 3+10*s.length(), 30-8);

		if (editpos >= 0) {
			g.setColor(Color.DARK_GRAY);
			g.drawRect(x+5+editpos*10, y+3, 10, 19);
		}

		g.setColor(Color.BLACK);
		int n = s.length();
		if (n > 32) n = 32;
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			if (c == 0xa) { // newline
				g.drawString("\\", x+6+i*10-2, y+18);
				g.drawString("n", x+6+i*10+2, y+18);
			} else
				g.drawString(s.substring(i, i+1), x+6+i*10, y+18);
		}
    }

    private State getState(CircuitState circuitState) {
        State state = (State) circuitState.getData(this);
        if (state == null) {
            state = new State();
            circuitState.setData(this, state);
        }
        return state;
    }

	private class State implements ComponentState, Cloneable {
		public Value lastClock = null;
		public String buf = "";
		public int editpos = -1;

		State() { }

		public Object clone() { try { return super.clone(); } catch(CloneNotSupportedException e) { return null; } }

		public boolean tick(Value clk) {
			boolean rising = (lastClock == null || (lastClock == Value.FALSE && clk == Value.TRUE));
			lastClock = clk;
			return rising;
		}
	}

    private class MyListener implements Pokable {
        public Caret getPokeCaret(ComponentUserEvent event) {
			return new PokeCaret(event.getCircuitState());
		}
    }

    private class PokeCaret extends AbstractCaret {
        CircuitState circuitState;

        PokeCaret(CircuitState circuitState) {
            this.circuitState = circuitState;
            setBounds(Keyboard.this.getBounds());
            State state = getState(circuitState);
			synchronized(state) {
				state.editpos = state.buf.length();
			}
        }

        public void draw(Graphics g) {
            State state = getState(circuitState);
			Bounds bds = Keyboard.this.getBounds();
			drawString(g, bds.getX(), bds.getY(), state);
		}

        public void keyTyped(KeyEvent e) {
            State state = getState(circuitState);
			char c = e.getKeyChar();
			if ((c < 0x20 || c > 0x7e) && c != 0xa) //printable chars
				return;
			char a[] = new char[1];
			a[0] = c;
			String s = new String(a);
			synchronized(state) {
				if (state.editpos == state.buf.length())
					state.buf = state.buf + s;
				else if (state.editpos == 0)
					state.buf = s + state.buf;
				else
					state.buf = state.buf.substring(0, state.editpos)
						+ s + state.buf.substring(state.editpos);
				state.editpos++;
			}
		}

        public void stopEditing() { 
            State state = getState(circuitState);
			synchronized(state) {
				state.editpos = -1;
			}
		}
        public void cancelEditing() { }
		
		public void mousePressed(MouseEvent e) { }
        public void mouseDragged(MouseEvent e) { }
        public void mouseReleased(MouseEvent e) { }

        public void keyPressed(KeyEvent e) {
            State state = getState(circuitState);
			synchronized(state) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_RIGHT:
					case KeyEvent.VK_KP_RIGHT:
						if (state.editpos < state.buf.length())
							state.editpos++;
						break;
					case KeyEvent.VK_LEFT:
					case KeyEvent.VK_KP_LEFT:
						if (state.editpos > 0)
							state.editpos--;
						break;
					case KeyEvent.VK_DELETE:
						if (state.editpos == state.buf.length())
							break;
						state.editpos++;
						// fall through to backspace
					case KeyEvent.VK_BACK_SPACE:
						if (state.editpos == 0)
							break;
						if (state.buf.length() == 1)
							state.buf = "";
						else if (state.editpos == state.buf.length())
							state.buf = state.buf.substring(0, state.buf.length()-1);
						else
							state.buf = state.buf.substring(0, state.editpos-1) +
								state.buf.substring(state.editpos);
						state.editpos--;
						break;
				}
			}
		}
    }

    private MyListener myListener = new MyListener();
}
