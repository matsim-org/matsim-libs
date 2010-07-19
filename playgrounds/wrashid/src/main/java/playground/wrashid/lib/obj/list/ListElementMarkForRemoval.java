package playground.wrashid.lib.obj.list;

import java.util.ArrayList;
import java.util.LinkedList;

public class ListElementMarkForRemoval {

	LinkedList markedForRemoval=new LinkedList();
	
	public void markForRemoval(Object object){
		markedForRemoval.add(object);
	}
	
	public void apply(LinkedList list){
		list.removeAll(markedForRemoval);
	}
	
	public void apply(ArrayList list){
		list.removeAll(markedForRemoval);
	}
	
}
