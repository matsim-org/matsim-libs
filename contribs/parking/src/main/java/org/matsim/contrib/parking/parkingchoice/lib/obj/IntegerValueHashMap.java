package org.matsim.contrib.parking.parkingchoice.lib.obj;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;


/**
 * Often one needs a HashMap, where the value can be incremented and
 * decremented. This data structure provides that facility for integers.
 *
 * TODO: delete this (DoubleValueHashMap can do most of the work here => just add a method there for conversion to id perhaps...)
 *
 * @author rashid_waraich
 *
 * @param <KeyClass>
 */
public class IntegerValueHashMap<KeyClass> {

	private HashMap<KeyClass, Integer> hm = new HashMap<KeyClass, Integer>();
	private int defaultStartValue=0;

	public IntegerValueHashMap(){}

	public Collection<Integer> values(){
		return hm.values();
	}

	public boolean containsKey(KeyClass id){
		return hm.containsKey(id);
	}

	public IntegerValueHashMap(int defaultStartValue) {
		this.defaultStartValue=defaultStartValue;
	}

	public Set<KeyClass> getKeySet() {
		return hm.keySet();
	}

	public int get(KeyClass id) {
		if (!hm.containsKey(id)) {
			hm.put(id, defaultStartValue);
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
			hm.put(id, defaultStartValue);
		}

		int oldValue = hm.get(id);

		hm.put(id, oldValue + incValue);
	}

	public void decrementBy(KeyClass id, Integer decValue) {
		incrementBy(id, -1 * decValue);
	}



}
