/* *********************************************************************** *
 * project: org.matsim.*
 * PerformanceTest.java
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

package playground.christoph.router.priorityqueue;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.router.util.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.misc.Counter;

public class PerformanceTest {

	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig();
//		config.network().setInputFile("../../matsim/mysimulations/census2000V2/input_10pct/network_IVTCH.xml.gz");
		config.network().setInputFile("../../matsim/mysimulations/census2000V2/input_10pct/network.xml.gz");
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		int numNodes = scenario.getNetwork().getNodes().values().size();
		Node[] nodes = new Node[numNodes];
		scenario.getNetwork().getNodes().values().toArray(nodes);
		
//		int numRoutes = 512;
		int numRoutes = 1024;
//		int numRoutes = 2048;
//		int numRoutes = 4096;
//		int numRoutes = 8192;
//		int numRoutes = 16384;
		
		TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutility travelDisutility = new RandomizingTimeDistanceTravelDisutility.Builder().createTravelDisutility(travelTime, config.planCalcScore());
//		LeastCostPathCalculatorFactory factory = new DijkstraFactory(); 
//		LeastCostPathCalculatorFactory factory = new AStarEuclideanFactory(scenario.getNetwork(), travelDisutility);
//		LeastCostPathCalculatorFactory factory = new AStarLandmarksFactory(scenario.getNetwork(), travelDisutility);
//		LeastCostPathCalculatorFactory factory = new FastDijkstraFactory();
//		LeastCostPathCalculatorFactory factory = new FastAStarEuclideanFactory(scenario.getNetwork(), travelDisutility);
		LeastCostPathCalculatorFactory factory = new FastAStarLandmarksFactory(scenario.getNetwork(), travelDisutility);
		
		LeastCostPathCalculator router = factory.createPathCalculator(scenario.getNetwork(), travelDisutility, travelTime);
		
		Counter counter = new Counter("# calculated routes: ");
		Gbl.startMeasurement();
		for (int i = 0; i < numRoutes; i++) {
			Node fromNode = nodes[MatsimRandom.getRandom().nextInt(numNodes)];
			Node toNode = nodes[MatsimRandom.getRandom().nextInt(numNodes)];
			
			router.calcLeastCostPath(fromNode, toNode, 0.0, null, null);
			counter.incCounter();
		}
		Gbl.printElapsedTime();
	}
}
