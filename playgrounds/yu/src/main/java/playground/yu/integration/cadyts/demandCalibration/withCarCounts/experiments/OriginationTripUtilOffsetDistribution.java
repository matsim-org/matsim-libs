/* *********************************************************************** *
 * project: org.matsim.*
 * OriginationTripUtilOffsetDistribution.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

import playground.yu.integration.cadyts.demandCalibration.withCarCounts.BseLinkCostOffsetsXMLFileIO;
import playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.actLocUtilOffset.forLeg.ActivityLocationUtilOffsetExtractor;
import playground.yu.utils.io.DistributionCreator;
import cadyts.utilities.misc.DynamicData;

/**
 * @author yu
 * 
 */
public class OriginationTripUtilOffsetDistribution extends
		ActivityLocationUtilOffsetExtractor implements ActivityEndEventHandler,
		ActivityStartEventHandler {
	/*
	 * If there isn 't ended activity before linkEntering , linkEntering events
	 * don 't make sense
	 */
	private Map<Id/* agentId */, ActivityEndEvent> endedActs = new HashMap<Id, ActivityEndEvent>();
	private List<Double> origTripUtilOffsetList = new ArrayList<Double>();
	private Map<String/* actType */, List<Double>> actType_origTripUtilOffsetLists = new HashMap<String, List<Double>>();
	private Map<Integer, Set<Coord>> time_centerCoords = null;
	private Map<Integer/* timeBin */, Map<String/* actType */, List<Double>>> time_actType_origTripUtilOffsetLists = null;

	public OriginationTripUtilOffsetDistribution(Network net, Counts counts,
			DynamicData<Link> linkUtilOffsets, int caliStartTime,
			int caliEndTime, int lowerLimit, double gridLength) {
		super(net, counts, linkUtilOffsets, caliStartTime, caliEndTime,
				lowerLimit, gridLength);
	}

	public OriginationTripUtilOffsetDistribution(Network net, Counts counts,
			DynamicData<Link> linkUtilOffsets, int caliStartTime,
			int caliEndTime, int lowerLimit, double gridLength,
			Map<Integer, Set<Coord>> time_centerCoords) {
		this(net, counts, linkUtilOffsets, caliStartTime, caliEndTime,
				lowerLimit, gridLength);
		this.time_centerCoords = time_centerCoords;
		time_actType_origTripUtilOffsetLists = new HashMap<Integer, Map<String, List<Double>>>();
	}

	public List<Double> getOrigTripUtilOffsetList() {
		return origTripUtilOffsetList;
	}

	/**
	 * one trip ends
	 */
	public void handleEvent(ActivityStartEvent event) {// means a Leg is
		// finished
		Id agentId = event.getPersonId();
		// internalHandleEvent(this.endedActs.remove(agentId));
		if (event != null) {
			Double legUtilOffset = tmpAgentLegUtilOffsets
					./**/remove(agentId)/**/;
			ActivityEndEvent actEndEvent = endedActs.remove(agentId);
			if (legUtilOffset != null && actEndEvent != null/*
															 * an
															 * ActivityStartEvent
															 * can't exit
															 * without an
															 * ActivityEndEvent
															 * happend
															 */) {
				origTripUtilOffsetList.add(legUtilOffset);

				// actTypes
				String actType = actEndEvent.getActType().substring(0, 1);
				List<Double> list = actType_origTripUtilOffsetLists
						.get(actType);
				if (list == null) {
					list = new ArrayList<Double>();
					actType_origTripUtilOffsetLists.put(actType, list);
				}
				list.add(legUtilOffset);
			}
		}
	}

	public void handleEvent(ActivityEndEvent event) {
		endedActs.put(event.getPersonId(), event);
		/* beginning of new Leg */
	}

	public void write(String filenameBase, double interval) {
		DistributionCreator creator = new DistributionCreator(
				origTripUtilOffsetList, interval);
		creator.write(filenameBase + "total.log");
		creator.createChart(filenameBase + "total.png",
				"total origination trip Utility offset distribution",
				"value of origination trip utility offset",
				"number of trips with value in range (interval = " + interval
						+ ") of x");

		List<Double> origTripUtilOffsetList2 = new ArrayList<Double>();
		for (Double d : origTripUtilOffsetList) {
			if (d != 0d) {
				origTripUtilOffsetList2.add(d);
			}
		}

		DistributionCreator creator2 = new DistributionCreator(
				origTripUtilOffsetList2, interval);
		creator2
				.createChart(
						filenameBase + "totalUn0.png",
						"total \"unzero\" origination trip Utility offset distribution",
						"\"non-zero\" value of origination trip utility offset",
						"number of trips with value (!=0) in (interval = "
								+ interval + ") range of x");

		// all actType
		Map<String, List<Double>> localActType_origTripUtilOffsets = new HashMap<String, List<Double>>();
		for (String actType : actType_origTripUtilOffsetLists.keySet()) {
			List<Double> list = new ArrayList<Double>();
			localActType_origTripUtilOffsets.put(actType, list);
			for (Double d : actType_origTripUtilOffsetLists.get(actType)) {
				if (d != 0d) {
					list.add(d);
				}
			}
		}

		DistributionCreator creator3 = new DistributionCreator(
				localActType_origTripUtilOffsets, interval);
		creator3.write2(filenameBase + "totalNon0AllActs.log");
		creator3.createChart2(filenameBase + "totalNon0AllActs.png",
				"total nonzero origination trip Utility offset distribution",
				"value of origination trip utility offset",
				"number of trips with value (!= 0) in (interval = " + interval
						+ ") range of x", false);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String linkOffsetUtilOffsetFilename = "test/DestinationUtilOffset/1000.linkCostOffsets.xml"//
		, networkFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml"//
		, countsFilename = "../schweiz-ivtch-SVN/baseCase/counts/countsIVTCH.xml"//
		, eventsFilename = "test/DestinationUtilOffset/1000.events.txt.gz"//
		, outputFilenameBase = "test/DestinationUtilOffset2/1000.origUtiloffset."//
		;

		int arStartTime = 7, arEndTime = 20, lowerLimit = 50;

		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		BseLinkCostOffsetsXMLFileIO utilOffsetIO = new BseLinkCostOffsetsXMLFileIO(
				net);
		DynamicData<Link> linkUtilOffsets = utilOffsetIO
				.read(linkOffsetUtilOffsetFilename);

		OriginationTripUtilOffsetDistribution aluoe = new OriginationTripUtilOffsetDistribution(
				net, counts, linkUtilOffsets, arStartTime, arEndTime,
				lowerLimit, 1000d);

		EventsManager events = new EventsManagerImpl();
		// /////////////////////////////////
		events.addHandler(aluoe);
		// /////////////////////////////////
		new MatsimEventsReader(events).readFile(eventsFilename);

		// aluoe.output(outputFilenameBase);
		aluoe.write(outputFilenameBase, 0.1);
	}

}
