/* *********************************************************************** *
 * project: org.matsim.*
 * CountsSimCompareDayTest.java
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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

/**
 * quote from playground.balmermi.Scenario
 * 
 * @author yu
 * 
 */
public class CountsSimCompareDayTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		String countsFilename = "../schweiz-ivtch-SVN/baseCase/counts/countsIVTCH.xml";
		String eventsFilename = "../runs/run628/it.500/500.events.txt.gz";
		String outputPath = "../runs/run628/it.500/500.compareCountsSim.";
		double countsScaleFactor = 10.0;

		System.out.println("  reading the network...");
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils
				.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		System.out.println("  reading the counts...");
		final Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		System.out.println("  reading the events...");
		EventsManager events = EventsUtils.createEventsManager();
		VolumesAnalyzer va = new VolumesAnalyzer(3600, 24 * 3600 - 1, network);
		events.addHandler(va);
		new MatsimEventsReader(events).readFile(eventsFilename);

		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(outputPath
					+ "24h.txt.gz");
			writer.write("linkId\t"
					+ "X\tY\tcountValue\tsimValue\tdeviation((sim-count)/count)\n");
			for (Id linkId : counts.getCounts().keySet()) {
				Count count = counts.getCount(linkId);
				Link link = network.getLinks().get(linkId);
				if (link != null) {
					Coord toCoord = link.getToNode().getCoord();
					Coord fromCoord = link.getFromNode().getCoord();
					double x = 0.7 * toCoord.getX() + 0.3 * fromCoord.getX();
					double y = 0.7 * toCoord.getY() + 0.3 * fromCoord.getY();
					if (x != 0 && y != 0
							&& va.getVolumesForLink(linkId) != null) {
						double countVal = 0.0;
						double simVal = 0.0;
						for (int h = 0; h < 24; h++) {
							countVal += count.getVolume(h + 1).getValue();
							simVal += va.getVolumesForLink(linkId)[h]
									* countsScaleFactor;
						}
						writer.write(linkId + "\t" + x + "\t" + y + "\t"
								+ countVal + "\t" + simVal + "\t"
								+ (simVal - countVal) / countVal + "\n");
					}
				}
			}
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("  done!");
	}

}
