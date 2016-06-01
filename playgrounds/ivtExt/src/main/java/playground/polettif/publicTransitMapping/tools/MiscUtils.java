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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Some utils for collection manipulation
 *
 * @author polettif
 */
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
	public static <K> Map<K, Double> normalize(Map<K, Double> map) {
		Double maxValue = 0.0;
		for(Double v : map.values()) {
			if(v > maxValue)
				maxValue = v;
		}

		// scale weights
		for(Map.Entry<K, Double> e : map.entrySet()) {
			map.put(e.getKey(), map.get(e.getKey()) / maxValue);
		}
		return map;
	}

	/**
	 * Normalizes the values of a map via 1-value/maxValue
	 *
	 * @return the normalized map
	 */
	public static <K> Map<K, Double> normalizeInvert(Map<K, Double> map) {
		Double maxValue = 0.0;
		for(Double v : map.values()) {
			if(v > maxValue)
				maxValue = v;
		}
		for(Map.Entry<K, Double> e : map.entrySet()) {
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
	public static <K, V extends Comparable<V>> Map<K, V> sortAscendingByValue(Map<K, V> unsortMap) {
		// Convert Map to List
		List<Map.Entry<K, V>> list = new LinkedList<>(unsortMap.entrySet());

		// Sort list with comparator, to compare the Map values
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			public int compare(Map.Entry<K, V> o1,
							   Map.Entry<K, V> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});

		// Convert sorted map back to a Map
		Map<K, V> sortedMap = new LinkedHashMap<>();
		for(Map.Entry<K, V> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}

	/**
	 * Checks whether list1 is a subset of list2. Returns false
	 * if list1 is greater than list 2. Returns true if the reversed
	 * list1 is a subset of list2
	 */
	public static <E> boolean listIsSubset(List<E> list1, List<E> list2) {
		int size1 = list1.size();
		int size2 = list2.size();
		if(size1 > size2) {
			return false;
		}
		if(size1 == 1) {
			return list2.contains(list1.get(0));
		}
		if(size2 == 1) {
			return list1.contains(list2.get(0));
		}

		for(int i = 0; i < size2; i++) {
			if(list2.get(i).equals(list1.get(0))) {

				if(i < size2-1 && list1.get(1).equals(list2.get(i + 1))) {
					// check forward
					for(int j = 1; j < list1.size(); j++) {
						if((i + j) == size2) {
							return false;
						}
						if(!list1.get(j).equals(list2.get(i + j))) {
							return false;
						}
					}
					return true;
				} else if(list1.get(1).equals(list2.get(i - 1))) {
					// check backward
					for(int j = 1; j < size1; j++) {
						if((i - j) == -1) {
							return false;
						}
						if(!list1.get(j).equals(list2.get(i - j))) {
							return false;
						}
					}
					return true;
				} else {
					return false;
				}
			}
		}
		return false;
	}


	/**
	 * Gets the tree set associated with the key in this map, or create an empty set if no mapping exists yet.
	 *
	 * @param key the key of the mapping
	 * @param map the map in which to search
	 * @param <K> type of the key
	 * @param <V> parameter of the set type
	 * @return the set (evt. newly) associated with the key
	 * @see org.matsim.core.utils.collections.MapUtils
	 */
	public static <K, V> TreeSet<V> getTreeSet(
			final K key,
			final Map<K, TreeSet<V>> map) {
		TreeSet<V> coll = map.get(key);

		if(coll == null) {
			coll = new TreeSet<>();
			map.put(key, coll);
		}

		return coll;
	}
}