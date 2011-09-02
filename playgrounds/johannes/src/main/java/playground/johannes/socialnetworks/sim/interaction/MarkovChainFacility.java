/* *********************************************************************** *
 * project: org.matsim.*
 * MarkovChainFacility.java
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
package playground.johannes.socialnetworks.sim.interaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.sna.util.ProgressLogger;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.johannes.socialnetworks.sim.locationChoice.ActivityMover;

/**
 * @author illenberger
 *
 */
public class MarkovChainFacility extends MarkovChain {

	private static final Logger logger = Logger.getLogger(MarkovChainFacility.class);
	
	private static int numChoices = 10;
	
	private static double alpha = -1.4;
	
	private Map<Person, List<Id>> choiceSets;
	
	/**
	 * @param mover
	 * @param desiredArrivalTimes
	 * @param desiredDurations
	 * @param random
	 */
	public MarkovChainFacility(ActivityMover mover, Map<Person, Double> desiredArrivalTimes,
			Map<Person, Double> desiredDurations, Random random, Population population, ActivityFacilities facilities, Network network) {
		super(mover, desiredArrivalTimes, desiredDurations, random);
		
		List<ActivityFacility> lFacilities = new ArrayList<ActivityFacility>(facilities.getFacilities().size());
		for(Entry<Id, ? extends ActivityFacility> entry : facilities.getFacilities().entrySet()) {
			ActivityFacility facility = entry.getValue();
			for(ActivityOption option : facility.getActivityOptions().values()) {
				if(option.getType().equals("leisure")) {
					lFacilities.add(facility);
					Link link = ((NetworkImpl)network).getNearestLink(facility.getCoord());
					((ActivityFacilityImpl)facility).setLinkId(link.getId());
					break;
				}
			}
		}
		
		choiceSets = new HashMap<Person, List<Id>>(population.getPersons().size());
		
		logger.info("Generating choice sets...");
		ProgressLogger.init(population.getPersons().size(), 1, 5);
		for(Person person : population.getPersons().values()) {
			Activity home = (Activity) person.getSelectedPlan().getPlanElements().get(0);
			Link link = network.getLinks().get(home.getLinkId());
			Coord c1 = link.getCoord();
			double sum = 0;
			for(ActivityFacility facility : lFacilities) {
				double d = CoordUtils.calcDistance(c1, facility.getCoord());
				sum += Math.pow(d, alpha); 
			}
			
			List<Id> choiceSet = new ArrayList<Id>((int) (numChoices * 1.5));
			for(ActivityFacility facility : lFacilities) {
				double d = CoordUtils.calcDistance(c1, facility.getCoord());
				double p = numChoices/sum * Math.pow(d, alpha);
				if(random.nextDouble() < p)
					choiceSet.add(facility.getId());
					
			}
			
			choiceSets.put(person, choiceSet);
			ProgressLogger.step();
		}
	}

	@Override
	protected List<Id> generateChoiceSet(Set<Person> egos) {
		List<Id> choices = new ArrayList<Id>((int) (egos.size() * numChoices * 1.5));
		for(Person ego : egos) {
			choices.addAll(choiceSets.get(ego));
		}
		return choices;
	}

}
