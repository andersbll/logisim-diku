package dk.diku.logisim.pla;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TruthTableRow implements Serializable{
	private static final long serialVersionUID = 8245571955222077612L;
	private final List<InBitState> inBits;
	private final List<OutBitState> outBits;
	private String comment ="";
	
	public TruthTableRow(int inSize, int outSize){
		inBits = new ArrayList<InBitState>(inSize);
		outBits = new ArrayList<OutBitState>(outSize);
		for(int i=0;i<inSize; i++)	inBits.add(new InBitState());
		for(int o=0;o<outSize;o++)	outBits.add(new OutBitState());
	}
	public int getInSize(){				return inBits.size();	}
	public int getOutSize(){				return outBits.size();	}
	public InBitState getInBit(int c){		return inBits.get(c);	}
	public OutBitState getOutBit(int c){	return outBits.get(c);	}
	public void changeInBit(int c){	inBits.get(c).nextState();	}
	public void changeOutBit(int c){	outBits.get(c).nextState();	}

	void setInBit(int c, InBitState s){
		while(!inBits.get(c).equals(s)) 
			changeInBit(c);
	}
	void setOutBit(int c, OutBitState s){
		while(!outBits.get(c).equals(s)) 
			changeOutBit(c);
	}
	
	void truncate(int newInSize, int newOutSize){
		int inDiff = Math.abs(inBits.size() - newInSize);
		int outDiff = Math.abs(outBits.size() - newOutSize);
		
		//Adjust in-bits
		if(newInSize>inBits.size()){
			for(;inDiff>0;inDiff--)				inBits.add(0, new InBitState());
		}else{
			for(;inDiff>0;inDiff--)				inBits.remove(0);//inBits.remove(newInSize-1);
		}
		
		//Adjust out-bits
		if(newOutSize>outBits.size()){
			for(;outDiff>0;outDiff--)			outBits.add(0,new OutBitState());
		}else{
			for(;outDiff>0;outDiff--)			outBits.remove(0);//outBits.remove(newOutSize-1);
		}
	}
	
	public String toString(){
		String ret = "";
		for(InBitState inBit: inBits){
			ret+=inBit;
		}
		ret+=" ";
		for(OutBitState outBit: outBits){
			ret+=outBit;
		}
		ret += "#" + comment;

		return ret;
	}

	private static final int READ_INS = 0;
	private static final int READ_OUTS = 1;
	private static final int READ_COMMENT = 2;

	// public static TruthTableRow fromString(String str, int inSize, int outSize) {
	// 	TruthTableRow row = new TruthTableRow(inSize, outSize);
	// 	int state = 0;
	// 	int i = inSize;
	// 	for(char c : str.toCharArray()) {
	// 		if(state == READ_INS) {
	// 			if(c == '0') {
	// 				row.inBits[
	// 			}
	// 			else if(c == '1') {
	// 			}
	// 			else if(c == ' '){
	// 				state = READ_OUTS;
	// 			}
	// 		} else if (state == READ_OUTS) {
	// 			if(c == '0') {
	// 			}
	// 			else if(c == '1') {
	// 			}
	// 			else if(c == ' '){
	// 				state = READ_COMMENT;
	// 			}
	// 		} else if (state == READ_COMMENT) {
	// 			row.comment = row.comment + c;
	// 		}		
		   
			
	// 	}
	// }
	
	public void setComment(String c){
		comment = c;
	}
	public String getComment(){
		return comment;
	}
}