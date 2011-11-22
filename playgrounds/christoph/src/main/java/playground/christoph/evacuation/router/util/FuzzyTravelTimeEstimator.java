/* *********************************************************************** *
 * project: org.matsim.*
 * FuzzyTravelTimeEstimator.java
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

package playground.christoph.evacuation.router.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * Get travel times from a given travel time calculator and adds a random fuzzy value.
 * This values depends on the agent, the link and the distance between the link and 
 * the agent's current position. The calculation of the fuzzy values is deterministic, 
 * multiple calls with identical parameters will result in identical return values.
 * 
 * @author cdobler
 */
public class FuzzyTravelTimeEstimator implements PersonalizableTravelTime, LinkEnterEventHandler, 
	ActivityStartEventHandler, ActivityEndEventHandler {

	private final PersonalizableTravelTime travelTime;
	private final Scenario scenario;
	private final Map<Id, Coord> personLocations;

	private int pInt;
	private Id pId;
	private double personFuzzyFactor;
	
	public FuzzyTravelTimeEstimator(PersonalizableTravelTime travelTime, Scenario scenario) {
		this.travelTime = travelTime;
		this.scenario = scenario;
		
		this.personLocations = new HashMap<Id, Coord>();
		
		/*
		 * Get persons' coordinates when the simulation starts.
		 */
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Activity activity = (Activity) person.getSelectedPlan().getPlanElements().get(0);
			Coord coord = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(activity.getFacilityId()).getCoord();
			personLocations.put(person.getId(), coord);
		}
	}
	
	@Override
	public double getLinkTravelTime(Link link, double time) {
			
		double tt = this.travelTime.getLinkTravelTime(link, time);
		
		double distanceFuzzyFactor = calcDistanceFuzzyFactor(link);
		double linkFuzzyFactor = calcLinkFuzzyFactor(link);
		
		/*
		 * Sum of the three factors is between ~0.0 and 3.0
		 * Shift it by -1.5 and scale it by 3 to have an interval 
		 * between -0.50..0.50
		 */
		double factor = (personFuzzyFactor + distanceFuzzyFactor + linkFuzzyFactor - 1.5)/3;
		
		return tt * factor;
	}

	@Override
	public void setPerson(Person person) {
		this.travelTime.setPerson(person);
		this.pId = person.getId();
		
//		this.pInt = Integer.valueOf(this.pId.toString());
		this.pInt = person.getId().toString().hashCode();
		Random pRandom = new Random(pInt);
		prepareRNG(pRandom);
		this.personFuzzyFactor = pRandom.nextDouble();
	}

	/*
	 * So far use hard-coded values between 0.017 (distance 0.0) 
	 * and 1.0 (distance ~ 15000.0).
	 */
	private double calcDistanceFuzzyFactor(Link link) {
		double distance = CoordUtils.calcDistance(this.personLocations.get(pId), link.getCoord());
		
		return (1 / (Math.exp(-distance/1500.0) + 4.0));
	}
	
	/*
	 * Returns a fuzzy value between 0.0 and 1.0 which
	 * depends on the current person and the given link. 
	 */
	private double calcLinkFuzzyFactor(Link link) {		
//		int lInt =  Integer.valueOf(link.getId().toString());
		int lInt = link.getId().toString().hashCode();
		Random lRandom = new Random(lInt + pInt);
		prepareRNG(lRandom);
		return lRandom.nextDouble();
	}
	
	/*
	 * Draw some random numbers to better initialize the pseudo-random number generator.
	 *
	 * @param rng the random number generator to initialize.
	 */
	private static void prepareRNG(final Random rng) {
		for (int i = 0; i < 10; i++) {
			rng.nextDouble();
		}
	}


	@Override
	public void handleEvent(LinkEnterEvent event) {
		Coord coord = scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
		personLocations.put(event.getPersonId(), coord);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		Coord coord = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(event.getFacilityId()).getCoord();
		personLocations.put(event.getPersonId(), coord);
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		Coord coord = scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
		personLocations.put(event.getPersonId(), coord);
	}

	
	@Override
	public void reset(int iteration) {
		this.personLocations.clear();
	}
}
