package com.cburch.logisim.std.ark;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.*;
import com.cburch.logisim.data.*;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Graphics;
import java.awt.Color;

public class ALU extends ManagedComponent
{
    public static final ComponentFactory factory = new Factory();

    private static class Factory extends AbstractComponentFactory {
        private Factory() { }
        public String getName() { return "Mips ALU"; }
        public String getDisplayName() { return "Mips ALU"; }
        public Component createComponent(Location loc, AttributeSet attrs) { return new ALU(loc, attrs); }
        public Bounds getOffsetBounds(AttributeSet attrs) { return Bounds.create(-30, -50, 60, 100); }
        public void paintIcon(ComponentDrawContext context, int x, int y, AttributeSet attrs) {
			drawALUIcon(context, x, y);
        }
    }

    public ALU(Location loc, AttributeSet attrs) {
        super(loc, attrs, 5);
        setEnd(0, loc.translate(-30, -30), BITWIDTH_32, 1);
        setEnd(1, loc.translate(-30, 30), BITWIDTH_32, 1);
        setEnd(2, loc.translate(-10, 40), BITWIDTH_4, 1);
        setEnd(3, loc.translate(10, 30), BITWIDTH_5, 1);
        setEnd(4, loc.translate(30, -20), BITWIDTH_1, 2);
        setEnd(5, loc.translate(30, 0), BITWIDTH_32, 2);
    }

    public ComponentFactory getFactory() { return factory; }

    public void propagate(CircuitState state) {
        int A = state.getValue(getEndLocation(0)).toIntValue();
        int B = state.getValue(getEndLocation(1)).toIntValue();
        int op = state.getValue(getEndLocation(2)).toIntValue();
        int shift = state.getValue(getEndLocation(3)).toIntValue();
        int ans = 0;
        switch(op)
        {
        case 0x0:
            ans = A & B;
            break;
        case 0x1:
            ans = A | B;
            break;
        case 0x2:
            ans = A + B;
        case 0x6:
            ans = A - B;
            break;
        case 0x7:
            ans = (A < B) ? 0x1 : 0x0;
            break;
        case 0xC:
            ans = ~(A | B);
            break;
//        case 0x0:
//        case 0x1:
//            ans = B << shift;
//            break;
//
//        case 0x2:
//        case 0x3:
//            ans = A + B;
//            break;
//
//        case 0x4:
//            ans = B >>> shift; // logical
//            break;
//
//        case 0x5:
//            ans = B >> shift; // arithmetic
//            break;
//
//        case 0x6:
//        case 0x7:
//            ans = A - B;
//            break;
//
//        case 0x8:
//            ans = A & B;
//            break;
//
//        case 0xA:
//            ans = A | B;
//            break;
//
//        case 0xC:
//            ans = A ^ B;
//            break;
//
//        case 0xE:
//            ans = ~(A | B);
//            break;
//
//        case 0x9:
//	    ans = (A == B) ? 0x1 : 0x0;
//	    break;
//
//        case 0xB:
//	    ans = (A != B) ? 0x1 : 0x0;
//	    break;
//
//        case 0xD:
//	    ans = (A > 0) ? 0x1 : 0x0;
//	    break;
//
//        case 0xF:
//	    ans = (A <= 0) ? 0x1 : 0x0;
//	    break;
        }
        Value out = Value.createKnown(BITWIDTH_32, ans);
        Value outZero = Value.createKnown(BITWIDTH_1, (ans==0) ? 1:0);
        state.setValue(getEndLocation(4), outZero, this, 4);
        state.setValue(getEndLocation(5), out, this, 5);
    }

    static void drawALUIcon(ComponentDrawContext context, int x, int y) {
	Graphics g = context.getGraphics();
	g.setColor(Color.BLACK);
        int xp[] = {
            x, x+15, x+15, x   , x   , x+3, x
        };
        int yp[] = {
            y, y+5 , y+10, y+15, y+10, y+8, y+6
        };
        g.drawPolygon(xp, yp, 7);
    }

    static void drawALU(Graphics g, Bounds bds)
    {
        int wid = bds.getWidth();
        int ht = bds.getHeight();
        int x0 = bds.getX();
        int x1 = x0 + wid;
        int y0 = bds.getY();
        int y1 = y0 + ht;
        int xp[] = {
            x0, x1, x1, x0, x0, x0 + 20, x0
        };
        int yp[] = {
            y0, y0 + 30, y1 - 30, y1, y1 - 40, y1 - 50, y1 - 60
        };
        GraphicsUtil.switchToWidth(g, 2);
        g.drawPolygon(xp, yp, 7);
    }

    public void draw(ComponentDrawContext context)
    {
        drawALU(context.getGraphics(), getBounds());
        context.drawPin(this, 0, "A", Direction.EAST);
        context.drawPin(this, 1, "B", Direction.EAST);
        context.drawPin(this, 2, "OP", Direction.SOUTH);
        context.drawPin(this, 3, "SA", Direction.SOUTH);
        context.drawPin(this, 4, "Zero", Direction.WEST);
        context.drawPin(this, 5, "Res", Direction.WEST);
    }

    private static final BitWidth BITWIDTH_32 = BitWidth.create(32);
    private static final BitWidth BITWIDTH_4 = BitWidth.create(4);
    private static final BitWidth BITWIDTH_5 = BitWidth.create(5);
    private static final BitWidth BITWIDTH_1 = BitWidth.create(1);

}
