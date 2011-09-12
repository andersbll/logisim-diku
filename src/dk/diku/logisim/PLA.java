/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package dk.diku.logisim;


import java.io.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Font;
import java.awt.Window;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.circuit.ExpressionComputer;
import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.proj.Project;

import dk.diku.logisim.pla.*;

 
class PLA extends InstanceFactory {
	private static final int IN_PORT = 0;
	private static final int OUT_PORT = 1;

	private static final Attribute<BitWidth> ATTR_IN_WIDTH
		= Attributes.forBitWidth("in_width", Strings.getter("Bit width in"));
	private static final Attribute<BitWidth> ATTR_OUT_WIDTH
		= Attributes.forBitWidth("out_width", Strings.getter("Bit width out"));
	private static Attribute<TruthTable> ATTR_TRUTH_TABLE = new TruthTableAttribute();

	public static InstanceFactory FACTORY = new PLA();

	private static final Color BACKGROUND_COLOR = new Color(230, 230, 230);
	
	private static final List<Attribute<?>> ATTRIBUTES
		= Arrays.asList(new Attribute<?>[] {
				ATTR_IN_WIDTH, ATTR_OUT_WIDTH, ATTR_TRUTH_TABLE,
				StdAttr.LABEL, StdAttr.LABEL_FONT
		});

	// singleton: the "contents: edit" line in the properties
	private static class TruthTableAttribute extends Attribute<TruthTable> {
		public TruthTableAttribute() { 
			super("truth_table", Strings.getter("Truth Table"));
		}
		
		@Override
		public java.awt.Component getCellEditor(Window source, TruthTable tt) {
			ContentsCell editTruthTableContentCell = new ContentsCell((Frame)source,
			                                                          tt);
			editTruthTableContentCell.mouseClicked(null); // this cannot be called in constructor
			return editTruthTableContentCell;
		}

		@Override
		public String toDisplayString(TruthTable value) { 
			return Strings.get("(click to edit)");
		}
		@Override 
		public String toStandardString(TruthTable tt) {
			String str = "";
			for(int r = 0; r<tt.getRowCount(); r++){
				String andStr = "";
				String orStr = "";
				for(int c=0;c<tt.getInSize();c++) andStr += tt.getInBit(r,c);
				for(int c=0;c<tt.getOutSize();c++) orStr += tt.getOutBit(r,c);
						
				str += andStr + ",";
				str += orStr + "#";
				str += tt.getRow(r).getComment();
				if(r+1 < tt.getRowCount())
					str += "\n";
			}
			return str;
		}
		
		@Override
		public TruthTable parse(String str) {
			// TODO: do this in a better way or make it unnecessary
			if(str.equals("")) return new TruthTable (2,2);
			TruthTable tt = null;
			String[] lines = str.split("\n");
			int r = 0;
			for(String row : lines) {
				// Split up the string
				String andBits, orBits, comment;
				int i = row.indexOf(",");
				int j = row.indexOf("#");
				andBits = row.substring(0, i);
				orBits = row.substring(i+1, j);
				comment = row.substring(j+1, row.length());
				
				if(tt == null) {
					tt = new TruthTable(andBits.length(), orBits.length());
				}
				tt.addNewRow();
				// Parse fields
				for(int c=0;c<andBits.length();c++){
					tt.setInBit(r, c, andBits.charAt(c));
				}
				for(int c=0;c<orBits.length();c++){
					tt.setOutBit(r, c, orBits.charAt(c));
				}
				tt.getRow(r).setComment(comment);
				r++;
			}
			return tt;
		}
	}

	private static class ContentsCell extends JLabel implements MouseListener {
		TruthTable truthTable;
		Frame parent;
		ContentsCell(Frame parent, TruthTable truthTable) {
			super(Strings.get("(click to edit)"));
			this.truthTable = truthTable;
			this.parent = parent;
			addMouseListener(this);
			//mouseClicked(null);
		}

		public void mouseClicked(MouseEvent e) {
			if(truthTable == null) return;
			TruthTableEditorDialog dialog = new TruthTableEditorDialog(this.parent);
			dialog.showAndSet(truthTable, truthTable.tmpIns, truthTable.tmpOuts);
			dialog.toFront();
		}

		public void mousePressed(MouseEvent e) { }
		public void mouseReleased(MouseEvent e) { }
		public void mouseEntered(MouseEvent e) { }
		public void mouseExited(MouseEvent e) { }
	}
	
	private class PLAAttributes extends AbstractAttributeSet {
		private String label = "PLA";
		private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
		private BitWidth widthIn = BitWidth.create(2);
		private BitWidth widthOut = BitWidth.create(2);
		private TruthTable truthTable = new TruthTable(2,2);

		@Override
		protected void copyInto(AbstractAttributeSet destObj) {
			PLAAttributes dest = (PLAAttributes) destObj;
			dest.label = this.label;
			dest.labelFont = this.labelFont;
			dest.widthIn = this.widthIn;
			dest.widthOut = this.widthOut;
			dest.truthTable = this.truthTable;
		}

		@Override
		public List<Attribute<?>> getAttributes() {
			return ATTRIBUTES;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <V> V getValue(Attribute<V> attr) {
			if (attr == ATTR_IN_WIDTH)  return (V) widthIn;
			if (attr == ATTR_OUT_WIDTH) return (V) widthOut;
			if (attr == ATTR_TRUTH_TABLE) return (V) truthTable;
			if (attr == StdAttr.LABEL) return (V) label;
			if (attr == StdAttr.LABEL_FONT) return (V) labelFont;
			return null;
		}

		@Override
		public <V> void setValue(Attribute<V> attr, V value) {
			if (attr == ATTR_IN_WIDTH) {
				widthIn = (BitWidth) value;
				truthTable.tmpIns = widthIn.getWidth();
			} else if (attr == ATTR_OUT_WIDTH) {
				widthOut = (BitWidth) value;
				truthTable.tmpOuts = widthOut.getWidth();
			} else if (attr == ATTR_TRUTH_TABLE) {
				truthTable = (TruthTable) value;
			} else if (attr == StdAttr.LABEL) {
				label = (String) value;
			} else if (attr == StdAttr.LABEL_FONT) {
				labelFont = (Font) value;
			} else {
				throw new IllegalArgumentException("unknown attribute " + attr);
			}
			fireAttributeValueChanged(attr, value);
		}
	}
	
	private static class PLAExpression implements ExpressionComputer {
		private Instance instance;
		
		public PLAExpression(Instance instance) {
			this.instance = instance;
		}
		
		public void computeExpression(Map<Location,Expression> expressionMap) {
			AttributeSet attrs = instance.getAttributeSet();
			int intValue = 5;

			expressionMap.put(instance.getLocation(),
					Expressions.constant(intValue));
		}
	}
	
	public PLA() {
		super("PLA", Strings.getter("PLA"));
		// setKeyConfigurator(JoinedConfigurator.create(
		// 		new ConstantConfigurator(),
		// 		new BitWidthConfigurator(StdAttr.WIDTH)));
	}

	@Override
	public AttributeSet createAttributeSet() {
		return new PLAAttributes();
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		super.configureNewInstance(instance);
		TruthTable tt = instance.getAttributeValue(ATTR_TRUTH_TABLE);
		PLAAttributes attributes = (PLAAttributes)instance.getAttributeSet();
		attributes.truthTable = tt.clone();
		
		instance.addAttributeListener();
		updatePorts(instance);
	}
	
	private void updatePorts(Instance instance) {
		Port[] ps = { new Port(-40, 0, Port.INPUT, ATTR_IN_WIDTH),
		              new Port(30, 0, Port.OUTPUT, ATTR_OUT_WIDTH) };
		instance.setPorts(ps);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == ATTR_OUT_WIDTH) {
			instance.recomputeBounds();
			updatePorts(instance);
		} else if (attr == ATTR_IN_WIDTH) {
			instance.recomputeBounds();
			updatePorts(instance);
		}
	}
	
	@Override
	protected Object getInstanceFeature(Instance instance, Object key) {
		if (key == ExpressionComputer.class) return new PLAExpression(instance);
		return super.getInstanceFeature(instance, key);
	}

	@Override
	public void propagate(InstanceState state) {
		BitWidth outWidth = state.getAttributeValue(ATTR_OUT_WIDTH);
		TruthTable truthTable = state.getAttributeValue(ATTR_TRUTH_TABLE);
		Value input = state.getPort(IN_PORT);
		Value out = computeOutConnectorValue(truthTable, input, outWidth);
		state.setPort(1, out, 1);
	}

	// // TODO: Call this on changes in truth table to update circuit
	// //       state
	// void repropagate() {
	// 	fireComponentInvalidated(new ComponentEvent(this));
	// }

	public Value computeOutConnectorValue(TruthTable tt, Value inValue, BitWidth outWidth) {
		int out = 0;
		int input = inValue.toIntValue();
		for(int r=0; r < tt.getRowCount(); r++){
			//First check if in signal matches the and-plane
			TruthTableRow row = tt.getRow(r);
			//Set outBits accordingly
			if(matchesIn(input, row)) {
				for(int o=0; o < row.getOutSize(); o++) {
					if(row.getOutBit(o).getState() == BitState.State.connected) {
						out = (out << 1) | 1;
					}
					else {
						out = out << 1;
					}
				}
			}
		}
		return Value.createKnown(outWidth, out);
	}

	private boolean matchesIn(int input, TruthTableRow row) {
		for(int i=0; i < row.getInSize(); i++){
			BitState.State rowState = row.getInBit(row.getInSize()-1-i).getState();
			int inputBit = (input >> i) & 1;
			if(rowState == BitState.State.connected &&
			   inputBit != 1) {
				return false;
			}
			if(rowState == BitState.State.unconnected &&
			   inputBit != 0) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		Bounds ret = Bounds.create(-40, -40, 70, 80);
		return ret;
	}

	//
	// painting methods
	//
	@Override
	public void paintIcon(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		g.setFont(g.getFont().deriveFont(9.0f));
		GraphicsUtil.drawCenteredText(g, "PLA", 10, 9);
		//g.fillOval(pinx, piny, 3, 3);
	}

	@Override
	public void paintGhost(InstancePainter painter) {
		Bounds bds = getOffsetBounds(painter.getAttributeSet());
		Location loc = painter.getLocation();
		int x = loc.getX();
		int y = loc.getY();
		String label = painter.getAttributeValue(StdAttr.LABEL);
		Font labelFont = painter.getAttributeValue(StdAttr.LABEL_FONT);

		Graphics g = painter.getGraphics();
		GraphicsUtil.switchToWidth(g, 2);
		g.fillOval(-2, -2, 5, 5);

		g.setColor(Color.BLACK);
		g.drawOval(x + bds.getX(), y + bds.getY(), bds.getWidth(), bds.getHeight());
		g.setFont(labelFont);
		GraphicsUtil.drawCenteredText(g, label,
		                              x + bds.getX() + bds.getWidth() / 2,
		                              y + bds.getY() + bds.getHeight() / 2 - 2);
	}
	
	@Override
	public void paintInstance(InstancePainter painter) {
		Bounds bds = painter.getOffsetBounds();
		BitWidth width = painter.getAttributeValue(ATTR_IN_WIDTH);
		Location loc = painter.getLocation();
		int x = loc.getX();
		int y = loc.getY();
		String label = painter.getAttributeValue(StdAttr.LABEL);
		Font labelFont = painter.getAttributeValue(StdAttr.LABEL_FONT);
		Graphics g = painter.getGraphics();
		if (painter.shouldDrawColor()) {
			g.setColor(BACKGROUND_COLOR);
			g.fillOval(x + bds.getX(), y + bds.getY(), bds.getWidth(), bds.getHeight());
		}

		
		g.setColor(Color.BLACK);
		g.drawOval(x + bds.getX(), y + bds.getY(), bds.getWidth(), bds.getHeight());
		g.setFont(labelFont);
		GraphicsUtil.drawCenteredText(g, label,
		                              x + bds.getX() + bds.getWidth() / 2,
		                              y + bds.getY() + bds.getHeight() / 2 - 2);

		painter.drawPorts();
	}

}
