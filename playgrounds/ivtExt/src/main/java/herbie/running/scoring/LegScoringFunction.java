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

import java.util.TreeSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.routes.ExperimentalTransitRoute;


/*
 * 
 * WARNING: Do not use this class without adaptation to HERBIE!!!!!!
 * 
 */

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

	private final HerbieConfigGroup ktiConfigGroup;
	private Config config;
	private Network network;
	public LegScoringFunction(Plan plan,
			CharyparNagelScoringParameters params,
			Config config,
			Network network,
			HerbieConfigGroup ktiConfigGroup) {
		super(plan, params);
		this.config = config;
		this.network = network;
		this.ktiConfigGroup = ktiConfigGroup;
	}

	@Override
	protected double calcLegScore(double departureTime, double arrivalTime, Leg leg) {
		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // traveltime in seconds
		double dist = 0.0; // distance in meters
		if (TransportMode.car.equals(leg.getMode())) {
			tmpScore += super.params.constantCar;
			if (this.params.marginalUtilityOfDistanceCar_m != 0.0) {
				Route route = leg.getRoute();
//				dist = route.getDistance();
				dist = DistanceCalculations.getLegDistance(route, network);
				
				// TODO: correct following line!!!!! I do not get the point!
				
				tmpScore += this.params.marginalUtilityOfDistanceCar_m * this.params.monetaryDistanceCostRateCar/1000d * dist;
				
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling_s;
		} else if (TransportMode.pt.equals(leg.getMode())) {
			
			/* ------------------------------------------------------------------------
			 * TODO: make following code compatible with pt simulation!
			 */			
//			KtiPtRoute ktiPtRoute = (KtiPtRoute) leg.getRoute();
//			if (ktiPtRoute.getFromStop() != null) {
//				dist = ((KtiPtRoute) leg.getRoute()).calcAccessEgressDistance(
//						((PlanImpl) this.plan).getPreviousActivity(leg), ((PlanImpl) this.plan).getNextActivity(leg));
//				
//				travelTime = PlansCalcRouteKti.getAccessEgressTime(dist, this.plansCalcRouteConfigGroup);
//				
//				tmpScore += this.getWalkScore(dist, travelTime);
//				dist = ((KtiPtRoute) leg.getRoute()).calcInVehicleDistance();
//				travelTime = ((KtiPtRoute) leg.getRoute()).getInVehicleTime();				
//				tmpScore += this.getPtScore(dist, travelTime);
//			} else {
//				dist = leg.getRoute().getDistance();
//				tmpScore += this.getPtScore(dist, travelTime);
				tmpScore += getInVehiclePtScore(travelTime, leg);
//			}
			/*
			 * ------------------------------------------------------------------------
			 */
		} else if (TransportMode.walk.equals(leg.getMode())) {
			if (this.params.marginalUtilityOfDistanceWalk_m != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			tmpScore += this.getWalkScore(dist, travelTime);
		}else if (TransportMode.transit_walk.equals(leg.getMode())){
			if (this.params.marginalUtilityOfDistanceWalk_m != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			tmpScore += this.getWalkScore(dist, travelTime);
		} else if (TransportMode.bike.equals(leg.getMode())) {
			tmpScore += this.params.constantBike;
			tmpScore += travelTime * super.params.marginalUtilityOfTravelingBike_s / 3600d;
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
			throw new RuntimeException("Person " + this.plan.getPerson().getId() + 
					" has an invalid travelcard. This should never happen.");
		}
		score += this.params.marginalUtilityOfDistancePt_m * distanceCost / 1000d * distance;
		score += travelTime * this.params.marginalUtilityOfTravelingPT_s;
		return score;
	}
	
	private double getInVehiclePtScore(double travelTime, Leg leg) {
		
		double score = 0.0;
		
//		TransitRoute route = (TransitRoute) leg.getRoute();
//		RouteFactory routeFactory = new LinkNetworkRouteFactory();
//		NetworkRoute route = (NetworkRoute) routeFactory.createRoute(leg.getRoute().getStartLinkId(), leg.getRoute().getEndLinkId());
		
//		ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) leg.getRoute();
//		double distance = ptRoute.getDistance();
		
		Id linkIDStart = leg.getRoute().getStartLinkId();
		Id linkIDEnd = leg.getRoute().getEndLinkId();
		Coord startCoord = network.getLinks().get(linkIDStart).getCoord();
		Coord endCoord = network.getLinks().get(linkIDEnd).getCoord();
		double distance = CoordUtils.calcDistance(startCoord, endCoord) / 1000.0;
		
//		double distance = DistanceCalculations.getLegDistance(leg.getRoute(), network);
		
		
		double distanceCost = 0.0;
		TreeSet<String> travelCards = ((PersonImpl) this.plan.getPerson()).getTravelcards();
		if (travelCards == null) {
			distanceCost = this.ktiConfigGroup.getDistanceCostPtNoTravelCard();
		} else if (travelCards.contains("unknown")) {
			distanceCost = this.ktiConfigGroup.getDistanceCostPtUnknownTravelCard();
		} else {
			throw new RuntimeException("Person " + this.plan.getPerson().getId() + 
					" has an invalid travelcard. This should never happen.");
		}
		score += this.params.marginalUtilityOfDistancePt_m * distanceCost / 1000d * distance;
		
		score += travelTime * this.params.marginalUtilityOfTravelingPT_s;
		
		return score;
	}
	
}
