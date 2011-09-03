package dk.diku.logisim;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.Icon;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
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

class SevenSegment extends ManagedComponent {
    public static final ComponentFactory factory = new Factory();

	static final String[] COLOR_OPTIONS = { "Red", "Green", "Yellow", "Blue", "White" };
		
	public static final Attribute COLOR_OPTION = Attributes.forOption("color",
			new StringGetter() { public String get() { return "Color"; } }, COLOR_OPTIONS);

    private static final Attribute[] ATTRIBUTES = { COLOR_OPTION };

    private static class Factory extends AbstractComponentFactory {
        private Factory() { }
        public String getName() { return "SevenSegmentDisplay"; }
        public String getDisplayName() { return "SevenSegmentDisplay"; }
        public AttributeSet createAttributeSet() {
            return AttributeSets.fixedSet(ATTRIBUTES, new Object[] { "Red" });
        }
        public Component createComponent(Location loc, AttributeSet attrs) { return new SevenSegment(loc, attrs); }
        public Bounds getOffsetBounds(AttributeSet attrs) { return Bounds.create(-20, -10, 20, 30); }
        public void paintIcon(ComponentDrawContext context, int x, int y, AttributeSet attrs) {
			drawSevenSegmentIcon(context, x, y, attrs.getValue(COLOR_OPTION));
        }
		/* public void drawGhost(ComponentDrawContext context, Color color,
				int x, int y, AttributeSet attrs) {
			Graphics g = context.getGraphics();
			Bounds bds = getOffsetBounds(attrs);
			g.setColor(color);
			GraphicsUtil.switchToWidth(g, 2);
			g.drawOval(x + bds.getX() + 1, y + bds.getY() + 1,
				bds.getWidth() - 1, bds.getHeight() - 1);
		} */
    }

    private SevenSegment(Location loc, AttributeSet attrs) {
        super(loc, attrs, 8);
        setEnd(0, getLocation().translate(-20, -10), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(1, getLocation().translate(-20, 0), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(2, getLocation().translate(-20, 10), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(3, getLocation().translate(-20, 20), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(4, getLocation().translate(0, -10), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(5, getLocation().translate(0, 0), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(6, getLocation().translate(0, 10), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(7, getLocation().translate(0, 20), BitWidth.ONE, EndData.INPUT_ONLY);
    }

    public ComponentFactory getFactory() { return factory; }
    public void propagate(CircuitState state) { }

    public void draw(ComponentDrawContext context) {
        Location loc = getLocation();
		drawSevenSegment(context, loc.getX(), loc.getY(), getAttributeSet().getValue(COLOR_OPTION));
	}

	static void drawSevenSegmentIcon(ComponentDrawContext context, int x, int y, Object colorName) {
        Graphics g = context.getGraphics();

		g.setColor(Color.BLACK);
		g.drawRect(x+3, y, 10-1, 16-1);

		for (int i = 0; i < 4; i++) {
			g.drawLine(x+2, y+5*i, x+3, y+5*i);
			g.drawLine(x+12, y+5*i, x+13, y+5*i);
		}

		if (colorFor((String)colorName, true) == Color.WHITE) {
			g.setColor(Color.LIGHT_GRAY);
			g.fillRect(x+4, y+1, 9, 15);
		}

		g.setColor(colorFor((String)colorName, true));
		g.fillRect(x+5, y+3, 2, 4);
		g.fillRect(x+5, y+8, 2, 5);
		g.fillRect(x+9, y+3, 2, 5);
		g.fillRect(x+9, y+9, 2, 4);

		g.fillRect(x+6, y+2, 4, 2);
		g.fillRect(x+6, y+7, 4, 2);
		g.fillRect(x+6, y+12, 4, 2);

		g.setColor(Color.BLACK);
	}

	void drawSevenSegment(ComponentDrawContext context, int x, int y, Object colorName) {
        Graphics g = context.getGraphics();

		x += -20;
		y += -10;

		boolean segs[] = new boolean[8];
		for (int i = 0; i < 8; i++) {
			context.drawPin(this, i);
			segs[i] = context.getCircuitState().getValue(
					getEnd(i).getLocation()).toIntValue() == 1;
		}
		g.setColor(Color.BLACK);
		g.drawRect(x, y, 19, 29);

		if (colorFor((String)(String)colorName, true) == Color.WHITE) {
			g.setColor(Color.LIGHT_GRAY);
			g.fillRect(x+1, y+1, 18, 28);
		}

		g.setColor(colorFor((String)colorName, segs[0]));
		g.drawLine(x+5, y+2, x+16, y+2);
		g.drawLine(x+6, y+3, x+15, y+3);
		g.drawLine(x+7, y+4, x+14, y+4);
		g.setColor(colorFor((String)colorName, segs[5]));
		g.drawLine(x+6, y+13, x+15, y+13);
		g.drawLine(x+5, y+14, x+16, y+14);
		g.drawLine(x+6, y+15, x+15, y+15);
		g.setColor(colorFor((String)colorName, segs[7]));
		g.drawLine(x+5, y+26, x+16, y+26);
		g.drawLine(x+6, y+25, x+15, y+25);
		g.drawLine(x+7, y+24, x+14, y+24);
		g.setColor(colorFor((String)colorName, segs[1]));
		g.drawLine(x+4, y+3, x+4, y+13);
		g.drawLine(x+5, y+4, x+5, y+12);
		g.drawLine(x+6, y+5, x+6, y+11);
		g.setColor(colorFor((String)colorName, segs[3]));
		g.drawLine(x+4, y+15, x+4, y+25);
		g.drawLine(x+5, y+16, x+5, y+24);
		g.drawLine(x+6, y+17, x+6, y+23);
		g.setColor(colorFor((String)colorName, segs[4]));
		g.drawLine(x+17, y+3, x+17, y+13);
		g.drawLine(x+16, y+4, x+16, y+12);
		g.drawLine(x+15, y+5, x+15, y+11);
		g.setColor(colorFor((String)colorName, segs[6]));
		g.drawLine(x+17, y+15, x+17, y+25);
		g.drawLine(x+16, y+16, x+16, y+24);
		g.drawLine(x+15, y+17, x+15, y+23);
		g.setColor(colorFor((String)colorName, segs[2]));
		g.drawLine(x+2, y+26, x+3, y+26);
		g.drawLine(x+1, y+27, x+4, y+27);
		g.drawLine(x+2, y+28, x+3, y+28);

		g.setColor(Color.BLACK);
	}


	static Color colorFor(String s, boolean on) {
		if (on) {
			if (s.equalsIgnoreCase("red")) return Color.RED;
			if (s.equalsIgnoreCase("yellow")) return Color.YELLOW;
			if (s.equalsIgnoreCase("green")) return Color.GREEN;
			if (s.equalsIgnoreCase("blue")) return Color.BLUE;
			if (s.equalsIgnoreCase("white")) return Color.WHITE;
		} else {
			return Color.LIGHT_GRAY;
		}
		return Color.DARK_GRAY;
	}
}
