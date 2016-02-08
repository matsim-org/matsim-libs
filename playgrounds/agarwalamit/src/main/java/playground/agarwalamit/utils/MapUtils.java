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
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

import playground.benjamin.utils.BkNumberUtils;

/**
 * @author amit
 */

public final class MapUtils {

	private MapUtils(){}

	public static int intValueSum(final Map<?, Integer> intMap){
		if(intMap==null ) throw new NullPointerException("The map is null. Aborting ...");
		int sum =0;
		for(Integer i :intMap.values()){
			sum+=i;	
		}
		return sum;
	}

	public static double doubleValueSum(final Map<?, Double> doubleMap){
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
	public static <T> Map<Id<T>, Double> subtractMaps(final Map<Id<T>, Double> m1, final Map<Id<T>, Double> m2){
		if(m1==null || m2 ==null) throw new NullPointerException("Either of the maps is null. Aborting ...");
		Set<Id<T>> keys = new HashSet<>(m1.keySet());
		keys.addAll(m2.keySet());
		Map<Id<T>, Double> outMap = new HashMap<Id<T>, Double>();
		for(Id<T> id : keys){
			double v1 = m1.containsKey(id) ? m1.get(id) : 0;
			double v2 = m2.containsKey(id) ? m2.get(id) : 0;
			outMap.put(id, v2-v1);
		}
		return outMap;
	}

	/**
	 * @return m1+m2
	 * <p> if key does not exist in either of map, value for that is assumed as <b>zero.
	 */
	public static SortedMap<String, Double> addMaps (final Map<String, Double> m1, final Map<String, Double> m2) {
		if(m1==null || m2 ==null) throw new NullPointerException("Either of the maps is null. Aborting ...");
		SortedMap<String, Double> outMap = new TreeMap<>(m1);
		for (String str : m2.keySet()){
			double existingValue = outMap.containsKey(str) ? outMap.get(str) : 0.;
			outMap.put(str, m2.get(str)+existingValue);
		}
		return outMap;
	}

	public static SortedMap<String, Double> getPercentShare(final SortedMap<String, Integer> inMap){
		SortedMap<String, Double> outMap = new TreeMap<>();
		double valueSum = (double) MapUtils.intValueSum(inMap);
		for(String str : inMap.keySet()) {
			double legs = (double) inMap.get(str);
			double pctShare = BkNumberUtils.roundDouble( legs*100 / valueSum, 3); 
			outMap.put(str, pctShare);
		}
		return outMap;
	}
}