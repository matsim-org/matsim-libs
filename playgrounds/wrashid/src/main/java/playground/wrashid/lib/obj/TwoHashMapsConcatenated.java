package playground.wrashid.lib.obj;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
//TODO: write tests.
public class TwoHashMapsConcatenated<ClassKey1,ClassKey2,ClassValue> {

	private HashMap<ClassKey1, HashMap<ClassKey2, ClassValue>> hashMap=new HashMap<ClassKey1, HashMap<ClassKey2,ClassValue>>(); 
	
	public Collection<ClassValue> getValues(){
		LinkedList<ClassValue> list=new LinkedList<ClassValue>();
		for (ClassKey1 key1:getKeySet1()){
			for (ClassKey2 key2:getKeySet2(key1)){
				list.add(get(key1, key2));
			}
		}
		return list;
	}
	
	public void put(ClassKey1 key1, ClassKey2 key2, ClassValue value){
		checkHashMapAndInitializeIfNeeded(key1);
		hashMap.get(key1).put(key2, value);
	}
	
	public ClassValue get(ClassKey1 key1, ClassKey2 key2){
		checkHashMapAndInitializeIfNeeded(key1);
		
		return hashMap.get(key1).get(key2);
	}
	
	private void checkHashMapAndInitializeIfNeeded(ClassKey1 key1){
		if (!hashMap.containsKey(key1)){
			hashMap.put(key1, new HashMap<ClassKey2, ClassValue>());
		}
	}
	
	public Set<ClassKey2> getKeySet2(ClassKey1 key1){
		return hashMap.get(key1).keySet();
	}

	public Set<ClassKey1> getKeySet1(){
		return hashMap.keySet();
	}
	
	public void removeValue(ClassKey1 key1, ClassKey2 key2){
		checkHashMapAndInitializeIfNeeded(key1);
		
		hashMap.get(key1).remove(key2);
	}
	
	public boolean containsValue(ClassKey1 key1, ClassKey2 key2){
		if (!hashMap.containsKey(key1)){
			return false;
		} else {
			if (!hashMap.containsKey(key2)){
				return false;
			} else {
				return true;
			}
		}
	}
	
}
