/* *********************************************************************** *
 * project: org.matsim.*
 * HomelocUtilOffsetExtractor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.actLocUtilOffset.forHomeLocation;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

import playground.yu.integration.cadyts.demandCalibration.withCarCounts.BseLinkCostOffsetsXMLFileIO;
import playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.DestinationTripUtilOffsetDistributionWithoutGrids;
import playground.yu.utils.io.SimpleWriter;
import utilities.math.BasicStatistics;
import utilities.misc.DynamicData;

/**
 * tries to
 * 
 * @author yu
 * 
 */
public class HomelocUtilOffsetDistribution implements ActivityEndEventHandler,
		LinkEnterEventHandler {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		run(args);
	}

	private static void run(String[] args) {
		String linkOffsetUtilOffsetFilename = "test/input/bln2pct/1536.2500.linkUtilOffsets.xml"//
		, networkFilename = "D:/Daten/work/shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml.gz"//
		, countsFilename = "D:/Daten/work/shared-svn/studies/countries/de/berlin/counts/iv_counts/vmz_di-do.xml"//
		, eventsFilename = "test/input/bln2pct/1536.2500.events.xml.gz"//
		, outputFilenameBase = "test/output/bln2pct/"// UOD.9.

		;

		int arStartTime = 7, arEndTime = 24;// lowerLimit = 50;
		double interval = 0.25;

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		Network net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		BseLinkCostOffsetsXMLFileIO utilOffsetIO = new BseLinkCostOffsetsXMLFileIO(
				net);
		DynamicData<Link> linkUtilOffsets = utilOffsetIO
				.read(linkOffsetUtilOffsetFilename);

		HomelocUtilOffsetDistribution hluod = new HomelocUtilOffsetDistribution(
				net, counts, linkUtilOffsets, arStartTime, arEndTime);

		EventsManager events = EventsUtils.createEventsManager();
		// /////////////////////////////////
		events.addHandler(hluod);
		// /////////////////////////////////
		new MatsimEventsReader(events).readFile(eventsFilename);

		// aluoe.write(outputFilenameBase, interval);

		hluod.output(outputFilenameBase + "home_dayPlan_UO.log");
	}

	private final Map<Id/* personId */, Coord/* home Location */> recordedPopHomeLocs;
	private final Network net;
	private final Map<Id/* personId */, Double/* dayUtilOffset */> dayUtilOffsets;
	private final DynamicData<Link> linkUOs;
	private static int timeBinSize = 3600;
	private final Map<Coord/* home location */, BasicStatistics> homeStats;

	public HomelocUtilOffsetDistribution(Network net, Counts counts,
			DynamicData<Link> linkUtilOffsets, int arStartTime, int arEndTime) {
		recordedPopHomeLocs = new HashMap<Id, Coord>();
		this.net = net;
		dayUtilOffsets = new HashMap<Id, Double>();
		linkUOs = linkUtilOffsets;
		homeStats = new HashMap<Coord, BasicStatistics>();
	}

	/**
	 * each home location has an AVG. dayUtilOffset and their size
	 * 
	 */
	private void calcDayUtilOffsetDistributionOfHome() {
		for (Id personId : dayUtilOffsets.keySet()) {
			Double duo = dayUtilOffsets.get(personId);
			if (duo > 1d) {
				Coord homeLoc = recordedPopHomeLocs.get(personId);
				BasicStatistics bs = homeStats.get(homeLoc);
				if (bs == null) {
					bs = new BasicStatistics();
					homeStats.put(homeLoc, bs);
				}
				bs.add(duo);
			}
		}

	}

	private double getLinkUtilOffset(Id linkId, int timeStep) {
		return linkUOs.getSum(net.getLinks().get(linkId), (timeStep - 1)
				* timeBinSize, timeStep * timeBinSize - 1);
	}

	/**
	 * registers the home location of each agent
	 */
	@Override
	public void handleEvent(ActivityEndEvent event) {
		Id personId = event.getPersonId();
		Coord homeLoc = recordedPopHomeLocs.get(personId);
		if (homeLoc == null) {// first time
			recordedPopHomeLocs.put(personId,
					net.getLinks().get(event.getLinkId()).getCoord());
		}
	}

	/**
	 * adds LinkUtilOffset of the link, where the event happens, to the
	 * dayUtilOffset of the agent
	 */
	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id linkId = event.getLinkId();
		Id agentId = event.getPersonId();
		int timeStep = DestinationTripUtilOffsetDistributionWithoutGrids
				.getTimeStep(event.getTime());

		// if (timeStep >= caliStartTime && timeStep <= caliEndTime) {
		double linkUtilOffset = getLinkUtilOffset(linkId, timeStep);
		Double duo = dayUtilOffsets.get(agentId);
		if (duo == null) {
			dayUtilOffsets.put(agentId, linkUtilOffset);
		} else {
			duo += linkUtilOffset;
		}
	}

	private void output(String outputFilenameBase) {
		calcDayUtilOffsetDistributionOfHome();
		write(outputFilenameBase);
	}

	private void prepareResult(SimpleWriter writer) {
		for (Coord crd : homeStats.keySet()) {
			BasicStatistics bs = homeStats.get(crd);
			int volume = bs.size();
			if (volume > 0)/* at least 50 residents */{
				double avgDayUO = bs.getAvg();
				writer.writeln(crd.getX() + "\t" + crd.getY() + "\t" + avgDayUO
						+ "\t" + volume + "\t" + Math.sqrt(volume));
				writer.flush();
			}
		}
	}

	@Override
	public void reset(int iteration) {

	}

	private void write(String filename) {
		SimpleWriter writer = new SimpleWriter(filename);
		writer.writeln("x\ty\tavg.DayUO\tvolume\tsqrt_vol");
		writer.flush();
		prepareResult(writer);
		writer.close();
	}

}
