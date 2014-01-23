/* *********************************************************************** *
 * project: org.matsim.*
 * RandomDijkstraRoute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.MultiNodeDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import playground.christoph.router.util.SimpleRouter;

public class RandomDijkstraRoute extends SimpleRouter {

	protected static int errorCounter = 0;
	
	protected boolean removeLoops = false;
	protected double dijkstraWeightFactor = 0.5;
	protected LeastCostPathCalculator leastCostPathCalculator;
	protected TravelDisutilityFactory travelCostFactory;
	protected TravelDisutility travelCost;
	protected TravelTime travelTime;
	protected int maxLinks = 50000; // maximum number of links in a created plan
	
	private final static Logger log = Logger.getLogger(RandomDijkstraRoute.class);
	
	public RandomDijkstraRoute(Network network, TravelDisutilityFactory travelCostFactory, TravelTime travelTime) {
		super(network);
		this.travelCostFactory = travelCostFactory;
		this.travelTime = travelTime;
		this.travelCost = travelCostFactory.createTravelDisutility(travelTime, new PlanCalcScoreConfigGroup());
		this.leastCostPathCalculator = new MultiNodeDijkstraFactory().createPathCalculator(network, travelCost, travelTime);
	}
	
	@Override
	public Path calcLeastCostPath(Node fromNode, Node toNode, double startTime, final Person person, final Vehicle vehicle) {
		return findRoute(fromNode, toNode, person, vehicle);
	}
	
	public void setDijsktraWeightFactor(double weightFactor) {
		this.dijkstraWeightFactor = weightFactor;
	}
	
	private Path findRoute(Node fromNode, Node toNode, final Person person, final Vehicle vehicle) {
		Node currentNode = fromNode;
		Link currentLink;
		double routeLength = 0.0;
		
		ArrayList<Node> nodes = new ArrayList<Node>();
		ArrayList<Link> links = new ArrayList<Link>();
		
		nodes.add(fromNode);
		
		while(!currentNode.equals(toNode)) {
			// stop searching if to many links in the generated Route...
			if (nodes.size() > maxLinks) {
				log.warn("Route has reached the maximum allowed length - break!");
				errorCounter++;
				break;
			}
			if (nodes.size() > maxLinks) {
				log.warn("Routelength has reached the maximum allowed number of links - stop searching!");
				break;
			}
			
			Link[] linksArray = currentNode.getOutLinks().values().toArray(new Link[currentNode.getOutLinks().size()]);
					
			if (linksArray.length == 0) {
				log.error("Looks like Node is a dead end. Routing could not be finished!");
				break;
			}
							
			/*
			 * Dijkstra
			 */
			double[] dijkstraProbabilities = new double[linksArray.length];
						
			Path path = this.leastCostPathCalculator.calcLeastCostPath(currentNode, toNode, Time.UNDEFINED_TIME, person, vehicle);
			
			Link dijskstraNextLink = null;
			if (path.links.size() > 0) dijskstraNextLink = path.links.get(0);
			
			// set Probabilities
			for(int i = 0; i < dijkstraProbabilities.length; i++) {
				Link link = linksArray[i];
				if(link.equals(dijskstraNextLink)) dijkstraProbabilities[i] = 1.0;
				else dijkstraProbabilities[i] = 0.0;
			}
			
			/*
			 * Random
			 */
			double[] randomProbabilities = new double[linksArray.length];
			for(int i = 0; i < randomProbabilities.length; i++) randomProbabilities[i] = 1.0 / randomProbabilities.length;
			
			/*
			 * Merge Dijkstra and Random based on the dijkstraProbability.
			 * The higher the probability the more likely the next Link is
			 * chosen based on the results from the Dijkstra Algorithm.
			 * (p = 1.0 means that a Dijsktra Route will be returned) 
			 */
			for(int i = 0; i < dijkstraProbabilities.length; i++) dijkstraProbabilities[i] = dijkstraProbabilities[i] * dijkstraWeightFactor;
			for(int i = 0; i < randomProbabilities.length; i++) randomProbabilities[i] = randomProbabilities[i] * (1.0 - dijkstraWeightFactor);
			
			double[] sumProbabilities = new double[linksArray.length];
			for (int i = 0; i < sumProbabilities.length; i++) {
				sumProbabilities[i] = dijkstraProbabilities[i] + randomProbabilities[i];
			}
			
			/*
			 * Select next Link based on the summarized Probabilities.
			 */
			Link nextLink = null;
			double randomDouble = random.nextDouble();
			
			double sumProb = 0.0;
			for (int i = 0; i < sumProbabilities.length; i++) {
				/*
				 *  If randomDouble is exactly 0.0 select the first Link.
				 *  The next if statement wouldn't do it.
				 */
				if (randomDouble == 0.0) {
					nextLink = linksArray[0];
					break;
				}

				if(randomDouble > sumProb && randomDouble <= sumProb + sumProbabilities[i]) {
					nextLink = linksArray[i];
					break;
				}
							
				sumProb = sumProb + sumProbabilities[i];
			}
				
			// make the chosen link to the current link
			if(nextLink != null) {
				currentLink = nextLink;
				currentNode = currentLink.getToNode();
				routeLength = routeLength + currentLink.getLength();
			} else {
				log.error("Number of Links " + linksArray.length);
				log.error("Return object was null!");
				break;
			}
			
			nodes.add(currentNode);
			links.add(currentLink);
		}	// while(!currentNode.equals(toNode))

		Path path = new Path(nodes, links, 0, 0);

		if (maxLinks == path.links.size()) {
//			log.info("LinkCount " + path.links.size() + " distance " + routeLength);
		}

		if (removeLoops) loopRemover.removeLoops(path);
				
		return path;
	}
		
	public static int getErrorCounter() {
		return errorCounter;
	}
	
	public static void setErrorCounter(int i) {
		errorCounter = i;
	}
	
	@Override
	public SimpleRouter createInstance() {
		RandomDijkstraRoute route = new RandomDijkstraRoute(network, travelCostFactory, travelTime);
		route.setDijsktraWeightFactor(dijkstraWeightFactor);
		return route;
	}
}