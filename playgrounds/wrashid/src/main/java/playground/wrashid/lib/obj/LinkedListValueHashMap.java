package playground.wrashid.lib.obj;

import java.util.HashMap;
import java.util.LinkedList;

public class LinkedListValueHashMap<KeyClass,ValueClass> {

	HashMap<KeyClass, LinkedList<ValueClass>> hashMap=new HashMap<KeyClass, LinkedList<ValueClass>>();
	
	public void put(KeyClass key,ValueClass value){
		initKey(key);
		
		LinkedList<ValueClass> list=hashMap.get(key);
		list.add(value);
		
		hashMap.put(key, list);
	}
	
	public LinkedList<ValueClass> get(KeyClass key){
		initKey(key);
		
		return hashMap.get(key);
	}
	
	private void initKey(KeyClass key){
		if (!hashMap.containsKey(key)){
			hashMap.put(key, new LinkedList<ValueClass>());
		}
	}
	
	public int size(){
		return hashMap.size();
	}
	
	public int getNumberOfEntriesInLongestList(){
		int maxEntries=Integer.MIN_VALUE;
		
		for (KeyClass key:hashMap.keySet()){
			if (maxEntries<hashMap.get(key).size()){
				maxEntries=hashMap.get(key).size();
			}
		}
		
		return maxEntries;
	}
	
	
	
}
