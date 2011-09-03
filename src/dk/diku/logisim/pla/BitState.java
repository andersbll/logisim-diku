package dk.diku.logisim.pla;

import java.io.Serializable;

public abstract class BitState  implements Serializable{
	public enum State{unconnected, connected, dontcare};
	protected State myState = State.unconnected;
	
	public abstract void nextState();
	
	public boolean equals(Object o){
		if(o instanceof BitState){
			return myState==((BitState)o).getState(); 
		}
		return super.equals(o);
	}
	
	public State getState(){
		return myState;
	}
}
