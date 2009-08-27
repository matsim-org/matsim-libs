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
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.scoring.CharyparNagelScoringParameters;

import playground.meisterk.kti.config.KtiConfigGroup;


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

	public LegScoringFunction(PlanImpl plan,
			CharyparNagelScoringParameters params,
			KtiConfigGroup ktiConfigGroup) {
		super(plan, params);
		this.ktiConfigGroup = ktiConfigGroup;
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
			if (this.params.marginalUtilityOfDistancePt != 0.0) {
				dist = leg.getRoute().getDistance();
				double distanceCost = 0.0;
				TreeSet<String> travelCards = this.plan.getPerson().getTravelcards();
				if (travelCards == null) {
					distanceCost = this.ktiConfigGroup.getDistanceCostPtNoTravelCard();
				} else if (travelCards.contains("unknown")) {
					distanceCost = this.ktiConfigGroup.getDistanceCostPtUnknownTravelCard();
				} else {
					throw new RuntimeException("Person " + this.plan.getPerson().getId() + " has an invalid travelcard. This should never happen.");
				}
				tmpScore += this.params.marginalUtilityOfDistancePt * distanceCost/1000d * dist;
			}
			tmpScore += 
				travelTime * this.params.marginalUtilityOfTravelingPT;
				

		} else if (TransportMode.walk.equals(leg.getMode())) {
			if (this.params.marginalUtilityOfDistanceWalk != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingWalk + this.params.marginalUtilityOfDistanceWalk * dist;
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

//		double legScore = super.calcLegScore(departureTime, arrivalTime, leg);

//		if (leg.getMode().equals(TransportMode.bike)) {
//		legScore += this.ktiConfigGroup.getConstBike();
//		}

//		return legScore;
	}


}
