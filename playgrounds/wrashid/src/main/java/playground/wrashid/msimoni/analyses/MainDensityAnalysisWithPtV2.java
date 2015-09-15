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
//		String networkFile = "H:/thesis/output_no_pricing_v3_subtours_bugfix/output_network.xml.gz";
//		String eventsFile =  "H:/thesis/output_no_pricing_v3_subtours_bugfix/ITERS/it.50/50.events.xml.gz";
		String networkFile = "D:/Users/Christoph/workspace/matsim/mysimulations/FundamentalDiagram/output_network.xml.gz";
		String eventsFile =  "D:/Users/Christoph/workspace/matsim/mysimulations/FundamentalDiagram/it.50/50.events_jdeqsim.xml.gz";
		
		Coord center = null; // center=null means use all links
		int binSizeInSeconds = 300;	// 5 minute bins

		// String
		// networkFile="C:/Users/Nan/Desktop/For matsim/matsim-0.1.1/examples/equil/network.xml";
		// String
		// eventsFile="C:/Users/Nan/Desktop/For matsim/matsim-0.1.1/output/equil/ITERS/it.5/5.events.txt.gz";
		// Coord center=new CoordImpl(0,0);
		// boolean isOldEventFile=false;

		double radiusInMeters = 1500;
		double length = 50.0;

		// input/set center and radius
//		Map<Id, ? extends Link> links = NetworkReadExample.getNetworkLinks(networkFile, center, radiusInMeters);
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		//center = scenario.getNetwork().getNodes().get(scenario.createId("17560000113841FT")).getCoord();
		center = new Coord(682548.0, 247525.5);
		
		Map<Id<Link>, Link> links = LinkSelector.selectLinks(scenario.getNetwork(), center, radiusInMeters, length);
		
		InFlowInfoAcuumulatorWithPt inflowHandler = new InFlowInfoAcuumulatorWithPt(links, binSizeInSeconds);
		OutFlowInfoAccumulatorWithPt outflowHandler = new OutFlowInfoAccumulatorWithPt(links, binSizeInSeconds);

		int avgBinSize = 5;	// calculate a value every 5 seconds
		InFlowInfoAcuumulatorWithPt avgInflowHandler = new InFlowInfoAcuumulatorWithPt(links, avgBinSize);
		OutFlowInfoAccumulatorWithPt avgOutflowHandler = new OutFlowInfoAccumulatorWithPt(links, avgBinSize);
				
		inflowHandler.reset(0);
		outflowHandler.reset(0);
		avgInflowHandler.reset(0);
		avgOutflowHandler.reset(0);

		EventsManager events = EventsUtils.createEventsManager();

		events.addHandler(inflowHandler); // add handler
		events.addHandler(outflowHandler);
		events.addHandler(avgInflowHandler); // add handler
		events.addHandler(avgOutflowHandler);
		
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventsFile);

		HashMap<Id, int[]> linkInFlow = inflowHandler.getLinkInFlow();
		HashMap<Id, int[]> linkOutFlow = outflowHandler.getLinkOutFlow();

		System.out.println("Entries from the inflow handler:  " + linkInFlow.size());
		System.out.println("Entries from the outflow handler: " + linkOutFlow.size());
		
		HashMap<Id, int[]> deltaFlow = deltaFlow(linkInFlow, linkOutFlow);
		HashMap<Id<Link>, double[]> density = calculateDensity(deltaFlow, links);

		System.out.println("inflows-----------------------------------------------");
		printFlow(linkInFlow, links);

		System.out.println("outflows-----------------------------------------------");
		printFlow(linkOutFlow, links);

		
		System.out.println("density-----------------------------------------------");
		printDensity(density, links);
		
		HashMap<Id, int[]> avgLinkInFlow = avgInflowHandler.getLinkInFlow();
		HashMap<Id, int[]> avgLinkOutFlow = avgOutflowHandler.getLinkOutFlow();

		HashMap<Id, int[]> avgDeltaFlow = deltaFlow(avgLinkInFlow, avgLinkOutFlow);
		int valuesPerBin = binSizeInSeconds / avgBinSize;
		if (binSizeInSeconds % avgBinSize != 0) throw new RuntimeException("binSize in seconds % binSize for averaging is != 0");
		HashMap<Id<Link>, double[]> avgDensity = calculateAverageDensity(calculateDensity(avgDeltaFlow, links), valuesPerBin);
		
		System.out.println("avg density-----------------------------------------------");
		printDensity(avgDensity, links);
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

	public static HashMap<Id<Link>, double[]> calculateDensity(HashMap<Id, int[]> deltaFlow, Map<Id<Link>, ? extends Link> links) {

		HashMap<Id<Link>, double[]> density = new HashMap<Id<Link>, double[]>();

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

	public static HashMap<Id<Link>, double[]> calculateAverageDensity(HashMap<Id<Link>, double[]> density, int valuesPerBin) {

		HashMap<Id<Link>, double[]> avgDensity = new HashMap<Id<Link>, double[]>();
		
		for (Id linkId : density.keySet()) {

			double[] linkDensity = density.get(linkId);
			
			double[] avgLinkDensity = new double[(int) Math.ceil(linkDensity.length / valuesPerBin) + 1];
			avgDensity.put(linkId, avgLinkDensity);
			
			int index = 0;
			double sumDensity = 0.0;
			for (int i = 0; i < linkDensity.length; i++) {

				sumDensity += linkDensity[i];
				
				// if all entries of the time bin have been processed
				if ((i+1) % valuesPerBin == 0) {
					avgLinkDensity[index] = sumDensity / valuesPerBin;
					sumDensity = 0.0;
					index++;
				}
			}
		}

		return avgDensity;
	}
	
	public static void printDensity(HashMap<Id<Link>, double[]> density,
			Map<Id<Link>, ? extends Link> links) { // print
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
				Coord coord = link.getCoord();
				System.out.print(linkId.toString() + " : \t");
				System.out.print(coord.getX() + "\t");
				System.out.print(coord.getY() + "\t");
				
				for (int i = 0; i < bins.length; i++) {
					System.out.print(bins[i] + "\t");
				}

				System.out.println();
			}
		}
	}
	
	public static void printFlow(HashMap<Id, int[]> flow,
			Map<Id<Link>, ? extends Link> links) { // print
		for (Id linkId : flow.keySet()) {
			int[] bins = flow.get(linkId);

			Link link = links.get(linkId);

			boolean hasTraffic = false;
			for (int i = 0; i < bins.length; i++) {
				if (bins[i] != 0.0) {
					hasTraffic = true;
					break;
				}
			}

			if (hasTraffic) {
				Coord coord = link.getCoord();
				System.out.print(linkId.toString() + " : \t");
				System.out.print(coord.getX() + "\t");
				System.out.print(coord.getY() + "\t");
				
				for (int i = 0; i < bins.length; i++) {
					System.out.print(bins[i] + "\t");
				}

				System.out.println();
			}
		}
	}

}
