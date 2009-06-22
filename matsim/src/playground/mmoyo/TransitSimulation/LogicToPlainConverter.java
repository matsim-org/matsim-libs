package playground.mmoyo.TransitSimulation;

import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.collections.map.LRUMap;

public class LogicToPlainConverter {
	MultiKeyMap joiningLinkMap = MultiKeyMap.decorate(new LRUMap(50));
	
	
	public LogicToPlainConverter() {
		
	}

	private void test (){
		MultiKeyMap multiKeyMap = MultiKeyMap.decorate(new LRUMap(50));
    //	multiKeyMap.put(key1, key2, "Esta es la clave 1 y 2");
	//	multiKeyMap.put(key3, key4, "Esta es la clave 3 y 4");
	///	String value = (String) multiKeyMap.get(key6, key4);
		
	}
		
}
