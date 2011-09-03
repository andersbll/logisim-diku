package dk.diku.logisim.pla;

import java.io.Serializable;
import java.util.*;

public class TruthTable  implements Serializable{
	private static final long serialVersionUID = -2899092911654195844L;
	private List<TruthTableRow> rows;
	private int inSize;
	private int outSize;	

	public int tmpIns, tmpOuts;
	
	public TruthTable(int inSz, int outSz){
		assert inSz>0;
		assert outSz>0;
		
		rows = new ArrayList<TruthTableRow>();
		inSize = inSz;
		outSize = outSz;
		tmpIns = inSize;
		tmpOuts = outSz;
	}
	
	public TruthTable clone(){
		TruthTable ret = new TruthTable(inSize, outSize);
		ret.copyFrom(this);
		return ret;
	}
	public void copyFrom(TruthTable tt){
		while(!rows.isEmpty()) rows.remove(0);
		inSize = tt.getInSize();
		outSize = tt.getOutSize();
	
		int rows = tt.getRowCount();
		for(int r=0;r<rows;r++){
			addNewRow();
			//Set the inbits
			for(int c=0;c<inSize;c++){
				setInBit(r,c, tt.getInBit(r,c));
			}
			//Set the outbits
			for(int c=0;c<outSize;c++){
				setOutBit(r,c, tt.getOutBit(r,c));
			}
			
			this.rows.get(r).setComment(tt.getRow(r).getComment());
		}	
	}
	
	public void resize(int newInSize, int newOutSize){
		for(TruthTableRow r: rows){
			r.truncate(newInSize, newOutSize);
		}
		inSize = newInSize;
		outSize = newOutSize;
	}
	
	public void addNewRow(){					rows.add(new TruthTableRow(inSize, outSize));	}
	public void deleteRow(TruthTableRow row){	rows.remove(row);								}
	public void deleteRow(int i){				rows.remove(i);									}
	
	
	public TruthTableRow getRow(int r){	return rows.get(r);				}
	public TruthTableRow getLastRow(){		return rows.get(rows.size()-1);	}
	public int getRowCount(){				return rows.size();				}
	public int getInSize(){				return inSize;					}
	public int getOutSize(){				return outSize;					}

	public InBitState getInBit(int r, int c){
		assert c>=0 && c<inSize;
		assert r>=0 && r<getRowCount();
		
		return rows.get(r).getInBit(c);
	}
	public OutBitState getOutBit(int r, int c){
		assert c>=0 && c<outSize;
		assert r>=0 && r<getRowCount();
		
		return rows.get(r).getOutBit(c);
	}
	
	public void changeInBit(int r, int c){
		assert c>=0 && c<inSize;
		assert r>=0 && r<getRowCount();
		
		rows.get(r).changeInBit(c);
	}
	public void changeOutBit(int r, int c){
		assert c>=0 && c<outSize;
		assert r>=0 && r<rows.size();
		
		rows.get(r).changeOutBit(c);
	}
	

	void setInBit(int r, int c, InBitState s){
		rows.get(r).setInBit(c, s);
	}
	void setOutBit(int r, int c, OutBitState s){
		rows.get(r).setOutBit(c, s);
	}

	public void setInBit(int r, int c, char s){
		InBitState bs = new InBitState(s);
		rows.get(r).setInBit(c, bs);
	}
	public void setOutBit(int r, int c, char s){
		OutBitState bs = new OutBitState(s);
		rows.get(r).setOutBit(c, bs);
	}
	
	public String toString(){
		String ret = "";
		for(TruthTableRow r: rows){
			ret+=r+"\n";
		}
		return ret;
	}
	
	
}
