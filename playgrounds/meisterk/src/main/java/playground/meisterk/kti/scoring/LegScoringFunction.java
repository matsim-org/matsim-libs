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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.CharyparNagelScoringParameters;

import playground.meisterk.kti.config.KtiConfigGroup;
import playground.meisterk.kti.router.KtiPtRoute;
import playground.meisterk.kti.router.PlansCalcRouteKti;


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
public class LegScoringFunction extends org.matsim.core.scoring.charyparNagel.LegScoringFunction {

	private final KtiConfigGroup ktiConfigGroup;
	private final PlansCalcRouteConfigGroup plansCalcRouteConfigGroup;

	private final static Logger log = Logger.getLogger(LegScoringFunction.class);

	public LegScoringFunction(Plan plan,
			CharyparNagelScoringParameters params,
			Config config,
			KtiConfigGroup ktiConfigGroup) {
		super(plan, params);
		this.ktiConfigGroup = ktiConfigGroup;
		this.plansCalcRouteConfigGroup = config.plansCalcRoute();
	}

	@Override
	protected double calcLegScore(double departureTime, double arrivalTime, Leg leg) {

		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // traveltime in seconds

		double dist = 0.0; // distance in meters

		if (TransportMode.car.equals(leg.getMode())) {

			tmpScore += this.ktiConfigGroup.getConstCar();

			if (this.params.marginalUtilityOfDistanceCar_m != 0.0) {
				Route route = leg.getRoute();
				dist = route.getDistance();
				tmpScore += this.params.marginalUtilityOfDistanceCar_m * ktiConfigGroup.getDistanceCostCar()/1000d * dist;
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling_s;

		} else if (TransportMode.pt.equals(leg.getMode())) {

			KtiPtRoute ktiPtRoute = (KtiPtRoute) leg.getRoute();

			if (ktiPtRoute.getFromStop() != null) {

//				String nanoMsg = "Scoring kti pt:\t";

//				long nanos = System.nanoTime();
				dist = ((KtiPtRoute) leg.getRoute()).calcAccessEgressDistance(((PlanImpl) this.plan).getPreviousActivity(leg), ((PlanImpl) this.plan).getNextActivity(leg));
//				nanos = System.nanoTime() - nanos;
//				nanoMsg += Long.toString(nanos) + "\t";

//				nanos = System.nanoTime();
				travelTime = PlansCalcRouteKti.getAccessEgressTime(dist, this.plansCalcRouteConfigGroup);
//				nanos = System.nanoTime() - nanos;
//				nanoMsg += Long.toString(nanos) + "\t";

				tmpScore += this.getWalkScore(dist, travelTime);

//				nanos = System.nanoTime();
				dist = ((KtiPtRoute) leg.getRoute()).calcInVehicleDistance();
//				nanos = System.nanoTime() - nanos;
//				nanoMsg += Long.toString(nanos) + "\t";

//				nanos = System.nanoTime();
				travelTime = ((KtiPtRoute) leg.getRoute()).getInVehicleTime();
//				nanos = System.nanoTime() - nanos;
//				nanoMsg += Long.toString(nanos) + "\t";

				tmpScore += this.getPtScore(dist, travelTime);
//				log.info(nanoMsg);

			} else {

				dist = leg.getRoute().getDistance();
				tmpScore += this.getPtScore(dist, travelTime);

			}

		} else if (TransportMode.walk.equals(leg.getMode())) {

			if (this.params.marginalUtilityOfDistanceWalk_m != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			tmpScore += this.getWalkScore(dist, travelTime);

		} else if (TransportMode.bike.equals(leg.getMode())) {

			tmpScore += this.ktiConfigGroup.getConstBike();

			tmpScore += travelTime * this.ktiConfigGroup.getTravelingBike() / 3600d;

		} else {

			if (this.params.marginalUtilityOfDistanceCar_m != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			// use the same values as for "car"
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling_s + this.params.marginalUtilityOfDistanceCar_m * dist;

		}

		return tmpScore;
	}

	private double getWalkScore(double distance, double travelTime) {

		double score = 0.0;

		score += travelTime * this.params.marginalUtilityOfTravelingWalk_s + this.params.marginalUtilityOfDistanceWalk_m * distance;

		return score;

	}

	private double getPtScore(double distance, double travelTime) {

		double score = 0.0;

		double distanceCost = 0.0;
		TreeSet<String> travelCards = ((PersonImpl) this.plan.getPerson()).getTravelcards();
		if (travelCards == null) {
			distanceCost = this.ktiConfigGroup.getDistanceCostPtNoTravelCard();
		} else if (travelCards.contains("unknown")) {
			distanceCost = this.ktiConfigGroup.getDistanceCostPtUnknownTravelCard();
		} else {
			throw new RuntimeException("Person " + this.plan.getPerson().getId() + " has an invalid travelcard. This should never happen.");
		}
		score += this.params.marginalUtilityOfDistancePt_m * distanceCost / 1000d * distance;
		score += travelTime * this.params.marginalUtilityOfTravelingPT_s;

		return score;

	}

}
