package playground.jhackney.socialnetworks.algorithms;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SortHashMapByValue {
	
	public SortHashMapByValue() {
		// make this a static class, which cannot be instantiated
	}

//	public static LinkedHashMap makeSortedMap (LinkedHashMap m) {
	public LinkedHashMap makeSortedMap (LinkedHashMap m) {
		LinkedHashMap sortedMap=new LinkedHashMap();
		List outputList = sortMap(m);
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
	private static List<Map.Entry> sortMap(Map map) {
//		List<Map.Entry> outputList = null;
//		int count = 0;
		// Logic:
		// get a set from Map
		// Build a Map.Entry[] from set
		// Sort the list using Arrays.sort
		// Add the sorted Map.Entries into arrayList and return

		Set<Map.Entry> set = map.entrySet();
		Map.Entry[] entries = set.toArray(new Map.Entry[set.size()]);

		// Sort the entries with your own comparator for the values:
		Arrays.sort(entries, new Comparator<Map.Entry>() {
			public int compare(Map.Entry lhs, Map.Entry rhs) {
				return ((Comparable)lhs.getValue()).compareTo((Comparable)rhs.getValue());
			}
		});
		return Arrays.asList(entries);
//		outputList = new ArrayList<Map.Entry>(entries);
//		for(int i = 0; i < entries.length; i++) {
//			outputList.add(entries[i]);
//		}
//		return outputList;
	}//End of sortMap
}
