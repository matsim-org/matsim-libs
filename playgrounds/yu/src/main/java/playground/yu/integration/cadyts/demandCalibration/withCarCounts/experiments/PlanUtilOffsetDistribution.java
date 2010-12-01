/* *********************************************************************** *
 * project: org.matsim.*
 * DestinationTripUtilOffsetDistribution.java
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
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
public class PlanUtilOffsetDistribution extends
		ActivityLocationUtilOffsetExtractor {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// run9(args);
		run(args);
	}

	private static void run9(String[] args) {
		String linkOffsetUtilOffsetFilename = "test/DestinationUtilOffset/1000.linkCostOffsets.xml"//
		, networkFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml"//
		, countsFilename = "../schweiz-ivtch-SVN/baseCase/counts/countsIVTCH.xml"//
		, eventsFilename = "test/DestinationUtilOffset/1000.events.txt.gz"//
		, outputFilenameBase = "test/DestinationUtilOffset2/1000.PlanUtiloffsetDistribution.9."//
		;

		int arStartTime = 9, arEndTime = 9, lowerLimit = 50;
		double interval = 0.25;

		Scenario scenario = new ScenarioImpl();
		Network net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		BseLinkCostOffsetsXMLFileIO utilOffsetIO = new BseLinkCostOffsetsXMLFileIO(
				net);
		DynamicData<Link> linkUtilOffsets = utilOffsetIO
				.read(linkOffsetUtilOffsetFilename);

		PlanUtilOffsetDistribution aluoe = new PlanUtilOffsetDistribution(net,
				counts, linkUtilOffsets, arStartTime, arEndTime, lowerLimit,
				1000d);

		EventsManager events = new EventsManagerImpl();
		// /////////////////////////////////
		events.addHandler(aluoe);
		// /////////////////////////////////
		new MatsimEventsReader(events).readFile(eventsFilename);

		// aluoe.output(outputFilenameBase);
		aluoe.fillPlanUtilOffsets();
		aluoe.write(outputFilenameBase, interval);

	}

	protected List<Double> planUtilOffsetList = new ArrayList<Double>();
	private Map<Integer, Set<Coord>> time_centerCoords = null;
	private int timeBinSize = 3600;

	// for grid version

	public PlanUtilOffsetDistribution(Network net, Counts counts,
			DynamicData<Link> linkUtilOffsets, int caliStartTime,
			int caliEndTime, int lowerLimit, double gridLength) {
		super(net, counts, linkUtilOffsets, caliStartTime, caliEndTime,
				lowerLimit, gridLength);
	}

	/**
	 * @param net
	 * @param counts
	 * @param linkUtilOffsets
	 * @param caliStartTime
	 * @param caliEndTime
	 * @param lowerLimit
	 * @param gridLength
	 * @param time_centerCoords
	 *            contains information about timeBin and grid center coordinate
	 */
	public PlanUtilOffsetDistribution(Network net, Counts counts,
			DynamicData<Link> linkUtilOffsets, int caliStartTime,
			int caliEndTime, int lowerLimit, double gridLength,
			Map<Integer, Set<Coord>> time_centerCoords) {
		this(net, counts, linkUtilOffsets, caliStartTime, caliEndTime,
				lowerLimit, gridLength);
		this.time_centerCoords = time_centerCoords;
	}

	public List<Double> getPlanUtilOffsetList() {
		return planUtilOffsetList;
	}

	public void setTimeBinSize(int timeBinSize) {
		this.timeBinSize = timeBinSize;
	}

	public void write(String filenameBase, double interval) {
		DistributionCreator creator = new DistributionCreator(
				planUtilOffsetList, interval);
		creator.write(filenameBase + "total.log");
		creator.createChartPercent(filenameBase + "total.png", "",
				"value of daily journey utility offset",
				"fraction of daily journeys with utility offset (interval = "
						+ interval + ")");

		List<Double> nonzeroPlanUtilOffsetList = new ArrayList<Double>();
		for (Double d : planUtilOffsetList) {
			if (d != 0d) {
				nonzeroPlanUtilOffsetList.add(d);
			}
		}

		DistributionCreator creator2 = new DistributionCreator(
				nonzeroPlanUtilOffsetList, interval);
		creator2.createChartPercent(filenameBase + "totalUn0.png", "",
				"\"non-zero\" value of daily journey utility offset",
				"fraction of daily journeys with utility offset (interval = "
						+ interval + ")");

	}

	public void handleEvent(LinkEnterEvent event) {
		Id linkId = event.getLinkId();
		Id agentId = event.getPersonId();
		int timeStep = getTimeStep(event.getTime());

		if (timeStep >= caliStartTime && timeStep <= caliEndTime) {
			double linkUtilOffset = getLinkUtilOffset(linkId, timeStep);

			Double legUtilOffset = tmpAgentLegUtilOffsets.get(agentId);
			if (legUtilOffset == null) {
				tmpAgentLegUtilOffsets.put(agentId, linkUtilOffset);
			} else {
				tmpAgentLegUtilOffsets.put(agentId, legUtilOffset
						+ linkUtilOffset);
			}
		}
	}

	public void fillPlanUtilOffsets() {
		planUtilOffsetList.addAll(tmpAgentLegUtilOffsets.values());
	}

	private static void run(String[] args) {
		String linkOffsetUtilOffsetFilename = "test/DestinationUtilOffset/1000.linkCostOffsets.xml"//
		, networkFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml"//
		, countsFilename = "../schweiz-ivtch-SVN/baseCase/counts/countsIVTCH.xml"//
		, eventsFilename = "test/DestinationUtilOffset/1000.events.txt.gz"//
		, outputFilenameBase = "test/DestinationUtilOffset2/1000.PlanUtiloffsetDistribution."//
		;

		int arStartTime = 7, arEndTime = 20, lowerLimit = 50;
		double interval = 0.25;

		Scenario scenario = new ScenarioImpl();
		Network net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		BseLinkCostOffsetsXMLFileIO utilOffsetIO = new BseLinkCostOffsetsXMLFileIO(
				net);
		DynamicData<Link> linkUtilOffsets = utilOffsetIO
				.read(linkOffsetUtilOffsetFilename);

		PlanUtilOffsetDistribution aluoe = new PlanUtilOffsetDistribution(net,
				counts, linkUtilOffsets, arStartTime, arEndTime, lowerLimit,
				1000d);

		EventsManager events = new EventsManagerImpl();
		// /////////////////////////////////
		events.addHandler(aluoe);
		// /////////////////////////////////
		new MatsimEventsReader(events).readFile(eventsFilename);

		// aluoe.output(outputFilenameBase);
		aluoe.fillPlanUtilOffsets();
		aluoe.write(outputFilenameBase, interval);

	}
}
