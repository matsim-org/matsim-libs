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

package playground.wrashid.parkingSearch.withindayFW.kti;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

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
public class LegScoringFunction extends org.matsim.core.scoring.functions.CharyparNagelLegScoring {

	private final KtiConfigGroup ktiConfigGroup;
	private final PlansCalcRouteConfigGroup plansCalcRouteConfigGroup;
	private Plan plan;

	/*
	 * This is a workaround that became necessary after the switch to score events and
	 * not plans (autumn '11, see e.g. EventsToScore, EventsToLegs). There, dummy legs
	 * are created from the occurring events. However, this does not work for pt trips
	 * because the scoring function needs some additional information which is not
	 * provided in the events. Therefore, we get the route information for pt trips
	 * still from the plan.
	 * Please note that this will cause problems when using within-day replanning!
	 *  
	 * cdobler, Nov'11
	 */
	private List<Leg> legs;
	private int legIndex;
	
	private final static Logger log = Logger.getLogger(LegScoringFunction.class);

	public LegScoringFunction(Plan plan,
			CharyparNagelScoringParameters params,
			Config config,
            Network network,
			KtiConfigGroup ktiConfigGroup) {
		super(params, network);
		this.ktiConfigGroup = ktiConfigGroup;
		this.plansCalcRouteConfigGroup = config.plansCalcRoute();
		this.plan = plan;
		
		legs = new ArrayList<Leg>();
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Leg) legs.add((Leg) planElement);
		}
		legIndex = 0;
	}

	@Override
	protected double calcLegScore(double departureTime, double arrivalTime, Leg leg) {
		
		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // traveltime in seconds

		double dist = 0.0; // distance in meters

		if (TransportMode.car.equals(leg.getMode())) {

			tmpScore += this.ktiConfigGroup.getConstCar();

			if (this.params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m != 0.0) {
				Route route = leg.getRoute();
				if (route instanceof NetworkRoute) {
					dist = RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) route, network);
				} else {
					dist = route.getDistance();
				}
				tmpScore += this.params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m * ktiConfigGroup.getDistanceCostCar()/1000d * dist;
			}
			tmpScore += travelTime * this.params.modeParams.get(TransportMode.car).marginalUtilityOfTraveling_s;

		} else if (TransportMode.pt.equals(leg.getMode())) {
			if (this.params.modeParams.get(TransportMode.pt).marginalUtilityOfDistance_m != 0.0) {
				Route route = leg.getRoute();
				if (route instanceof NetworkRoute) {
					dist = RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) route, network);
				} else {
					dist = route.getDistance();
				}
			}
			tmpScore += travelTime * this.params.modeParams.get(TransportMode.pt).marginalUtilityOfTraveling_s + this.params.modeParams.get(TransportMode.pt).marginalUtilityOfDistance_m * dist;
			tmpScore += this.params.modeParams.get(TransportMode.pt).constant ;


		} else if (TransportMode.walk.equals(leg.getMode())) {

			if (this.params.modeParams.get(TransportMode.walk).marginalUtilityOfDistance_m != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			tmpScore += this.getWalkScore(dist, travelTime);

		} else if (TransportMode.bike.equals(leg.getMode())) {

			tmpScore += this.ktiConfigGroup.getConstBike();

			tmpScore += travelTime * this.ktiConfigGroup.getTravelingBike() / 3600d;

		} else {

			if (this.params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			// use the same values as for "car"
			tmpScore += travelTime * this.params.modeParams.get(TransportMode.car).marginalUtilityOfTraveling_s + this.params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m * dist;

		}
		
		legIndex++;
		
		return tmpScore;
	}

	private double getWalkScore(double distance, double travelTime) {

		double score = 0.0;

		score += travelTime * this.params.modeParams.get(TransportMode.walk).marginalUtilityOfTraveling_s + this.params.modeParams.get(TransportMode.walk).marginalUtilityOfDistance_m * distance;

		return score;

	}

	private double getPtScore(double distance, double travelTime) {

		double score = 0.0;

		double distanceCost = 0.0;
		TreeSet<String> travelCards = PersonUtils.getTravelcards(this.plan.getPerson());
		if (travelCards == null) {
			distanceCost = this.ktiConfigGroup.getDistanceCostPtNoTravelCard();
		} else if (travelCards.contains("unknown")) {
			distanceCost = this.ktiConfigGroup.getDistanceCostPtUnknownTravelCard();
		} else {
			throw new RuntimeException("Person " + this.plan.getPerson().getId() + " has an invalid travelcard. This should never happen.");
		}
		score += this.params.modeParams.get(TransportMode.pt).marginalUtilityOfDistance_m * distanceCost / 1000d * distance;
		score += travelTime * this.params.modeParams.get(TransportMode.pt).marginalUtilityOfTraveling_s;

		return score;

	}

	// balmermi: changed org.matsim.core.scoring.functions.CharyparNagelLegScoring.getDistance(Route route) to protected
	/*
	 * EventsToLegs creates dummy routes from events.
	 * If a car leg starts and end on the same link, a GenericRouteImpl
	 * is created, otherwise a NetworkRoute. Therefore, we have to
	 * check whether the route is a NetworkRoute or not when
	 * calculating its distance.
	 */
//	private double getDistance(Route route) {
//		double dist;
//		if (route instanceof NetworkRoute) {
//			dist =  RouteUtils.calcDistance((NetworkRoute) route, network);
//		} else {
//			dist = route.getDistance();
//		}
//		return dist;
//	}
}
