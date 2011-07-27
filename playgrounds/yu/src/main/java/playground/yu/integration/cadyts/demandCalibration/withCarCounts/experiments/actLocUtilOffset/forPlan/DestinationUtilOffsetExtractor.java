/* *********************************************************************** *
 * project: org.matsim.*
 * DestinationUtilOffsetExtractor.java
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
package playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.actLocUtilOffset.forPlan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

import playground.yu.integration.cadyts.demandCalibration.withCarCounts.BseLinkCostOffsetsXMLFileIO;
import playground.yu.integration.cadyts.demandCalibration.withCarCounts.utils.qgis.ActivityLocationUtilOffset2QGIS;
import cadyts.utilities.misc.DynamicData;

/**
 * shows the Utility Offset of activity location for terminating traffic
 *
 * @author yu
 *
 */
public class DestinationUtilOffsetExtractor extends
		ActivityLocationUtilOffsetExtractor implements AgentArrivalEventHandler {
	private Map<Integer/* timeStep */, List<Tuple<Id/* agent */, Coord/* gridCenter */>>> arrivingTime_agent_locs = new HashMap<Integer, List<Tuple<Id, Coord>>>();

	public DestinationUtilOffsetExtractor(Network net, Counts counts,
			DynamicData<Link> linkUtilOffsets, int caliStartTime,
			int caliEndTime, int lowerLimit, double gridLength) {
		super(net, counts, linkUtilOffsets, caliStartTime, caliEndTime,
				lowerLimit, gridLength);
	}

	// public void handleEvent(ActivityStartEvent event) {
	// internalHandleEvent(event);
	// }

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		Id agentId = event.getPersonId();
		Id linkId = event.getLinkId();
		int timeStep = getTimeStep(event.getTime());
		if (timeStep >= caliStartTime && timeStep <= caliEndTime) {
			List<Tuple<Id, Coord>> agent_locs = arrivingTime_agent_locs
					.get(timeStep);
			if (agent_locs == null) {
				agent_locs = new ArrayList<Tuple<Id, Coord>>();
				arrivingTime_agent_locs.put(timeStep, agent_locs);
			}
			agent_locs.add(new Tuple<Id, Coord>(agentId,
					getGridCenterCoord(linkId)));
		}
	}

	@Override
	public void locatePlanUtilOffsets() {

		for (Integer timeStep : arrivingTime_agent_locs.keySet()) {
			List<Tuple<Id, Coord>> agent_locs = arrivingTime_agent_locs
					.get(timeStep);
			for (Tuple<Id, Coord> agent_loc : agent_locs) {
				Double planUtilOffset = tmpAgentPlanUtilOffsets.get(agent_loc
						.getFirst()/* agentId */);
				if (planUtilOffset != null) {
					addGridUtilOffset(timeStep, agent_loc.getSecond()/*
																	 * grid
																	 * center
																	 * location
																	 */,
							planUtilOffset);
				}
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String linkOffsetUtilOffsetFilename = "../../runs-svn/run1300/sp/1000.linkCostOffsets.xml"//
			, networkFilename = "D:/Daten/work/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch-osm.xml"//
			, countsFilename = "D:/Daten/work/shared-svn/studies/schweiz-ivtch/baseCase/counts/countsIVTCH.xml"//
			, eventsFilename = "../../runs-svn/run1300/sp/1000.events.txt.gz"//
		, outputFilenameBase = "test/output/test/1000.destPlanUtiloffset."//
		;

		int arStartTime = 9, arEndTime = 9, lowerLimit = 50;

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		BseLinkCostOffsetsXMLFileIO utilOffsetIO = new BseLinkCostOffsetsXMLFileIO(
				net);
		DynamicData<Link> linkUtilOffsets = utilOffsetIO
				.read(linkOffsetUtilOffsetFilename);

		ActivityLocationUtilOffsetExtractor aluoe = new DestinationUtilOffsetExtractor(
				net, counts, linkUtilOffsets, arStartTime, arEndTime,
				lowerLimit, 1000d);

		EventsManager events = EventsUtils.createEventsManager();
		// /////////////////////////////////
		events.addHandler(aluoe);
		// /////////////////////////////////
		new MatsimEventsReader(events).readFile(eventsFilename);

		((DestinationUtilOffsetExtractor) aluoe).locatePlanUtilOffsets();
		aluoe.output(outputFilenameBase);

		Map<Integer, Map<Coord, Tuple<Integer, Double>>> gridUtilOffsets = aluoe
				.getGridUtilOffsets();
		for (Entry<Integer, Map<Coord, Tuple<Integer, Double>>> timeGridUtilOffsetsPair : gridUtilOffsets
				.entrySet()) {
			if (timeGridUtilOffsetsPair.getValue().size() > 0) {
				ActivityLocationUtilOffset2QGIS aluo2qgis = new ActivityLocationUtilOffset2QGIS(
						scenario, ch1903, 1000d, timeGridUtilOffsetsPair
								.getValue());
				aluo2qgis.writeShapeFile(outputFilenameBase + "grid."
						+ timeGridUtilOffsetsPair.getKey() + ".shp");
			}
		}
	}

}
