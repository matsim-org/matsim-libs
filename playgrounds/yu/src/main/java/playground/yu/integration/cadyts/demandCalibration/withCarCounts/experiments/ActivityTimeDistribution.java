/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityTimeDistribution.java
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

package playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.core.scoring.ActivityUtilityParameters;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.actLocUtilOffset.GridUtils;
import playground.yu.utils.io.DistributionCreator;
import playground.yu.utils.io.GridCenterReader;

public class ActivityTimeDistribution implements ActivityStartEventHandler,
		ActivityEndEventHandler {
	public static class RealActivityTimes {
		private double realActivityStartTime, realActivityEndTime,
				typicalDuration;

		public RealActivityTimes(double arrivalTime, double departureTime,
				ActivityUtilityParameters actParams) {
			if (actParams == null) {
				throw new NullPointerException(
						"ActivityUtilityParameters -> null");
			}

			typicalDuration = actParams.getTypicalDuration();

			double openingTime = actParams.getOpeningTime();
			double closingTime = actParams.getClosingTime();

			realActivityStartTime = arrivalTime;
			realActivityEndTime = departureTime;

			if (openingTime >= 0 && arrivalTime < openingTime) {
				realActivityStartTime = openingTime;
			}
			if (closingTime >= 0 && closingTime < departureTime) {
				realActivityEndTime = closingTime;
			}
			if (openingTime >= 0
					&& closingTime >= 0
					&& (openingTime > departureTime || closingTime < arrivalTime)) {
				// agent could not perform action
				realActivityStartTime = departureTime;
				realActivityEndTime = departureTime;
			}
		}

		public double getRealActivityStartTime() {
			return realActivityStartTime;
		}

		public double getRealActivityEndTime() {
			return realActivityEndTime;
		}

		public double getTypicalDuration() {
			return typicalDuration;
		}

		public double getRealPerformingTime() {
			return realActivityEndTime - realActivityStartTime;
		}

		public double getPerformingRatio() {
			return getRealPerformingTime() / typicalDuration;
		}

	}

	protected final CharyparNagelScoringParameters params;

	private Map<Id/* agentId */, Double/* startTime */> agent_startTimes = new HashMap<Id, Double>();
	private Map<String/* type */, List<Double/* startTime */>> type_starTimes = new HashMap<String, List<Double>>(),
			type_endTimes = new HashMap<String, List<Double>>();
	private Map<Integer, Set<Coord>> DTimeCenterCoords = null;
	private int timeBinSize = 3600;
	private Network network = null;
	private double gridLength = -1;
	private List<Double> perfRatios;
	private Map<String/* shortType */, List<Double>> act_perfRatios;

	public ActivityTimeDistribution(CharyparNagelScoringParameters params,
			Map<Integer, Set<Coord>> DTimeCenterCoords, Network network,
			double gridLength) {
		this.params = params;
		this.DTimeCenterCoords = DTimeCenterCoords;
		this.network = network;
		this.gridLength = gridLength;
		perfRatios = new ArrayList<Double>();
		act_perfRatios = new HashMap<String, List<Double>>();
	}

	public ActivityTimeDistribution(CharyparNagelScoringParameters params) {
		this(params, null, null, 0);
	}

	public void setTimeBinSize(int timeBinSize) {
		this.timeBinSize = timeBinSize;
	}

	public void handleEvent(ActivityStartEvent event) {
		if (event != null) {
			agent_startTimes.put(event.getPersonId(), event.getTime());
		}
	}

	public void reset(int iteration) {

	}

	public void handleEvent(ActivityEndEvent event) {
		if (event != null) {
			Id agentId = event.getPersonId();
			if (agent_startTimes.containsKey(agentId)) {
				double arrivalTime = agent_startTimes.remove(agentId);
				double departureTime = event.getTime();
				String type = event.getActType();

				RealActivityTimes realActTimes = new RealActivityTimes(
						arrivalTime, departureTime, params.utilParams
								.get(type));
				if (realActTimes.getRealPerformingTime() != 0d) {
					String shortType = type.substring(0, 1);
					if (DTimeCenterCoords == null) {

						// endTime
						List<Double> endTimes = type_endTimes
								.get(shortType);
						if (endTimes == null) {
							endTimes = new ArrayList<Double>();
							type_endTimes.put(shortType, endTimes);
						}
						endTimes.add(realActTimes.getRealActivityEndTime());

						// startTime
						List<Double> startTimes = type_starTimes
								.get(shortType);
						if (startTimes == null) {
							startTimes = new ArrayList<Double>();
							type_starTimes.put(shortType, startTimes);
						}
						startTimes.add(realActTimes.getRealActivityStartTime());

						// performingRatio
						double perfRatio = realActTimes.getPerformingRatio();
						perfRatios.add(perfRatio);

						if (perfRatio < 2d) {//
							List<Double> perfRatiosOfAct = act_perfRatios
									.get(shortType);
							if (perfRatiosOfAct == null) {
								perfRatiosOfAct = new ArrayList<Double>();
								act_perfRatios.put(shortType,
										perfRatiosOfAct);
							}

							perfRatiosOfAct.add(perfRatio);
						}//

					} else {
						int arrivalTimeBin = (int) arrivalTime
								/ timeBinSize + 1;
						// check time
						if (DTimeCenterCoords
								.containsKey(arrivalTimeBin/*
															 * startTime because
															 * of D
															 */)) {
							Coord actCoord = network.getLinks().get(
									event.getLinkId()).getCoord();
							Set<Coord> centers = DTimeCenterCoords
									.get(arrivalTimeBin);
							// check in Grid
							loop: for (Coord center : centers) {
								if (gridLength > 0) {
									if (GridUtils.inGrid(center, actCoord,
											gridLength)) {
										// startTime because of D(estination)
										List<Double> startTimes = type_starTimes
												.get(shortType);
										if (startTimes == null) {
											startTimes = new ArrayList<Double>();
											type_starTimes.put(shortType,
													startTimes);
										}
										startTimes.add(realActTimes
												.getRealActivityStartTime());

										// TODO
										// ratios###################################
										// #################################################

										break loop;
									}// end if(GridUtils.inGrid(center,..
								}// end if (this.gridLength > 0)
							}// end loop for

						}// if(this.DTimeCenterCoords.containsKey(arrivalTimeBin...

					}

				}// end if (realActTimes != null)
			}// end if (this.agent_startTimes.containsKey(agentId))
		}// end if (event != null)
	}

	/**
	 * some codes copied from {@code
	 * org.matsim.core.scoring.charyparNagel.ActivityScoringFunction.
	 * getOpeningInterval(Activity act)} and {@code
	 * org.matsim.core.scoring.charyparNagel.ActivityScoringFunction.
	 * calcActScore(double arrivalTime, double departureTime, Activity act)}
	 * 
	 * @param arrivalTime
	 *            a double value
	 * @param departureTime
	 *            a double value
	 * @param activityType
	 *            a String
	 * @return the real activity start and end time
	 */
	private Tuple<Double, Double> getRealActivityTime(double arrivalTime,
			double departureTime, String activityType) {
		ActivityUtilityParameters actParams = params.utilParams
				.get(activityType);
		if (actParams == null) {
			throw new IllegalArgumentException("acttype \"" + activityType
					+ "\" is not known in utility parameters.");
		}

		double openingTime = actParams.getOpeningTime();
		double closingTime = actParams.getClosingTime();

		double activityStart = arrivalTime;
		double activityEnd = departureTime;

		if (openingTime >= 0 && arrivalTime < openingTime) {
			activityStart = openingTime;
		}
		if (closingTime >= 0 && closingTime < departureTime) {
			activityEnd = closingTime;
		}
		if (openingTime >= 0 && closingTime >= 0
				&& (openingTime > departureTime || closingTime < arrivalTime)) {
			// agent could not perform action
			// realActivityStartTime = departureTime;
			// realActivityEndTime = departureTime;
			return null;
		}

		return new Tuple<Double, Double>(activityStart, activityEnd);
	}

	public void output(String filenameBase, double interval) {

		DistributionCreator creatorStart = new DistributionCreator(
				type_starTimes, interval/* [s] */);
		creatorStart.write2(filenameBase + "StartTime.log");
		creatorStart.createChart2(filenameBase + "startTime.png",
				"distribution of \"real\" realActivityStartTime",
				"activity start time [s]",
				"number of activities starting at time x (interval = "
						+ interval + ")", true/*
											 * not time xAxis
											 */);
		creatorStart.createChart2percent(filenameBase + "startTimePercent.png",
				"distribution of \"real\" realActivityStartTime %",
				"activity start time [s]",
				"fraction of activities starting at time x (interval = "
						+ interval + ")", true/*
											 * not time xAxis
											 */);

		// ratio
		DistributionCreator performRatioCreator = new DistributionCreator(
				perfRatios, 0.01);
		performRatioCreator.write(filenameBase + "PerfRatio.log");
		performRatioCreator.createChart(filenameBase + "PerfRatio.png",
				"Distribution of performingTime/activityTypicalDuration",
				"ratio", "number of activities with ratios");
		performRatioCreator.createChartPercent(filenameBase
				+ "PerfRatioPercent.png",
				"Distribution of performingTime/activityTypicalDuration",
				"ratio", "fraction of activities with ratios [%]");

		// act_ratios
		DistributionCreator act_performRatioCreator = new DistributionCreator(
				act_perfRatios, 0.01);
		act_performRatioCreator.write2(filenameBase + "ActPerfRatio.log");
		act_performRatioCreator
				.createChart2(
						filenameBase + "ActPerfRatio.png",
						"Distribution of performingTime/activityTypicalDuration for activities",
						"ratio", "number of activities with ratios x", false);
		act_performRatioCreator
				.createChart2percent(
						filenameBase + "ActPerfRatioPercent.png",
						"Distribution of performingTime/activityTypicalDuration for activities",
						"ratio", "fraction of activities with ratios x [%]",
						false);

		// with constraint about center coords
		if (DTimeCenterCoords != null) {
			DistributionCreator creatorEnd = new DistributionCreator(
					type_endTimes, interval/* [s] */);
			creatorEnd.write2(filenameBase + "EndTime.log");
			creatorEnd.createChart2(filenameBase + "endTime.png",
					"distribution of \"real\" realActivityEndTime",
					"activity end time [s]",
					"number of activities ending at time x (interval = "
							+ interval + ")", true/*
												 * not time xAxis
												 */);
			creatorEnd.createChart2percent(filenameBase + "endTimePercent.png",
					"distribution of \"real\" realActivityEndTime %",
					"activity end time [s]",
					"fraction of activities ending at time x (interval = "
							+ interval + ")", true/*
												 * not time xAxis
												 */);
		}
	}

	private static void run(String[] args) {
		String eventsFilename = "../integration-demandCalibration/test/DestinationUtilOffset/1000.events.txt.gz";
		String configFilename = "../integration-demandCalibration/test/DestinationUtilOffset/analysis.xml";
		String outputFilenameBase = "../integration-demandCalibration/test/DestinationUtilOffset/DistributionReal";
		double interval = 600;
		EventsManager events = new EventsManagerImpl();

		ActivityTimeDistribution atd = null;
		atd = new ActivityTimeDistribution(
				new CharyparNagelScoringParameters(ConfigUtils.loadConfig(
						configFilename).planCalcScore()));
		events.addHandler(atd);
		new MatsimEventsReader(events).readFile(eventsFilename);

		atd.output(outputFilenameBase, interval);

	}

	private static void runGrids(String[] args) throws IOException {
		String eventsFilename = "../integration-demandCalibration/test/DestinationUtilOffset/1000.events.txt.gz";
		String configFilename = "../integration-demandCalibration/test/DestinationUtilOffset/analysis.xml";
		String outputFilenameBase = "../integration-demandCalibration/test/DestinationUtilOffset/DistributionInGridsReal";
		String networkFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		String gridFilenameBase = "../integration-demandCalibration/test/DestinationUtilOffset2/1000.destUtiloffset.";// +"??.grid.log";
		double interval = 600;
		int arStartTime = 7, arEndTime = 20;
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils
		.loadConfig(configFilename));

		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		// prepare grid coordinates
		Map<Integer, Set<Coord>> DTimeCenterCoords = new HashMap<Integer, Set<Coord>>();
		for (int i = arStartTime; i <= arEndTime; i++) {
			GridCenterReader gridReader = new GridCenterReader(gridFilenameBase
					+ i + ".grid.log");
			gridReader.readFile();
			DTimeCenterCoords.put(i, gridReader.getCenters());
		}
		// events
		EventsManager events = new EventsManagerImpl();

		ActivityTimeDistribution atd = null;
		atd = new ActivityTimeDistribution(new CharyparNagelScoringParameters(
				scenario.getConfig().planCalcScore()),
				DTimeCenterCoords, network, 1000d/* gridLength */);
		events.addHandler(atd);

		new MatsimEventsReader(events).readFile(eventsFilename);

		if (atd != null) {
			atd.output(outputFilenameBase, interval);
		}
	}

	public static void main(String[] args) throws IOException {
		run(args);
		// runGrids(args);
	}
}
