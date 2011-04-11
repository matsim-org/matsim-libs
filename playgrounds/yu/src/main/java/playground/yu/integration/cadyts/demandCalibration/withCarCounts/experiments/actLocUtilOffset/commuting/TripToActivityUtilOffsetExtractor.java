/* *********************************************************************** *
 * project: org.matsim.*
 * TripToActivityUtilOffsetExtractor.java
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
package playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.actLocUtilOffset.commuting;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
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
import playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.actLocUtilOffset.TripUtilOffsetExtractor;
import playground.yu.integration.cadyts.demandCalibration.withCarCounts.utils.qgis.Trips2QGISNormal;
import playground.yu.utils.qgis.X2QGIS;
import cadyts.utilities.misc.DynamicData;

/**
 * extract trips to one activity
 * 
 * @author yu
 * 
 */
public class TripToActivityUtilOffsetExtractor extends TripUtilOffsetExtractor {

	private String activityPrefix;

	public TripToActivityUtilOffsetExtractor(Counts counts, Network net,
			DynamicData<Link> linkUtilOffsets, double gridLength,
			int calibrationStartTime, int calibrationEndTime,
			String activityPrefix) {
		super(counts, net, linkUtilOffsets, gridLength, calibrationStartTime,
				calibrationEndTime);
		this.activityPrefix = activityPrefix;
	}

	protected void convert2TripInternal(TripEvents tripEvents) {
		List<LinkEnterEvent> enters = tripEvents.getEnters();
		if (enters != null) {

			ActivityEndEvent tripBegins = tripEvents.getTripBegins();
			Coord orig = getGridCenterCoord(tripBegins.getLinkId());
			ActivityStartEvent tripEnds = tripEvents.getTripEnds();
			Coord dest = getGridCenterCoord(tripEnds.getLinkId());
			Tuple<Coord, Coord> odRelationship = new Tuple<Coord, Coord>(orig,
					dest);
			TripsWithUtilOffset trips = tripsWithUtilOffsetMap
					.get(odRelationship);
			if (trips == null) {
				trips = new TripsWithUtilOffset(odRelationship);
			}
			double utilOffset = 0;

			double lastTime = getTimeStep(enters.get(enters.size() - 1)
					.getTime());
			if (lastTime >= caliStartTime && lastTime <= caliEndTime) {
				for (LinkEnterEvent enter : enters) {
					Id linkId = enter.getLinkId();
					int timeStep = getTimeStep(enter.getTime());
					// if (timeStep >= caliStartTime && timeStep <= caliEndTime)
					// {
					utilOffset += getLinkUtilOffset(linkId, timeStep);
					// }
				}
			}

			if (utilOffset != 0d || involveZeroOffset) {
				trips.addTrip(utilOffset);
				trips.setTimeRange(new int[] {
						getTimeStep(tripBegins.getTime()),
						getTimeStep(tripEnds.getTime()) });
				tripsWithUtilOffsetMap.put(odRelationship, trips);
			}
		}
	}

	/** only trips to activity with activityPrefix is converted and saved here */
	@Override
	protected void convert2Trip(TripEvents tripEvents) {
		if (activityPrefix == null) {
			convert2TripInternal(tripEvents);
			return;
		}
		if (tripEvents.getTripEnds().getActType().startsWith(activityPrefix)) {
			convert2TripInternal(tripEvents);
		}
	}

	public static void main(String args[]) {
		String linkOffsetUtilOffsetFilename = "test/DestinationUtilOffset/1000.linkCostOffsets.xml"//
		, networkFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml"//
		, countsFilename = "../schweiz-ivtch-SVN/baseCase/counts/countsIVTCH.xml"//
		, eventsFilename = "test/DestinationUtilOffset/1000.events.txt.gz"//
		, outputFilenameBase = "test/DestinationUtilOffset2/9/1000.destUtiloffset.9."//
		;

		int arStartTime = 9, arEndTime = 9, lowerLimit = 50;
		double gridLength = 1000d;

		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		BseLinkCostOffsetsXMLFileIO utilOffsetIO = new BseLinkCostOffsetsXMLFileIO(
				net);
		DynamicData<Link> linkUtilOffsets = utilOffsetIO
				.read(linkOffsetUtilOffsetFilename);
		TripUtilOffsetExtractor tuoExtractor = new TripToActivityUtilOffsetExtractor(
				counts, net, linkUtilOffsets, gridLength, arStartTime,
				arEndTime, /**/null/**/);
		tuoExtractor.setInvolveZeroOffset(true);/*
												 * false by using rigorous
												 * version
												 */
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		// /////////////////////////////////
		events.addHandler(tuoExtractor);
		// /////////////////////////////////
		new MatsimEventsReader(events).readFile(eventsFilename);

		Trips2QGISNormal t2q = new Trips2QGISNormal(X2QGIS.ch1903, tuoExtractor
				.getTripsWithUtilOffsetMap(), 50);/*
												 * or with rigorous version
												 * (normal distribution, avg>1
												 * or 2 standard deviation
												 */
		t2q.setBarWidthScale(10);
		t2q
				.writeShapeFile(outputFilenameBase
						+ "tripsWithUtilOffset.toAll.shp");
	}

}
