package playground.wrashid.lib.obj;

import java.util.HashMap;
import java.util.Set;

import playground.wrashid.lib.GeneralLib;

/**
 * Often one needs a HashMap, where the value can be incremented and
 * decremented. This data structure provides that facility for integers.
 * 
 * @author rashid_waraich
 * 
 * @param <KeyClass>
 */
public class IntegerValueHashMap<KeyClass> {

	private HashMap<KeyClass, Integer> hm = new HashMap<KeyClass, Integer>();

	public Set<KeyClass> getKeySet() {
		return hm.keySet();
	}

	public int get(KeyClass id) {
		if (!hm.containsKey(id)) {
			hm.put(id, 0);
		}

		return hm.get(id);
	}

	public void set(KeyClass id, Integer value) {
		hm.put(id, value);
	}

	public void increment(KeyClass id) {
		incrementBy(id, 1);
	}

	public void decrement(KeyClass id) {
		decrementBy(id, 1);
	}

	public void incrementBy(KeyClass id, Integer incValue) {
		if (!hm.containsKey(id)) {
			hm.put(id, 0);
		}

		int oldValue = hm.get(id);

		hm.put(id, oldValue + incValue);
	}

	public void decrementBy(KeyClass id, Integer decValue) {
		incrementBy(id, -1 * decValue);
	}

	public void printToConsole() {
		GeneralLib.printHashmapToConsole(hm);
	}

	
}
