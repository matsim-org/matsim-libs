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
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

import playground.yu.integration.cadyts.demandCalibration.withCarCounts.BseLinkCostOffsetsXMLFileIO;
import playground.yu.integration.cadyts.demandCalibration.withCarCounts.utils.qgis.ActivityLocationUtilOffset2QGIS;
import cadyts.utilities.misc.DynamicData;

/**
 * shows the Utility Offset of activity location for originating traffic
 * 
 * @author yu
 * 
 */
public class OriginationUtilOffsetExtractor extends
		ActivityLocationUtilOffsetExtractor implements
		AgentDepartureEventHandler {
	private Map<Integer/* timeStep */, List<Tuple<Id/* agent */, Coord/* gridCenter */>>> departureTime_agent_locs = new HashMap<Integer, List<Tuple<Id, Coord>>>();

	// /*
	// * If there isn 't ended activity before linkEntering , linkEntering
	// events
	// * don 't make sense
	// */
	// private Map<Id/* agentId */, ActivityEndEvent> endedActs = new
	// HashMap<Id, ActivityEndEvent>();

	public OriginationUtilOffsetExtractor(Network net, Counts counts,
			DynamicData<Link> linkUtilOffsets, int caliStartTime,
			int caliEndTime, int lowerLimit, double gridLength) {
		super(net, counts, linkUtilOffsets, caliStartTime, caliEndTime,
				lowerLimit, gridLength);
	}

	public void handleEvent(AgentDepartureEvent event) {
		Id agentId = event.getPersonId();
		Id linkId = event.getLinkId();
		int timeStep = getTimeStep(event.getTime());

		if (timeStep >= caliStartTime && timeStep <= caliEndTime) {
			List<Tuple<Id, Coord>> agent_locs = departureTime_agent_locs
					.get(timeStep);
			if (agent_locs == null) {
				agent_locs = new ArrayList<Tuple<Id, Coord>>();
				departureTime_agent_locs.put(timeStep, agent_locs);
			}
			agent_locs.add(new Tuple<Id, Coord>(agentId,
					getGridCenterCoord(linkId)));
		}
	}

	@Override
	public void locatePlanUtilOffsets() {
		for (Integer timeStep : departureTime_agent_locs.keySet()) {
			List<Tuple<Id, Coord>> agent_locs = departureTime_agent_locs
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
		String linkOffsetUtilOffsetFilename = "test/DestinationUtilOffset/1000.linkCostOffsets.xml"//
		, networkFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml"//
		, countsFilename = "../schweiz-ivtch-SVN/baseCase/counts/countsIVTCH.xml"//
		, eventsFilename = "test/DestinationUtilOffset/1000.events.txt.gz"//
		, outputFilenameBase = "test/DestinationUtilOffset2/1000.origPlanUtiloffset."//
		;

		int arStartTime = 9, arEndTime = 9, lowerLimit = 50;

		Scenario scenario = new ScenarioImpl();
		Network net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		BseLinkCostOffsetsXMLFileIO utilOffsetIO = new BseLinkCostOffsetsXMLFileIO(
				net);
		DynamicData<Link> linkUtilOffsets = utilOffsetIO
				.read(linkOffsetUtilOffsetFilename);

		ActivityLocationUtilOffsetExtractor aluoe = new OriginationUtilOffsetExtractor(
				net, counts, linkUtilOffsets, arStartTime, arEndTime,
				lowerLimit, 1000d);

		EventsManager events = new EventsManagerImpl();
		// /////////////////////////////////
		events.addHandler(aluoe);
		// /////////////////////////////////
		new MatsimEventsReader(events).readFile(eventsFilename);
		aluoe.locatePlanUtilOffsets();
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
