/* *********************************************************************** *
 * project: org.matsim.*
 * DestTripUtilOffset_perfRatio.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

import playground.yu.integration.cadyts.demandCalibration.withCarCounts.BseLinkCostOffsetsXMLFileIO;
import playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.ActivityTimeDistribution.RealActivityTimes;
import playground.yu.utils.container.Collection2Array;
import playground.yu.utils.io.SimpleWriter;
import cadyts.utilities.misc.DynamicData;

/**
 * creates scatter plots about: TripUtilityOffsets <-> performing time /
 * typicalDuration
 * 
 * @author yu
 * 
 */
public class DestTripUtilOffset_perfRatio extends
		DestinationTripUtilOffsetDistribution implements
		ActivityEndEventHandler {
	protected final CharyparNagelScoringParameters params;
	private Map<Id/* agentId */, Double> agent_startTimes, agent_tripUtilOffsets;
	private Map<String/**/, List<Double>> tripUtilOffsets, perfRatios;

	public DestTripUtilOffset_perfRatio(Network net, Counts counts,
			DynamicData<Link> linkUtilOffsets, int caliStartTime,
			int caliEndTime, int lowerLimit, double gridLength,
			CharyparNagelScoringParameters params) {
		super(net, counts, linkUtilOffsets, caliStartTime, caliEndTime,
				lowerLimit, gridLength);
		this.params = params;
		agent_startTimes = new HashMap<Id, Double>();
		agent_tripUtilOffsets = new HashMap<Id, Double>();
		tripUtilOffsets = new HashMap<String, List<Double>>();
		perfRatios = new HashMap<String, List<Double>>();
	}

	public void handleEvent(ActivityStartEvent event) {
		if (event != null) {
			Id agentId = event.getPersonId();
			Double legUtilOffset = tmpAgentLegUtilOffsets./**/remove(agentId)/**/;
			if (legUtilOffset != null) {
				agent_startTimes.put(agentId, event.getTime());
				agent_tripUtilOffsets.put(agentId, legUtilOffset);
			}
		}
	}

	public void handleEvent(ActivityEndEvent event) {
		if (event != null) {
			Id agentId = event.getPersonId();
			Double arrivalTime = agent_startTimes.remove(agentId), tripUtilOffset = agent_tripUtilOffsets
					.remove(agentId);
			if (arrivalTime != null && tripUtilOffset != null) {
				double departureTime = event.getTime();
				String type = event.getActType();

				RealActivityTimes realActTimes = new RealActivityTimes(
						arrivalTime, departureTime, params.utilParams.get(type));
				if (realActTimes.getRealPerformingTime() != 0d
						&& tripUtilOffset != 0d) {
					String shortType = type.substring(0, 1);
					// performingRatio
					double perfRatio = realActTimes.getPerformingRatio();
					List<Double> perfRatiosOfAct = perfRatios.get(shortType);
					if (perfRatiosOfAct == null) {
						perfRatiosOfAct = new ArrayList<Double>();
						perfRatios.put(shortType, perfRatiosOfAct);
					}
					perfRatiosOfAct.add(perfRatio);

					// tripUtilOffsets
					List<Double> tripUtilOffsetsOfAct = tripUtilOffsets
							.get(shortType);
					if (tripUtilOffsetsOfAct == null) {
						tripUtilOffsetsOfAct = new ArrayList<Double>();
						tripUtilOffsets.put(shortType, tripUtilOffsetsOfAct);
					}
					tripUtilOffsetsOfAct.add(tripUtilOffset);
				}
			}// if (this.agent_startTimes.containsKey(agentId))
		}// if (event != null)
	}

	public void output(String filenameBase) {
		SimpleWriter writer = new SimpleWriter(filenameBase
				+ "TripUtilOffset_perfRatio.log");
		// XYScatterChart chart = new XYScatterChart(
		// "TripUtilityOffsets <-> performingRatios (performing time / activity typical duration)"/*
		// title */,
		// "TripUtilityOffset"/* xAxisLabel */, "performingRatio"/* yAxisLabel
		// */);

		for (String shortType : tripUtilOffsets.keySet()) {
			writer
					.writeln(shortType
							+ "\ntripUtilityOffset\tperformingRatio (performing time / activity typical duration)");

			double[] tripUtilOffsetArray /* x-axis */= Collection2Array
					.toArrayFromDouble(tripUtilOffsets.get(shortType));
			double[] perfRatioArray/* y-axis */= Collection2Array
					.toArrayFromDouble(perfRatios.get(shortType));
			XYScatterChart chart = new XYScatterChart(
					"TripUtilityOffsets <-> performingRatios (performing time / activity typical duration) for activity "
							+ shortType/* title */,
					"TripUtilityOffset of activity " + shortType/* xAxisLabel */,
					"performingRatio of activity " + shortType/* yAxisLabel */);
			chart.addSeries(shortType, tripUtilOffsetArray, perfRatioArray);
			chart.saveAsPng(filenameBase + "TripUtilOffset_perfRatio."
					+ shortType + ".png", 1024, 768);
			if (tripUtilOffsetArray.length != perfRatioArray.length) {
				throw new RuntimeException(
						"activity\t"
								+ shortType
								+ "\tshould have same size of tripUtilOffsetArray and perfRatioArray");
			}

			for (int i = 0; i < tripUtilOffsetArray.length; i++) {
				writer.writeln(tripUtilOffsetArray[i] + "\t"
						+ perfRatioArray[i]);
			}
			writer.writeln("---------------------------");
		}

		writer.close();
		// chart.saveAsPng(filenameBase + "TripUtilOffset_perfRatio.png", 1024,
		// 768);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String linkOffsetUtilOffsetFilename = "test/DestinationUtilOffset/1000.linkCostOffsets.xml"//
		, networkFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml"//
		, countsFilename = "../schweiz-ivtch-SVN/baseCase/counts/countsIVTCH.xml"//
		, eventsFilename = "test/DestinationUtilOffset/1000.events.txt.gz"//
		, outputFilenameBase = "test/DestinationUtilOffset2/1000.destUtiloffset."//
		, configFilename = "test/DestinationUtilOffset/analysis.xml"//
		;

		CharyparNagelScoringParameters params;
		try {
			params = new CharyparNagelScoringParameters(ConfigUtils.loadConfig(
					configFilename).planCalcScore());
			int arStartTime = 7, arEndTime = 20, lowerLimit = 50;
			// double interval = 0.25;

			Scenario scenario = new ScenarioImpl();
			Network net = scenario.getNetwork();
			new MatsimNetworkReader(scenario).readFile(networkFilename);

			Counts counts = new Counts();
			new MatsimCountsReader(counts).readFile(countsFilename);

			BseLinkCostOffsetsXMLFileIO utilOffsetIO = new BseLinkCostOffsetsXMLFileIO(
					net);
			DynamicData<Link> linkUtilOffsets = utilOffsetIO
					.read(linkOffsetUtilOffsetFilename);

			DestTripUtilOffset_perfRatio dtuoPr = new DestTripUtilOffset_perfRatio(
					net, counts, linkUtilOffsets, arStartTime, arEndTime,
					lowerLimit, 1000d, params);

			EventsManager events = new EventsManagerImpl();
			// /////////////////////////////////
			events.addHandler(dtuoPr);
			// /////////////////////////////////
			new MatsimEventsReader(events).readFile(eventsFilename);

			dtuoPr.output(outputFilenameBase);
			// aluoe.write(outputFilenameBase, interval);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
