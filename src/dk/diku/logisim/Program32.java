package dk.diku.logisim;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Font;
import java.awt.Window;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.proj.Project;
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
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.tools.AbstractCaret;
import com.cburch.logisim.tools.Caret;
import com.cburch.logisim.tools.Pokable;
import com.cburch.logisim.gui.main.Frame;

/** Represents a program ROM.
 */
class Program32 extends ManagedComponent {

	static final int NO_OP = 0;

	static final int NUM_ROWS = 5; // must be odd

	// size
	static final int CHIP_WIDTH = 240;
	static final int CHIP_DEPTH = 20 * NUM_ROWS + 20;

    public static final ComponentFactory factory =
		new ProgramFactory("MIPSProgramROM", "Instruction memory", CHIP_WIDTH, CHIP_DEPTH);

	static final BitWidth PC_WIDTH = BitWidth.create(32);
	static final BitWidth OP_WIDTH = BitWidth.create(32);

	static final int BOX_WIDTH = 196;
	static final int ACOL_WIDTH = 54;
	static final int ARROW_WIDTH = 20;

	static String toHex(int i, int digits) {
	    if (digits > 8) digits = 8;
	    String s = Long.toHexString(i & 0xffffffffL);
	    if (s.length() >= digits) return "0x"+s;
	    else return "0x00000000".substring(0, 2+digits-s.length()) + s;
	}

	// singleton: factory creates sets of attributes (containing code) , then components out of them
    protected static class ProgramFactory extends AbstractComponentFactory {
		int W, D;
		String N, DN;
        protected ProgramFactory(String n, String dn, int w, int d) { N = n; DN = dn; W = w; D = d; }
        public String getName() { return N; }
        public String getDisplayName() { return DN; }
        public Component createComponent(Location loc, AttributeSet attrs) { return new Program32(loc, attrs); }
        public Bounds getOffsetBounds(AttributeSet arg0) { return Bounds.create(-1*W, -1*D/2, W, D); }
		public AttributeSet createAttributeSet() { return new ProgramAttributes(); }
		public void paintIcon(ComponentDrawContext context, int x, int y,
				AttributeSet attrs) {
			Graphics g = context.getGraphics();
			Font old = g.getFont();
			g.setFont(old.deriveFont(9.0f));
			GraphicsUtil.drawCenteredText(g, "ASM", x + 10, y + 9);
			g.setFont(old);
			g.drawRect(x, y + 4, 19, 12);
			for(int dx = 2; dx < 20; dx += 5) {
				g.drawLine(x + dx, y + 2, x + dx, y + 4);
				g.drawLine(x + dx, y + 16, x + dx, y + 18);
			}
		}
		public static Attribute CODE_ATTR = new ContentsAttribute();

		// singleton: the "contents: edit" line in the properties
		private static class ContentsAttribute extends Attribute {
			public ContentsAttribute() { super("contents", new StringGetter() { public String get() { return "MIPS Program Listing"; } }); }
			public java.awt.Component getCellEditor(Window source, Object value) {
				if(source instanceof Frame) {
					Project proj = ((Frame) source).getProject();
					Listing code = (Listing)value;
					State state = code.getState();
					if (state != null) state.setProject(proj); // will set on Program/AutoProgram
				}
				ContentsCell cell = new ContentsCell((Listing)value);
				cell.mouseClicked(null); // this cannot be called in constructor
				return cell;
			}
			public String toDisplayString(Object value) { return "(click to edit)"; }
			public String toStandardString(Object value) {
				try {
				return ((Listing)value).write();
				} catch(IOException e) {
					// too bad this will be in back of the splash
					JOptionPane.showMessageDialog(null, "The contents of the Program chip could not be written: " +
							e.getMessage(),
							"Error saving MIPS program." ,
							JOptionPane.ERROR_MESSAGE);
					return "";
				}
			}
			public Object parse(String value) {
				try {
					return new Listing(value);
				} catch(IOException e) {
					// too bad this will be in back of the splash
					JOptionPane.showMessageDialog(null, "The contents of the Program chip could not be read: " +
							e.getMessage(),
							"Error loading MIPS program." ,
							JOptionPane.ERROR_MESSAGE);
					return new Listing();
				}
			}
		}

		// transient (many per chip): the "edit" cell in the line
		private static class ContentsCell extends JLabel implements MouseListener {
			Listing code;
			
			ContentsCell(Listing code) {
				super("(click here to edit)");
				this.code = code;
				addMouseListener(this);
				//mouseClicked(null);
			}

			public void mouseClicked(MouseEvent e) {
				if(code == null) return;
				ProgramFrame32 frame = ProgramAttributes.getProgramFrame(code);
				frame.setVisible(true);
				frame.toFront();
			}

			public void mousePressed(MouseEvent e) { }
			public void mouseReleased(MouseEvent e) { }
			public void mouseEntered(MouseEvent e) { }
			public void mouseExited(MouseEvent e) { }
		}
    }

	// one per chip: attributes for the chip (including the listing), launches the editor
	static class ProgramAttributes extends AbstractAttributeSet {
		private static List ATTRIBUTES = Arrays.asList(new Object[]
			{ ProgramFactory.CODE_ATTR });

		private static WeakHashMap windowRegistry = new WeakHashMap(); // Listing -> ProgramFrame

		static ProgramFrame32 getProgramFrame(Listing value) {
			synchronized(windowRegistry) {
				ProgramFrame32 ret = (ProgramFrame32) windowRegistry.get(value);
				if(ret == null) {
					ret = new ProgramFrame32(value);
					ret.setLocationRelativeTo(null);
					ret.setLocation(300, 200);
					windowRegistry.put(value, ret);
				}
				return ret;
			}
		}

		private Listing contents;
		ProgramAttributes() { contents = new Listing(); }
		
		protected void copyInto(AbstractAttributeSet dest) {
			ProgramAttributes d = (ProgramAttributes) dest;
			d.contents = (Listing) contents.clone();
		}
		
		public List getAttributes() { return ATTRIBUTES; }
		
		public Object getValue(Attribute attr) {
			if(attr == ProgramFactory.CODE_ATTR) return contents;
			return null;
		}
		
		public void setValue(Attribute attr, Object value) {
			if(attr == ProgramFactory.CODE_ATTR) {
				contents = (Listing) value;
			}
			fireAttributeValueChanged(attr, value);
		}
	}
    
    protected class MyListener implements Pokable {
        public Caret getPokeCaret(ComponentUserEvent event) {
			Canvas canvas = event.getCanvas();
			if(canvas != null) {
				ProgramAttributes attrs = (ProgramAttributes) getAttributeSet();
				setProject(canvas.getProject());
			}
			return null;
		}
    }
   
    protected MyListener myListener;

	static final int P_PC = 0;
	static final int P_OP = 1;
	static final int NUM_PINS = 2;
    
    protected Program32(Location loc, AttributeSet attrs) {
        super(loc, attrs, NUM_PINS);
		setEnds(loc);
	}

	protected void setEnds(Location loc) {
		int left = -1*CHIP_WIDTH;
		int right = 0;
        setEnd(P_PC,  loc.translate(left+10,  CHIP_DEPTH/2), PC_WIDTH, EndData.INPUT_ONLY);
        setEnd(P_OP,  loc.translate(right, 0), OP_WIDTH, EndData.OUTPUT_ONLY);
		myListener = new MyListener();
	}

    public Object getFeature(Object key) { return (key == Pokable.class) ? myListener : super.getFeature(key); }
    public ComponentFactory getFactory() { return factory; }

    public static void main(String args[]) {
	if (args.length != 1) {
	    System.err.println("usage: Program32 <mips-asm-file>");
	    System.exit(1);
	}
	Listing code = new Listing();
	try {
	    code.load(new File(args[0]));
	    for (int s = 0; s < code.seg.length; s++) {
		for (int i = 0; i < code.seg[s].data.length; i++) {
		    int pc = code.seg[s].start_pc + i;
		    int instr = code.instr(pc);
		    System.out.println(toHex(pc*4, 8)+" : " + toHex(instr, 8) + " : " + disassemble(instr, pc*4));
		}
		System.out.println();
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

	public static class Segment implements Cloneable {
		public int start_pc;
		public int data[];
		public Segment(int pc, int d[]) { start_pc = pc; data = d; }
	}

	static class Listing implements Cloneable {
		public String src;
		public Segment seg[];
		public State state;
		public ArrayList src_lines, addr_map;

		public Listing() { src = ""; seg = new Segment[0]; addr_map = new ArrayList(); src_lines = new ArrayList(); }
		public void setListener(State state) {
			this.state = state;
		}
		public void load(File file) throws IOException {
			String s = readFully(file);
			setSource(s);
		}

		public State getState() { return state; }

		public void setSource(String s) throws IOException {
			ArrayList sl = splitLines(s);
			ArrayList am = new ArrayList();
			seg = assemble(sl, 0, am);
			src = s;
			addr_map = am;
			src_lines = sl;
		}

		public Listing(String value) throws IOException {
		    this();
		    setSource(value);
		}

		public String write() throws IOException {
			return src;
		}

		int instr(int i) {
			Segment s = segmentOf(i);
			if (s != null)
			    return s.data[i - s.start_pc];
			else
			    return NO_OP;
		}

		Segment segmentOf(int i) {
			for (int s = 0; s < seg.length; s++) {
			    if (i >= seg[s].start_pc && i < seg[s].start_pc + seg[s].data.length) {
				//System.out.println("segment of "+i+" is "+seg[s].start_pc);
				return seg[s];
			    }
			}
			//System.out.println("segment of "+i+" is null");
			return null;
		}


		public Object clone() { try { return super.clone(); } catch(CloneNotSupportedException e) { return null; } }
	}

    Listing getCode() {
        return (Listing) getAttributeSet().getValue(ProgramFactory.CODE_ATTR);
    }


	// MIPS subset assembler and disassembler

	static String readFully(File file) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line;
		StringBuffer buf = new StringBuffer();
		while ((line = in.readLine()) != null)
			buf.append(line+"\n");
		return buf.toString();
	}

	static ArrayList splitLines(String src) throws IOException {
		BufferedReader in = new BufferedReader(new StringReader(src));
		String line;
		ArrayList buf = new ArrayList();
		while ((line = in.readLine()) != null)
			buf.add(line);
		return buf;
	}

	static Pattern pat0 = Pattern.compile("\\s+");
	static Pattern pat1 = Pattern.compile("\\s*,\\s*");

	static ArrayList normalize(ArrayList lines) {
		ArrayList res = new ArrayList();
		for (int lineno = 0; lineno < lines.size(); lineno++) {
			String line = (String)lines.get(lineno);
			int i = line.indexOf('#');
			if (i == 0) line = "";
			else if (i > 0) line = line.substring(0, i);
			line = line.trim();
			line = pat0.matcher(line).replaceAll(" ");
			line = pat1.matcher(line).replaceAll(",");
			if (line.length() == 0) res.add(null);
			else res.add(line);
		}
		return res;
	}

	// some patterns
	static String _reg = "\\$(\\d+|zero|at|v[01]|a[0-3]|t[0-9]|s[0-7]|k[01]|gp|sp|fp|ra)";
	static String __hex = "0x[a-fA-F0-9]+";
	static String __decimal = "-?\\d+";
	static String __label = "[a-zA-Z]\\w*";
	static String _imm = "("+__hex+"|"+__decimal+"|"+__label+")";

	static int parseSegmentAddress(int lineno, String addr) throws IOException {
		if (addr.toLowerCase().startsWith("0x"))
			return Integer.parseInt(addr.substring(2), 16);
		char c = addr.charAt(0);
		if ((c >= '0' && c <= '9'))
			return Integer.parseInt(addr);
		throw new ParseException("Line " + (lineno+1) + ": illegal address '"+addr+"' in assembly directive");
	}

	static class ParseException extends IOException {
	    StringBuffer msg;
	    int count = 0;
	    public ParseException() { msg = new StringBuffer(); }
	    public ParseException(String m) { this(); add(m); }
	    public void add(String m) { msg.append("\n"); msg.append(m); count++; }
	    public void add(ParseException e) { msg.append(e.msg.toString()); count += e.getCount(); }
	    public String getMessage() { return "Assembling MIPS instructions: " +count+(count == 1 ? " error:":" errors:") + msg.toString(); }
	    public int getCount() { return count; }
	}
	

	static Pattern pat_label = Pattern.compile("("+__label+")");
	static HashMap pass1(ArrayList lines, int start_address, ArrayList addr_map) throws IOException {
		HashMap map = new HashMap();
		int addr = start_address;
		addr_map.clear();
		ParseException err = new ParseException();
		for (int lineno = 0; lineno < lines.size(); lineno++) {
			String line = (String)lines.get(lineno);
			if (line == null) {
				addr_map.add(null);
				continue;
			}
			int i;
			if (line.toLowerCase().startsWith(".text")) {
			    i = line.indexOf(' ');
			    if (i > 0) {
				try {
				    int a = parseSegmentAddress(lineno, line.substring(i+1));
				    if ((a & 3) != 0)
					err.add("Line " + (lineno+1) + ": mis-aligned address '"+line.substring(i+1)+"' in .text assembly directive");
				    addr = a & ~3;
				} catch (ParseException e){
				    err.add(e);
				}
			    }
			    addr_map.add(null);
			    continue;
			}
			if (line.toLowerCase().startsWith(".word")) {
			    addr_map.add(new Integer(addr));
			    addr += 4;
			    continue;
			}
			if (line.startsWith(".")) {
			    err.add("Line " + (lineno+1) + ": unrecognized assembly directive '"+line+"'");
			    continue;
			}

			i = line.indexOf(':');
			if (i >= 0) {
				String name = line.substring(0, i).trim();
				if (name.length() == 0) {
					err.add("Line " + (lineno+1) + ": expected label name before ':'");
					continue;
				}
				Matcher m = pat_label.matcher(name);
				if (name.equalsIgnoreCase("pc") || !m.matches()) {
					err.add("Line " + (lineno+1) + ": illegal label name '"+name+"' before ':'");
					continue;
				}
				map.put(name, new Integer(addr));
				if (i < line.length()-1) {
					// label: instruction
					line = line.substring(i+1).trim();
					lines.set(lineno, line);
					addr_map.add(new Integer(addr));
					addr += 4;
				} else {
					// label:
					addr_map.add(null);
					lines.set(lineno, null);
				}
			} else {
				addr_map.add(new Integer(addr));
				addr += 4;
			}
		}
		if (err.getCount() > 0)
		    throw err;
		return map;
	}

	static HashMap cmds = new HashMap();
	static HashMap opcodes = new HashMap();
	static HashMap fcodes = new HashMap();

	// returns an n-bit number (with leading zeros for n<32).
	// if SIGNED_ABSOLUTE, the accepted inputs are:
	//   - hex (with no more than n bits)
	//   - decimal (positive or negative values in the range -2^(n-1)..2^(n-1)-1)
	//   - "pc" (as long as it is in the range)
	//   - label (as long as it is in the range)
	// if UNSIGNED_ABSOLUTE, the accepted inputs are:
	//   - as above, but no negative decimals, and with a range check of 0..2^n-1 instead
	// if SIGNED_RELATIVE, the accepted inputs are:
	//   - hex or decimal, as above (no relative offsetting)
	//   - "pc" or label, minus (addr+4) (as long as this result is in the range)
	// if ANY_ABSOLUTE, the accepted inputs are:
	//   - anything that fits in n bits)
	static String SIGNED_RELATIVE = "signed pc-relative offset";
	static String SIGNED_ABSOLUTE = "signed immediate";
	static String UNSIGNED_ABSOLUTE = "unsigned immediate";
	static String ANY_ABSOLUTE = "hex value";
	static int resolve(int lineno, String imm, int addr, HashMap sym, String type, int nbits) throws IOException {
		int offset = (type == SIGNED_RELATIVE ? addr+4 : 0);
		long min = (type == UNSIGNED_ABSOLUTE ? 0 : (-1L << (nbits-1)));
		long max = (type == UNSIGNED_ABSOLUTE ? ((1L << nbits)-1) : ((1L << (nbits-1)) - 1));
		int mask = (int)(1L << nbits) - 1;
		long val;
		try {
			if (imm.length() == 0)
				throw new NumberFormatException();
			char c = imm.charAt(0);
			if (imm.equalsIgnoreCase("pc")) {
			    val = ((long)addr & 0xffffffffL) - offset;
			} else if (imm.toLowerCase().startsWith("0x")) {
			    val = Long.parseLong(imm.substring(2), 16);
			    if ((val & mask) != val) // nb: this check is different, to allow 0xffff to mean "-1" for signed abs/relative, but 65535 for unsigned absolute
				throw new ParseException("Line "+(lineno+1)+": overflow in "+type+" '"+imm+"' ("+nbits+" bits maximum)");
			    return (int)(val & mask);
			} else if ((c == '-') || (c >= '0' && c <= '9')) {
			    val = Long.parseLong(imm);
			} else {
			    Integer a = (Integer)sym.get(imm);
			    if (a == null)
				throw new ParseException("Line "+(lineno+1)+": expecting "+type+", but no such label or number '"+imm+"'");
			    val = ((long)a.intValue() & 0xffffffffL) - offset;
			    imm = imm + " ("+val+")";
			}
			if (type == ANY_ABSOLUTE) {
			    if ((val & mask) != val) // nb: this check is different, to allow 0xffff to mean "-1" for signed abs/relative, but 65535 for unsigned absolute
				throw new ParseException("Line "+(lineno+1)+": overflow in "+type+" '"+imm+"' ("+nbits+" bits maximum)");
			} else {
			    if (val < min || val > max)
				throw new ParseException("Line "+(lineno+1)+": overflow in "+type+" '"+imm+"' : allowed range is "+min+" ("+toHex((int)min & mask, 1)+") to "+max+" ("+toHex((int)max & mask, 1)+")");
			}
			return (int)(val & mask);
		} catch (NumberFormatException e) {
			throw new ParseException("Line "+(lineno+1)+": invalid "+type+" '"+imm+"'");
		}
	}

	private static abstract class Command {
		String name;
		int opcode;
		Command(String name, int op) {
			this.name = name;
			opcode = op;
			cmds.put(name, this);
		}
		abstract String decode(int addr, int instr) throws IOException;
		abstract int encode(int lineno, int addr, String args, HashMap sym) throws IOException;
	}

	private static class Nop extends Command {
	    Nop(String name, int op) { super(name, op); }
	    String decode(int addr, int instr) throws IOException {
		return name;
	    }
	    int encode(int lineno, int addr, String args, HashMap hashmap) throws IOException {
		return 0;
	    }
	}

	static Pattern pat_word = Pattern.compile(_imm);
	private static class Word extends Command {
		Word(String name, int op) {
			super(name, op);
			opcodes.put(new Integer(op), this);
		}
		int encode(int lineno, int addr, String args, HashMap sym) throws IOException {
			Matcher m = pat_word.matcher(args);
			if (!m.matches())
				throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects integer argument");
			int word = resolve(lineno, m.group(1), addr, sym, ANY_ABSOLUTE, 32);
			return word;
		}
		String decode(int addr, int instr) throws IOException {
			return name+" "+toHex(instr, 8);
		}
	}

	private static int reg(String r) throws NumberFormatException {
	    switch (r.charAt(0)) {
		case 'z': // zero
		    return 0;
		case 'a':
		    if (r.equals("at")) return 1;
		    else return 4 + Integer.parseInt(r.substring(1));
		case 'v':
		    return 2 + Integer.parseInt(r.substring(1));
		case 't':
		    int i = Integer.parseInt(r.substring(1));
		    if (i <= 7) return 8 + i;
		    else return 24 + (i-8);
		case 's':
		    if (r.equals("sp")) return 29;
		    else return 16 + Integer.parseInt(r.substring(1));
		case 'k':
		    return 26 + Integer.parseInt(r.substring(1));
		case 'g': // gp
		    return 28;
		case 'f': // fp
		    return 30;
		case 'r': // ra
		    return 31;
		default:
		    return Integer.parseInt(r);
	    }
	}

	private static abstract class IType extends Command {
		IType(String name, int op) {
			super(name, op);
			opcodes.put(new Integer(op), this);
		}
		int encode(String rd, String rs, int imm, int lineno) throws IOException {
			try {
				int dest = reg(rd);
				int src = reg(rs);
				imm = imm & 0x0000ffff;
				if ((dest & 0x1f) != dest)
					throw new ParseException("Line "+(lineno+1)+": invalid destination register: $"+dest);
				if ((src & 0x1f) != src)
					throw new ParseException("Line "+(lineno+1)+": invalid source register: $"+src);
				return (opcode << 26) | (src << 21) | (dest << 16) | imm;
			} catch (NumberFormatException e) {
				throw new ParseException("Line "+(lineno+1)+": invalid arguments to '"+name+"': "+e.getMessage());
			}
		}
		String rD(int instr) { return "$"+((instr >> 16)&0x1f); }
		String rS(int instr) { return "$"+((instr >> 21)&0x1f); }
		String sImm(int instr) { return toHex(instr & 0x0000ffff, 4); }
	}

	static Pattern pat_arith_imm = Pattern.compile(_reg+","+_reg+","+_imm);
	private static class ArithImm extends IType {
		String itype;
		ArithImm(String name, int op, boolean signed) { super(name, op); this.itype = (signed ? SIGNED_ABSOLUTE : UNSIGNED_ABSOLUTE); }
		int encode(int lineno, int addr, String args, HashMap sym) throws IOException {
			Matcher m = pat_arith_imm.matcher(args);
			if (!m.matches())
				throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects $D, $S, "+itype);
			int imm = resolve(lineno, m.group(3), addr, sym, itype, 16);
			return encode(m.group(1), m.group(2), imm, lineno);
		}
		String decode(int addr, int instr) throws IOException {
			return name+" "+rD(instr)+", "+rS(instr)+", "+sImm(instr);
		}
	}

	static Pattern pat_lui = Pattern.compile(_reg+","+_imm);
	private static class Lui extends IType {
		Lui(String name, int op) { super(name, op); }
		int encode(int lineno, int addr, String args, HashMap sym) throws IOException {
			Matcher m = pat_lui.matcher(args);
			if (!m.matches())
				throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects $D, imm");
			int imm = resolve(lineno, m.group(2), addr, sym, ANY_ABSOLUTE, 16);
			return encode(m.group(1), "0", imm, lineno);
		}
		String decode(int addr, int instr) throws IOException {
			return name+" "+rD(instr)+", "+sImm(instr);
		}
	}

	static Pattern pat_mem = Pattern.compile(_reg+","+_imm+"\\s*\\("+_reg+"\\)");
	private static class Mem extends IType {
		Mem(String name, int op) { super(name, op); }
		int encode(int lineno, int addr, String args, HashMap sym) throws IOException {
			Matcher m = pat_mem.matcher(args);
			if (!m.matches())
				throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects $D, signed_imm($S)");
			int imm = resolve(lineno, m.group(2), addr, sym, SIGNED_ABSOLUTE, 16);
			return encode(m.group(1), m.group(3), imm, lineno);
		}
		String decode(int addr, int instr) throws IOException {
			return name+" "+rD(instr)+", "+sImm(instr)+"("+rS(instr)+")";
		}
	}

	static Pattern pat_br = Pattern.compile(_reg+","+_reg+","+_imm);
	private static class Br extends IType {
		Br(String name, int op) { super(name, op); }
		int encode(int lineno, int addr, String args, HashMap sym) throws IOException {
			Matcher m = pat_br.matcher(args);
			if (!m.matches())
				throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects offset or label");
			int offset = resolve(lineno, m.group(3), addr, sym, SIGNED_RELATIVE, 18);
			if ((offset & 0x3) != 0)
				throw new ParseException("Line "+(lineno+1)+": mis-aligned offset in '"+name+"'");
			return encode(m.group(2), m.group(1), offset >> 2, lineno);
		}
		String decode(int addr, int instr) throws IOException {
			return name+" "+rS(instr)+", "+rD(instr)+", "+sImm(instr<<2);
		}
	}

	static Pattern pat_bz = Pattern.compile(_reg+","+_imm);
	private static class Bz extends IType {
		Bz(String name, int op) { super(name, op); }
		int encode(int lineno, int addr, String args, HashMap sym) throws IOException {
			Matcher m = pat_bz.matcher(args);
			if (!m.matches())
				throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects offset or label");
			int offset = resolve(lineno, m.group(2), addr, sym, SIGNED_RELATIVE, 18);
			if ((offset & 0x3) != 0)
				throw new ParseException("Line "+(lineno+1)+": mis-aligned offset in '"+name+"'");
			return encode("0", m.group(1), offset >> 2, lineno);
		}
		String decode(int addr, int instr) throws IOException {
			return name+" "+rS(instr)+", "+sImm(instr<<2);
		}
	}

	static Pattern pat_j0 = Pattern.compile(_imm);
	private static class J extends Command {
		J(String name, int op) {
			super(name, op);
			opcodes.put(new Integer(op), this);
		}
		int encode(int lineno, int addr, String args, HashMap sym) throws IOException {
			Matcher m = pat_j0.matcher(args);
			if (!m.matches())
				throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects address or label");
			int absaddr = resolve(lineno, m.group(1), addr, sym, UNSIGNED_ABSOLUTE, 32);
			if ((absaddr & 0x3) != 0)
				throw new ParseException("Line "+(lineno+1)+": mis-aligned address in '"+name+"'");
			if ((absaddr & 0xf0000000) != ((addr+4) & 0xf0000000))
				throw new ParseException("Line "+(lineno+1)+": overflow in address in '"+name+"': can't jump from "+toHex(addr, 8)+" to " + toHex(absaddr, 8));
			return (opcode << 26) | ((absaddr>>2) & 0x03ffffff);
		}
		String decode(int addr, int instr) throws IOException {
			return name+" "+toHex(((addr+4)&0xf0000000)|((instr & 0x03ffffff)<<2), 8);
		}
	}

	private static abstract class RType extends Command {
		int f;
		RType(String name, int zero, int f) {
			super(name, 0);
			this.f = f;
			fcodes.put(new Integer(f), this);
		}
		int encode(String rd, String rs, String rt, int sa, int lineno) throws IOException {
			try {
				int dest = reg(rd);
				int src = reg(rs);
				int trg = reg(rt);
				if ((dest & 0x1f) != dest)
					throw new ParseException("Line "+(lineno+1)+": invalid destination register: $"+dest);
				if ((src & 0x1f) != src)
					throw new ParseException("Line "+(lineno+1)+": invalid source1 register: $"+src);
				if ((trg & 0x1f) != trg)
					throw new ParseException("Line "+(lineno+1)+": invalid source2 register: $"+trg);
				if ((sa & 0x1f) != sa)
					throw new ParseException("Line "+(lineno+1)+": invalid shift amount: "+sa);
				return (opcode << 26) | (src << 21) | (trg << 16) | (dest << 11) | (sa << 6) | f;
			} catch (NumberFormatException e) {
				throw new ParseException("Line "+(lineno+1)+": invalid arguments to '"+name+"': "+e.getMessage());
			}
		}
		String rD(int instr) { return "$"+((instr >> 11)&0x1f); }
		String rS(int instr) { return "$"+((instr >> 21)&0x1f); }
		String rT(int instr) { return "$"+((instr >> 16)&0x1f); }
		String sSa(int instr) { return ""+((instr >> 6)&0x1f); }
	}

	static Pattern pat_arith_reg = Pattern.compile(_reg+","+_reg+","+_reg);
	private static class ArithReg extends RType {
		ArithReg(String name, int zero, int f) { super(name, zero, f); }
		int encode(int lineno, int addr, String args, HashMap sym) throws IOException {
			Matcher m = pat_arith_reg.matcher(args);
			if (!m.matches())
				throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects $D, $S, $T");
			return encode(m.group(1), m.group(2), m.group(3), 0, lineno);
		}
		String decode(int addr, int instr) throws IOException {
			return name+" "+rD(instr)+", "+rS(instr)+", "+rT(instr);
		}
	}

	static Pattern pat_shift_c = Pattern.compile(_reg+","+_reg+","+_imm);
	private static class ShiftConstant extends RType {
		ShiftConstant(String name, int zero, int f) { super(name, zero, f); }
		int encode(int lineno, int addr, String args, HashMap sym) throws IOException {
			Matcher m = pat_shift_c.matcher(args);
			if (!m.matches())
				throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects $D, $T, sa");
			int sa = resolve(lineno, m.group(3), addr, sym, UNSIGNED_ABSOLUTE, 5);
			return encode(m.group(1), "0", m.group(2), sa, lineno);
		}
		String decode(int addr, int instr) throws IOException {
			return name+" "+rD(instr)+", "+rT(instr)+", "+sSa(instr);
		}
	}

	static Pattern pat_shift_v = Pattern.compile(_reg+","+_reg+","+_reg);
	private static class ShiftVariable extends RType {
		ShiftVariable(String name, int zero, int f) { super(name, zero, f); }
		int encode(int lineno, int addr, String args, HashMap sym) throws IOException {
			Matcher m = pat_shift_v.matcher(args);
			if (!m.matches())
				throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects $D, $T, $S");
			return encode(m.group(1), m.group(3), m.group(2), 0, lineno);
		}
		String decode(int addr, int instr) throws IOException {
			return name+" "+rD(instr)+", "+rT(instr)+", "+rS(instr);
		}
	}

	static Pattern pat_jr = Pattern.compile(_reg);
	private static class Jr extends RType {
		Jr(String name, int zero, int f) { super(name, zero, f); }
		int encode(int lineno, int addr, String args, HashMap sym) throws IOException {
			Matcher m = pat_jr.matcher(args);
			if (!m.matches())
				throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects $S");
			return encode("0", m.group(1), "0", 0, lineno);
		}
		String decode(int addr, int instr) throws IOException {
			return name+" "+rS(instr);
		}
	}

	static Pattern pat_jalr = Pattern.compile(_reg + "(?:," + _reg + ")?");
	private static class Jalr extends RType {
	    Jalr(String name, int zero, int f) { super(name, zero, f); }
	    int encode(int lineno, int addr, String args, HashMap sym) throws IOException
	    {
		Matcher m = Program32.pat_jalr.matcher(args);
		if(!m.matches()) {
		    throw new ParseException("Line " + (lineno + 1) + ": '" + name + "' expects $S");
		} else {
		    boolean haveOpt = m.group(2) != null;
		    String rd = haveOpt ? m.group(1) : "31";
		    String rs = m.group(haveOpt ? 2 : 1);
		    return encode(rd, rs, "0", 0, lineno);
		}
	    }

	    String decode(int addr, int instr) throws IOException
	    {
		if("31".equals(rD(instr)))
		    return name + " " + rS(instr);
		else
		    return name + " " + rD(instr) + ", " + rS(instr);
	    }
	}

	static {
		new Word(".word", -1);
		new Nop("nop", 0x0);
		new J("j", 0x2);
		new J("jal", 0x3);
		new Br("beq", 0x4);
		new Br("bne", 0x5);
		new Bz("blez", 0x6);
		new Bz("bgtz", 0x7);
		new ArithImm("addi", 0x8, true);
		new ArithImm("addiu", 0x9, true);
		new ArithImm("andi", 0xc, false);
		new ArithImm("ori", 0xd, false);
		new ArithImm("xori", 0xe, false);
		new ArithImm("slti", 0xa, false);
		new Lui("lui", 0xf);
		new Mem("lw", 0x23);
		new Mem("lb", 0x20);
		new Mem("lbu", 0x24);
		new Mem("sw", 0x2b);
		new Mem("sb", 0x28);
		new ArithReg("add", 0, 0x20);
		new ArithReg("addu", 0, 0x21);
		new ArithReg("sub", 0, 0x22);
		new ArithReg("subu", 0, 0x23);
		new ArithReg("and", 0, 0x24);
		new ArithReg("or", 0, 0x25);
		new ArithReg("xor", 0, 0x26);
		new ArithReg("nor", 0, 0x27);
		new ArithReg("slt", 0, 0x2a);
		new ArithReg("sltu", 0, 0x2b);
		new ShiftConstant("sll", 0, 0x00);
		new ShiftConstant("srl", 0, 0x02);
		new ShiftConstant("sra", 0, 0x03);
		new ShiftVariable("sllv", 0, 0x04);
		new ShiftVariable("srlv", 0, 0x06);
		new ShiftVariable("srav", 0, 0x07);
		new Jr("jr", 0, 0x08);
		new Jalr("jalr", 0, 0x09);
	}

	static Segment[] pass2(ArrayList lines, int start_address, HashMap sym) throws IOException {
		ParseException err = new ParseException();
		int addr = start_address;
		int cnt = 0;
		ArrayList seglist = new ArrayList();
		int pc = start_address >>> 2;
		for (int lineno = 0; lineno < lines.size(); lineno++) {
			String line = (String)lines.get(lineno);
			if (line == null) continue;
			if (line.toLowerCase().startsWith(".text ")) {
			    if (cnt > 0)
				seglist.add(new Segment(pc, new int[cnt]));
			    cnt = 0;
			    pc = parseSegmentAddress(lineno, line.substring(line.indexOf(' ')+1)) >>> 2;
			} else {
			    cnt++;
			}
		}
		if (cnt > 0)
		    seglist.add(new Segment(pc, new int[cnt]));
		Segment[] seg = new Segment[seglist.size()];
		if (seg.length == 0)
		    return seg;
		for (int s = 0; s < seg.length; s++) {
		    seg[s] = (Segment)seglist.get(s);
		    for (int s2 = 0; s2 < s; s2++) {
			if (seg[s].start_pc < seg[s2].start_pc + seg[s2].data.length &&
				seg[s2].start_pc < seg[s].start_pc + seg[s].data.length)
			    err.add("Assembly segment at "+toHex(seg[s].start_pc*4, 8)+".."+toHex((seg[s].start_pc+seg[s].data.length)*4, 8)+" overlaps with segment at "+
						toHex(seg[s2].start_pc*4, 8)+".."+toHex((seg[s2].start_pc+seg[s2].data.length)*4, 8));
		    }
		}

		int cs = 0;
		cnt = 0;
		for (int lineno = 0; lineno < lines.size(); lineno++) {
			String line = (String)lines.get(lineno);
			if (line == null) continue;
			int i = line.indexOf(' ');
			String instr = i >= 0 ? line.substring(0, i) : line;
			String args = i >= 0 ? line.substring(i+1) : "";
			if (instr.equalsIgnoreCase(".text")) {
			    cs = -1;
			    pc = parseSegmentAddress(lineno, line.substring(line.indexOf(' ')+1)) >>> 2;
			    addr = pc << 2;
			    cnt = 0;
			    for (int s = 0; s < seg.length; s++) {
				if (seg[s].start_pc == pc) {
				    cs = s;
				    break;
				}
			    }
			    if (cs < 0)
				    err.add("Line " + (lineno+1)+": internal error: bad segment");
			} else {
			    Command cmd = (Command)cmds.get(instr.toLowerCase());
			    if (cmd == null) {
				    err.add("Line " + (lineno+1)+": unrecognized instruction: '"+instr+"'");
			    } else if (cs >= 0) {
				try {
				    seg[cs].data[cnt++] = cmd.encode(lineno, addr, args, sym);
				} catch (ParseException e) {
				    err.add(e);
				}
				addr += 4;
			    }
			}
		}
		if (err.getCount() > 0)
		    throw err;
		return seg;
	}

	static Segment[] assemble(ArrayList src_lines, int start_address, ArrayList addr_map) throws IOException {
		ArrayList lines = normalize(src_lines);
		HashMap sym = pass1(lines, start_address, addr_map);
		return pass2(lines, start_address, sym);
	}

	static String disassemble(int code[], int start_addr) throws IOException {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < code.length; i++) {
			int instr = code[i];
			int op = (instr >> 26) & 0x3f;
			Command cmd;
			if (op == 0) {
				int f = (instr & 0x3f);
				cmd = (Command)fcodes.get(new Integer(f));
			} else {
				cmd = (Command)opcodes.get(new Integer(op));
			}
			if (cmd == null)
				throw new ParseException("Instruction " + (i+1)+" unrecognized: "+toHex(instr, 8));
			buf.append(cmd.decode(start_addr+4*i, instr)+"\n");
		}
		return buf.toString();
	}

	static String disassemble(int instr, int addr) {
		int op = (instr >> 26) & 0x3f;
		Command cmd;
		if (op == 0) {
			int f = (instr & 0x3f);
			cmd = (Command)fcodes.get(new Integer(f));
		} else {
			cmd = (Command)opcodes.get(new Integer(op));
		}
		if (cmd == null)
		    cmd = (Command)opcodes.get(new Integer(-1));
		try {
			return cmd.decode(addr, instr);
		} catch (IOException e) {
			return "??? <"+toHex(instr, 8)+">";
		}
	}

	Project proj;
	void setProject(Project p) { proj = p; }

	class State implements ComponentState, Cloneable {
		Listing code;
		public int pc;
		public static final int PC_UNDEFINED = -1;
		public static final int PC_ERROR = -2;

		public State(Listing code) { this.code = code; code.setListener(this); pc = PC_UNDEFINED; }

		public Project getProject() { return proj; }
		public void setProject(Project p) { proj = p; }

		public void codeChanged() {
			if (proj != null) propagate(proj.getCircuitState());
		}

		String decode(int i) { return disassemble(code.instr(i), 4*i); }

		Value instr() {
		    if (isValidPC())
			return Value.createKnown(OP_WIDTH, code.instr(pc));
		    else
			return Value.createError(OP_WIDTH);
		}

		boolean haveCodeFor(int i) { return code.segmentOf(i) != null; }
		boolean isValidPC() { return pc >= 0; }
		boolean isUndefinedPC() { return pc == PC_UNDEFINED; }
		boolean isErrorPC() { return pc == PC_ERROR; }

		public Object clone() { try { return super.clone(); } catch(CloneNotSupportedException e) { return null; } }

		public void update(Value pc_in) {
		    if (pc_in.isErrorValue())
			pc = PC_ERROR;
		    else if (!pc_in.isFullyDefined())
			pc = PC_UNDEFINED;
		    else if ((pc_in.toIntValue() & 3) != 0)
			pc = PC_ERROR;
		    else
			pc = pc_in.toIntValue() >>> 2;
		}
	}

	Location loc(int pin) { return getEndLocation(pin); }
	Value val(CircuitState s, int pin) { return s.getValue(loc(pin)); }
	int addr(CircuitState s, int pin) { return val(s, pin).toIntValue(); }

    public void propagate(CircuitState circuitState) {
	State state = getState(circuitState);
	state.update(val(circuitState, P_PC));
	circuitState.setValue(loc(P_OP), state.instr(), this, 9);
    }

    protected State getState(CircuitState circuitState) {
        State state = (State) circuitState.getData(this);
        if (state == null) {
            state = new State(getCode());
            circuitState.setData(this, state);
        }
        return state;
    }

	public void drawBox(Graphics g, Bounds bds, Color color) {
		g.setColor(Color.WHITE);
		g.fillRect(bds.getX() + ARROW_WIDTH, bds.getY() + 5,
				ACOL_WIDTH, 20*NUM_ROWS+10);
		g.fillRect(bds.getX() + ARROW_WIDTH, bds.getY() + 5,
				BOX_WIDTH, 20*NUM_ROWS+10);
		g.setColor(color);
		g.drawRect(bds.getX() + ARROW_WIDTH, bds.getY() + 5,
				ACOL_WIDTH, 20*NUM_ROWS+10);
		g.drawRect(bds.getX() + ARROW_WIDTH, bds.getY() + 5,
				BOX_WIDTH, 20*NUM_ROWS+10);
		g.setColor(Color.BLACK);
	}
	public void drawArrow(Graphics g, Bounds bds, Color color) {
		int left = bds.getX()+ARROW_WIDTH-13;
		int c = bds.getY() + 20*NUM_ROWS/2 + 10;
		int[] xs = { left, left+8, left, left };
		int[] ys = { c-5, c, c+5, c-5 };
		g.setColor(color);
		g.fillPolygon(xs, ys, 4);
		g.setColor(Color.BLACK);
		g.drawPolyline(xs, ys, 4);
	}

    public void draw(ComponentDrawContext context) {
        context.drawRectangle(this);
		Graphics g = context.getGraphics();
		Bounds bds = getBounds();

		GraphicsUtil.drawText(g, "PC",
				bds.getX() + 2, bds.getY() + CHIP_DEPTH - 12,
				GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		GraphicsUtil.drawText(g, "Op",
				bds.getX() + CHIP_WIDTH - 2, bds.getY() + CHIP_DEPTH/2,
				GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);
		context.drawPin(this, P_PC /*, "PC", Direction.EAST */);
		context.drawPin(this, P_OP /*, "Op", Direction.WEST */);

		// draw some rectangles 
		drawBox(g, bds, Color.GRAY);
        
		if (context.getShowState())
			drawState(context);
	}

	static final Font font = new Font("Monospaced", Font.PLAIN, 10);

	public void drawState(ComponentDrawContext context) {
		State state = getState(context.getCircuitState());
		if (state.code.seg.length == 0) return;

		Graphics g = context.getGraphics();
		Bounds bds = getBounds();

		Color arrowcolor;
		if (state.isErrorPC()) arrowcolor = Color.RED;
		else if (state.isUndefinedPC()) arrowcolor = Color.GRAY;
		else if (state.haveCodeFor(state.pc)) arrowcolor = Color.BLUE;
		else arrowcolor = Color.BLUE; // Color.YELLOW;
		drawArrow(g, bds, arrowcolor);

		int j = -1;
		int pc = (state.isValidPC() ? state.pc : -1);
		for (int i = pc - (NUM_ROWS-1)/2; i <= pc + (NUM_ROWS-1)/2; i++) {
			j++;
			if (i < 0 || i > 0x3fffffff) continue;
			if (i == state.pc) g.setColor(Color.BLUE);
			else if (!state.haveCodeFor(i)) g.setColor(Color.GRAY);
			GraphicsUtil.drawText(g, font, StringUtil.toHexString(32, i*4),
					bds.getX() + ARROW_WIDTH + 2,
					bds.getY() + 20*j + 20/2 + 10,
					GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
			GraphicsUtil.drawText(g, font, state.decode(i), 
					bds.getX() + ARROW_WIDTH + ACOL_WIDTH + 1,
					bds.getY() + 20*j + 20/2 + 10,
					GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
			g.setColor(Color.BLACK);
		}
    }

}
