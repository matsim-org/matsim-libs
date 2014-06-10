package playground.wrashid.lib.obj;

import java.util.HashMap;

import org.matsim.contrib.parking.lib.obj.LinkedListValueHashMap;

public class HashMapInverter<KeyClass,ValueClass> {

	private final HashMap<KeyClass, ValueClass> hashMap;

	public HashMapInverter(HashMap<KeyClass, ValueClass> hashMap) {
		this.hashMap = hashMap;
	}
	
	public LinkedListValueHashMap<ValueClass, KeyClass> getLinkedListValueHashMap(){
		LinkedListValueHashMap<ValueClass,KeyClass> linkedListValueHashMap=new LinkedListValueHashMap<ValueClass, KeyClass>();
		
		for (KeyClass key:hashMap.keySet()){
			linkedListValueHashMap.put(hashMap.get(key), key);
		}
		
		return linkedListValueHashMap;
	}
	
}
