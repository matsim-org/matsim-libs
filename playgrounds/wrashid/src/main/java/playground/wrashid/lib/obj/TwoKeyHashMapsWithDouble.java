/* *********************************************************************** *
 * project: org.matsim.*
 * TwoKeyHashMapsWithDouble.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.wrashid.lib.obj;

import java.util.HashMap;

public class TwoKeyHashMapsWithDouble<ClassKey1,ClassKey2> {

	// TODO: write test
private HashMap<ClassKey1, DoubleValueHashMap<ClassKey2>> hashMap=new HashMap<ClassKey1, DoubleValueHashMap<ClassKey2>>();
	
	public void put(ClassKey1 key1, ClassKey2 key2, double value){
		checkHashMapAndInitializeIfNeeded(key1);
		hashMap.get(key1).put(key2, value);
	}
	
	public void incrementBy(ClassKey1 key1, ClassKey2 key2, double incValue){
		checkHashMapAndInitializeIfNeeded(key1);
		hashMap.get(key1).incrementBy(key2, incValue);
	}
	
	public DoubleValueHashMap<ClassKey2> get(ClassKey1 key1){
		return hashMap.get(key1);
	}
	
	public double getAverage(){
		double sum = 0;
		for (ClassKey1 key : hashMap.keySet()) {
			DoubleValueHashMap<ClassKey2> curValue = hashMap.get(key);
			sum += curValue.getAverage();
		}
		return sum / hashMap.size();
	}
	
	public double get(ClassKey1 key1, ClassKey2 key2){
		checkHashMapAndInitializeIfNeeded(key1);
		return hashMap.get(key1).get(key2);
	}
	
	
	
	private void checkHashMapAndInitializeIfNeeded(ClassKey1 key1){
		if (!hashMap.containsKey(key1)){
			hashMap.put(key1, new DoubleValueHashMap<ClassKey2>());
		}
	}
	
}
