/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.tools;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.*;
import java.util.stream.Collectors;

public class MiscUtils {

	/**
	 * @return true, if two sets (e.g. scheduleTransportModes and
	 * networkTransportModes) have at least one identical entry.
	 */
	public static boolean setsShareMinOneStringEntry(Set<String> set1, Set<String> set2) {
		if(set1 == null || set2 == null) {
			return false;
		} else {
			for(String entry1 : set1) {
				for(String entry2 : set2) {
					if(entry1.equalsIgnoreCase(entry2)) {
						return true;
					}
				}
			}
			return false;
		}
	}

	/**
	 * @return a set with entries present in both sets.
	 */
	public static Set<String> getSharedSetStringEntries(Set<String> set1, Set<String> set2) {
		Set<String> shared = new HashSet<>();
		for(String entry1 : set1) {
			shared.addAll(set2.stream().filter(entry1::equalsIgnoreCase).map(entry2 -> entry1).collect(Collectors.toList()));
		}
		return shared;
	}

	/**
	 * @return true, if two sets have at least one identical entry.
	 */
	public static <E> boolean setsShareMinOneEntry(Set<E> set1, Set<E> set2) {
		if(set1 == null || set2 == null) {
			return false;
		} else {
			for(E entry1 : set1) {
				for(E entry2 : set2) {
					if(entry1.equals(entry2)) {
						return true;
					}
				}
			}
			return false;
		}
	}

	/**
	 * @return a set with entries present in both sets.
	 */
	public static <E> Set<E> getSharedSetEntries(Set<E> set1, Set<E> set2) {
		Set<E> shared = new HashSet<>();
		for(E entry1 : set1) {
			shared.addAll(set2.stream().filter(entry1::equals).map(entry2 -> entry1).collect(Collectors.toList()));
		}
		return shared;
	}


	/**
	 * normalizes the values of a map via value/maxValue
	 *
	 * @return the normalized map
	 */
	public static Map<Id<Link>, Double> normalize(Map<Id<Link>, Double> map) {
		// get maximal weight
		double maxValue = 0;
		for(Double v : map.values()) {
			if(v > maxValue)
				maxValue = v;
		}

		// scale weights
		for(Map.Entry<Id<Link>, Double> e : map.entrySet()) {
			map.put(e.getKey(), map.get(e.getKey()) / maxValue);
		}
		return map;
	}

	/**
	 * Normalizes the values of a map via 1-value/maxValue
	 *
	 * @return the normalized map
	 */
	public static Map<Id<Link>, Double> normalizeInvert(Map<Id<Link>, Double> map) {
		double maxValue = 0;
		for(Double v : map.values()) {
			if(v > maxValue)
				maxValue = v;
		}

		for(Map.Entry<Id<Link>, Double> e : map.entrySet()) {
			map.put(e.getKey(), 1 - map.get(e.getKey()) / maxValue);
		}

		return map;
	}

	/**
	 * Sorts a map by its values.
	 *
	 * @param unsortMap the unsortedMap
	 * @return the sorted map
	 */
	public static Map<String, Integer> sortAscending(Map<String, Integer> unsortMap) {
		// Convert Map to List
		List<Map.Entry<String, Integer>> list = new LinkedList<>(unsortMap.entrySet());

		// Sort list with comparator, to compare the Map values
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1,
							   Map.Entry<String, Integer> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});

		// Convert sorted map back to a Map
		Map<String, Integer> sortedMap = new LinkedHashMap<>();
		for(Iterator<Map.Entry<String, Integer>> it = list.iterator(); it.hasNext(); ) {
			Map.Entry<String, Integer> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
}