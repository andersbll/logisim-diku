// from Carl Burch's com.cburch.incr.Incrementer 
package dk.diku.logisim;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.*;
import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;

// Referenced classes of package com.cburch.incR:
//            IncrementerFactory

class Incrementer extends ManagedComponent
{
    public static final ComponentFactory factory = new Factory();
    private static final Attribute[] ATTRIBUTES = { StdAttr.WIDTH };
    static final BitWidth WIDTH_DEFAULT = BitWidth.create(8);

    private static class Factory extends AbstractComponentFactory {
        private Factory() { }
        public String getName() { return "Incrementer"; }
        public String getDisplayName() { return "Incrementer"; }
        public AttributeSet createAttributeSet() {
            return AttributeSets.fixedSet(ATTRIBUTES, new Object[] { WIDTH_DEFAULT });
        }
        public Component createComponent(Location loc, AttributeSet attrs) { return new Incrementer(loc, attrs); }
        public Bounds getOffsetBounds(AttributeSet attrs) { return Bounds.create(-30, -15, 30, 30); }
    }

    private class MyListener implements AttributeListener {
        private MyListener() { }
        MyListener(MyListener mylistener) { this(); }
        public void attributeListChanged(AttributeEvent attributeevent) { }
        public void attributeValueChanged(AttributeEvent e) {
            if(e.getAttribute() == StdAttr.WIDTH)
                computeEnds();
        }
    }


    Incrementer(Location loc, AttributeSet attrs)
    {
        super(loc, attrs, 2);
        myListener = new MyListener(null);
        attrs.addAttributeListener(myListener);
        computeEnds();
    }

    private void computeEnds()
    {
        Location loc = getLocation();
        BitWidth width = (BitWidth)getAttributeSet().getValue(StdAttr.WIDTH);
        setEnd(0, loc.translate(-30, 0), width, 1);
        setEnd(1, loc, width, 2);
    }

    public ComponentFactory getFactory() { return factory; }

    public void propagate(CircuitState circuitState)
    {
        Value in = circuitState.getValue(getEndLocation(0));
        Value out;
        if(in.isFullyDefined())
            out = Value.createKnown(in.getBitWidth(), in.toIntValue() + 1);
        else
        if(in.isErrorValue())
            out = Value.createError(in.getBitWidth());
        else
            out = Value.createUnknown(in.getBitWidth());
        circuitState.setValue(getEndLocation(1), out, this, in.getBitWidth().getWidth() + 1);
    }

    public void draw(ComponentDrawContext context)
    {
        context.drawRectangle(this, "+1");
        context.drawPins(this);
    }

    private MyListener myListener;

}
