package playground.wrashid.lib.obj;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import playground.wrashid.lib.GeneralLib;

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
	
	public void printToConsole(){
		GeneralLib.printHashmapToConsole(hm);
	}
	
	public KeyClass getKeyForMaxValue(){
		double maxValue=Double.MIN_VALUE;
		KeyClass maxKey=null;
		for (KeyClass key:hm.keySet()){
			double curValue=hm.get(key);
			if (curValue>maxValue){
				maxValue=curValue;
				maxKey=key;
			}
		}	
		return maxKey;
	}
	
	public LinkedList<KeyClass> getKeysWithHigherValueThanThresholdValue(double thresholdValue){
		LinkedList<KeyClass> result = new LinkedList<KeyClass>();
		for (KeyClass key:hm.keySet()){
			double curValue=hm.get(key);
			if (curValue>thresholdValue){
				result.add(key);
			}
		}
		return result;
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
	
}
