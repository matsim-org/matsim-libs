/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler5.java
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

package playground.yu.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

public class VolumefromEventsOfIters {

	/**
	 * @param args array: 
	 *            [0] - netfile;
	 *            [1] - first EventsFilePath;
	 *            [2] - countsFilename;
	 *            [3] - picPath;
	 */
	public static void main(final String[] args) {
		final String netFilename = args[0];
		final String eventsFilename = args[1];
		String countsFilename = args[2];
		String picPath = args[3];

		System.out.println(">>>>>reading the network...");
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		System.out.println(">>>>>reading the counts...");
		final Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		Set<Id> linksInCircle = new NetworkLinksInCircle(network).getLinks(
				682845.0, 247388.0, 2000.0);

		List<VolumesAnalyzer> vols = new ArrayList<VolumesAnalyzer>();
		for (int i = 500; i < 505; i++) {
			System.out.println(">>>>>reading the events...");
			EventsManagerImpl events = new EventsManagerImpl();
			VolumesAnalyzer volumes = new VolumesAnalyzer(900, 24 * 3600 - 1,
					network);
			events.addHandler(volumes);
			new MatsimEventsReader(events).readFile(eventsFilename + "it." + i
					+ "/" + i + ".events.txt.gz");
			vols.add(volumes);
			events = null;
		}

		for (Id linkId : counts.getCounts().keySet()) {
			if (linksInCircle.contains(linkId)) {
				XYLineChart chart = new XYLineChart("link " + linkId.toString()
						+ " traffic volume", "time", "traffic volume");
				double[] x = new double[97];
				for (int j = 0; j < 97; j++) {
					x[j] = j * 900.0;
				}
				for (int i = 0; i < 5; i++) {
					int[] v = vols.get(i).getVolumesForLink(linkId);
					double[] vd = new double[97];
					for (int k = 0; k < 97; k++) {
						vd[k] = (v != null ? v[k] * 40.0 : 0);
					}

					chart.addSeries("it." + (i + 500), x, vd);
				}

				chart.saveAsPng(picPath + linkId.toString() + ".png", 800, 600);
			}
		}
		System.out.println("-> Done!");
		System.exit(0);
	}
}
