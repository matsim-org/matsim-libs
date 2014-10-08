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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

import playground.yu.integration.cadyts.demandCalibration.withCarCounts.BseLinkCostOffsetsXMLFileIO;
import playground.yu.utils.io.DistributionCreator;
import playground.yu.utils.io.SimpleWriter;
import utilities.math.BasicStatistics;
import utilities.misc.DynamicData;

/**
 * @author yu
 * 
 */
public class DestinationTripUtilOffsetDistributionWithoutGrids implements
		ActivityStartEventHandler, LinkEnterEventHandler {
	// public static class TripUtilityOffsets {
	// // private static double timeBinSize = 3600d;
	//
	// // private String activityType;
	// // private int timeStep;
	//
	// private int zeroUO_Counter = 0;// UO - Utility Offset
	//
	// private final List<Double> nonZeroTUOs;// UO - Trip Utility Offset
	//
	// public TripUtilityOffsets() {
	// nonZeroTUOs = new ArrayList<Double>();
	// }
	//
	// // public static double getTimeBinSize() {
	// // return timeBinSize;
	// // }
	//
	// // public static void setTimeBinSize(double timeBinSize) {
	// // TripUtilityOffsets.timeBinSize = timeBinSize;
	// // }
	//
	// /**
	// * adds {@code Leg} Utility Offset in this {@code TripUtilityOffset}
	// *
	// * @param utilOffset
	// */
	// public void add(double utilOffset) {
	// if (utilOffset == 0d) {
	// zeroUO_Counter++;
	// } else {
	// nonZeroTUOs.add(utilOffset);
	// }
	// }
	//
	// public List<Double> getNonZeroTripUtilOffsets() {
	// return nonZeroTUOs;
	// }
	//
	// public int getZeroUtilOffsetCounter() {
	// return zeroUO_Counter;
	// }
	// }

	public static int getTimeStep(double time) {
		return (int) time / timeBinSize + 1;
	}

	private static boolean isInRange(final Id<Link> linkId, final Network net) {
		Node distanceFilterCenterNode = net.getNodes().get(Id.create("2531", Node.class));
		if (distanceFilterCenterNode == null) {
			return true;
		}
		Coord distanceFilterCenterNodeCoord = distanceFilterCenterNode
				.getCoord();
		double distanceFilter = 30000;
		Link l = net.getLinks().get(linkId);
		if (l == null) {
			System.out.println("Cannot find requested link: "
					+ linkId.toString());
			return false;
		}
		return ((LinkImpl) l).calcDistance(distanceFilterCenterNodeCoord) < distanceFilter;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		run(args);
		// runGrids(args);
	}

	private static void run(String[] args) {
		String linkOffsetUtilOffsetFilename = "test/input/bln2pct/1536.2500.linkUtilOffsets.xml"//
		, networkFilename = "D:/Daten/work/shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml.gz"//
		, countsFilename = "D:/Daten/work/shared-svn/studies/countries/de/berlin/counts/iv_counts/vmz_di-do.xml"//
		, eventsFilename = "test/input/bln2pct/1536.2500.events.xml.gz"//
		, outputFilenameBase = "test/output/bln2pct/UOD.allday."// UOD.9.

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

		DestinationTripUtilOffsetDistributionWithoutGrids aluoe = new DestinationTripUtilOffsetDistributionWithoutGrids(
				net, counts, linkUtilOffsets, arStartTime, arEndTime
		// ,lowerLimit,1000d
		);

		EventsManager events = EventsUtils.createEventsManager();
		// /////////////////////////////////
		events.addHandler(aluoe);
		// /////////////////////////////////
		new MatsimEventsReader(events).readFile(eventsFilename);

		// aluoe.write(outputFilenameBase, interval);

		aluoe.calcAvgTUOonActLoc(outputFilenameBase);

		// System.out.println(aluoe.output());
	}

	protected List<Double> dTUO_List /* d - destination */= new ArrayList<Double>();
	private final Map<String, List<Double>> actType_dTUO_Lists = new HashMap<String, List<Double>>();
	private static int timeBinSize = 3600;
	protected Map<Id/* agentId */, Double/* legUtilOffset */> tmpAgent_TUOs = new HashMap<Id, Double>();
	private final Network net;// for grid version
	private final Counts counts;
	private final DynamicData<Link> linkUOs;
	private final int caliStartTime;
	private final Map<Coord/* linkCenter */, BasicStatistics/* statistics */> actLocStat = new HashMap<Coord, BasicStatistics>();
	// private int lowerLimit; // private double gridLength;

	private final int caliEndTime;

	private final Map<Integer/* time step */, List<Double>> timeStep_UOs = new HashMap<Integer, List<Double>>();

	public DestinationTripUtilOffsetDistributionWithoutGrids(Network net,
			Counts counts, DynamicData<Link> linkUtilOffsets,
			int caliStartTime, int caliEndTime
	// , int lowerLimit,double gridLength
	) {
		this.net = net;
		this.counts = counts;
		linkUOs = linkUtilOffsets;
		this.caliStartTime = caliStartTime;
		this.caliEndTime = caliEndTime;
		// this.lowerLimit = lowerLimit;
		// this.gridLength = gridLength;
	}

	// public String output() {
	// StringBuffer toReturn = new StringBuffer();
	// // for (int timeStep = caliStartTime; timeStep <= caliEndTime;
	// // timeStep++) {
	// toReturn.append("----------------------\nTimeStep\tno. of trips with nonzero utility offsets\tno. of all trips\n");
	// for (Entry<Integer, TripUtilityOffsets> entry : timeStep_UOs.entrySet())
	// {
	// Integer timeStep = entry.getKey();
	// TripUtilityOffsets tuo = entry.getValue();
	// int nonzeroUtilOffsetsSize = tuo.getNonZeroTripUtilOffsets().size();
	// toReturn.append(timeStep + "\t" + nonzeroUtilOffsetsSize + "\t"
	// + (tuo.getZeroUtilOffsetCounter() + nonzeroUtilOffsetsSize)
	// + "\n");
	// }
	// // }
	// return toReturn.toString();
	// }
	public void calcAvgTUOonActLoc(String filenameBase) {
		SimpleWriter writer = new SimpleWriter(filenameBase + "destActLoc.log");
		writer.writeln("x\ty\tavg.TUO\tvolumes\tln_vol");
		for (Coord crd : actLocStat.keySet()) {
			BasicStatistics bs = actLocStat.get(crd);
			double avgTUO = bs.getAvg();
			int size = bs.size();
			if (avgTUO != 0d && size > 1/*
										 * 50 times a day, that means this place
										 * should be visited at least 100 times
										 * in a day
										 */) {
				writer.writeln(crd.getX() + "\t" + crd.getY() + "\t" + avgTUO
						+ "\t" + size + "\t" + Math.log(size));
				writer.flush();
			}
		}
		writer.close();
	}

	public List<Double> getDestTripUtilOffsetList() {
		return dTUO_List;
	}

	protected double getLinkUtilOffset(Id<Link> linkId, int time) {
		if (counts.getCounts().containsKey(linkId)) {
			if (isInRange(linkId, net)) {
				return linkUOs.getSum(net.getLinks().get(linkId), (time - 1)
						* timeBinSize, time * timeBinSize - 1);
			}
		}
		return 0d;
	}

	@Override
	public void handleEvent(ActivityStartEvent/* trip ending */event) {
		// if (event != null) {
		int timeStep = getTimeStep(event.getTime());
		if (timeStep >= caliStartTime && timeStep <= caliEndTime) {
			// trip arriving in appropriate time
			Id<Person> agentId = event.getPersonId();
			Double tripUO = tmpAgent_TUOs./**/remove(agentId)/**/;
			String actType = event.getActType().substring(0, 1);
			// TODO

			if (tripUO != null) {
				Coord actLoc = net.getLinks().get(event.getLinkId()).getCoord();
				BasicStatistics ba = actLocStat.get(actLoc);
				if (ba == null) {
					ba = new BasicStatistics();
					actLocStat.put(actLoc, ba);
				}
				ba.add(tripUO);

				// if (time_centerCoords == null) {

				dTUO_List.add(tripUO)/**/;

				List<Double> actType_dTUO_List = actType_dTUO_Lists
						.get(actType);
				if (actType_dTUO_List == null) {
					actType_dTUO_List = new ArrayList<Double>();
					actType_dTUO_Lists.put(actType, actType_dTUO_List);
				}
				actType_dTUO_List.add(tripUO);

				List<Double> timeStep_dTUO_List = timeStep_UOs.get(timeStep);
				if (timeStep_dTUO_List == null) {
					timeStep_dTUO_List = new ArrayList<Double>();
					timeStep_UOs.put(timeStep, timeStep_dTUO_List);
				}
				timeStep_dTUO_List.add(tripUO);
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Link> linkId = event.getLinkId();
		Id<Person> agentId = event.getPersonId();
		int timeStep = getTimeStep(event.getTime());

		// if (timeStep >= caliStartTime && timeStep <= caliEndTime) {
		double linkUtilOffset = getLinkUtilOffset(linkId, timeStep);

		Double tripUtilOffset/* i.e. LegUtilOffset */= tmpAgent_TUOs
				.get(agentId);
		if (tripUtilOffset == null) {
			tmpAgent_TUOs.put(agentId, linkUtilOffset);
		} else {
			tmpAgent_TUOs.put(agentId, tripUtilOffset + linkUtilOffset);
		}
		// }
	}

	@Override
	public void reset(int iteration) {

	}

	public void setTimeBinSize(int timeBinSize) {
		DestinationTripUtilOffsetDistributionWithoutGrids.timeBinSize = timeBinSize;
	}

	public void write(String filenameBase, double interval) {
		// calcAvgTUOonActLoc(filenameBase);
		// if (time_centerCoords == null) {

		// DistributionCreator creator = new DistributionCreator(
		// destTripUtilOffsetList, interval);
		// creator.write(filenameBase + "total.log");
		// creator.createChart(filenameBase + "total.png",
		// "total destination trip Utility offset distribution",
		// "value of destination trip utility offset",
		// "number of trips with value in range (interval = " + interval
		// + ") of x");

		for (String actType : actType_dTUO_Lists.keySet()) {
			List<Double> act_destTripUtilOffsetList2 = new ArrayList<Double>();
			for (Double d : actType_dTUO_Lists.get(actType)) {
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
		for (Integer timeStep : timeStep_UOs.keySet()) {
			List<Double> timeStep_UO_List2 = new ArrayList<Double>();
			for (Double d : timeStep_UOs.get(timeStep)) {
				if (d != 0d) {
					timeStep_UO_List2.add(d);
				}
			}
			DistributionCreator creatorTimeStep2 = new DistributionCreator(
					timeStep_UO_List2, interval);
			creatorTimeStep2.write(filenameBase + timeStep + ".Non0.log");
			creatorTimeStep2.createChart(filenameBase + ".ts." + timeStep
					+ ".Non0.png", "time step " + timeStep
					+ " destination trip Utility offset distribution",
					"value of nonzero destination trip utility offset",
					"number of trips with value (!=0) in range (interval = "
							+ interval + ") of x");
		}
		// List<Double> destTripUtilOffsetList2 = new ArrayList<Double>();
		// for (Double d : dTUO_List) {
		// if (d != 0d) {
		// destTripUtilOffsetList2.add(d);
		// }
		// }

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

		Map<String, List<Double>> actType_dTUO_Lists4 = new HashMap<String, List<Double>>();
		for (String key : actType_dTUO_Lists.keySet()) {
			List<Double> list = new ArrayList<Double>();
			actType_dTUO_Lists4.put(key, list);
			for (Double d : actType_dTUO_Lists.get(key)) {
				if (d != 0d) {
					list.add(d);
				}
			}
		}

		DistributionCreator creator4 = new DistributionCreator(
				actType_dTUO_Lists4, interval);
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

		// /////////////////////////////////////////////TODO the next 2 lines
		Map<String, List<Double>> timeStep_UOs_List4 = new HashMap<String, List<Double>>();
		for (Integer key : timeStep_UOs.keySet()) {
			List<Double> list = new ArrayList<Double>();
			timeStep_UOs_List4.put(key.toString(), list);
			for (Double d : timeStep_UOs.get(key)) {
				if (d != 0d) {
					list.add(d);
				}
			}
		}

		DistributionCreator creator5 = new DistributionCreator(
				timeStep_UOs_List4, interval);
		creator5.write2(filenameBase + "totalNon0AllTimeSteps.log");
		creator5.createChart2(filenameBase + "totalNon0AllTimeSteps.png",
				"total nonzero destination trip Utility offset distribution",
				"value of destination trip utility offset",
				"number of trips with value (!= 0) in (interval = " + interval
						+ ") range of x", false/*
												 * not time xAxis
												 */);
		creator5.createChart2percent(filenameBase
				+ "totalNon0AllTimeStepsPercent.png", "",
				"value of destination trip utility offset",
				"frequency (in percent)", false);
		creator5.write2percent(filenameBase
				+ "totalNon0AllTimeStepsPercent.txt");
		// }
	}
}