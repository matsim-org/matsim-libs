/* *********************************************************************** *
 * project: org.matsim.*
 * HerbieJointLegScoringFunction.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.cliques.herbie.scoring;

import herbie.running.config.HerbieConfigGroup;
import herbie.running.pt.DistanceCalculations;
import herbie.running.scoring.TravelScoringFunction;

import java.util.TreeSet;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.thibautd.hitchiking.HitchHikingConstants;
import playground.thibautd.socnetsim.jointtrips.population.JointActingTypes;

/**
 * @author thibautd
 */
public class HerbieJointLegScoringFunction extends CharyparNagelLegScoring {

	private final HerbieConfigGroup ktiConfigGroup;
	private final Config config;
	private final TravelScoringFunction travelScoring;
	private final Plan plan;

	public HerbieJointLegScoringFunction(
			final Plan plan,
			final CharyparNagelScoringParameters params,
			final Config config,
			final Network network,
			final HerbieConfigGroup ktiConfigGroup) {
		super(params, network);
		this.plan = plan;
		this.travelScoring = new TravelScoringFunction(params, ktiConfigGroup);
		this.config = config;;
		this.ktiConfigGroup = ktiConfigGroup;
	}

	@Override
	protected double calcLegScore(double departureTime, double arrivalTime, Leg leg) {
		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // traveltime in seconds
		
		if (TransportMode.car.equals(leg.getMode()) || JointActingTypes.DRIVER.equals( leg.getMode() ) ) {
			double dist = 0.0;
			if (this.params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m != 0.0) {
				Route route = leg.getRoute();
				try {
					dist = DistanceCalculations.getLegDistance(route, network);
				}
				catch (Exception e) {
					throw new RuntimeException( "mode="+leg.getMode()+", route="+route, e );
				}
			}
			tmpScore += travelScoring.getCarScore(dist, travelTime);
		}
		else if ( JointActingTypes.PASSENGER.equals( leg.getMode() ) ) {
			// do not consider distance in the cost of traveling for passengers.
			tmpScore += travelScoring.getCarScore(0, travelTime);
		}
		else if (TransportMode.pt.equals(leg.getMode())) {
			Route route = leg.getRoute();

			double distance = DistanceCalculations.getLegDistance(route, network);
		
			double distanceCost = 0.0;
			TreeSet<String> travelCards = ((PersonImpl) this.plan.getPerson()).getTravelcards();
			if (travelCards == null) {
				distanceCost = this.ktiConfigGroup.getDistanceCostPtNoTravelCard();
			}
			else if (travelCards.contains("unknown")) {
				distanceCost = this.ktiConfigGroup.getDistanceCostPtUnknownTravelCard();
			}
			else {
				throw new RuntimeException("Person " + this.plan.getPerson().getId() + 
						" has an invalid travelcard. This should never happen.");
			}
			
			tmpScore += travelScoring.getInVehiclePtScore(distance, travelTime, distanceCost);
			
		}
		else if (TransportMode.walk.equals(leg.getMode())) {
			
			double distance =  DistanceCalculations.getWalkDistance((GenericRouteImpl) leg.getRoute(), network)
								* this.config.plansCalcRoute().getModeRoutingParams().get( TransportMode.walk ).getBeelineDistanceFactor();
			
			travelTime = distance / this.config.plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk);
			tmpScore += travelScoring.getWalkScore(distance, travelTime);
			
		}
		else if (TransportMode.transit_walk.equals(leg.getMode())){
			
			double distance = 0.0;
			if (this.params.modeParams.get(TransportMode.walk).marginalUtilityOfDistance_m != 0.0) {
				distance = DistanceCalculations.getWalkDistance((GenericRouteImpl) leg.getRoute(), network)
					* this.config.plansCalcRoute().getModeRoutingParams().get( TransportMode.walk ).getBeelineDistanceFactor();
			}
			
			tmpScore += travelScoring.getWalkScore(distance, travelTime);
			
		}
		else if (TransportMode.bike.equals(leg.getMode())) {
			double distance = DistanceCalculations.getWalkDistance((GenericRouteImpl) leg.getRoute(), network)
				* this.config.plansCalcRoute().getModeRoutingParams().get( TransportMode.bike ).getBeelineDistanceFactor();
			tmpScore += travelScoring.getBikeScore(distance, travelTime);
			
		}
		else if (HitchHikingConstants.DRIVER_MODE.equals(leg.getMode()) ||
				HitchHikingConstants.PASSENGER_MODE.equals( leg.getMode() ) ) {
			// distance is handled by a money event in the QSim
			tmpScore += travelScoring.getCarScore(0, travelTime);
		}
		else {
			double dist = 0.0;
			if (this.params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m != 0.0) {
				dist = DistanceCalculations.getLegDistance(leg.getRoute(), network);				
			}
			tmpScore += travelScoring.getAlternativeModeScore(dist, travelTime);
			
		}
		return tmpScore;
	}	
}
