/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.misc;

import gnu.trove.iterator.TDoubleDoubleIterator;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.matsim.contrib.common.stats.DummyDiscretizer;
import org.matsim.contrib.common.stats.Histogram;
import playground.johannes.synpop.source.mid2008.generator.RowHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 * 
 */
public class LinkAttLogic {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		final Map<String, TDoubleArrayList> ttmap = new HashMap<String, TDoubleArrayList>();
		final Map<String, TDoubleArrayList> capmap = new HashMap<String, TDoubleArrayList>();

		RowHandler handler = new RowHandler() {

			@Override
			protected void handleRow(Map<String, String> attributes) {
				String typenr = attributes.get("TYPNR");
				String lanes = attributes.get("ANZFAHRSTREIFEN");

				String capStr = attributes.get("KAPIV");
				String ttStr = attributes.get("V0IV");

				String key = String.format("%s\t%s", typenr, lanes);

				if (ttStr != null && !ttStr.isEmpty()) {

					double tt = Double.parseDouble(ttStr);

					TDoubleArrayList ttVals = ttmap.get(key);
					if (ttVals == null) {
						ttVals = new TDoubleArrayList();
						ttmap.put(key, ttVals);
					}
					ttVals.add(tt);
				}

				if (capStr != null && !capStr.isEmpty()) {
					double cap = Double.parseDouble(capStr);
					TDoubleArrayList capVals = capmap.get(key);
					if (capVals == null) {
						capVals = new TDoubleArrayList();
						capmap.put(key, capVals);
					}
					capVals.add(cap);
				}
			}
		};

		handler.setColumnOffset(1);
		handler.read("/home/johannes/gsv/prognose-update/strecken.de.txt");

		System.out.println("*** stats for travel time ***");
		dumpStats(ttmap);
		System.out.println();
		System.out.println("*** stats for capacity ***");
		dumpStats(capmap);
	}

	private static void dumpStats(Map<String, TDoubleArrayList> map) {
		for (Map.Entry<String, TDoubleArrayList> entry : map.entrySet()) {
			TDoubleDoubleHashMap hist = Histogram.createHistogram(entry.getValue().toArray(), DummyDiscretizer.getInstance(), false);

			System.out.print(entry.getKey());
//			System.out.print(" = ");
			System.out.print("\t");
			TDoubleDoubleIterator it = hist.iterator();
			for (int i = 0; i < hist.size(); i++) {
				it.advance();
				System.out.print(String.valueOf(it.key()));
//				System.out.print(":");
				System.out.print("\t");
				System.out.print(String.valueOf(it.value()));
//				System.out.print(" ");
				System.out.print("\t");
			}

			System.out.println();
		}
	}
}
