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
package org.matsim.contrib.bicycle;

import org.apache.log4j.Logger;
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
public class BicycleLinkScoring implements SumScoringFunction.ArbitraryEventScoring, MotorizedInteractionEventHandler {
	private static final Logger LOG = Logger.getLogger(BicycleLinkScoring.class);
	
	private Scenario scenario;
	private BicycleTravelDisutility bicycleTravelDisutility;
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();
	private Id<Link> previousLink;
	private double previousLinkRelativePosition;
	private double previousLinkEnterTime;
	private double score;
	private int carCountOnLink;
	
	public BicycleLinkScoring(Scenario scenario, BicycleTravelTime bicycleTravelTime, BicycleTravelDisutilityFactory bicycleTravelDisutilityFactory) {
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
//			LOG.warn(event.toString());
			VehicleEntersTrafficEvent vehEvent = (VehicleEntersTrafficEvent) event;
			
			// Establish connection between driver and vehicle
			delegate.handleEvent(vehEvent);
			
			// No LinkEnterEvent on first link of a leg
			previousLink = vehEvent.getLinkId();
			carCountOnLink = 0;
			previousLinkRelativePosition = vehEvent.getRelativePositionOnLink();
			previousLinkEnterTime =vehEvent.getTime();
			
		}
		if (event instanceof VehicleLeavesTrafficEvent) {
//			LOG.warn(event.toString());
			VehicleLeavesTrafficEvent vehEvent = (VehicleLeavesTrafficEvent) event;
			
			Id<Vehicle> vehId = vehEvent.getVehicleId();
			double enterTime = previousLinkEnterTime;
			double travelTime = vehEvent.getTime() - enterTime;
			calculateScoreForPreviousLink(vehEvent.getLinkId(), enterTime, vehId, travelTime, previousLinkRelativePosition);
			
			// End connection between driver and vehicle
			delegate.handleEvent(vehEvent);
		}
		if (event instanceof LinkEnterEvent) {
			// This only works because setPassLinkEventsToPerson is activated (via ScoringFunctionsForPopulation)
			// Otherwise ArbitraryEventScoring only handles events that are instance of HasPersonId, which is not the case for LinkEnterEvents
//			LOG.warn(event.toString());
			LinkEnterEvent linkEnterEvent = (LinkEnterEvent) event;
			
			Id<Vehicle> vehId = linkEnterEvent.getVehicleId();
			double enterTime = previousLinkEnterTime;
			double travelTime = linkEnterEvent.getTime() - enterTime;
			calculateScoreForPreviousLink(previousLink, enterTime, vehId, travelTime, previousLinkRelativePosition);
			
			previousLink = linkEnterEvent.getLinkId();
			carCountOnLink = 0;
			previousLinkRelativePosition = 0.;
			previousLinkEnterTime = linkEnterEvent.getTime();
		}
	}
	
	private void calculateScoreForPreviousLink(Id<Link> linkId, Double enterTime, Id<Vehicle> vehId, double travelTime, double relativeLinkEnterPosition) {
		if (relativeLinkEnterPosition != 1.0) {
			Link link = scenario.getNetwork().getLinks().get(linkId);
			Person person = scenario.getPopulation().getPersons().get(delegate.getDriverOfVehicle(vehId));
			Vehicle vehicle = scenario.getVehicles().getVehicles().get(vehId);
			
			double carScoreOffset = -(this.carCountOnLink * 0.04);
			this.score += carScoreOffset;
			LOG.warn("----- link = " + linkId + " -- car score offset = " + carScoreOffset);
			
//			this.score += bicycleTravelDisutility.getTravelDisutilityBasedOnTTime(link, enterTime, person, vehicle, travelTime);
			this.score += bicycleTravelDisutility.getLinkTravelDisutility(link, enterTime, person, vehicle);
//			LOG.warn("score = " + score + " -- linkId = " + link.getId() + " -- enterTime = " + enterTime + " -- personId = " + person.getId() + " -- travelTime = " + travelTime);
		}
		else {
			// If agent was already at the end of the link and thus did not travel on it, do nothing
		}
		// TODO Use relative position in a more sophisticated way.
	}

	@Override
	public void handleEvent(MotorizedInteractionEvent event) {
//		LOG.info("event received from link " + event.getLinkId());
		if (event.getLinkId().equals(previousLink)) {
//			LOG.warn("USED: Event from link " + event.getLinkId());
			this.carCountOnLink++;
		}
	}
}