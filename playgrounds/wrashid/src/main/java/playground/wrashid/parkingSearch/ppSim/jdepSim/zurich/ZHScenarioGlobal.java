/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.ppSim.jdepSim.zurich;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.core.config.Config;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.threads.RerouteThreadDuringSim;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.ParkingSearchStrategy;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.analysis.ComparisonGarageCounts;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.analysis.ParkingEventDetails;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.analysis.StrategyStats;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.manager.ParkingStrategyManager;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.score.ParkingScoreEvaluator;
import playground.wrashid.parkingSearch.ppSim.ttmatrix.TTMatrix;

public class ZHScenarioGlobal {

	public static ParkingScoreEvaluator parkingScoreEvaluator;
	public static String outputFolder = null;
	public static StrategyStats strategyScoreStats = new StrategyStats();
	public static int iteration = 0;
	public static int numberOfIterations = -1;
	public static int writeEventsEachNthIteration = -1;
	public static int skipOutputInIteration = -1;
	public static LinkedList<ParkingEventDetails> parkingEventDetails;
	public static int populationExpensionFactor = -1;
	public static int numberOfRoutingThreadsAtBeginning = -1;
	public static int numberOfRoutingThreadsDuringSim = -1;
	public static boolean turnParallelRoutingOnDuringSim = true;
	public static Config config;
	public static int parkingStrategyScenarioId = -1;
	public static IntegerValueHashMap<Id> privateParkingCalibrationOriginalCapacities;

	// personId, legIndex, route
	public static TwoHashMapsConcatenated<Id, Integer, LinkNetworkRouteImpl> initialRoutes;
	public static String plansFile;
	public static RerouteThreadDuringSim[] rerouteThreadsDuringSim;
	public static Scenario scenario;
	public static Map<Id<Link>, Double> linkSlopes;
	public static TTMatrix ttMatrix;

	public static String getItersFolderPath() {
		return outputFolder + "ITERS/";
	}

	public static void init(TTMatrix ttMatrix, Network network) {
		ZHScenarioGlobal.ttMatrix = ttMatrix;
		rerouteThreadsDuringSim = new RerouteThreadDuringSim[numberOfRoutingThreadsDuringSim];

		for (int i = 0; i < numberOfRoutingThreadsDuringSim; i++) {
			rerouteThreadsDuringSim[i] = new RerouteThreadDuringSim(ttMatrix, network);
			rerouteThreadsDuringSim[i].start();
		}
	}

	public static void reset() {
		parkingEventDetails = new LinkedList<ParkingEventDetails>();
	}

	public static void produceOutputStats() {
		printGeneralParkingStats();
		printFilteredParkingStatsParkingType("stp");
		printFilteredParkingStatsParkingType("gp");
		printFilteredParkingStatsParkingType("private");
		printFilteredParkingStatsParkingType("illegal");
		printFilteredParkingStatsParkingType("publicPOutsideCityZH");
		printFilteredParkingStatsParkingType("backupParking");

		for (String groupNames : getUniqueGroupNames()) {
			printParkingStraregyStats(groupNames);
		}

		if (writeOutputInCurrentIteration()) {
			writeAllParkingEventsToFile();
			ComparisonGarageCounts.logOutput(parkingEventDetails, getItersFolderPath() + iteration + ".parkingCountsComparison");
		}
		ComparisonGarageCounts.logRelativeError(parkingEventDetails, outputFolder  + "parkingCountsComparison-relativeError.png");
	}
	
	private static HashSet<String> getUniqueGroupNames(){
		HashSet<String> groupNames=new HashSet<String>();
		for (ParkingSearchStrategy strategy : ParkingStrategyManager.allStrategies) {
			groupNames.add(strategy.getGroupName());
		}
		return groupNames;
	}

	private static void writeAllParkingEventsToFile() {
		ArrayList<String> list = new ArrayList<String>();

		list.add(ParkingEventDetails.getTabSeparatedTitleString());

		for (ParkingEventDetails ped : parkingEventDetails) {
			list.add(ped.getTabSeparatedLogString());
		}

		GeneralLib.writeList(list, getItersFolderPath() + iteration + ".parkingEvents.txt.gz");
	}

	private static void printParkingStraregyStats(String groupName) {
		double averageParkingDuration = 0;
		double averageSearchDuration = 0;
		double averageWalkDuration = 0;
		double averageWalkDistance = 0;

		System.out.println("stats for parking strategy group: " + groupName);

		int numberOfParkingOperations = 0;
		for (ParkingEventDetails ped : parkingEventDetails) {
			if (ped.parkingStrategy.getGroupName().equalsIgnoreCase(groupName)) {
				averageParkingDuration += ped.parkingActivityAttributes.getParkingDuration();
				averageSearchDuration += ped.parkingActivityAttributes.getParkingSearchDuration();
				averageWalkDuration += ped.parkingActivityAttributes.getToActWalkDuration();
				averageWalkDistance += ped.parkingActivityAttributes.getWalkDistance();
				numberOfParkingOperations++;
			}
		}

		System.out.println("averageParkingDuration [s]: " + averageParkingDuration / numberOfParkingOperations);
		System.out.println("averageSearchDuration [s]: " + averageSearchDuration / numberOfParkingOperations);
		System.out.println("averageWalkDuration [s]: " + averageWalkDuration / numberOfParkingOperations);
		System.out.println("averageWalkDistance [m]: " + averageWalkDistance / numberOfParkingOperations);
		System.out.println("numberOfParkingOperations: " + numberOfParkingOperations);
		System.out.println("========================");
	}

	private static void printFilteredParkingStatsParkingType(String type) {
		double averageParkingDuration = 0;
		double averageSearchDuration = 0;
		double averageWalkDuration = 0;
		double averageWalkDistance = 0;

		System.out.println("stats for parking type: " + type);

		int numberOfParkingOperations = 0;
		for (ParkingEventDetails ped : parkingEventDetails) {
			if (ped.parkingActivityAttributes.getFacilityId().toString().contains(type)) {
				averageParkingDuration += ped.parkingActivityAttributes.getParkingDuration();
				averageSearchDuration += ped.parkingActivityAttributes.getParkingSearchDuration();
				averageWalkDuration += ped.parkingActivityAttributes.getToActWalkDuration();
				averageWalkDistance += ped.parkingActivityAttributes.getWalkDistance();
				numberOfParkingOperations++;
			}
		}

		System.out.println("averageParkingDuration [s]: " + averageParkingDuration / numberOfParkingOperations);
		System.out.println("averageSearchDuration [s]: " + averageSearchDuration / numberOfParkingOperations);
		System.out.println("averageWalkDuration [s]: " + averageWalkDuration / numberOfParkingOperations);
		System.out.println("averageWalkDistance [m]: " + averageWalkDistance / numberOfParkingOperations);
		System.out.println("numberOfParkingOperations: " + numberOfParkingOperations);
		System.out.println("========================");
	}

	private static void printGeneralParkingStats() {
		double averageParkingDuration = 0;
		double averageSearchDuration = 0;
		double averageWalkDuration = 0;
		double averageWalkDistance = 0;
		

		System.out.println("general parking stats");

		for (ParkingEventDetails ped : parkingEventDetails) {
			averageParkingDuration += ped.parkingActivityAttributes.getParkingDuration();
			averageSearchDuration += ped.parkingActivityAttributes.getParkingSearchDuration();
			averageWalkDuration += ped.parkingActivityAttributes.getToActWalkDuration();
			averageWalkDistance += ped.parkingActivityAttributes.getWalkDistance();
		}

		int numberOfParkingOperations = parkingEventDetails.size();
		System.out.println("averageParkingDuration [s]: " + averageParkingDuration / numberOfParkingOperations);
		System.out.println("averageSearchDuration [s]: " + averageSearchDuration / numberOfParkingOperations);
		System.out.println("averageWalkDuration [s]: " + averageWalkDuration / numberOfParkingOperations);
		System.out.println("averageWalkDistance [m]: " + averageWalkDistance / numberOfParkingOperations);
		System.out.println("numberOfParkingOperations: " + numberOfParkingOperations);
		System.out.println("========================");
	}

	public static void init() {

		loadConfigParamters();
		File file = new File(outputFolder);

		// file.mkdir();

		if (file.list().length > 2 && !ZHScenarioGlobal.loadBooleanParam("developingMode")) {
			DebugLib.stopSystemAndReportInconsistency("output folder exists already!" + outputFolder);
		}

		file = new File(getItersFolderPath());
		file.mkdir();

		//ComparisonGarageCounts.init();

	}

	public static void initNetworkLinkSlopes() {
		linkSlopes = new HashMap<>();
		if (!paramterExists("networkLinkSlopes")) {
			linkSlopes = null;
		} else {
			String linkSlopeAttributeFile = ZHScenarioGlobal.loadStringParam("networkLinkSlopes");
			ObjectAttributes lp = new ObjectAttributes();
			new ObjectAttributesXmlReader(lp).parse(linkSlopeAttributeFile);

			for (Id<Link> linkId : scenario.getNetwork().getLinks().keySet()) {
				linkSlopes.put(linkId, (Double) lp.getAttribute(linkId.toString(), "slope"));
			}
		}
	}

	private static void loadConfigParamters() {
		ZHScenarioGlobal.outputFolder = loadStringParam("outputFolder");

		ParkingLoader.parkingsOutsideZHCityScaling = loadDoubleParam("ParkingLoader.parkingsOutsideZHCityScaling");
		ParkingLoader.streetParkingCalibrationFactor = loadDoubleParam("ParkingLoader.streetParkingCalibrationFactor");
		ParkingLoader.garageParkingCalibrationFactor = loadDoubleParam("ParkingLoader.garageParkingCalibrationFactor");
		ParkingLoader.privateParkingCalibrationFactorZHCity = loadDoubleParam("ParkingLoader.privateParkingCalibrationFactorZHCity");
		ParkingLoader.populationScalingFactor = loadDoubleParam("ParkingLoader.populationScalingFactor");

		ZHScenarioGlobal.parkingStrategyScenarioId = loadIntParam("ZHScenarioGlobal.parkingStrategyScenarioId");
		ZHScenarioGlobal.numberOfIterations = loadIntParam("ZHScenarioGlobal.numberOfIterations");
		ZHScenarioGlobal.writeEventsEachNthIteration = loadIntParam("ZHScenarioGlobal.writeEventsEachNthIteration");
		ZHScenarioGlobal.skipOutputInIteration = loadIntParam("ZHScenarioGlobal.skipOutputInIteration");
		ZHScenarioGlobal.populationExpensionFactor = loadIntParam("ZHScenarioGlobal.populationExpensionFactor");
		ZHScenarioGlobal.numberOfRoutingThreadsAtBeginning = loadIntParam("ZHScenarioGlobal.numberOfRoutingThreadsAtBeginning");
		ZHScenarioGlobal.numberOfRoutingThreadsDuringSim = loadIntParam("ZHScenarioGlobal.numberOfRoutingThreadsDuringSim");
	}

	public static boolean writeOutputInCurrentIteration() {
		if (iteration == ZHScenarioGlobal.loadIntParam("ZHScenarioGlobal.writeOutputAtIteration-1")) {
			return true;
		}

		if (iteration == ZHScenarioGlobal.loadIntParam("ZHScenarioGlobal.writeOutputAtIteration-2")) {
			return true;
		}

		return iteration % writeEventsEachNthIteration == 0 && iteration != skipOutputInIteration;
	}

	public static boolean paramterExists(String paramName) {
		return config.findParam("parkingSearch", paramName) != null;
	}

	public static double loadDoubleParam(String paramName) {
		return Double.parseDouble(config.getParam("parkingSearch", paramName));
	}

	public static boolean loadBooleanParam(String paramName) {
		return Boolean.parseBoolean(config.getParam("parkingSearch", paramName));
	}

	public static int loadIntParam(String paramName) {
		return Integer.parseInt(config.getParam("parkingSearch", paramName));
	}

	public static String loadStringParam(String paramName) {
		return config.getParam("parkingSearch", paramName);
	}
}
