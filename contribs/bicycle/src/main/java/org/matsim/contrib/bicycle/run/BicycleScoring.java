/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle.run;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.vehicles.Vehicle;

/**
 * @author dziemke
 */
public class BicycleScoring implements SumScoringFunction.ArbitraryEventScoring {
	private Scenario scenario;
	private BicycleTravelDisutility bicycleTravelDisutility;
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();
	private Map<Id<Vehicle>,Double> previousLinkEnterTimes = new HashMap<>();
	private double score;
	
	public BicycleScoring(Scenario scenario, BicycleTravelTime bicycleTravelTime, BicycleTravelDisutilityFactory bicycleTravelDisutilityFactory) {
				this.scenario = scenario;
		this.bicycleTravelDisutility = (BicycleTravelDisutility) bicycleTravelDisutilityFactory.createTravelDisutility(bicycleTravelTime);
	}

	@Override public void finish() {}
	
	@Override
	public double getScore() {
		return score;
	}
	
	@Override
	public void handleEvent(Event event) {
		if (event instanceof VehicleEntersTrafficEvent) {
			VehicleEntersTrafficEvent vehEvent = (VehicleEntersTrafficEvent) event;

			// Establish connection between driver and vehicle
			delegate.handleEvent(vehEvent);
			
			// No LinkEnterEvent on first link of a leg
			previousLinkEnterTimes.put(vehEvent.getVehicleId(), vehEvent.getTime());
		}
		if (event instanceof VehicleLeavesTrafficEvent) {
			VehicleLeavesTrafficEvent vehEvent = (VehicleLeavesTrafficEvent) event;
			
			Id<Vehicle> vehId = vehEvent.getVehicleId();
			double enterTime = previousLinkEnterTimes.get(vehId);
			double travelTime = vehEvent.getTime() - enterTime;
			calculateScoreForPreviousLink(vehEvent.getLinkId(), enterTime, vehId, travelTime);
			
			// End connection between driver and vehicle
			delegate.handleEvent(vehEvent);
		}
		if ( event instanceof LinkEnterEvent ) {
			// This only works because setPassLinkEventsToPerson is activated (via ScoringFunctionsForPopulation)
			// Otherwise ArbitraryEventScoring only handles events that are instance of HasPersonId, which is not the case for LinkEnterEvents
			System.err.println("New link enter event: " + event.toString());
			LinkEnterEvent linkEnterEvent = (LinkEnterEvent) event;
			
			Id<Vehicle> vehId = linkEnterEvent.getVehicleId();
			double enterTime = previousLinkEnterTimes.get(vehId);
			double travelTime = linkEnterEvent.getTime() - enterTime;
			calculateScoreForPreviousLink(linkEnterEvent.getLinkId(), enterTime, vehId, travelTime);
			
			previousLinkEnterTimes.put(vehId, linkEnterEvent.getTime());
		}	
	}
	
	private void calculateScoreForPreviousLink(Id<Link> linkId, Double enterTime, Id<Vehicle> vehId, double travelTime) {
		Link link = scenario.getNetwork().getLinks().get(linkId);
		Person person = scenario.getPopulation().getPersons().get(delegate.getDriverOfVehicle(vehId));
		Vehicle vehicle = scenario.getVehicles().getVehicles().get(vehId);
		
		this.score += bicycleTravelDisutility.getTravelDisutilityBasedOnTTime(link, enterTime, person, vehicle, travelTime);
		System.out.println("score = " + score + " -- linkId = " + link.getId() + " -- enterTime = " + enterTime + " -- personId = " + person.getId() + " -- travelTime = " + travelTime);
	}
}