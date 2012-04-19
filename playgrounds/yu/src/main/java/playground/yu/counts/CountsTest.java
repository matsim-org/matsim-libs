/* *********************************************************************** *
 * project: org.matsim.*
 * CountsTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.yu.counts;

import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Volume;

/**
 * @author yu
 * 
 */
public class CountsTest {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();

		// final String netFilename =
		// "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String countsFilename = "../schweiz-ivtch-SVN/baseCase/counts/countsIVTCH.xml";
		// final String outputFilename = "output/counts.txt";

		// Gbl.createConfig(null);
		//
		// NetworkLayer network = new NetworkLayer();
		// new MatsimNetworkReader(network).readFile(netFilename);

		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		double countsSummeValueofTime[] = new double[24];
		for (Id linkId : counts.getCounts().keySet()) {
			Map<Integer, Volume> volumes = counts.getCount(linkId).getVolumes();
			for (Entry<Integer, Volume> itgEntry : volumes.entrySet()) {
				double value = itgEntry.getValue().getValue();
				String output = "link_ID=" + linkId + "\th="
						+ itgEntry.getKey() + "\tvolume=" + value;
				countsSummeValueofTime[itgEntry.getKey() - 1] += value;
				System.out.println(output);
			}
		}
		for (int i = 0; i < 24; i++) {
			System.out.println(i + 1 + "\t" + countsSummeValueofTime[i]);
		}

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
