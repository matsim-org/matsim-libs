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
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.vehicles.Vehicle;

/**
 * @author dziemke
 * 
 * This is an alternative to BicycleLegScoring. Currently yields slightly different scores than BicyleLegScoring.
 * This link-based scoring should be used when true times spent on an individual link are relevant
 * and for the scoring of the interaction with motorized traffic.
 */
class BicycleLinkScoring implements SumScoringFunction.ArbitraryEventScoring {
	private static final Logger LOG = Logger.getLogger(BicycleLinkScoring.class);
	
	private final ScoringParameters params;
	
	private Scenario scenario;
	private Vehicle2DriverEventHandler vehicle2Driver = new Vehicle2DriverEventHandler();
	private Id<Link> previousLink;
	private double previousLinkRelativePosition;
	private double previousLinkEnterTime;
	private double score;
	private int carCountOnLink;
	
	private final double marginalUtilityOfInfrastructure_m;
	private final double marginalUtilityOfComfort_m;
	private final double marginalUtilityOfGradient_m_100m;
	
	private static int ccc=0 ;
	
	BicycleLinkScoring( final ScoringParameters params, Scenario scenario, BicycleConfigGroup bicycleConfigGroup ) {
		this.params = params;
		this.scenario = scenario;
		
		this.marginalUtilityOfInfrastructure_m = bicycleConfigGroup.getMarginalUtilityOfInfrastructure_m();
		this.marginalUtilityOfComfort_m = bicycleConfigGroup.getMarginalUtilityOfComfort_m();
		this.marginalUtilityOfGradient_m_100m = bicycleConfigGroup.getMarginalUtilityOfGradient_m_100m();
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
			vehicle2Driver.handleEvent(vehEvent);
			
			// No LinkEnterEvent on first link of a leg
			previousLink = vehEvent.getLinkId();
			carCountOnLink = 0;
			previousLinkRelativePosition = vehEvent.getRelativePositionOnLink();
			previousLinkEnterTime =vehEvent.getTime();
			
		}
		if (event instanceof VehicleLeavesTrafficEvent) {
			VehicleLeavesTrafficEvent vehEvent = (VehicleLeavesTrafficEvent) event;
			
			Id<Vehicle> vehId = vehEvent.getVehicleId();
			double enterTime = previousLinkEnterTime;
			double travelTime = vehEvent.getTime() - enterTime;
			calculateScoreForPreviousLink(vehEvent.getLinkId(), enterTime, vehId, travelTime, previousLinkRelativePosition);
			
			// End connection between driver and vehicle
			vehicle2Driver.handleEvent(vehEvent);
		}
		if (event instanceof LinkEnterEvent) {
			// This only works since ScoringFunctionsForPopulation passes link events to persons; quite new; dz, june'18
			// Otherwise ArbitraryEventScoring only handles events that are instance of HasPersonId, which is not the case for LinkEnterEvents
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
		if ( event instanceof MotorizedInteractionEvent ) {
			if ( ((MotorizedInteractionEvent) event).getLinkId().equals(previousLink )){
				this.carCountOnLink++;
			}
		}

	}
	
	private void calculateScoreForPreviousLink(Id<Link> linkId, Double enterTime, Id<Vehicle> vehId, double travelTime, double relativeLinkEnterPosition) {
		if (relativeLinkEnterPosition != 1.0) {
			// Link link = scenario.getNetwork().getLinks().get(linkId);
			// Person person = scenario.getPopulation().getPersons().get(vehicle2Driver.getDriverOfVehicle(vehId));
			// Vehicle vehicle = scenario.getVehicles().getVehicles().get(vehId);
			
			double carScoreOffset = -(this.carCountOnLink * 0.04);
			this.score += carScoreOffset;
			// LOG.warn("----- link = " + linkId + " -- car score offset = " + carScoreOffset);
			
			double scoreOnLink = BicycleUtilityUtils.computeLinkBasedScore(scenario.getNetwork().getLinks().get(linkId),
					marginalUtilityOfComfort_m, marginalUtilityOfInfrastructure_m, marginalUtilityOfGradient_m_100m);
			// LOG.warn("----- link = " + linkId + " -- scoreOnLink = " + scoreOnLink);
			this.score += scoreOnLink;
			
			double timeDistanceBasedScoreComponent = computeTimeDistanceBasedScoreComponent(travelTime, scenario.getNetwork().getLinks().get(linkId).getLength());
			// LOG.warn("----- link = " + linkId + " -- timeDistanceBasedScoreComponent = " + timeDistanceBasedScoreComponent);
			this.score += timeDistanceBasedScoreComponent;
		}
		else {
			double timeDistanceBasedScoreComponent = computeTimeDistanceBasedScoreComponent(travelTime, 0.);
			this.score += timeDistanceBasedScoreComponent;
		}
	}
	
	
	// Copied and adapted from CharyparNagelLegScoring
	private double computeTimeDistanceBasedScoreComponent( double travelTime, double dist ) {
		double tmpScore = 0.0;
		ModeUtilityParameters modeParams = this.params.modeParams.get("bicycle");
		if (modeParams == null) {
			throw new RuntimeException("no scoring parameters are defined for bicycle") ;
		}
		tmpScore += travelTime * modeParams.marginalUtilityOfTraveling_s;
		if (modeParams.marginalUtilityOfDistance_m != 0.0 || modeParams.monetaryDistanceCostRate != 0.0) {
			if ( Double.isNaN(dist) ) {
				if ( ccc<10 ) {
					ccc++ ;
					Logger.getLogger(this.getClass()).warn("distance is NaN. Will make score of this plan NaN. Possible reason: Simulation does not report " +
							"a distance for this trip. Possible reason for that: mode is teleported and router does not " +
							"write distance into plan.  Needs to be fixed or these plans will die out.") ;
					if ( ccc==10 ) {
						Logger.getLogger(this.getClass()).warn(Gbl.FUTURE_SUPPRESSED) ;
					}
				}
			}
			tmpScore += modeParams.marginalUtilityOfDistance_m * dist;
			tmpScore += modeParams.monetaryDistanceCostRate * this.params.marginalUtilityOfMoney * dist;
		}
		tmpScore += modeParams.constant;
		return tmpScore;
	}
	
}
