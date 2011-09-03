package dk.diku.logisim.pla;


public class OutBitState extends BitState{
	private static final long serialVersionUID = -3583073456440033502L;
	OutBitState(){}
	
	OutBitState(char s){
		switch(s){
			case '1':  myState = State.connected;break;
			case '0': myState = State.unconnected;break;
			case '-': myState = State.unconnected;break;
		}
	}
	public void nextState(){
		myState = State.values()[ (myState.ordinal()+1)%2 ];
	}
	public String toString(){
		return myState.ordinal()+"";
	}
}
