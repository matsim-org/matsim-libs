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
package playground.agarwalamit.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 * @author amit
 */

public final class MapUtils {
	
	private MapUtils(){}

	public static int intSum(final Map<?, Integer> intMap){
		if(intMap==null ) throw new NullPointerException("The map is null. Aborting ...");
		int sum =0;
		for(Integer i :intMap.values()){
			sum+=i;	
		}
		return sum;
	}

	public static double doubleSum(final Map<?, Double> doubleMap){
		if(doubleMap==null ) throw new NullPointerException("The map is null. Aborting ...");
		double sum =0;
		for(Double i :doubleMap.values()){
			sum+=i;	
		}
		return sum;
	}
	/**
	 * @return m1-m2
	 * <p> if key does not exist in either of map, value for that is assumed as <b>zero.
	 */
	public static Map<Id<Person>, Double> subtractMaps(final Map<Id<Person>, Double> m1, final Map<Id<Person>, Double> m2){
		Set<Id<Person>> keys = new HashSet<>(m1.keySet());
		keys.addAll(m2.keySet());
		Map<Id<Person>, Double> outMap = new HashMap<Id<Person>, Double>();
		for(Id<Person> id : keys){
			double v1 = m1.containsKey(id) ? m1.get(id) : 0;
			double v2 = m2.containsKey(id) ? m2.get(id) : 0;
			outMap.put(id, v2-v1);
		}
		return outMap;
	}
}
