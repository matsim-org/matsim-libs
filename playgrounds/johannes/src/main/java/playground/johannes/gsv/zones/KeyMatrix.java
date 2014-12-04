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

	public Double get(String key1, String key2) {
		Map<String, Double> col = matrix.get(key1);
		if(col == null) {
			return null;
		} else {
			return col.get(key2);
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
