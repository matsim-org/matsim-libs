package org.matsim.socialnetworks.algorithms;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class SortTreeMapByValue{
	TreeMap tm;
	
	public SortTreeMapByValue(TreeMap tm){
		this.tm=tm;
	}
	public static TreeSet sort(TreeMap tm) {

		System.out.println(tm.size());
		System.out.println(tm);

		TreeSet ts = new TreeSet(new Comparator() {
			public int compare(Object obj, Object obj1) {
				int vcomp = ((Comparable) ((Map.Entry) obj1).getValue()).compareTo(((Map.Entry) 
						obj).getValue());
				if (vcomp != 0) return vcomp;
				else return ((Comparable) ((Map.Entry) obj1).getKey()).compareTo(((Map.Entry) 
						obj).getKey());
			}
		});

		ts.addAll(tm.entrySet());
		int ts_lengde = ts.size();

		for (Iterator i = ts.iterator(); i.hasNext();) {

			Map.Entry entry = (Map.Entry) i.next();
			System.out.println("\n" + entry.getKey() + " has " + 
					entry.getValue() + " document(s) about this subject.\n");
			String name = (String)entry.getKey();
			String searchName = "\"" + name + "\"";
			System.out.println("Relevant documents:");
			//ds.documentSearchGivenCaseHandler(cacheDir, searchName);
			System.out.println("\n");

		}
		return ts;
	}

}