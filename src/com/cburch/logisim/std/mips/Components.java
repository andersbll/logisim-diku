package com.cburch.logisim.std.mips; // com.cburch.incr;

import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class Components extends Library {
//    private List tools;
	private List<Tool> tools = null;

    public Components() {
        tools = Arrays.asList(new Tool[] {
                new AddTool(RegisterFile32.factory),
                new AddTool(Program32.factory),
                new AddTool(ALU.factory),
//                new AddTool(Incrementer.factory),
//                new AddTool(Video.factory),
                new AddTool(new Ram()),
                new AddTool(new Adder()),
                //new AddTool(LCD.factory),
                //new AddTool(Keyboard.factory),
                //new AddTool(Joystick.factory),
        });
    }
    
//    public String getName() { return Components.class.getName(); }
    public String getDisplayName() { return "MIPS"; }
//    public List getTools() { return tools; }

    public String getName() { return "MIPS"; }

//	public String getDisplayName() { return Strings.get("baseLibrary"); }

	public List<Tool> getTools() {
		return tools;
	}
}
