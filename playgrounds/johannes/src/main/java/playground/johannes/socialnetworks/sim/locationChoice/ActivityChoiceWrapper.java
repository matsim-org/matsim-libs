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

import gnu.trove.TObjectDoubleHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.johannes.socialnetworks.graph.social.SocialGraph;

/**
 * @author illenberger
 *
 */
public class ActivityChoiceWrapper extends AbstractMultithreadedModule {

	private static final Logger logger = Logger.getLogger(ActivityChoiceWrapper.class);
	
	private final SocialGraph graph;
	
	private final Population population;
	
	private final Network network;
	
	private final TravelTime travelTime;
	
	private final Map<Person, Double> desiredArrivalTimes;
	
	private final Map<Person, Double> desiredDurations;
	
	private final Random random;
	
	private final ScenarioImpl scenario;
	
	private TObjectDoubleHashMap<Person> constants;
	
	final private List<Id> linkIds;
	
	public ActivityChoiceWrapper(int numOfThreads, SocialGraph graph, Population population, Network network, TravelTime travelTime, Map<Person, Double> desiredArrivalTimes, Map<Person, Double> desiredDurations, Random random, ScenarioImpl scenario) {
		super(numOfThreads);
		
		this.graph = graph;
		this.population = population;
		this.network = network;
		this.travelTime = travelTime;
		this.desiredArrivalTimes = desiredArrivalTimes;
		this.desiredDurations = desiredDurations;
		this.random = random;
		this.scenario = scenario;
		
		linkIds = new ArrayList<Id>();
		
		
		for(ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			boolean isLeisure = false;
			for(ActivityOption option : facility.getActivityOptions().values()) {
				if(option.getType().equalsIgnoreCase("leisure")) {
					isLeisure = true;
					break;
				}
			}
			
			if(isLeisure) {
				if(facility.getLinkId() != null)
					linkIds.add(facility.getLinkId());
				else {
					LinkImpl link = ((NetworkImpl)network).getNearestLink(facility.getCoord());
					if(link != null)
						linkIds.add(link.getId());
					else
						throw new RuntimeException("Unable to obtain link for facility.");
						
				}
			}
		}
		
		logger.info("Calculating norm constants...");
		Set<Person> persons = desiredArrivalTimes.keySet();
		constants = new TObjectDoubleHashMap<Person>();
		for(Person person : persons) {
			Link source = network.getLinks().get(((Activity)person.getSelectedPlan().getPlanElements().get(0)).getLinkId());
			double sum = 0;
			for(int i = 0; i < linkIds.size(); i++) {
				Link target = network.getLinks().get(linkIds.get(i));
				if (!source.equals(target)) {
					double d = CoordUtils.calcDistance(source.getCoord(), target.getCoord());
					sum += Math.pow(d, -1);
				}
			}
			constants.put(person, sum);
		}
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
		throw new RuntimeException();
//		ActivityMover mover = new ActivityMover(population.getFactory(), router, network);
//		ActivityChoiceRndFacility choice = new ActivityChoiceRndFacility(network, mover, random, desiredArrivalTimes, desiredDurations, linkIds, constants);
//		return choice;
//		return null;
	}

}
