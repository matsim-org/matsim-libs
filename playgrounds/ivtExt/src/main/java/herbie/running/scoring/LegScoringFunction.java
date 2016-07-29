/* *********************************************************************** *
 * project: org.matsim.*
 * LegScoringFunction.java
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

package herbie.running.scoring;

import herbie.running.config.HerbieConfigGroup;
import herbie.running.pt.DistanceCalculations;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import java.util.TreeSet;


/**
 * This class contains modifications of the standard leg scoring function for the KTI project.
 *
 * It is similar to the leg scoring function described in Kickhöfers master thesis (see also his playground),
 * but with some extensions regarding the handling of travel cards and bike legs.
 *
 * Reference:
 * Kickhöfer, B. (2009) Die Methodik der ökonomischen Bewertung von Verkehrsmaßnahmen
 * in Multiagentensimulationen, Master Thesis, Technical University Berlin, Berlin,
 * https://svn.vsp.tu-berlin.de/repos/public-svn/publications/
 * vspwp/2009/09-10/DAKickhoefer19aug09.pdf.
 * 
 * @author meisterk
 * 
 * Restructured in Mai, Juni 2011:
 * 
 *
 * @author bvitins, anhorni
 *
 */
public class LegScoringFunction extends org.matsim.deprecated.scoring.functions.CharyparNagelLegScoring {

	private final HerbieConfigGroup ktiConfigGroup;
	private Config config;
	private Network network;
	TravelScoringFunction travelScoring;
	private Plan plan;
	public LegScoringFunction(Plan plan,
			CharyparNagelScoringParameters params,
			Config config,
			Network network,
			HerbieConfigGroup ktiConfigGroup) {
		super(params, network);
		this.plan = plan;
		travelScoring = new TravelScoringFunction(params, ktiConfigGroup);
		this.config = config;
		this.network = network;
		this.ktiConfigGroup = ktiConfigGroup;
	}

	@Override
	protected double calcLegScore(double departureTime, double arrivalTime, Leg leg) {
		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // traveltime in seconds
		
		if (TransportMode.car.equals(leg.getMode())) {
			double dist = 0.0;
			if (this.params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m != 0.0) {
				Route route = leg.getRoute();
				dist = DistanceCalculations.getLegDistance(route, network);
				
//				carScore += this.params.marginalUtilityOfDistanceCar_m * this.params.monetaryDistanceCostRateCar/1000d * dist;
			}
			tmpScore += travelScoring.getCarScore(dist, travelTime);
			
		} else if (TransportMode.pt.equals(leg.getMode())) {
			
			double distance = DistanceCalculations.getLegDistance(leg.getRoute(), network);
			
			double distanceCost = 0.0;
			TreeSet<String> travelCards = PersonUtils.getTravelcards(this.plan.getPerson());
			if (travelCards == null) {
				distanceCost = this.ktiConfigGroup.getDistanceCostPtNoTravelCard();
			} else if (travelCards.contains("unknown")) {
				distanceCost = this.ktiConfigGroup.getDistanceCostPtUnknownTravelCard();
			} else {
				throw new RuntimeException("Person " + this.plan.getPerson().getId() + 
						" has an invalid travelcard. This should never happen.");
			}
			
			tmpScore += travelScoring.getInVehiclePtScore(distance, travelTime, distanceCost);
			
		} else if (TransportMode.walk.equals(leg.getMode())) {
			
			double distance =  DistanceCalculations.getWalkDistance((GenericRouteImpl) leg.getRoute(), network)
					* this.config.plansCalcRoute().getModeRoutingParams().get( TransportMode.walk ).getBeelineDistanceFactor()  ;
//								* this.config.plansCalcRoute().getBeelineDistanceFactor();
			
			travelTime = distance / this.config.plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk);
			
//			double tt = travelTime;
//			double beelinefact = this.config.plansCalcRoute().getBeelineDistanceFactor();
//			double timeParam = this.params.marginalUtilityOfTravelingWalk_s;
//			double distParam = this.params.marginalUtilityOfDistanceWalk_m;
//			double score = travelScoring.getWalkScore(distance, travelTime);
//			double dist_transitwalk = DistanceCalculations.getWalkDistance((GenericRouteImpl) leg.getRoute(), network);
//			double scoreOfWalk = travelScoring.getWalkScore(distance, travelTime);
//			
//			if(distance > 20000){
//				System.out.println();
//			}
			tmpScore += travelScoring.getWalkScore(distance, travelTime);
			
		}else if (TransportMode.transit_walk.equals(leg.getMode())){
			
			double distance = 0.0;
			if (this.params.modeParams.get(TransportMode.walk).marginalUtilityOfDistance_m != 0.0) {
				distance = DistanceCalculations.getWalkDistance((GenericRouteImpl) leg.getRoute(), network)
                        * this.config.plansCalcRoute().getModeRoutingParams().get( TransportMode.walk ).getBeelineDistanceFactor() ;
                        //					* this.config.plansCalcRoute().getBeelineDistanceFactor();
			}
			
			tmpScore += travelScoring.getWalkScore(distance, travelTime);
			
		} else if (TransportMode.bike.equals(leg.getMode())) {
			double distance = DistanceCalculations.getWalkDistance((GenericRouteImpl) leg.getRoute(), network)
                    * this.config.plansCalcRoute().getModeRoutingParams().get( TransportMode.bike ).getBeelineDistanceFactor() ;
                    //				* this.config.plansCalcRoute().getBeelineDistanceFactor();
			tmpScore += travelScoring.getBikeScore(distance, travelTime);
			
		} else {
			
			double dist = 0.0;
			if (this.params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m != 0.0) {
				dist = DistanceCalculations.getLegDistance(leg.getRoute(), network);				
//				carScore += this.params.marginalUtilityOfDistanceCar_m * this.params.monetaryDistanceCostRateCar/1000d * dist;
			}
			tmpScore += travelScoring.getAlternativeModeScore(dist, travelTime);
			
		}
		return tmpScore;
	}	
}
