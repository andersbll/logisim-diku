package dk.diku.logisim;


import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;

/** The library of components that the user can access. */
public class MIPS extends Library {
    /** The list of all tools contained in this library. Technically,
     * libraries contain tools, which is a slightly more general concept
     * than components; practically speaking, though, you'll most often want
     * to create AddTools for new components that can be added into the circuit.
     */
    private List<AddTool> tools;
    
    /** Constructs an instance of this library. This constructor is how
     * Logisim accesses first when it opens the JAR file: It looks for
     * a no-arguments constructor method of the user-designated class.
     */
    public MIPS() {
        tools = Arrays.asList(new AddTool[] {
                new AddTool(RegisterFile32.factory),
                new AddTool(Program32.factory),
                new AddTool(ALU.factory),
                new AddTool(ALU4Bit.factory),
//                new AddTool(Incrementer.factory),
//                new AddTool(Video.factory),
                new AddTool(new Ram()),
                new AddTool(new Adder()),
                new AddTool(new PLA()),
        });
    }
    
    /** Returns the name of the library that the user will see. */ 
    public String getDisplayName() {
        return "MIPS";
    }
    
    /** Returns a list of all the tools available in this library. */
    public List<AddTool> getTools() {
        return tools;
    }
}
