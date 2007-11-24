/* *********************************************************************** *
 * project: org.matsim.*
 * MyRunsRouting.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.lnicolas.routing;

import java.awt.Polygon;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.matsim.events.algorithms.TravelTimeCalculator;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkLayerBuilder;
import org.matsim.network.NetworkWriter;
import org.matsim.network.Node;
import org.matsim.network.algorithms.NetworkCleaner;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.PlansWriter;
import org.matsim.plans.algorithms.FromToSummary;
import org.matsim.plans.algorithms.PlansCalcTravelDistance;
import org.matsim.router.AStarEuclidean;
import org.matsim.router.AStarLandmarks;
import org.matsim.router.Dijkstra;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.PreProcessDijkstra;
import org.matsim.router.util.PreProcessEuclidean;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelMinCostI;
import org.matsim.router.util.TravelTimeI;

import playground.lnicolas.MyRuns;
import playground.lnicolas.convexhull.GrahamScan;
import playground.lnicolas.network.algorithm.RandomPlansGenerator;
import playground.lnicolas.network.algorithm.RandomPlansInTravelZoneGenerator;
import playground.lnicolas.plans.algorithms.PlansCalcAndCompareRoute;
import playground.lnicolas.plans.algorithms.PlansCalcRoute;
import playground.lnicolas.plans.algorithms.PlansRouteSummary;
import playground.lnicolas.routing.costcalculators.PeakTravTimeCalc;

public class MyRunsRouting extends MyRuns {

	public static void main(String[] args) {

//		readNetwork(); exportNetwork(args); System.exit(0);
//		readNetwork(); generateRandomPlans(500); System.exit(0);
//		readNetwork(); printFromToAvgDistance(); System.exit(0);
		readNetwork(); analyzeNetwork(); System.exit(0);

//		readNetwork();
//		AvgRouteDistance alg2 = new AvgRouteDistance();
//		Plans plans2 = new Plans();
//		PlansReaderI plansReader = PlansReaderBuilder.getPlansReader(plans2);
//		plans2.addAlgorithm(alg2);
//		plansReader.read();
//		alg2.printSummary();
//		System.exit(0);

		readNetwork();
//		readFacilities();
		Plans plans = readPlans();
		if (args.length > 2) {
			if (args[2].equals("aStarLZE") || args[2].equals("aStarLZ")
					|| args[2].equals("aStarLZ") || args[2].equals("aStarMF")) {
				calcRouteWithPlansPreprocessing(args, plans);
			} else {
				calcRoute(args, plans);
			}
		}

		// cutNetwork();

		// reduceNetwork();

		// updateLinkLengths();

		// generateRandomPlans((640000 + 740000) / 2, (200000 + 310000) / 2,
		// 1000, 10000);
		// exportNetwork(args, (640000 + 740000) / 2, (200000 + 310000) / 2,
		// 100000);

		// printNetworkConvexHull(args);

		// readNetwork();
		// Plans plans = readPlans();
		// PlansWriter plansWriter = new PlansWriter(plans);
		// plans.setPlansWriter(plansWriter);
		// plansWriter.write();
		// System.out.println("Wrote plans to "
		// + Gbl.getConfig().getParam(Config.PLANS, Config.PLANS_OUTFILE));
		// Plans plans = reducePlans(10000);
		// removeLinksFromActs(plans);

		// readNetwork();
		// exportRoute(args, 11273, 1893); // ivt-net

		// readNetwork();
		// exportRoute(args, 101455965, 101479161); // navteq cut
		// System.exit(0);
		// exportRoute(args, 100862671, 102188890); // navteq, long route

		// showAStarEstimation(args);
		// countOneWayNodes();

		// analyzeNetwork();

		// printLinkLengthDistr(); System.exit(0);

		// compareRoute(args);

		// compareAStar(12031, 6853);
		// removePersonRoutes();

		//	analyzeLinkLengths();
	}

	private static void calcRouteWithPlansPreprocessing(String[] args,
			Plans plans) {

		// long now = System.currentTimeMillis();
		FromToSummary sum = createPlansFromToSummary(plans, false);

		LeastCostPathCalculator routingAlgo = null;// getRoutingAlgo(args, sum);

		// if (routingAlgo instanceof RouteExportable) {
		// GdfExport writer = new GdfExport();
		// Node fromNode = network.getNode("2626");
		// ((RouteExportable)routingAlgo).exportCheapestRoute(fromNode,
		// fromNode, 0, writer);
		// writer.write(
		// "C:/usr/lnicolas/aStarLZ.gdf");
		// // "/var/tmp/lnicolas/routingPerformance/aStarLZ.gdf");
		// System.out.println("Wrote net");
		// }

		PlansWriter plansWriter = new PlansWriter(plans);
		plans.setPlansWriter(plansWriter);
		System.out.println("  done.");

		FromToSummary alg = new FromToSummary();
		plans.addAlgorithm(alg);

		long preProcessTimer = System.currentTimeMillis();
		plans.printPlansCount();

		if (Gbl.getConfig().plans().switchOffPlansStreaming()) {
			preProcessTimer = System.currentTimeMillis();
			plans.runAlgorithms();
		}

		preProcessTimer = System.currentTimeMillis() - preProcessTimer;

		boolean doPrintSummary = false;
		if (doPrintSummary) {
			alg.printSummary();
		}

		plans.clearAlgorithms();

		System.out.println("  running plans algorithm... ");
		PlansCalcRoute router = null;
		if (routingAlgo != null) {
			router = new PlansCalcRoute(routingAlgo);
			plans.addAlgorithm(router);
		}
		long now = System.currentTimeMillis();
		if (Gbl.getConfig().plans().switchOffPlansStreaming()) {
			now = System.currentTimeMillis();
			plans.runAlgorithms();
		}
		long timer = preProcessTimer + System.currentTimeMillis() - now;
		System.out.println("  done.");

		String routingAlgoName;
		if (router == null) {
			routingAlgoName = "no router";
		} else {
			routingAlgoName = router.getRouterAlgorithmName();
		}

		int tripCount = 0;
		Collection<Person> persons = plans.getPersons().values();
		for (Person person : persons) {
			for (Plan plan : person.getPlans()) {
				tripCount += (plan.getActsLegs().size() + 1) / 2;
			}
		}
		printNote("R O U T I N G   M E A S U R E M E N T",
				"Average elapsed time for routing of " + tripCount
						+ " trips using " + routingAlgoName + ":\n"
						+ (int) (timer / (24 * 60 * 60 * 1000F)) + " days "
						+ (int) ((timer / (60 * 60 * 1000F)) % 24) + " hours; "
						+ (int) ((timer / (60 * 1000F)) % 60) + " mins; "
						+ (int) ((timer / 1000F) % 60) + " secs; "
						+ (int) (timer % 1000) + " msecs; " + "(" + timer
						+ " msecs in total)");
		if (preProcessTimer > 0) {
			System.out.println("(Preprocessing took "
					+ (int) (preProcessTimer / (24 * 60 * 60 * 1000F))
					+ " days "
					+ (int) ((preProcessTimer / (60 * 60 * 1000F)) % 24)
					+ " hours; "
					+ (int) ((preProcessTimer / (60 * 1000F)) % 60) + " mins; "
					+ (int) ((preProcessTimer / 1000F) % 60) + " secs; "
					+ (int) (preProcessTimer % 1000) + " msecs; " + "("
					+ preProcessTimer + " msecs in total)");
		}

		plansWriter.write();

		System.out.println("  done.");
	}

	private static void updateLinkLengths() {

		Link maxDiscrLink = null;
		double maxDiscr = 0;
		int shortLinkCnt = 0;
		ArrayList<Link> superShortLinks = new ArrayList<Link>();
		ArrayList<Double> superShortLinkLengths = new ArrayList<Double>();
		for (Link link : network.getLinks().values()) {

			double linkLength = link.getLength();
			double eucDist = link.getFromNode().getCoord().calcDistance(
					link.getToNode().getCoord());
			if (linkLength < eucDist) {
				if (linkLength < eucDist - 5) {
					// System.out.println("Link " + link.getID() + ": length= "
					// + linkLength + ", euc. dist= " + eucDist);
					shortLinkCnt++;
					if (eucDist - linkLength > maxDiscr) {
						maxDiscr = eucDist - linkLength;
						maxDiscrLink = link;
					}
					if (linkLength < eucDist - 10) {
						superShortLinks.add(link);
						superShortLinkLengths.add(linkLength);
					}
				}
				link.setLength(eucDist);
			}
			// if (eucDist < linkLength) {
			// System.out.println("Link " + link.getID() + ": length= "
			// + linkLength + ", euc. dist= " + eucDist);
			// shortLinkCnt++;
			// if (linkLength - eucDist > maxDiscr) {
			// maxDiscr = linkLength - eucDist;
			// maxDiscrLink = link;
			// }
			// if (eucDist < linkLength) {
			// superShortLinks.add(link);
			// superShortLinkLengths.add(linkLength);
			// }
			// }
			// link.setLength(eucDist);
		}

		if (maxDiscrLink == null) {
			System.out.println("No link was updated!");
		} else {
			System.out.println("Link " + maxDiscrLink.getId()
					+ " has highest discrepancy: length= "
					+ (maxDiscrLink.getLength() + maxDiscr) + ", euc. dist= "
					+ maxDiscrLink.getLength());
			System.out.println(shortLinkCnt
					+ " links with length < euc. dist. - 5");
			System.out.println("Super short links:");
			for (int i = 0; i < superShortLinks.size(); i++) {
				Link l = superShortLinks.get(i);
				// System.out.println("Link " + l.getID() + ": length= "
				// + superShortLinkLengths.get(i) + ", euc. dist= "
				// + l.getLength());
				System.out.println(l.getOrigId());
			}
			NetworkWriter network_writer = new NetworkWriter(network);
			network_writer.write();
			System.out.println("Wrote network to "
					+ Gbl.getConfig().network().getOutputFile());
		}
	}

	private static void analyzeLinkLengths() {

		int linkCount = 0;
		double avgLinkLength = 0;
		double avgEucDist = 0;
		int shortLinkCnt = 0;
		double avgShortLinkLength = 0;
		double avgShortEucDist = 0;

		for (Link link : network.getLinks().values()) {

			double linkLength = link.getLength();
			double eucDist = link.getFromNode().getCoord().calcDistance(
					link.getToNode().getCoord());

			avgLinkLength = (avgLinkLength*linkCount + linkLength)
				/ (linkCount + 1);
			avgEucDist = (avgEucDist*linkCount + eucDist)
				/ (linkCount + 1);
			if (linkLength < eucDist) {
				avgShortLinkLength = (avgShortLinkLength*shortLinkCnt + linkLength)
				/ (shortLinkCnt + 1);
				avgShortEucDist = (avgShortEucDist*shortLinkCnt + eucDist)
				/ (shortLinkCnt + 1);
				shortLinkCnt++;
			}
			linkCount++;
		}

		System.out.println(linkCount + " links in total, of which " + shortLinkCnt + " are shorter than their eclidean distance");
		System.out.println("The average length of all links is " + avgLinkLength
				+ ", their avg eucl. dist is " + avgEucDist
				+ " (" + (1 - avgEucDist/avgLinkLength)*100 + "% deviation)");
		System.out.println("The average length of the short links is " + avgShortLinkLength
				+ ", their avg eucl. dist is " + avgShortEucDist
				+ " (" + (1 - avgShortEucDist/avgShortLinkLength)*100 + "% deviation)");
	}

	private static void analyzeNetwork() {
		System.out.println("Network name: " + Gbl.getConfig().network().getInputFile());
		System.out.println("Number of nodes: " + network.getNodes().size());
		System.out.println("Number of links: " + network.getLinks().size());
		double avgNodeDegree = 0;
		int i = 0;
		int oneWayNodeCount = 0;
		for (Node n : network.getNodes().values()) {
			avgNodeDegree = (i*avgNodeDegree + n.getIncidentLinks().size()) / (i+1);
			if (n.getOutLinks().size() == 1 && n.getInLinks().size() == 1) {
				oneWayNodeCount++;
			}
			i++;
		}
		System.out.println("Avg node degree: " + avgNodeDegree);
		System.out.println("Link density: " + (double)2*network.getLinks().size() /
				((double)network.getNodes().size()*(network.getNodes().size()-1)));
		double avgLinkLength = 0;
		double avgLinkFreespeed = 0;
		i = 0;
		double totalLength = 0;
		double totalEucDist = 0;
		for (Link l : network.getLinks().values()) {
			avgLinkLength = (i*avgLinkLength + l.getLength()) / (i+1);
			avgLinkFreespeed = (i*avgLinkFreespeed + l.getFreespeed()) / (i+1);
			totalLength += l.getLength();
			totalEucDist += l.getFromNode().getCoord().calcDistance(l.getToNode().getCoord());
			i++;
		}
		System.out.println("Number of one-way nodes: " + oneWayNodeCount);
		System.out.println("Avg link length: " + avgLinkLength);
		System.out.println("Avg link free speed: " + avgLinkFreespeed);
		System.out.println("Network density for switzerland: " + totalLength/41285e6);
		System.out.println("Detour index: " + totalEucDist/totalLength);
	}

	private static void printNetworkConvexHull(String[] args) {
		Vector<Node> nodes = new Vector<Node>();
		for (Node node : network.getNodes().values()) {
			nodes.add(node);
		}
		nodes = GrahamScan.computeHull(nodes);

		Polygon convexHull = new Polygon();
		for (Node n : nodes) {
			convexHull.addPoint((int)n.getCoord().getX(), (int)n.getCoord().getY());
		}

		System.out.println("  reading the network...");
		NetworkLayerBuilder.setNetworkLayerType(NetworkLayerBuilder.NETWORK_DEFAULT);
		network = (NetworkLayer) Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile("/home/lnicolas/data/studies/schweiz/2network/normalizedNetwork.xml");
		System.out.println("  done.");

		Collection<Node> allNodesCopy = new ArrayList<Node>(network.getNodes().values());
		int tot = network.getNodes().size();
		int i = 0;
		for (Node n : allNodesCopy) {
			if (convexHull.contains((int)n.getCoord().getX(), (int)n.getCoord().getY()) == false) {
				network.removeNode(n);
				i++;
			}
		}

		System.out.println("Removed " + i + " of " + tot + " nodes");

		NetworkCleaner cleaner = new NetworkCleaner();

		cleaner.run(network);

		NetworkWriter network_writer = new NetworkWriter(network);
		network_writer.write();
		System.out.println("Wrote network to "
				+ Gbl.getConfig().network().getOutputFile());
	}

	private static void countOneWayNodes() {
		int oneWayNodeCnt = 0;
		for (Node n : network.getNodes().values()) {
			if (n.getOutLinks().size() == 1) {
				oneWayNodeCnt++;
			}
		}

		System.out.println(oneWayNodeCnt + " out of " + network.getNodes().size()
				+ " nodes are one-way nodes (" + 100*oneWayNodeCnt/network.getNodes().size()
				+ "%)");
	}

	private static void generateRandomPlansInTravelZone(double xCenter, double yCenter, double nodeDist, int tripCount) {
		RandomPlansInTravelZoneGenerator gen = new RandomPlansInTravelZoneGenerator(xCenter, yCenter, nodeDist, tripCount);

		network.addAlgorithm(gen);

		network.runAlgorithms();

	}

	private static void generateRandomPlans(double fromToDistance, int tripCount) {
		RandomPlansGenerator gen
			= new RandomPlansGenerator(fromToDistance, tripCount);

//		network.addAlgorithm(gen);

//		network.runAlgorithms();

		gen.runDumb(network);

//		Plans plans = gen.getPlans();
//		FromToSummary sum = createPlansFromToSummary(plans, true);

	}

	private static Plans generateRandomPlans(int tripCount) {

		System.out.print("Generating " + tripCount + " random plans...");
		System.out.flush();

		ArrayList<Link> links = new ArrayList<Link>(network.getLinks().values());
		Plans plans = new Plans();
		PlansWriter plansWriter = new PlansWriter(plans);
		plans.setPlansWriter(plansWriter);

		try {

			for (int j = 0; j < tripCount; j++) {
				Person person = new Person(j + "", null, null, null, null, null);
				Plan plan = person.createPlan(null, null, "yes");

				int choice = (int) (Math.random() * links.size());
				plan.createAct("h", (Double)null, null, links.get(choice).getId().toString(),
						null, 6*3600+"", null, "false");

				plan.createLeg(Integer.toString(j), "car", null, null, null);

				// Get random link
				choice = (int) (Math.random() * links.size());

				plan.createAct("w", (Double)null, null, links.get(choice).getId().toString(),
						null, null, null, "false");
				plans.addPerson(person);
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		System.out.println("done.");

		links.clear();

		plansWriter.write();

		System.out.println("Wrote plans to " + Gbl.getConfig().plans().getOutputFile());

		return plans;
	}

	private static void printFromToAvgDistance() {
		Plans plans = new Plans();

		FromToSummary plansStatistics = createPlansFromToSummary(plans, false);
		double maxDist = 0;
		double avgDist = 0;
		int i = 0;

		ArrayList<Double> lengthList = new ArrayList<Double>();
		Iterator plansIt = plansStatistics.getFromToMap().entrySet().iterator();
		while (plansIt.hasNext()) {
			FromToSummary.NodePair nodePair
				= (FromToSummary.NodePair)((Map.Entry)plansIt.next()).getKey();
			double l = nodePair.getFirst().getCoord().calcDistance(nodePair.getSecond().getCoord());

			lengthList.add(l);
			avgDist = (avgDist*i + l) / (i+1);
			if (l > maxDist) {
				maxDist = l;
			}
			i++;
		}

		System.out.println("Avg dist = " + avgDist + " (#node pairs: " + i + ")");
		Collections.sort(lengthList);
		double median = -1;
		if (lengthList.size() % 2 == 0) {
			median = (lengthList.get(lengthList.size() / 2) + lengthList
				.get((lengthList.size() / 2) - 1)) / 2;
		} else {
			median = lengthList.get((lengthList.size() - 1) / 2);
		}
		System.out.println("Median dist = " + median + " (#node pairs: " + i + ")");

		double binLength = 500;
		ArrayList<Integer> lengthDistr = new ArrayList<Integer>();
		double binCount = 0;
		while (binCount <= maxDist) {
			lengthDistr.add(0);
			binCount += binLength;
		}
		// Add a bin for the 0 distance
		lengthDistr.add(0);

		plansIt = plansStatistics.getFromToMap().entrySet().iterator();
		while (plansIt.hasNext()) {
			FromToSummary.NodePair nodePair
				= (FromToSummary.NodePair)((Map.Entry)plansIt.next()).getKey();
			double l = nodePair.getFirst().getCoord().calcDistance(nodePair.getSecond().getCoord());
			int ind = (int)(l/binLength);
			if (l > 0) {
				ind++;
			}
			int c = lengthDistr.get(ind);
			c++;
			lengthDistr.set(ind, c);
		}

		String lengthsString = "0,";
		String countString = lengthDistr.get(0) + ",";
		for (i = 1; i < lengthDistr.size(); i++) {
			countString += lengthDistr.get(i) + ",";
			lengthsString += ((i-1)*binLength)/1000 + "-" + (i*binLength)/1000 + ",";
		}
		BufferedWriter out;
		String filename = "/home/lnicolas/FromToDistDistr.csv";
		try {
			out = new BufferedWriter(new FileWriter(filename));
			out.write(lengthsString);
			out.newLine();
			out.write(countString);
			out.newLine();

		out.close();
		} catch (IOException e) {
			System.out.println("Error writing to file " + filename + ": "
					+ e.getMessage());
			// e.printStackTrace();
		}
		System.out.println("Link length distribution written to " + filename);
	}

	// ////////////////////////////////////////////////////////////////////
	// calcRoute
	// ////////////////////////////////////////////////////////////////////

	public static FromToSummary createPlansFromToSummary(Plans plans,
			boolean doPrintSummary) {
		System.out.println("RUN: createPlansFromToSummary");

		if (Gbl.getConfig().plans().switchOffPlansStreaming()) {
			plans = readPlans();
		}
		AvgRouteDistance alg2 = new AvgRouteDistance();
		plans.addAlgorithm(alg2);
		FromToSummary alg = new FromToSummary();
		plans.addAlgorithm(alg);

		if (Gbl.getConfig().plans().switchOffPlansStreaming()) {
			plans.runAlgorithms();
		} else {
			PlansReaderI plansReader = new MatsimPlansReader(plans);
			plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		}

		if (doPrintSummary) {
			alg.printSummary();
		}
		alg2.printSummary();

		plans.clearAlgorithms();

		return alg;
	}

	public static void calcAndAnalyzeRoute() {
		LeastCostPathCalculator routingAlgo = null;

		double speedUpFactor = 1;

		PreProcessEuclidean pp = new PreProcessEuclidean(new FreespeedTravelTimeCost());
		pp.run(network);

		for (speedUpFactor = 0.95; speedUpFactor >= 0.1; speedUpFactor -= 0.05) {
			routingAlgo = new AStarEuclidean(network, pp,
					new TravelTimeCalculator(network), speedUpFactor);

			System.out.println("  setting up plans objects...");
			Plans plans = new Plans();
			PlansReaderI plansReader = new MatsimPlansReader(plans);
			System.out.println("  done.");

			System.out.println("  running plans algorithm... ");

			PlansCalcRoute router = null;
			if (routingAlgo != null) {
				router = new PlansCalcRoute(routingAlgo);
				plans.addAlgorithm(router);
			}
			PlansCalcTravelDistance calcDist = new PlansCalcTravelDistance();
			plans.addAlgorithm(calcDist);
			PlansRouteSummary routeSummary = new PlansRouteSummary();
			plans.addAlgorithm(routeSummary);
			long now = System.currentTimeMillis();
			plansReader.readFile(Gbl.getConfig().plans().getInputFile());
			if (Gbl.getConfig().plans().switchOffPlansStreaming()) {
				now = System.currentTimeMillis();
				plans.runAlgorithms();
			}
			plans.printPlansCount();
			now = System.currentTimeMillis() - now;
			routeSummary.printSummary();
			System.out.println("  done.");

			String routingAlgoName;
			if (router == null) {
				routingAlgoName = "no router";
			} else {
				routingAlgoName = router.getRouterAlgorithmName();
			}
			String pqTypeString = "";
			int tripCount = 0;
			Collection<Person> persons = plans.getPersons().values();
			for (Person person : persons) {
				for (Plan plan : person.getPlans()) {
					tripCount += (plan.getActsLegs().size() + 1) / 2;
				}
			}
			printNote("R O U T I N G   M E A S U R E M E N T",
					"Average elapsed time for routing of " + tripCount
							+ " trips using " + routingAlgoName + pqTypeString
							+ ":\n" + (int) (now / (24 * 60 * 60 * 1000F))
							+ " days " + (int) ((now / (60 * 60 * 1000F)) % 24)
							+ " hours; " + (int) ((now / (60 * 1000F)) % 60)
							+ " mins; " + (int) ((now / 1000F) % 60)
							+ " secs; " + (int) (now % 1000) + " msecs; " + "("
							+ now + " msecs in total)");
		}
	}

	public static void compareRoute(String[] args) {
		Plans plans = new Plans();

		PlansReaderI plansReader = new MatsimPlansReader(plans);

		FreespeedTravelTimeCost calculator = new FreespeedTravelTimeCost();
		LeastCostPathCalculator routingAlgo = getRoutingAlgo(args,
				getPreProcessData(args, calculator), calculator, calculator);
		PlansCalcAndCompareRoute routeCompare = new PlansCalcAndCompareRoute(
				routingAlgo);
		plans.addAlgorithm(routeCompare);
		long now = System.currentTimeMillis();
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		if (Gbl.getConfig().plans().switchOffPlansStreaming()) {
			now = System.currentTimeMillis();
			plans.runAlgorithms();
		}
		plans.printPlansCount();
		now = System.currentTimeMillis() - now;
		int tripCount = 0;
		Collection<Person> persons = plans.getPersons().values();
		for (Person person : persons) {
			for (Plan plan : person.getPlans()) {
				tripCount += (plan.getActsLegs().size() + 1) / 2;
			}
		}
		printNote("R O U T I N G   M E A S U R E M E N T",
				"Average elapsed time for routing of " + tripCount
						+ " trips using "
						+ routeCompare.getRouterAlgorithmName() + ":\n"
						+ (int) (now / (24 * 60 * 60 * 1000F)) + " days "
						+ (int) ((now / (60 * 60 * 1000F)) % 24) + " hours; "
						+ (int) ((now / (60 * 1000F)) % 60) + " mins; "
						+ (int) ((now / 1000F) % 60) + " secs; "
						+ (int) (now % 1000) + " msecs; " + "(" + now
						+ " msecs in total)");

		routeCompare.printSummary();
	}

	public static void calcRoute(String[] args, Plans plans) {

		System.out.println("RUN: calcRoute");

		TravelMinCostI calculator;
		if (args[args.length - 1].equals("ptc")) {
			calculator = new PeakTravTimeCalc(network);
		} else {
			calculator = new FreespeedTravelTimeCost();
		}
		PreProcessDijkstra preProcessData = getPreProcessData(args, calculator);
		if (preProcessData != null) {
			long now = System.currentTimeMillis();
			preProcessData.run(network);
			now = System.currentTimeMillis() - now;
			printNote("", "Elapsed time for preprocessing:\n" + (int) (now / (24 * 60 * 60 * 1000F))
							+ " days " + (int) ((now / (60 * 60 * 1000F)) % 24)
							+ " hours; " + (int) ((now / (60 * 1000F)) % 60)
							+ " mins; " + (int) ((now / 1000F) % 60) + " secs; "
							+ (int) (now % 1000) + " msecs; " + "(" + now
							+ " msecs in total)");
		}

		LeastCostPathCalculator routingAlgo = getRoutingAlgo(args, preProcessData,
				(TravelCostI)calculator, (TravelTimeI)calculator);

//		PlansWriter plansWriter = new PlansWriter(plans);
//		plans.setPlansWriter(plansWriter);

		System.out.println("  running plans algorithm... ");
		// plans.clearAlgorithms();
		PlansCalcRoute router = null;
		if (routingAlgo != null) {
			router = new PlansCalcRoute(routingAlgo);
			plans.addAlgorithm(router);
		}
		long now = System.currentTimeMillis();
		plans.printPlansCount();
		if (Gbl.getConfig().plans().switchOffPlansStreaming()) {
			now = System.currentTimeMillis();
			plans.runAlgorithms();
		}
		now = System.currentTimeMillis() - now;
		System.out.println("  done.");

		String routingAlgoName;
		if (router == null) {
			routingAlgoName = "no router";
		} else {
			routingAlgoName = router.getRouterAlgorithmName();
		}

		int tripCount = 0;
		Collection<Person> persons = plans.getPersons().values();
		for (Person person : persons) {
			for (Plan plan : person.getPlans()) {
				tripCount += (plan.getActsLegs().size() - 1) / 2;
			}
		}
		printNote("R O U T I N G   M E A S U R E M E N T",
				"Average elapsed time for routing of " + tripCount
						+ " trips using " + routingAlgoName
						+ ":\n" + (int) (now / (24 * 60 * 60 * 1000F))
						+ " days " + (int) ((now / (60 * 60 * 1000F)) % 24)
						+ " hours; " + (int) ((now / (60 * 1000F)) % 60)
						+ " mins; " + (int) ((now / 1000F) % 60) + " secs; "
						+ (int) (now % 1000) + " msecs; " + "(" + now
						+ " msecs in total)");

		if (routingAlgo != null && routingAlgo instanceof Dijkstra) {
			((Dijkstra) routingAlgo).printInformation();
		}

//		if (plansWriter != null) {
//			plansWriter.write();
//			System.out.println("Wrote plans to "
//					+ Gbl.getConfig().getParam(Config.PLANS, Config.PLANS_OUTFILE));
//		}

		System.out.println("RUN: calcRoute finished.");
		System.out.println();
	}

	private static PreProcessDijkstra getPreProcessData(String[] args,
			TravelMinCostI calculator) {
		PreProcessDijkstra preProcessData = null;
		final String dijkstraString = "dijkstra";
		final String aStarString = "aStar";
		final String aStarLandmarkString = "aStarL";
		final String dijkstraPruneDeadEndsString = "dijkstraP";
		final String aStarFastString = "aStarF";
		final String aStarFast2String = "aStarF2";
		final String aStarFast3String = "aStarF3";
		final String aStarFast1_5String = "aStarF1.5";

		if (args.length < 3) {
			Gbl.errorMsg("Please provide the name of the routing algorithm: "
					+ dijkstraString + " or " + aStarString + " or "
					+ aStarLandmarkString);
		}
		if (args[2].equals(dijkstraPruneDeadEndsString)) {
			preProcessData = new PreProcessDijkstra();
		} else if (args[2].equals(aStarString)
				|| args[2].equals(aStarFast1_5String)
				|| args[2].equals(aStarFast2String)
				|| args[2].equals(aStarFastString)
				|| args[2].equals(aStarFast3String)) {
			preProcessData = new PreProcessEuclidean(calculator);
		} else if (args[2].equals(aStarLandmarkString)) {
			if (args.length > 3) {
				try {
					int landmarkCount = Integer.parseInt(args[3]);
					preProcessData = new PreProcessLandmarks(calculator, landmarkCount);
				} catch (NumberFormatException e) {
					System.out.println("Could not parse " + args[3]
							+ ". Ignoring it.");
					preProcessData = new PreProcessLandmarks(calculator);
				}
			} else {
				preProcessData = new PreProcessLandmarks(calculator);
			}
		}

		return preProcessData;
	}

//	private static LeastCostPathCalculator getTestingRoutingAlgo(String[] args,
//			TravelMinCostI calculator) {
//		final String aStarString = "aStar";
//		final String aStarLandmarkString = "aStarL";
//
//		if (args[2].equals(aStarString)) {
//			return new NetworkAStarTesting(network, calculator);
//		} else  if (args[2].equals(aStarLandmarkString)) {
//			return new NetworkAStarLandmarks4Testing(network, calculator);
//		} else {
//			return getRoutingAlgo(args, getPreProcessData(args, calculator), calculator);
//		}
//	}

	private static LeastCostPathCalculator getRoutingAlgo(String[] args,
			PreProcessDijkstra preProcessData, TravelCostI costCalc,
			TravelTimeI timeCalc) {
		final String dijkstraOldString = "dijkstraOld";
		final String dijkstraString = "dijkstra";
		final String dijkstraPruneDeadEndsString = "dijkstraP";
		final String aStarString = "aStar";
		final String aStarFastString = "aStarF";
		final String aStarFast2String = "aStarF2";
		final String aStarFast3String = "aStarF3";
		final String aStarFast1_5String = "aStarF1.5";
		final String aStarLandmarkString = "aStarL";
		final String noneString = "none";

		if (args.length < 3) {
			Gbl.errorMsg("Please provide the name of the routing algorithm: "
					+ dijkstraString + " or " + dijkstraOldString + " or "
					+ aStarString + " or " + aStarFastString + " or "
					+ aStarLandmarkString + " or "
					+ dijkstraPruneDeadEndsString + " or " + noneString);
			return null;
		}
		if (args[2].equals(dijkstraString)) {
			return new Dijkstra(network, costCalc, timeCalc, null);
		} else if (args[2].equals(dijkstraPruneDeadEndsString)) {
			return new Dijkstra(network, costCalc, timeCalc, preProcessData);
		} else if (args[2].equals(dijkstraOldString)) {
			return new NetworkDijkstra(network, costCalc, timeCalc);
		} else if (args[2].equals(aStarString)) {
			return new AStarEuclidean(network, (PreProcessEuclidean)preProcessData, timeCalc);
		} else if (args[2].equals(aStarFast3String)) {
			return new AStarEuclidean(network, (PreProcessEuclidean)preProcessData,
				timeCalc, 3);
		} else if (args[2].equals(aStarFast2String)) {
			return new AStarEuclidean(network, (PreProcessEuclidean)preProcessData,
					 timeCalc, 2);
		} else if (args[2].equals(aStarFast1_5String)) {
			return new AStarEuclidean(network, (PreProcessEuclidean)preProcessData,
					 timeCalc, 1.5);
		} else if (args[2].equals(aStarFastString)) {
			if (args.length > 3) {
				try {
					double speedUpFactor = Double.parseDouble(args[3]);
					return new AStarEuclidean(network, (PreProcessEuclidean)preProcessData,
							 timeCalc, speedUpFactor);
				} catch (NumberFormatException e) {
					System.out.println("Could not parse " + args[3]
							+ ". Ignoring it.");
					return new AStarEuclidean(network, (PreProcessEuclidean)preProcessData,
							 timeCalc);
				}
			} else {
				return new AStarEuclidean(network, (PreProcessEuclidean)preProcessData,
						 timeCalc);
			}
		} else if (args[2].equals(aStarLandmarkString)) {
			return new AStarLandmarks(network,
					(PreProcessLandmarks)preProcessData, timeCalc);
		} else if (!args[2].equals(noneString)) {
			Gbl.errorMsg("The name of the routing algorithm must be "
					+ dijkstraString + " or " + aStarString + " or "
					+ aStarFast3String + " or " + aStarFast2String + " or "
					+ dijkstraOldString + " or " + aStarFastString + " or "
					+ aStarLandmarkString + " or "
					+ dijkstraPruneDeadEndsString + " or " + noneString);
		}
		return null;
	}
}
