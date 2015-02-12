/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.zones;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class KeyMatrix {

	private Map<String, Map<String, Double>> matrix;
	
	public KeyMatrix() {
		matrix = new HashMap<>();
	}
	
	public Double set(String key1, String key2, Double value) {
		Map<String, Double> col = matrix.get(key1);
		if(col == null) {
			col = new HashMap<String, Double>();
			matrix.put(key1, col);
		}
		
		return col.put(key2, value);
	}
	
	public Double add(String key1, String key2, double value) {
		Double val = get(key1, key2);
		if(val == null) {
			return set(key1, key2, value);
		} else {
			return set(key1, key2, val + value);
		}
	}

	public Double get(String key1, String key2) {
//		Map<String, Double> row = matrix.get(key1);
		Map<String, Double> row = getRow(key1);
		if(row == null) {
			return null;
		} else {
			return row.get(key2);
		}
	}
	
	public Map<String, Double> getRow(String key) {
		return matrix.get(key);
	}
	
	public void applyFactor(String i, String j, double factor) {
		Double val = get(i, j);
		if(val != null) {
			set(i, j, val * factor);
		}
	}
	
	public Set<String> keys() {
		Set<String> keys = new HashSet<>(matrix.keySet());
		for(Entry<String, Map<String, Double>> entry : matrix.entrySet()) {
			keys.addAll(entry.getValue().keySet());
		}
		
		return keys;
	}
}
