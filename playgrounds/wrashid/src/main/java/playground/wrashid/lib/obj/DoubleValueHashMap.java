package playground.wrashid.lib.obj;

import java.util.HashMap;

import playground.christoph.knowledge.utils.GetAllIncludedLinks;

/**
 * Often one needs a HashMap, where the value can be incremented and
 * decremented. This data structure provides that facility for doubles.
 * 
 * @author rashid_waraich
 * 
 * @param <KeyClass>
 */
public class DoubleValueHashMap<KeyClass> {

	private HashMap<KeyClass, Double> hm = new HashMap<KeyClass, Double>();

	public double get(KeyClass id) {
		if (!hm.containsKey(id)) {
			hm.put(id, 0.0);
		}

		return hm.get(id);
	}

	public void put(KeyClass id, Double value) {
		hm.put(id, value);
	}

	public void increment(KeyClass id) {
		incrementBy(id,1.0);
	}

	public void decrement(KeyClass id) {
		decrementBy(id,1.0);
	}

	public void incrementBy(KeyClass id, Double incValue) {
		if (!hm.containsKey(id)) {
			hm.put(id, 0.0);
		}

		double oldValue = hm.get(id);
		hm.put(id, oldValue + incValue);
	}

	public void decrementBy(KeyClass id, Double decValue) {
		incrementBy(id,-1.0*decValue);
	}

}
