///* *********************************************************************** *
// * project: org.matsim.*
// * HerbieLegWithNetworkPtDistanceScoringFunction.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2012 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//package playground.thibautd.herbie;
//
//import herbie.running.config.HerbieConfigGroup;
//import herbie.running.scoring.LegScoringFunction;
//import herbie.running.scoring.TravelScoringFunction;
//
//import java.util.TreeSet;
//
//import org.matsim.api.core.v01.TransportMode;
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.api.core.v01.population.Leg;
//import org.matsim.api.core.v01.population.Plan;
//import org.matsim.core.config.Config;
//import org.matsim.core.population.PersonImpl;
//import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
//
///**
// * @author thibautd
// */
//public class HerbieLegWithNetworkPtDistanceScoringFunction extends LegScoringFunction {
//	private final Plan plan;
//	private final HerbieConfigGroup configGroup;
//	private final TravelScoringFunction travelScoring;
//
//	public HerbieLegWithNetworkPtDistanceScoringFunction(
//			final Plan plan,
//			final CharyparNagelScoringParameters params,
//			final Config config,
//			final Network network,
//			final HerbieConfigGroup ktiConfigGroup) {
//		super(plan, params, config, network, ktiConfigGroup);
//		this.plan = plan;
//		this.configGroup = ktiConfigGroup;
//		this.travelScoring = new TravelScoringFunction( params , ktiConfigGroup );
//	}
//
//	@Override
//	protected double calcLegScore(
//			final double departureTime,
//			final double arrivalTime,
//			final Leg leg) {
//		if (TransportMode.pt.equals(leg.getMode())) {
//			
//			// this is the reason of the change: use route distance instead of bee-fly
//			// (possible because it was included in the "multi-leg" router)
//			double distance = leg.getRoute().getDistance();
//			double travelTime = arrivalTime - departureTime; // traveltime in seconds
//			
//			double distanceCost = 0.0;
//			TreeSet<String> travelCards = ((PersonImpl) this.plan.getPerson()).getTravelcards();
//			if (travelCards == null) {
//				distanceCost = this.configGroup.getDistanceCostPtNoTravelCard();
//			} else if (travelCards.contains("unknown")) {
//				distanceCost = this.configGroup.getDistanceCostPtUnknownTravelCard();
//			} else {
//				throw new RuntimeException("Person " + this.plan.getPerson().getId() + 
//						" has an invalid travelcard. This should never happen.");
//			}
//			
//			return travelScoring.getInVehiclePtScore(distance, travelTime, distanceCost);
//		}
//		return super.calcLegScore( departureTime , arrivalTime , leg );
//	}
//
//}
