/* *********************************************************************** *
 * project: org.matsim.*
 * LegScoringFunction.java
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

package playground.meisterk.kti.scoring;

import java.util.TreeSet;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.scoring.CharyparNagelScoringParameters;

import playground.meisterk.kti.config.KtiConfigGroup;
import playground.meisterk.kti.router.PlansCalcRouteKti;
import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;


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
 */
public class LegScoringFunction extends
org.matsim.core.scoring.charyparNagel.LegScoringFunction {

	private final KtiConfigGroup ktiConfigGroup;
	private final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo;
	private final NetworkLayer network;
	private final PlansCalcRouteConfigGroup plansCalcRouteConfigGroup;

	public LegScoringFunction(PlanImpl plan,
			CharyparNagelScoringParameters params,
			KtiConfigGroup ktiConfigGroup,
			PlansCalcRouteKtiInfo plansCalcRouteKtiInfo,
			NetworkLayer network,
			PlansCalcRouteConfigGroup plansCalcRouteConfigGroup) {
		super(plan, params);
		this.ktiConfigGroup = ktiConfigGroup;
		this.plansCalcRouteKtiInfo = plansCalcRouteKtiInfo;
		this.network = network;
		this.plansCalcRouteConfigGroup = plansCalcRouteConfigGroup;
	}

	@Override
	protected double calcLegScore(double departureTime, double arrivalTime,
			LegImpl leg) {

		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // traveltime in seconds

		double dist = 0.0; // distance in meters

		if (TransportMode.car.equals(leg.getMode())) {
			
			if (this.params.marginalUtilityOfDistanceCar != 0.0) {
				RouteWRefs route = leg.getRoute();
				dist = route.getDistance();
				tmpScore += this.params.marginalUtilityOfDistanceCar * ktiConfigGroup.getDistanceCostCar()/1000d * dist;
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling;
			
		} else if (TransportMode.pt.equals(leg.getMode())) {

			if (this.ktiConfigGroup.isUsePlansCalcRouteKti()) {

				// true pt access leg with mode "walk"
				LegImpl pseudoLeg = null;
				pseudoLeg = PlansCalcRouteKti.getPseudoAccessLeg(
						this.plan.getPreviousActivity(leg), 
						this.plansCalcRouteKtiInfo.getHaltestellen(), 
						this.network,
						this.plansCalcRouteConfigGroup);
				dist = pseudoLeg.getRoute().getDistance();
				travelTime = pseudoLeg.getTravelTime();
				tmpScore += this.getWalkScore(dist, travelTime);
				
				// true pt leg
				pseudoLeg = PlansCalcRouteKti.getPseudoPtLeg(
						this.plan.getPreviousActivity(leg), 
						this.plan.getNextActivity(leg), 
						this.plansCalcRouteKtiInfo,
						this.network);
				dist = pseudoLeg.getRoute().getDistance();
				travelTime = pseudoLeg.getTravelTime();
				tmpScore += this.getPtScore(dist, travelTime);
				
				// true pt egress leg with mode "walk"
				pseudoLeg = PlansCalcRouteKti.getPseudoEgressLeg(this.plan
						.getNextActivity(leg), this.plansCalcRouteKtiInfo
						.getHaltestellen(), this.network,
						this.plansCalcRouteConfigGroup);
				dist = pseudoLeg.getRoute().getDistance();
				travelTime = pseudoLeg.getTravelTime();
				tmpScore += this.getWalkScore(dist, travelTime);
				
			} else {
			
				if (this.params.marginalUtilityOfDistancePt != 0.0) {
					dist = leg.getRoute().getDistance();
				}
				tmpScore += this.getPtScore(dist, travelTime);
				
			}
			
		} else if (TransportMode.walk.equals(leg.getMode())) {
			
			if (this.params.marginalUtilityOfDistanceWalk != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			tmpScore += this.getWalkScore(dist, travelTime);
			
		} else if (TransportMode.bike.equals(leg.getMode())) {
			
			tmpScore += travelTime * this.ktiConfigGroup.getTravelingBike() / 3600d;
			
		} else {
			
			if (this.params.marginalUtilityOfDistanceCar != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			// use the same values as for "car"
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling + this.params.marginalUtilityOfDistanceCar * dist;
			
		}

		return tmpScore;
	}

	private double getWalkScore(double distance, double travelTime) {
		
		double score = 0.0;
		
		score += travelTime * this.params.marginalUtilityOfTravelingWalk + this.params.marginalUtilityOfDistanceWalk * distance;
		
		return score;
		
	}

	private double getPtScore(double distance, double travelTime) {

		double score = 0.0;

		double distanceCost = 0.0;
		TreeSet<String> travelCards = this.plan.getPerson().getTravelcards();
		if (travelCards == null) {
			distanceCost = this.ktiConfigGroup.getDistanceCostPtNoTravelCard();
		} else if (travelCards.contains("unknown")) {
			distanceCost = this.ktiConfigGroup.getDistanceCostPtUnknownTravelCard();
		} else {
			throw new RuntimeException("Person " + this.plan.getPerson().getId() + " has an invalid travelcard. This should never happen.");
		}
		score += this.params.marginalUtilityOfDistancePt	* distanceCost / 1000d * distance;
		score += travelTime * this.params.marginalUtilityOfTravelingPT;

		return score;
		
	}
	
}
