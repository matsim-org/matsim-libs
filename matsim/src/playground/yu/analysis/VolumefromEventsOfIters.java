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
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.charts.XYLineChart;
import org.matsim.world.World;

public class VolumefromEventsOfIters {

	/**
	 * @param args
	 *            [0] - netfile;
	 * @param args
	 *            [1] - first EventsFilePath;
	 * @param args
	 *            [2] - countsFilename;
	 * @param args
	 *            [3] - picPath;
	 */
	@SuppressWarnings("unchecked")
	public static void main(final String[] args) {
		final String netFilename = args[0];
		final String eventsFilename = args[1];
		String countsFilename = args[2];
		String picPath = args[3];
		Gbl.createConfig(null
		// new String[] { "./test/yu/test/configTest.xml" }
				);

		World world = Gbl.getWorld();

		System.out.println(">>>>>reading the network...");
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);
		world.complete();

		System.out.println(">>>>>reading the counts...");
		final Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		Set<Link> linksInCircle = new NetworkLinksInCircle(network).getLinks(
				682845.0, 247388.0, 2000.0);

		List<VolumesAnalyzer> vols = new ArrayList<VolumesAnalyzer>();
		for (int i = 500; i < 505; i++) {
			System.out.println(">>>>>reading the events...");
			Events events = new Events();
			VolumesAnalyzer volumes = new VolumesAnalyzer(900, 24 * 3600 - 1,
					network);
			events.addHandler(volumes);
			new MatsimEventsReader(events).readFile(eventsFilename + "it." + i
					+ "/" + i + ".events.txt.gz");
			vols.add(volumes);
			events = null;
		}

		for (Id linkId : counts.getCounts().keySet()) {
			if (linksInCircle.contains(network.getLink(linkId))) {
				XYLineChart chart = new XYLineChart("link " + linkId.toString()
						+ " traffic volume", "time", "traffic volume");
				double[] x = new double[97];
				for (int j = 0; j < 97; j++) {
					x[j] = (double) j * 900.0;
				}
				for (int i = 0; i < 5; i++) {
					int[] v = vols.get(i).getVolumesForLink(linkId.toString());
					double[] vd = new double[97];
					for (int k = 0; k < 97; k++) {
						vd[k] = (double) (v != null ? v[k] * 40.0 : 0);
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
