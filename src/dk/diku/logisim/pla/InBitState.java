package dk.diku.logisim.pla;

public class InBitState extends BitState {
	private static final long serialVersionUID = -6407641046622190973L;


	InBitState(){}
	
	InBitState(char s){
		switch(s){
			case '1':  myState = State.connected;break;
			case '0': myState = State.unconnected;break;
			case 'x': myState = State.dontcare;break;
		}
	}
	
	public void nextState(){
		myState = State.values()[ (myState.ordinal()+1)%3 ];
	}

	
	public String toString(){
		if(myState==State.dontcare) return "x";
		else return ""+myState.ordinal();
	}
}