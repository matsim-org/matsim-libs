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
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

import playground.yu.integration.cadyts.demandCalibration.withCarCounts.BseLinkCostOffsetsXMLFileIO;
import playground.yu.utils.io.DistributionCreator;
import cadyts.utilities.misc.DynamicData;

/**
 * @author yu
 * 
 */
public class DestinationTripUtilOffsetDistributionWithoutGrids implements
		ActivityStartEventHandler, LinkEnterEventHandler {// TODO
	public static class TripUtilityOffsets {
		// private static double timeBinSize = 3600d;

		// private String activityType;
		// private int timeStep;

		private int zeroUtilOffsetCounter = 0;

		private List<Double> nonZeroTripUtilOffsets;

		public TripUtilityOffsets() {
			nonZeroTripUtilOffsets = new ArrayList<Double>();
		}

		// public static double getTimeBinSize() {
		// return timeBinSize;
		// }

		// public static void setTimeBinSize(double timeBinSize) {
		// TripUtilityOffsets.timeBinSize = timeBinSize;
		// }

		public int getZeroUtilOffsetCounter() {
			return zeroUtilOffsetCounter;
		}

		public List<Double> getNonZeroTripUtilOffsets() {
			return nonZeroTripUtilOffsets;
		}

		public void add(double utilOffset) {
			if (utilOffset == 0d) {
				zeroUtilOffsetCounter++;
			} else {
				nonZeroTripUtilOffsets.add(utilOffset);
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		run(args);
		// runGrids(args);
	}

	private static void run(String[] args) {
		String linkOffsetUtilOffsetFilename = "../integration-demandCalibration/test/DestinationUtilOffset/1000.linkCostOffsets.xml"//
		, networkFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml"//
		, countsFilename = "../schweiz-ivtch-SVN/baseCase/counts/countsIVTCH.xml"//
		, eventsFilename = "../integration-demandCalibration/test/DestinationUtilOffset/1000.events.txt.gz"//
		, outputFilenameBase = "../integration-demandCalibration/test/DestinationUtilOffset2/tmp/UOD.allday."// UOD.9.

		;

		int arStartTime = 7, arEndTime = 24;// lowerLimit = 50;
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

		DestinationTripUtilOffsetDistributionWithoutGrids aluoe = new DestinationTripUtilOffsetDistributionWithoutGrids(
				net, counts, linkUtilOffsets, arStartTime, arEndTime
		// ,lowerLimit,1000d
		);

		EventsManager events = new EventsManagerImpl();
		// /////////////////////////////////
		events.addHandler(aluoe);
		// /////////////////////////////////
		new MatsimEventsReader(events).readFile(eventsFilename);

		// aluoe.output(outputFilenameBase);
		aluoe.write(outputFilenameBase, interval);
		System.out.println(aluoe.output());
	}

	protected List<Double> destTripUtilOffsetList = new ArrayList<Double>();
	private Map<String, List<Double>> actType_destTripUtilOffsetLists = new HashMap<String, List<Double>>();
	private int timeBinSize = 3600;
	protected Map<Id/* agentId */, Double/* legUtilOffset */> tmpAgentLegUtilOffsets = new HashMap<Id, Double>();
	private Network net;
	// for grid version
	private Counts counts;
	private DynamicData<Link> linkUtilOffsets;
	private int caliStartTime;
	private int caliEndTime;
	private Map<String/* actType-timeStep */, TripUtilityOffsets> activityType_Time_UOs = new HashMap<String, TripUtilityOffsets>();

	// private int lowerLimit;
	// private double gridLength;

	public DestinationTripUtilOffsetDistributionWithoutGrids(Network net,
			Counts counts, DynamicData<Link> linkUtilOffsets,
			int caliStartTime, int caliEndTime
	// , int lowerLimit,double gridLength
	) {
		this.net = net;
		this.counts = counts;
		this.linkUtilOffsets = linkUtilOffsets;
		this.caliStartTime = caliStartTime;
		this.caliEndTime = caliEndTime;
		// this.lowerLimit = lowerLimit;
		// this.gridLength = gridLength;
	}

	protected static int getTimeStep(double time) {
		return (int) time / 3600 + 1;
	}

	public List<Double> getDestTripUtilOffsetList() {
		return destTripUtilOffsetList;
	}

	public void setTimeBinSize(int timeBinSize) {
		this.timeBinSize = timeBinSize;
	}

	private static boolean isInRange(final Id linkId, final Network net) {
		Coord distanceFilterCenterNodeCoord = net.getNodes().get(
				new IdImpl("2531")).getCoord();
		double distanceFilter = 30000;
		Link l = net.getLinks().get(linkId);
		if (l == null) {
			System.out.println("Cannot find requested link: "
					+ linkId.toString());
			return false;
		}
		return ((LinkImpl) l).calcDistance(distanceFilterCenterNodeCoord) < distanceFilter;
	}

	protected double getLinkUtilOffset(Id linkId, int time) {
		if (counts.getCounts().containsKey(linkId)) {
			if (isInRange(linkId, net)) {
				return linkUtilOffsets.getSum(net.getLinks().get(linkId),
						(time - 1) * 3600, time * 3600 - 1);
			}
		}
		return 0d;
	}

	public void handleEvent(LinkEnterEvent event) {
		Id linkId = event.getLinkId();
		Id agentId = event.getPersonId();
		int timeStep = getTimeStep(event.getTime());

		// if (timeStep >= caliStartTime && timeStep <= caliEndTime) {
		double linkUtilOffset = getLinkUtilOffset(linkId, timeStep);

		Double legUtilOffset = tmpAgentLegUtilOffsets.get(agentId);
		if (legUtilOffset == null) {
			tmpAgentLegUtilOffsets.put(agentId, linkUtilOffset);
		} else {
			tmpAgentLegUtilOffsets.put(agentId, legUtilOffset + linkUtilOffset);
		}
		// }
	}

	public void handleEvent(ActivityStartEvent/* trip ending */event) {
		// if (event != null) {
		int timeStep = getTimeStep(event.getTime());
		if (timeStep >= caliStartTime && timeStep <= caliEndTime) {
			// trip arriving in appropriate time
			Id agentId = event.getPersonId();
			Double legUtilOffset = tmpAgentLegUtilOffsets./**/remove(agentId)/**/;
			String actType = event.getActType().substring(0, 1);

			if (legUtilOffset != null) {
				// if (time_centerCoords == null) {

				destTripUtilOffsetList.add(legUtilOffset)/**/;

				List<Double> actType_destTripUtilOffsetList = actType_destTripUtilOffsetLists
						.get(actType);
				if (actType_destTripUtilOffsetList == null) {
					actType_destTripUtilOffsetList = new ArrayList<Double>();
					actType_destTripUtilOffsetLists.put(actType,
							actType_destTripUtilOffsetList);
				}
				actType_destTripUtilOffsetList.add(legUtilOffset);

				String key = actType + "-" + timeStep;
				TripUtilityOffsets attu = activityType_Time_UOs.get(key);
				if (attu == null) {
					attu = new TripUtilityOffsets();
					activityType_Time_UOs.put(key, attu);
				}
				attu.add(legUtilOffset);
				// }
			}

		}

		// }
	}

	public void write(String filenameBase, double interval) {
		// if (time_centerCoords == null) {

		// DistributionCreator creator = new DistributionCreator(
		// destTripUtilOffsetList, interval);
		// creator.write(filenameBase + "total.log");
		// creator.createChart(filenameBase + "total.png",
		// "total destination trip Utility offset distribution",
		// "value of destination trip utility offset",
		// "number of trips with value in range (interval = " + interval
		// + ") of x");

		for (String actType : actType_destTripUtilOffsetLists.keySet()) {

			// DistributionCreator creatorAct = new DistributionCreator(
			// actType_destTripUtilOffsetLists.get(actType), interval);
			// creatorAct.write(filenameBase + actType + ".log");
			// creatorAct.createChart(filenameBase + actType + ".total.png",
			// actType + " destination trip Utility offset distribution",
			// "value of destination trip utility offset",
			// "number of trips with value in range (interval = "
			// + interval + ") of x");

			List<Double> act_destTripUtilOffsetList2 = new ArrayList<Double>();
			for (Double d : actType_destTripUtilOffsetLists.get(actType)) {
				if (d != 0d) {
					act_destTripUtilOffsetList2.add(d);
				}
			}

			DistributionCreator creatorAct2 = new DistributionCreator(
					act_destTripUtilOffsetList2, interval);
			creatorAct2.write(filenameBase + actType + ".Non0.log");
			creatorAct2.createChart(filenameBase + actType + ".Non0.png",
					actType + " destination trip Utility offset distribution",
					"value of nonzero destination trip utility offset",
					"number of trips with value (!=0) in range (interval = "
							+ interval + ") of x");
		}

		List<Double> destTripUtilOffsetList2 = new ArrayList<Double>();
		for (Double d : destTripUtilOffsetList) {
			if (d != 0d) {
				destTripUtilOffsetList2.add(d);
			}
		}

		// DistributionCreator creator2 = new DistributionCreator(
		// destTripUtilOffsetList2, interval);
		// creator2
		// .createChart(
		// filenameBase + "totalUn0.png",
		// "total \"unzero\" destination trip Utility offset distribution",
		// "\"non-zero\" value of destination trip utility offset",
		// "number of trips with value (!=0) in (interval = "
		// + interval + ") range of x");

		// DistributionCreator creator3 = new DistributionCreator(
		// actType_destTripUtilOffsetLists, interval);
		// creator3.write2(filenameBase + "totalAllActs.log");
		// creator3.createChart2(filenameBase + "totalAllActs.png",
		// "total destination trip Utility offset distribution",
		// "value of destination trip utility offset",
		// "number of trips with value  in (interval = " + interval
		// + ") range of x", false/* not time xAxis */);

		Map<String, List<Double>> actType_destTripUtilOffsetLists4 = new HashMap<String, List<Double>>();
		for (String key : actType_destTripUtilOffsetLists.keySet()) {
			List<Double> list = new ArrayList<Double>();
			actType_destTripUtilOffsetLists4.put(key, list);
			for (Double d : actType_destTripUtilOffsetLists.get(key)) {
				if (d != 0d) {
					list.add(d);
				}
			}
		}

		DistributionCreator creator4 = new DistributionCreator(
				actType_destTripUtilOffsetLists4, interval);
		creator4.write2(filenameBase + "totalNon0AllActs.log");
		creator4.createChart2(filenameBase + "totalNon0AllActs.png",
				"total nonzero destination trip Utility offset distribution",
				"value of destination trip utility offset",
				"number of trips with value (!= 0) in (interval = " + interval
						+ ") range of x", false/*
												 * not time xAxis
												 */);
		creator4.createChart2percent(filenameBase
				+ "totalNon0AllActsPercent.png", "",
				"value of destination trip utility offset",
				"frequency (in percent)", false);
		creator4.write2percent(filenameBase + "totalNon0AllActsPercent.txt");

		// }
	}

	public String output() {
		StringBuffer toReturn = new StringBuffer();
		// for (int timeStep = caliStartTime; timeStep <= caliEndTime;
		// timeStep++) {
		toReturn
				.append("----------------------\nActivityType-TimeStep\tno. of trips with nonzero utility offsets\tno. of all trips\n");
		for (Entry<String, TripUtilityOffsets> entry : activityType_Time_UOs
				.entrySet()) {
			String key = entry.getKey();
			TripUtilityOffsets tuo = entry.getValue();
			int nonzeroUtilOffsetsSize = tuo.getNonZeroTripUtilOffsets().size();
			toReturn.append(key + "\t" + nonzeroUtilOffsetsSize + "\t"
					+ (tuo.getZeroUtilOffsetCounter() + nonzeroUtilOffsetsSize)
					+ "\n");
		}
		// }
		return toReturn.toString();
	}

	@Override
	public void reset(int iteration) {

	}
}
