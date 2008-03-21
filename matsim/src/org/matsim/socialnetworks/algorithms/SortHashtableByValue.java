package org.matsim.socialnetworks.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

public class SortHashtableByValue {
	
	private SortHashtableByValue() {
		// make this a static class, which cannot be instantiated
	}

	public static Hashtable makeSortedMap (Hashtable m) {
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
	public static ArrayList<Map.Entry> sortMap(Map map) {
		ArrayList<Map.Entry> outputList = null;
		int count = 0;
		Set<Map.Entry> set = null;
		Map.Entry[] entries = null;
		// Logic:
		// get a set from Map
		// Build a Map.Entry[] from set
		// Sort the list using Arrays.sort
		// Add the sorted Map.Entries into arrayList and return

		set = map.entrySet();
		entries = set.toArray(new Map.Entry[set.size()]);

		// Sort the entries with your own comparator for the values:
		Arrays.sort(entries, new Comparator<Map.Entry>() {
			public int compareTo(Map.Entry lhs, Map.Entry rhs) {
				return ((Comparable)lhs.getValue()).compareTo((Comparable)rhs.getValue());
			}

			public int compare(Map.Entry lhs, Map.Entry rhs) {
				return ((Comparable)lhs.getValue()).compareTo((Comparable)rhs.getValue());
			}
		});
		outputList = new ArrayList<Map.Entry>();
		for(int i = 0; i < entries.length; i++) {
			outputList.add(entries[i]);
		}
		return outputList;
	}//End of sortMap
}
