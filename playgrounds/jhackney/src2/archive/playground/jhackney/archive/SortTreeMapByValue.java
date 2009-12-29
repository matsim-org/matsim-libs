
package org.matsim.socialnetworks.algorithms;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class SortTreeMapByValue {

	private SortTreeMapByValue() {
		// make this a static class, which cannot be instantiated
	}

	public static Set<Map.Entry> sort(TreeMap tm) {

		System.out.println(tm.size());
		System.out.println(tm);

		TreeSet<Map.Entry> ts = new TreeSet(new Comparator<Map.Entry>() {
			public int compare(Map.Entry obj, Map.Entry obj1) {
				int vcomp = ((Comparable) obj1.getValue()).compareTo(obj.getValue());
				if (vcomp != 0) {
					return vcomp;
				}
				return ((Comparable) obj1.getKey()).compareTo(obj.getKey());
			}
		});

		ts.addAll(tm.entrySet());

		for (Iterator i = ts.iterator(); i.hasNext();) {
			Map.Entry entry = (Map.Entry) i.next();
			System.out.println("\n" + entry.getKey() + " has " + entry.getValue()
					+ " document(s) about this subject.\n");
			System.out.println("Relevant documents:");
//			String name = (String) entry.getKey();
//			String searchName = "\"" + name + "\"";
			//ds.documentSearchGivenCaseHandler(cacheDir, searchName);
			System.out.println("\n");

		}
		return ts;
	}

}
