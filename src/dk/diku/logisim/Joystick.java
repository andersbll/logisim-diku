package dk.diku.logisim;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;

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

class Joystick extends ManagedComponent {

    public static final ComponentFactory factory = new Factory();
   
    // storage capacity	
    static final BitWidth RESOLUTION = BitWidth.create(3);

    private static class Factory extends AbstractComponentFactory {
        private Factory() { }
        public String getName() { return "Joystick"; }
        public String getDisplayName() { return "Joystick"; }
        public Component createComponent(Location loc, AttributeSet attrs) { return new Joystick(loc, attrs); }
        public Bounds getOffsetBounds(AttributeSet arg0) { return Bounds.create(-30,-20, 30, 30); }
    }
    
    private class MyListener implements Pokable {
        public Caret getPokeCaret(ComponentUserEvent event) {
			return new PokeCaret(event.getCircuitState());
		}
    }
    
    private class PokeCaret extends AbstractCaret {
        CircuitState circuitState;
		int px, py; // relative to center

        PokeCaret(CircuitState circuitState) { this.circuitState = circuitState; }

        public void draw(Graphics g) { drawJoystick(null, g, px, py); }

        public void keyTyped(KeyEvent e) { }
        public void keyPressed(KeyEvent e) { }
        public void stopEditing() { }
        public void cancelEditing() { }
		
		public void mousePressed(MouseEvent e) { move(e.getX(), e.getY()); }
        public void mouseDragged(MouseEvent e) { move(e.getX(), e.getY()); }
        public void mouseReleased(MouseEvent e) { set(0, 0); }

		private void move(int mx, int my) {
            Bounds bds = Joystick.this.getBounds();
			set(mx - bds.getX()-15, my - bds.getY()-15);
		}

		private void set(int mx, int my) {
			if (mx < -14) px = -14;
			else if (mx > 13) px = 13;
			else px = mx;
			if (my < -14) py = -14;
			else if (my > 13) py = 13;
			else py = my;
			jx = (px + 14)/4 - 3;
			jy = (py + 14)/4 - 3;
			propagate(circuitState);
		}
    }

	Location loc(int pin) { return getEndLocation(pin); }

    private MyListener myListener = new MyListener();

	static final int P_X = 0;
	static final int P_Y = 1;
	int jx, jy;
    
    private Joystick(Location loc, AttributeSet attrs) {
        super(loc, attrs, 2);
        setEnd(P_X, loc.translate(0, -10), RESOLUTION, EndData.OUTPUT_ONLY);
        setEnd(P_Y, loc.translate(0, 0), RESOLUTION, EndData.OUTPUT_ONLY);
    }
    
    public Object getFeature(Object key) { return (key == Pokable.class) ? myListener : super.getFeature(key); }
    public ComponentFactory getFactory() { return factory; }

    public void propagate(CircuitState circuitState) {
		circuitState.setValue(Joystick.this.loc(P_X), Value.createKnown(RESOLUTION, jx), this, 5);
		circuitState.setValue(Joystick.this.loc(P_Y), Value.createKnown(RESOLUTION, jy), this, 5);
	}

    public void draw(ComponentDrawContext context) {
		drawJoystick(context, context.getGraphics(), 0, 0);
	}

    public void drawJoystick(ComponentDrawContext context, Graphics g, int x, int y) {
		Bounds bds = getBounds();
        g.drawRoundRect(bds.getX(), bds.getY(), 30, 30, 6, 6);
        g.drawRoundRect(bds.getX()+2, bds.getY()+2, 26, 26, 4, 4);
        if (context != null) context.drawPin(this, P_X);
        if (context != null) context.drawPin(this, P_Y);

		int cx = bds.getX() + 15;
		int cy = bds.getY() + 15;

		g.setColor(Color.WHITE);
		g.fillOval(cx-5, cy-5, 10, 10);

		g.setColor(Color.BLACK);
		if (g instanceof Graphics2D)
			((Graphics2D)g).setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.drawLine(cx, cy, cx+x, cy+y);
		if (g instanceof Graphics2D)
			((Graphics2D)g).setStroke(new BasicStroke());
		g.fillOval(cx+x-5, cy+y-5, 10, 10);

		g.setColor(new Color(0x880000));
		g.fillOval(cx+x-4, cy+y-4, 8, 8);
		g.setColor(new Color(0xbb4444));
		g.fillOval(cx+x-3, cy+y-3, 5, 5);
		g.setColor(new Color(0xff8888));
		g.fillOval(cx+x-2, cy+y-2, 3, 3);

		g.setColor(Color.BLACK);
    }

}
