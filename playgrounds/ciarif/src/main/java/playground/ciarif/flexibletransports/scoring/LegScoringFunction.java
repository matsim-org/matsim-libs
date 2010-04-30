/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.ciarif.flexibletransports.scoring;

import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.CharyparNagelScoringParameters;

import playground.ciarif.flexibletransports.config.FtConfigGroup;
import playground.ciarif.flexibletransports.data.MyTransportMode;
import playground.ciarif.flexibletransports.router.FtCarSharingRoute;
import playground.meisterk.kti.router.KtiPtRoute;
import playground.meisterk.kti.router.PlansCalcRouteKti;

public class LegScoringFunction extends org.matsim.core.scoring.charyparNagel.LegScoringFunction{

	private final FtConfigGroup ftConfigGroup;
	private final PlansCalcRouteConfigGroup plansCalcRouteConfigGroup;

	private final static Logger log = Logger.getLogger(LegScoringFunction.class);

	public LegScoringFunction(PlanImpl plan,
			CharyparNagelScoringParameters params,
			Config config,
			FtConfigGroup ftConfigGroup) {
		super(plan, params);
		this.ftConfigGroup = ftConfigGroup;
		this.plansCalcRouteConfigGroup = config.plansCalcRoute();
	}

	@Override
	protected double calcLegScore(double departureTime, double arrivalTime, Leg leg) {

		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // traveltime in seconds

		double dist = 0.0; // distance in meters
		Activity actPrev = ((PlanImpl) this.plan).getPreviousActivity(leg);
		Activity actNext = ((PlanImpl) this.plan).getNextActivity(leg);

		if (MyTransportMode.car.equals(leg.getMode())) {

			tmpScore += this.ftConfigGroup.getConstCar();

			if (this.params.marginalUtilityOfDistanceCar != 0.0) {
				Route route = leg.getRoute();
				dist = route.getDistance();
				tmpScore += this.params.marginalUtilityOfDistanceCar * ftConfigGroup.getDistanceCostCar()/1000d * dist;
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling;

		} else if (MyTransportMode.carsharing.equals(leg.getMode())) {


			if (!MyTransportMode.carsharing.equals(((PlanImpl) this.plan).getPreviousLeg(actPrev).getMode())){
				dist = ((FtCarSharingRoute) leg.getRoute()).calcAccessDistance (actPrev, actNext);
				// TODO now no stations are defined,
				// introduce an input with car sharing station positions

				travelTime = PlansCalcRouteKti.getAccessEgressTime(dist, this.plansCalcRouteConfigGroup);

				tmpScore += this.getWalkScore(dist, travelTime);
			}

			if (this.params.marginalUtilityOfDistanceCar != 0.0) {
				Route route = leg.getRoute();
				dist = ((FtCarSharingRoute) leg.getRoute()).calcCarDistance(actPrev, actNext);
				tmpScore += this.params.marginalUtilityOfDistanceCar * ftConfigGroup.getDistanceCostCar()/1000d * dist;
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling;

		} else if (MyTransportMode.pt.equals(leg.getMode())) {

			KtiPtRoute ktiPtRoute = (KtiPtRoute) leg.getRoute();

			if (ktiPtRoute.getFromStop() != null) {

				dist = ((KtiPtRoute) leg.getRoute()).calcAccessEgressDistance(((PlanImpl) this.plan).getPreviousActivity(leg), ((PlanImpl) this.plan).getNextActivity(leg));
				travelTime = PlansCalcRouteKti.getAccessEgressTime(dist, this.plansCalcRouteConfigGroup);
				tmpScore += this.getWalkScore(dist, travelTime);
				dist = ((KtiPtRoute) leg.getRoute()).calcInVehicleDistance();
				travelTime = ((KtiPtRoute) leg.getRoute()).getInVehicleTime();
				tmpScore += this.getPtScore(dist, travelTime);

			} else {

				dist = leg.getRoute().getDistance();
				tmpScore += this.getPtScore(dist, travelTime);

			}

		} else if (MyTransportMode.walk.equals(leg.getMode())) {

			if (this.params.marginalUtilityOfDistanceWalk != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			tmpScore += this.getWalkScore(dist, travelTime);

		} else if (MyTransportMode.bike.equals(leg.getMode())) {

			tmpScore += this.ftConfigGroup.getConstBike();

			tmpScore += travelTime * this.ftConfigGroup.getTravelingBike() / 3600d;

		} else if (MyTransportMode.ride.equals(leg.getMode())) {

			if (this.ftConfigGroup.getMarginalUtilityOfDistanceRide()!= 0.0) {
				dist = 1.2*leg.getRoute().getDistance();
			}
			travelTime= travelTime*1.2;
			tmpScore += this.getRideScore(dist, travelTime);


		} else {

			if (this.params.marginalUtilityOfDistanceCar != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			// use the same values as for "car"
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling + this.params.marginalUtilityOfDistanceCar * dist;

		}

		return tmpScore;
	}

	private double getRideScore(double distance, double travelTime) {
		double score = 0.0;

		score += this.ftConfigGroup.getConstRide();

		score += this.ftConfigGroup.getMarginalUtilityOfDistanceRide() * this.ftConfigGroup.getDistanceCostRide() / 1000d * distance;

		score += travelTime * this.ftConfigGroup.getTravelingRide() / 3600d;// TODO Auto-generated method stub

		return score;
	}

	private double getWalkScore(double distance, double travelTime) {

		double score = 0.0;

		score += travelTime * this.params.marginalUtilityOfTravelingWalk + this.params.marginalUtilityOfDistanceWalk * distance;

		return score;

	}

	private double getPtScore(double distance, double travelTime) {

		double score = 0.0;

		double distanceCost = 0.0;
		TreeSet<String> travelCards = ((PersonImpl) this.plan.getPerson()).getTravelcards();
		if (travelCards == null) {
			distanceCost = this.ftConfigGroup.getDistanceCostPtNoTravelCard();
		} else if (travelCards.contains("unknown")) {
			distanceCost = this.ftConfigGroup.getDistanceCostPtUnknownTravelCard();
		} else {
			throw new RuntimeException("Person " + this.plan.getPerson().getId() + " has an invalid travelcard. This should never happen.");
		}
		score += this.params.marginalUtilityOfDistancePt * distanceCost / 1000d * distance;
		score += travelTime * this.params.marginalUtilityOfTravelingPT;
		score += score += this.ftConfigGroup.getConstPt();

		return score;

	}


}
