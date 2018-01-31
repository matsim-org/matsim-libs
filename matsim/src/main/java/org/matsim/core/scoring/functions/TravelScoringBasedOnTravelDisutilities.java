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
package org.matsim.core.scoring.functions;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.vehicles.Vehicle;

/**
 * @author dziemke
 */
public class TravelScoringBasedOnTravelDisutilities implements SumScoringFunction.ArbitraryEventScoring {
	 final class LocalTravelTime implements TravelTime {
		private double travelTime = Double.NaN ;

		@Override public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
			return travelTime ;
		}

		void setLinkTravelTime( double ttime ) {
			travelTime = ttime ;
		}
	}

	private static final Logger LOG = Logger.getLogger(TravelScoringBasedOnTravelDisutilities.class);
	
	private Scenario scenario;
	private Map<String,TravelDisutility> travelDisutilities = new HashMap<>();
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();
	private Id<Link> previousLink;
	private double previousLinkRelativePosition;
	private double previousLinkEnterTime;
	private double score;
//	private int carCountOnLink;

	private String mode;

	private Map<String, LocalTravelTime> travelTimes = new HashMap<>();
	
	public TravelScoringBasedOnTravelDisutilities(Scenario scenario, Map<String,TravelDisutilityFactory> travelDisutilityFactories) {
		this.scenario = scenario;
//		for (ModeParams mode : scenario.getConfig().planCalcScore().getModes().values()) { // All modes
		for (String mode : scenario.getConfig().plansCalcRoute().getNetworkModes()) { // Network modes
			LOG.warn("Mode = " + mode);
			LOG.warn("travelTimes = " + travelTimes);
			travelTimes.put(mode , new LocalTravelTime());
			LOG.warn("Mode = " + mode + "; travel time = " + travelTimes);
			// yyyyyy only use network modes
			TravelDisutility td = travelDisutilityFactories.get(mode).createTravelDisutility(travelTimes.get(mode));
			travelDisutilities.put( mode, td ) ;
		}
	}

	@Override public void finish() {}
	
	@Override
	public double getScore() {
		return score;
	}
	
	@Override
	public void handleEvent(Event event) {
		if ( event instanceof PersonDepartureEvent ) {
			mode = ((PersonDepartureEvent) event).getLegMode();
			LOG.warn("Mode of the PersonDepartureEvent = " + mode);
			
			
			// yyyyyy this this really arriving here?
//			throw new RuntimeException("needs to be tested"); 
		}
		if (event instanceof VehicleEntersTrafficEvent) {
			LOG.warn(event.toString());
			VehicleEntersTrafficEvent vehEvent = (VehicleEntersTrafficEvent) event;
			
			// Establish connection between driver and vehicle
			delegate.handleEvent(vehEvent);
			
			// No LinkEnterEvent on first link of a leg
			previousLink = vehEvent.getLinkId();
//			carCountOnLink = 0;
			previousLinkRelativePosition = vehEvent.getRelativePositionOnLink();
			previousLinkEnterTime =vehEvent.getTime();
			
		}
		if (event instanceof VehicleLeavesTrafficEvent) {
			LOG.warn(event.toString());
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
			LOG.warn(event.toString());
			LinkEnterEvent linkEnterEvent = (LinkEnterEvent) event;
			
			Id<Vehicle> vehId = linkEnterEvent.getVehicleId();
			double travelTime = linkEnterEvent.getTime() - previousLinkEnterTime;
			calculateScoreForPreviousLink(previousLink, previousLinkEnterTime, vehId, travelTime, previousLinkRelativePosition);
			
			previousLink = linkEnterEvent.getLinkId();
//			carCountOnLink = 0;
			previousLinkRelativePosition = 0.;
			previousLinkEnterTime = linkEnterEvent.getTime();

		}	
	}
	
	private void calculateScoreForPreviousLink(Id<Link> linkId, Double enterTime, Id<Vehicle> vehId, double travelTime, double relativeLinkEnterPosition) {
		if (relativeLinkEnterPosition != 1.0) {
			Link link = scenario.getNetwork().getLinks().get(linkId);
			Person person = scenario.getPopulation().getPersons().get(delegate.getDriverOfVehicle(vehId));
			Vehicle vehicle = scenario.getVehicles().getVehicles().get(vehId);
			
			// new
			PlanCalcScoreConfigGroup cnScoringGroup = scenario.getConfig().planCalcScore();
			PlanCalcScoreConfigGroup.ModeParams params = cnScoringGroup.getModes().get( mode ) ;
//			double marginalCostOfTime_s = (-params.getMarginalUtilityOfTraveling() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);
			double factor = (-params.getMarginalUtilityOfTraveling() / 3600.0) / ((-params.getMarginalUtilityOfTraveling() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0));
			
//			double carScoreOffset = -(this.carCountOnLink * 0.04);
//			this.score += carScoreOffset;
//			LOG.warn("----- link = " + linkId + " -- car score offset = " + carScoreOffset);
			
//			this.score += travelDisutilities.get(mode).getTravelDisutilityBasedOnTTime(link, enterTime, person, vehicle, travelTime);
			this.travelTimes.get(mode).setLinkTravelTime(travelTime);
			LOG.warn("score offset = " + travelDisutilities.get(mode).getLinkTravelDisutility(link, enterTime, person, vehicle));
//			this.score += travelDisutilities.get(mode).getLinkTravelDisutility(link, enterTime, person, vehicle);
			this.score -= factor * travelDisutilities.get(mode).getLinkTravelDisutility(link, enterTime, person, vehicle);

			LOG.warn("score = " + score + " -- linkId = " + link.getId() + " -- enterTime = " + enterTime + " -- personId = " + person.getId() + " -- travelTime = " + travelTime);
		}
		else {
			// If agent was already at the end of the link and thus did not travel on it, do nothing
		}
		// TODO Use relative position in a more sophisticated way.
	}
}