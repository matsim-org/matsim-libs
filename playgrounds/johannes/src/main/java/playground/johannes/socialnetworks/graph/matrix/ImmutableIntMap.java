/* *********************************************************************** *
 * project: org.matsim.*
 * ImmutableIntMap.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.matrix;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntProcedure;

import java.util.Arrays;

/**
 * @author illenberger
 *
 */
public class ImmutableIntMap {

	private final int[] keys;
	
	private int[] values;
	
	public ImmutableIntMap(TIntArrayList keys2) {
		this.keys = new int[keys2.size()];
		this.values = new int[keys2.size()];
		for(int i = 0; i < keys2.size(); i++)
			keys[i] = keys2.get(i);
//		keys2.forEach(new TIntProcedure() {
//			int i = 0;
//			public boolean execute(int value) {
//				keys[i] = value;
//				i++;
//				return true;
//			}
//		});
	}
	public ImmutableIntMap(int[] keys) {
		this.keys = new int[keys.length];
		this.values = new int[keys.length];
		System.arraycopy(keys, 0, this.keys, 0, keys.length);
		Arrays.sort(keys);
	}
	
	private int index(int key) {
		return Arrays.binarySearch(keys, key);
	}
	
	public void set(int key, int value) {
		int index = index(key);
		if(index < 0)
			throw new IllegalArgumentException();
		else
			values[index] = value;
	}
	
	public int get(int key) {
		return values[index(key)];
	}
}
