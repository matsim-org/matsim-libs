package org.matsim.socialnetworks.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SortHashtableByValue {
	/**
	 * test sortMap
	 */
	public Hashtable makeSortedMap (Hashtable m) {
		Hashtable sortedMap=new Hashtable();
		ArrayList outputList = sortMap(m);
//		int count = 0;
//		count = outputList.size();
//		while(count > 0) {
//			Map.Entry entry = (Map.Entry) outputList.get(--count);
//			sortedMap.put(entry.getKey(),entry.getValue());
//			System.out.print("Key:" + entry.getKey());
//			System.out.println("\tValue:" + entry.getValue());
//		}
		for(int count=0;count<outputList.size();count++){
			Map.Entry entry = (Map.Entry) outputList.get(count);
			sortedMap.put(entry.getKey(),entry.getValue());
		}
		return sortedMap;
	}

	/**
	 * This method will use Arrays.sort for sorting Map
	 * @param map
	 * @return outputList of Map.Entries
	 */
	public ArrayList sortMap(Map map) {
		ArrayList outputList = null;
		int count = 0;
		Set set = null;
		Map.Entry[] entries = null;
		// Logic:
		// get a set from Map
		// Build a Map.Entry[] from set
		// Sort the list using Arrays.sort
		// Add the sorted Map.Entries into arrayList and return

		set = (Set) map.entrySet();
		Iterator iterator = set.iterator();
		entries = new Map.Entry[set.size()];
		while(iterator.hasNext()) {
			entries[count++] = (Map.Entry) iterator.next();
		}

		// Sort the entries with your own comparator for the values:
		Arrays.sort(entries, new Comparator() {
			public int compareTo(Object lhs, Object rhs) {
				Map.Entry le = (Map.Entry)lhs;
				Map.Entry re = (Map.Entry)rhs;
				return ((Comparable)le.getValue()).compareTo((Comparable)re.getValue());
			}

			public int compare(Object lhs, Object rhs) {
				Map.Entry le = (Map.Entry)lhs;
				Map.Entry re = (Map.Entry)rhs;
				return ((Comparable)le.getValue()).compareTo((Comparable)re.getValue());
			}
		});
		outputList = new ArrayList();
		for(int i = 0; i < entries.length; i++) {
			outputList.add(entries[i]);
		}
		return outputList;
	}//End of sortMap
}
