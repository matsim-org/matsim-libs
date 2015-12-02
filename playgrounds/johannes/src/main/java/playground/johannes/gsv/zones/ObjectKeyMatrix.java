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

import java.util.*;
import java.util.Map.Entry;

/**
 * @author johannes
 * 
 */
public class ObjectKeyMatrix<T> {

	private Map<String, Map<String, T>> matrix;
	
	public ObjectKeyMatrix() {
		matrix = new HashMap<>();
	}
	
	public T set(String key1, String key2, T value) {
		Map<String, T> col = matrix.get(key1);
		if(col == null) {
			col = new HashMap<String, T>();
			matrix.put(key1, col);
		}
		
		return col.put(key2, value);
	}
	
//	public T add(String key1, String key2, T value) {
//		T val = get(key1, key2);
//		if(val == null) {
//			return set(key1, key2, value);
//		} else {
//			return set(key1, key2, val + value);
//		}
//	}

	public T get(String key1, String key2) {
//		Map<String, Double> row = matrix.get(key1);
		Map<String, T> row = getRow(key1);
		if(row == null) {
			return null;
		} else {
			return row.get(key2);
		}
	}
	
	public Map<String, T> getRow(String key) {
		return matrix.get(key);
	}
	
//	public void applyFactor(String i, String j, T factor) {
//		Double val = get(i, j);
//		if(val != null) {
//			set(i, j, val * factor);
//		}
//	}
	
	public Set<String> keys() {
		Set<String> keys = new HashSet<>(matrix.keySet());
		for(Entry<String, Map<String, T>> entry : matrix.entrySet()) {
			keys.addAll(entry.getValue().keySet());
		}
		
		return keys;
	}

	public List<T> values() {
		List<T> values = new ArrayList<>();
		for(Entry<String, Map<String, T>> entry : matrix.entrySet()) {
			values.addAll(entry.getValue().values());
		}
		return values;
	}
}
