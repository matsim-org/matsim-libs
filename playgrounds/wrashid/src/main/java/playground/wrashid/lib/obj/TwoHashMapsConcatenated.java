package playground.wrashid.lib.obj;

import java.util.HashMap;
//TODO: write tests.
public class TwoHashMapsConcatenated<ClassKey1,ClassKey2,ClassValue> {

	private HashMap<ClassKey1, HashMap<ClassKey2, ClassValue>> hashMap=new HashMap<ClassKey1, HashMap<ClassKey2,ClassValue>>(); 
	
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
	
	
	
}
