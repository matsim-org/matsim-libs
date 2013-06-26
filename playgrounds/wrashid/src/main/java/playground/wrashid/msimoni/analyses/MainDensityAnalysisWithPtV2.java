/* *********************************************************************** *
 * project: org.matsim.*
 * MainDensityAnalysisWithPt.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.wrashid.msimoni.analyses;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class MainDensityAnalysisWithPtV2 {

	public static void main(String[] args) {
		String networkFile = "H:/thesis/output_no_pricing_v3_subtours_bugfix/output_network.xml.gz";
		String eventsFile =  "H:/thesis/output_no_pricing_v3_subtours_bugfix/ITERS/it.50/50.events.xml.gz";
		Coord center = null; // center=null means use all links
		int binSizeInSeconds = 900;	// 15 minute bins

		// String
		// networkFile="C:/Users/Nan/Desktop/For matsim/matsim-0.1.1/examples/equil/network.xml";
		// String
		// eventsFile="C:/Users/Nan/Desktop/For matsim/matsim-0.1.1/output/equil/ITERS/it.5/5.events.txt.gz";
		// Coord center=new CoordImpl(0,0);
		// boolean isOldEventFile=false;

		double radiusInMeters = 2000;
		double length = 50.0;

		// input/set center and radius
//		Map<Id, ? extends Link> links = NetworkReadExample.getNetworkLinks(networkFile, center, radiusInMeters);
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
//		center = scenario.getNetwork().getNodes().get(scenario.createId("17560000106060FT")).getCoord();
		center = scenario.createCoord(683513.0, 246839.9);
		
		Map<Id, Link> links = LinkSelector.selectLinks(scenario.getNetwork(), center, radiusInMeters, length);
		
		InFlowInfoAcuumulatorWithPt inflowHandler = new InFlowInfoAcuumulatorWithPt(links, binSizeInSeconds);
		OutFlowInfoAccumulatorWithPt outflowHandler = new OutFlowInfoAccumulatorWithPt(links, binSizeInSeconds);

		inflowHandler.reset(0);
		outflowHandler.reset(0);

		EventsManager events = EventsUtils.createEventsManager();

		events.addHandler(inflowHandler); // add handler
		events.addHandler(outflowHandler);

		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventsFile);

		HashMap<Id, int[]> linkInFlow = inflowHandler.getLinkInFlow();
		HashMap<Id, int[]> linkOutFlow = outflowHandler.getLinkOutFlow();

		System.out.println("Entries from the inflow handler:  " + linkInFlow.size());
		System.out.println("Entries from the outflow handler: " + linkOutFlow.size());
		
		HashMap<Id, int[]> deltaFlow = deltaFlow(linkInFlow, linkOutFlow);
		HashMap<Id, double[]> density = calculateDensity(deltaFlow, links);

		printDensity(density, links);

	}

	public static HashMap<Id, int[]> deltaFlow(HashMap<Id, int[]> linkInFlow, HashMap<Id, int[]> linkOutFlow) {

		Set<Id> linkIds = new TreeSet<Id>();
		linkIds.addAll(linkInFlow.keySet());
		linkIds.addAll(linkOutFlow.keySet());
		
		HashMap<Id, int[]> result = new HashMap<Id, int[]>();
		for (Id linkId : linkIds) {

			// consistency checks
			boolean hasInflowEntry = linkInFlow.containsKey(linkId);
			boolean hasOutflowEntry = linkOutFlow.containsKey(linkId);
			
			if (!hasInflowEntry) {
				System.out.println("Found outflow but no inflow values for link " + linkId.toString());
				continue;
			} else if (!hasOutflowEntry) {
				System.out.println("Found inflow but no outflow values for link " + linkId.toString());
				continue;
			}
			
			int[] inflowBins = linkInFlow.get(linkId);
			int[] outflowBins = linkOutFlow.get(linkId);			
			int[] deltaflowBins = new int[inflowBins.length];
			
			result.put(linkId, deltaflowBins);// put them into result arrays
			for (int i = 0; i < inflowBins.length; i++) {
				int delta = inflowBins[i] - outflowBins[i];
				if (delta < 0) throw new RuntimeException("Delta < 0 found on link " + linkId.toString());
				deltaflowBins[i] = delta;
			}
		}

		return result;
	}

	public static HashMap<Id, double[]> calculateDensity(HashMap<Id, int[]> deltaFlow, Map<Id, ? extends Link> links) {

		HashMap<Id, double[]> density = new HashMap<Id, double[]>();

		for (Id linkId : deltaFlow.keySet()) {
			density.put(linkId, null);
		}

		for (Id linkId : density.keySet()) {

			int[] deltaflowBins = deltaFlow.get(linkId);
			double[] densityBins = new double[deltaflowBins.length];
			Link link = links.get(linkId);
			
			for (int i = 0; i < densityBins.length; i++) {
				densityBins[i] = deltaflowBins[i] / (link.getLength() * link.getNumberOfLanes() / 1000);
			}

			density.put(linkId, densityBins);
			deltaFlow.remove(linkId);
		}

		return density;
	}

	public static void printDensity(HashMap<Id, double[]> density,
			Map<Id, ? extends Link> links) { // print
		for (Id linkId : density.keySet()) {
			double[] bins = density.get(linkId);

			Link link = links.get(linkId);

			boolean hasTraffic = false;
			for (int i = 0; i < bins.length; i++) {
				if (bins[i] != 0.0) {
					hasTraffic = true;
					break;
				}
			}

			if (hasTraffic) {
				System.out.print(linkId + " - " + link.getCoord() + ": \t");

				for (int i = 0; i < bins.length; i++) {
					System.out.print(bins[i] + "\t");
				}

				System.out.println();
			}
		}
	}

}
