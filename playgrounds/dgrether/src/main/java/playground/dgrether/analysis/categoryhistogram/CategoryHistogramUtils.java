/* *********************************************************************** *
 * project: org.matsim.*
 * DgLegHistogramUtils
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis.categoryhistogram;

import java.util.Map;


/**
 * @author dgrether
 *
 */
final class CategoryHistogramUtils {

	static void increaseMapEntry(Map<Integer, Integer> map, Integer key){
		Integer value = map.get(key);
		if (value == null){
			value = 0;
		}
		value++;
		map.put(key, value);
	}
	
	static void add2MapEntry(Map<Integer, Integer> map, Integer key, Integer summand){
		Integer value = map.get(key);
		if (value == null){
			value = 0;
		}
		value = value + summand;
		map.put(key, value);
	}
	
	
	static Integer getNotNullInteger(Map map, Object key){
		Integer i = (Integer) map.get(key);
		if (i == null) {
			return Integer.valueOf(0);
		}
		return i;
	}
	
}
