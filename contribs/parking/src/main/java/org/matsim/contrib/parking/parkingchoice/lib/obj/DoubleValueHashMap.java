package org.matsim.contrib.parking.parkingchoice.lib.obj;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

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

	public boolean containsKey(KeyClass id){
		return hm.containsKey(id);
	}

	public Collection<Double> values(){
		return hm.values();
	}

	public Set<KeyClass> keySet(){
		return hm.keySet();
	}

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

	public void remove(KeyClass id){
		hm.remove(id);
	}

	// TODO: write test
	public double getAverage() {
		double sum = 0;
		for (KeyClass key : hm.keySet()) {
			double curValue = hm.get(key);
			sum += curValue;
		}
		return sum / hm.size();
	}

	public int size(){
		return hm.size();
	}

}
