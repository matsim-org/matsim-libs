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
package playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.actLocUtilOffset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
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
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

import playground.yu.integration.cadyts.demandCalibration.withCarCounts.BseLinkCostOffsetsXMLFileIO;
import playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.actLocUtilOffset.forLeg.ActivityLocationUtilOffsetExtractor;
import playground.yu.integration.cadyts.demandCalibration.withCarCounts.utils.qgis.HomeUtilOffset2QGIS;
import playground.yu.utils.SimpleStatistics;
import playground.yu.utils.qgis.X2QGIS;
import cadyts.utilities.misc.DynamicData;

/**
 * shows the Utility Offset of activity location for terminating traffic
 * 
 * @author yu
 * 
 */
public class HomeLoc_TipUtilOffsetExtractor extends
		ActivityLocationUtilOffsetExtractor implements
		ActivityStartEventHandler, ActivityEndEventHandler, X2QGIS {
	private Map<Coord/* home location */, List<Double>/* trip utility offsets */> home_tripUtilOffsets = new HashMap<Coord, List<Double>>();
	private Map<Id/* agent */, Coord/* home location */> agentId_Coord = new HashMap<Id, Coord>();
	private Map<Coord/* home location */, Integer/*
												 * count of all trip utility
												 * offsets
												 */> home_tuoCnts = new HashMap<Coord, Integer>();

	public HomeLoc_TipUtilOffsetExtractor(Network net, Counts counts,
			DynamicData<Link> linkUtilOffsets, int caliStartTime,
			int caliEndTime, int lowerLimit, double gridLength) {
		super(net, counts, linkUtilOffsets, caliStartTime, caliEndTime,
				lowerLimit, gridLength);
	}

	public void handleEvent(ActivityStartEvent event) {// register trip utility
		// offset
		if (event != null) {
			Id agentId = event.getPersonId();
			Double legUtilOffset = tmpAgentLegUtilOffsets./**/remove(agentId)/**/;
			if (legUtilOffset != null) {
				Coord home = agentId_Coord.get(agentId);
				if (home != null) {
					Integer cnt = home_tuoCnts.get(home);
					if (cnt == null) {
						home_tuoCnts.put(home, 1);
					} else {
						home_tuoCnts.put(home, cnt + 1);
					}

					if (legUtilOffset != 0d) {// number of non-zero trip utility
						// offsets can be gotten by the size
						// of of List<Double>
						List<Double> tripUtilOffsets = home_tripUtilOffsets
								.get(home);
						if (tripUtilOffsets == null) {
							tripUtilOffsets = new ArrayList<Double>();
							home_tripUtilOffsets.put(home, tripUtilOffsets);
						}
						tripUtilOffsets.add(legUtilOffset);
					}

				}

			}
		}
	}

	public void handleEvent(ActivityEndEvent event) {
		// identify the home location Grids.
		if (event.getActType().startsWith("h")) {
			Coord gridCoord = getGridCenterCoord(event.getLinkId());
			agentId_Coord.put(event.getPersonId(), gridCoord);
		}
	}

	public Set<Coord> getHomeLocations() {
		return home_tripUtilOffsets.keySet();
	}

	public double getAvgUtilOffset(Coord homeLocCoord) {
		List<Double> tripUtilOffsets = home_tripUtilOffsets.get(homeLocCoord);
		return SimpleStatistics.average(tripUtilOffsets);
	}

	public double getStddevUtilOffset(Coord homeLocCoord) {
		return Math.sqrt(SimpleStatistics.variance(home_tripUtilOffsets
				.get(homeLocCoord)));
	}

	public int getNumOfTrips(Coord homeLocCoord) {
		return home_tuoCnts.get(homeLocCoord);
	}

	public int getNumOfTripsWithNon0UtilOffset(Coord homeLocCoord) {
		return home_tripUtilOffsets.get(homeLocCoord).size();
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
		;

		int arStartTime = 7, arEndTime = 20, lowerLimit = 1;

		Scenario scenario = new ScenarioImpl();
		Network net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		BseLinkCostOffsetsXMLFileIO utilOffsetIO = new BseLinkCostOffsetsXMLFileIO(
				net);
		DynamicData<Link> linkUtilOffsets = utilOffsetIO
				.read(linkOffsetUtilOffsetFilename);

		HomeLoc_TipUtilOffsetExtractor aluoe = new HomeLoc_TipUtilOffsetExtractor(
				net, counts, linkUtilOffsets, arStartTime, arEndTime,
				lowerLimit, 1000d);

		EventsManager events = new EventsManagerImpl();
		// /////////////////////////////////
		events.addHandler(aluoe);
		// /////////////////////////////////
		new MatsimEventsReader(events).readFile(eventsFilename);

		// aluoe.output(outputFilenameBase);

		HomeUtilOffset2QGIS aluo2qgis = new HomeUtilOffset2QGIS(scenario,
				ch1903, aluoe);
		aluo2qgis.writeShapeFile(outputFilenameBase + "grid.homeTUO.shp");

	}
}
