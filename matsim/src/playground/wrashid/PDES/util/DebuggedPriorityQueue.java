package playground.wrashid.PDES.util;

import java.util.TreeMap;
/*
 * This class was implemented, because java.util.PriorityQueue class
 * has a remove method.
 * PriorityQueue.remove(Object o) will remove an object with the same priority as o.
 * This causes a problem, because often we really want to remove o and not just any
 * object with priority same as o from the priority queue.
 * Although there is a workaround, it is not very efficient:
 * queue.removeAll(Collections.singletonList(o)).
 * For this reason, this class is implemented based on TreeMaps. If a more efficient
 * data structure is found in future, the TreeMap can replace that data structure.
 */
public class DebuggedPriorityQueue<T> {

	TreeMap<T,Object> tm=new TreeMap<T,Object>();
	
	public T peek(){
		return tm.firstKey();
	}
	
	public T poll(){
		T t=tm.firstKey();
		tm.remove(t);
		return t;
	}
	
	public void remove(T t){
		tm.remove(t);
	}
	
	public boolean isEmpty(){
		return tm.size()==0;
	}
	
	public int size(){
		return tm.size();
	}
	
	public void add(T t){
		tm.put(t, null);
	}
	
	
	
	
}
