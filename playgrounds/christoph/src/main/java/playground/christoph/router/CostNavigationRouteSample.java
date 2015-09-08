/* *********************************************************************** *
 * project: org.matsim.*
 * CostNavigationRouteSample.java
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

package playground.christoph.router;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

public class CostNavigationRouteSample {

//	// link costs
//    clink[1, 2] = 7.2;
//    clink[1, 3] = 11.7;
//    clink[2, 4] = 7.7;
//    clink[2, 6] = 11.1;
//    clink[3, 4] = 12.6;
//    clink[3, 5] = 12.7;
//    clink[4, 5] = 10.8;
//    clink[4, 6] = 8.9;
//    clink[5, 7] = 24.5;
//    clink[6, 7] = 30.7;
    
	private static final Logger log = Logger.getLogger(CostNavigationRouteSample.class);
	
	/* A constant for the exactness when comparing doubles. */
	public static final double EPSILON = 1e-10;
	
	/* 
	 * Command line parameters
	 */
	/*package*/ boolean agentsLearn = true;
	/*package*/ double gamma = 1.00;
	/*package*/ double tau = 1.10;
	/*package*/ double tauplus = 0.75;
	/*package*/ double tauminus = 1.25;
	/*package*/ int followedAndAccepted = 1;
	/*package*/ int followedAndNotAccepted = 1;
	/*package*/ int notFollowedAndAccepted = 1;
	/*package*/ int notFollowedAndNotAccepted = 1;
	/*package*/ String eventsFile = null;
	
	public static void main(String[] args) {
		new CostNavigationRouteSample();
	}
	
    public CostNavigationRouteSample() {
    
    	Config config = ConfigUtils.createConfig();
    	Scenario scenario = ScenarioUtils.createScenario(config);
    	
    	createNetwork(scenario);
    	
    	TravelTime travelTime = new TravelTimeCost();
		OnlyTimeDependentTravelDisutilityFactory travelDisutilityFactory = new OnlyTimeDependentTravelDisutilityFactory();
		TravelDisutility travelDisutility = travelDisutilityFactory.createTravelDisutility(travelTime, null);
		
		LeastCostPathCalculatorFactory factory = new FastDijkstraFactory();
		LeastCostPathCalculator leastCostPathCalculator = factory.createPathCalculator(scenario.getNetwork(), travelDisutility, travelTime);
		
		for (int i = 1; i < 7; i++) {
			calcRoute(scenario, leastCostPathCalculator, travelDisutility, scenario.getNetwork().getNodes().get(Id.create("n" + i, Node.class)), scenario.getNetwork().getNodes().get(Id.create("n7", Node.class)));			
		}
		
		log.info("done");
    }
    
    private void createNetwork(Scenario scenario) {    	
    	
    	Network network = scenario.getNetwork();
    	NetworkFactory networkFactory = network.getFactory();

		Node n1 = networkFactory.createNode(Id.create("n1", Node.class), new Coord(0.0, 0.0));
		Node n2 = networkFactory.createNode(Id.create("n2", Node.class), new Coord(0.0, 0.0));
		Node n3 = networkFactory.createNode(Id.create("n3", Node.class), new Coord(0.0, 0.0));
		Node n4 = networkFactory.createNode(Id.create("n4", Node.class), new Coord(0.0, 0.0));
		Node n5 = networkFactory.createNode(Id.create("n5", Node.class), new Coord(0.0, 0.0));
		Node n6 = networkFactory.createNode(Id.create("n6", Node.class), new Coord(0.0, 0.0));
		Node n7 = networkFactory.createNode(Id.create("n7", Node.class), new Coord(0.0, 0.0));
    	
    	network.addNode(n1);
    	network.addNode(n2);
    	network.addNode(n3);
    	network.addNode(n4);
    	network.addNode(n5);
    	network.addNode(n6);
    	network.addNode(n7);
    	    	
        Link l1 = networkFactory.createLink(Id.create("l1", Link.class), n1, n2);
        Link l2 = networkFactory.createLink(Id.create("l2", Link.class), n1, n3);
        Link l3 = networkFactory.createLink(Id.create("l3", Link.class), n2, n4);
        Link l4 = networkFactory.createLink(Id.create("l4", Link.class), n2, n6);
        Link l5 = networkFactory.createLink(Id.create("l5", Link.class), n3, n4);
        Link l6 = networkFactory.createLink(Id.create("l6", Link.class), n3, n5);
        Link l7 = networkFactory.createLink(Id.create("l7", Link.class), n4, n5);
        Link l8 = networkFactory.createLink(Id.create("l8", Link.class), n4, n6);
        Link l9 = networkFactory.createLink(Id.create("l9", Link.class), n5, n7);
        Link l10 = networkFactory.createLink(Id.create("l10", Link.class), n6, n7);
        
        // use link length field to store link travel costs
        l1.setLength(7.2);
        l2.setLength(11.7);
        l3.setLength(7.7);
        l4.setLength(11.1);
        l5.setLength(12.6);
        l6.setLength(12.7);
        l7.setLength(10.8);
        l8.setLength(8.9);
        l9.setLength(24.5);
        l10.setLength(30.7);

        network.addLink(l1);
        network.addLink(l2);
        network.addLink(l3);
        network.addLink(l4);
        network.addLink(l5);
        network.addLink(l6);
        network.addLink(l7);
        network.addLink(l8);
        network.addLink(l9);
        network.addLink(l10);
    }

	private void calcRoute(Scenario scenario, LeastCostPathCalculator leastCostPathCalculator, 
			TravelDisutility travelDisutility, Node fromNode, Node toNode) {

		double phi = 1 - gamma;
			
		Map<Id<Link>, ? extends Link> outLinksMap = fromNode.getOutLinks();
		Map<Id, Path> paths = new TreeMap<Id, Path>();	// outLinkId
		Map<Id, Double> costs = new TreeMap<Id, Double>();	// outLinkId
		Map<Id, Double> probabilities = new TreeMap<Id, Double>();	// outLinkId
		
		/*
		 * Calculate path costs for each outgoing link
		 */
		double leastCosts = Double.MAX_VALUE;
		Id leastCostLinkId = null;
		for (Link outLink : outLinksMap.values()) {
			double outlinkTravelDisutility = travelDisutility.getLinkTravelDisutility(outLink, 0.0, null, null);
			Path path = leastCostPathCalculator.calcLeastCostPath(outLink.getToNode(), toNode, 0.0, null, null);
			paths.put(outLink.getId(), path);
			costs.put(outLink.getId(), path.travelCost + outlinkTravelDisutility);
			if (path.travelCost + outlinkTravelDisutility < leastCosts) {
				leastCosts = path.travelCost + outlinkTravelDisutility;
				leastCostLinkId = outLink.getId();
			}
			
//			log.info("outlink " + outLink.getId() + " path costs " + path.travelCost);
//			String pathString = "";
//			for (Link link : path.links) pathString += (" " + link.getId());
//			log.info("outlink " + outLink.getId() + " path " + pathString);
		}
//		log.info("least costs: " + leastCosts);
			
		/*
		 * Calculate the probabilities for each path. We use inverse values to
		 * give short travel times a higher probability.
		 * If phi = 0.0 (which means gamma0 = 1.0), we automatically use the 
		 * least cost link. Otherwise we would run into divide by zero problems.
		 */
		if (phi == 0.0) {
			for (Entry<Id, Double> entry : costs.entrySet()) {
				// if it is the least cost link
				if (entry.getKey().equals(leastCostLinkId)) probabilities.put(entry.getKey(), 1.0);
				
				// else
				else probabilities.put(entry.getKey(), 0.0);
			}
		}
		else {		
			double inverseSumLeastCosts = 0.0;
			for (Entry<Id, Double> entry : costs.entrySet()) {
				// if it is the least cost link
				if (entry.getKey().equals(leastCostLinkId)) inverseSumLeastCosts += 1 / (phi*entry.getValue());
				
				// else
				else inverseSumLeastCosts += 1 / entry.getValue();
			}
			for (Entry<Id, Double> entry : costs.entrySet()) {
				// if it is the least cost link
				if (entry.getKey().equals(leastCostLinkId)) probabilities.put(entry.getKey(), (1 / (phi*entry.getValue())) / inverseSumLeastCosts);
				
				// else
				else probabilities.put(entry.getKey(), (1 / entry.getValue()) / inverseSumLeastCosts);
			}
		}
		
		// theoretical values
		log.info("at node " + fromNode.getId());
		for (Entry<Id, Double> entry : probabilities.entrySet()) {
			log.info("\ttheoretical probability for link " + entry.getKey() + "\t" + entry.getValue());
		}
		
		// apply and verify selection algorithm
		Map<Id, Integer> linkSelection = new TreeMap<Id, Integer>();
		for (Id id : probabilities.keySet()) linkSelection.put(id, 0);
		double draws = 100000d;
		for (int i = 0; i < draws; i++) {
			
			double randomNumber = MatsimRandom.getRandom().nextDouble();
			
			double sumProb = 0.0;
			Id nextLinkId = null;
			for (Entry<Id, Double> entry : probabilities.entrySet()) {
				if (entry.getValue() + sumProb > randomNumber) {
					nextLinkId = entry.getKey();
					break;
				} else {
					sumProb += entry.getValue();
				}
			}
			linkSelection.put(nextLinkId, linkSelection.get(nextLinkId) + 1);
		}
		for (Entry<Id, Integer> entry : linkSelection.entrySet()) {
			log.info("\tsimulated probability for link " + entry.getKey() + "\t" + 
					entry.getValue()/draws + " (" + entry.getValue() + ")");
		}
		
		log.info("");
		
		/*
		 * Check whether the sum of all probabilities is 1.0
		 */
		double checkSum = 0.0;
		for (Double value : probabilities.values()) checkSum += value;
		assert Math.abs(checkSum - 1.0) < EPSILON;
	}
    
    private static class TravelTimeCost implements TravelTime {

		@Override
		public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
			return link.getLength();
		}
    	
    }
}
