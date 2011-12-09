/* *********************************************************************** *
 * project: org.matsim.*
 * FuzzyTravelTimeDataCollector.java
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.scenario.ScenarioImpl;

public class FuzzyTravelTimeDataCollector implements LinkEnterEventHandler, ActivityStartEventHandler, ActivityEndEventHandler {

	private final Scenario scenario;
	private final Map<Id, Coord> agentLocations;
	
	public FuzzyTravelTimeDataCollector(Scenario scenario) {
		this.scenario = scenario;
		
		this.agentLocations = new HashMap<Id, Coord>();
		
		/*
		 * Get persons' coordinates when the simulation starts.
		 */
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Activity activity = (Activity) person.getSelectedPlan().getPlanElements().get(0);
			Coord coord = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(activity.getFacilityId()).getCoord();
			agentLocations.put(person.getId(), coord);
		}
	}

	public Map<Id, Coord> getAgentLocations() {
		return this.agentLocations;
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		Coord coord = scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
		agentLocations.put(event.getPersonId(), coord);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		Coord coord = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(event.getFacilityId()).getCoord();
		agentLocations.put(event.getPersonId(), coord);
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		Coord coord = scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
		agentLocations.put(event.getPersonId(), coord);
	}
	
	@Override
	public void reset(int iteration) {
		this.agentLocations.clear();
	}
}
