/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityChoiceWrapper.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.sim.locationChoice;

import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.johannes.socialnetworks.graph.social.SocialGraph;

/**
 * @author illenberger
 *
 */
public class ActivityChoiceWrapper extends AbstractMultithreadedModule {

	private final SocialGraph graph;
	
	private final Population population;
	
	private final Network network;
	
	private final TravelTime travelTime;
	
	private final Map<Person, Double> desiredArrivalTimes;
	
	private final Map<Person, Double> desiredDurations;
	
	private final Random random;
	
	public ActivityChoiceWrapper(int numOfThreads, SocialGraph graph, Population population, Network network, TravelTime travelTime, Map<Person, Double> desiredArrivalTimes, Map<Person, Double> desiredDurations, Random random) {
		super(numOfThreads);
		
		this.graph = graph;
		this.population = population;
		this.network = network;
		this.travelTime = travelTime;
		this.desiredArrivalTimes = desiredArrivalTimes;
		this.desiredDurations = desiredDurations;
		this.random = random;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		TravelCost travelCost = new TravelCost() {
			
			@Override
			public double getLinkGeneralizedTravelCost(Link link, double time) {
				return travelTime.getLinkTravelTime(link, time);
			}
		};
		LeastCostPathCalculator router = new Dijkstra(network, travelCost, travelTime);
		ActivityMover mover = new ActivityMover(population.getFactory(), router, network);
		ActivityChoiceRndAlterHome choice = new ActivityChoiceRndAlterHome(graph, mover, random, desiredArrivalTimes, desiredDurations);
//		return choice;
		return null;
	}

}
