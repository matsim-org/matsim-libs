package playground.wrashid.PHEV.Utility;

import java.util.LinkedList;

public class TwoColumnTable<C1,C2> {

	private LinkedList<C1> c1List=new LinkedList<C1>();
	private LinkedList<C2> c2List=new LinkedList<C2>();
	
	
	public void add(C1 c1, C2 c2){
		c1List.add(c1);
		c2List.add(c2);
	}
	
	public C1 getC1(int index){
		return c1List.get(index);
	}
	
	public C2 getC2(int index){
		return c2List.get(index);
	}

	public int size(){
		return c1List.size();
	}
	
}
