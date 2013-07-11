///* *********************************************************************** *
// * project: org.matsim.*
// * MyLegScoringFunction.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
//
//package playground.ikaddoura.optimization.scoring;
//
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.TransportMode;
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.api.core.v01.population.Leg;
//import org.matsim.api.core.v01.population.Route;
//import org.matsim.core.api.experimental.events.ActivityEndEvent;
//import org.matsim.core.api.experimental.events.AgentDepartureEvent;
//import org.matsim.core.api.experimental.events.Event;
//import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
//import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
//import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
//import org.matsim.pt.PtConstants;
//
//
///**
//* Modification of CharyparNagelLegScoring. Scores the in-vehicle time and waiting time for pt as well as access and egress time separately.
//* @author Ihab
//*/
//public class OptimizationLegScoringFunction extends CharyparNagelLegScoring {
//	
//	private final double TRAVEL_PT_ACCESS;
//	private final double TRAVEL_PT_EGRESS;
//
//	private final Id personId;
//	private final PtLegHandler ptLegHandler;
//
//	private boolean nextEnterVehicleIsFirstOfTrip = true ;
//	private boolean nextStartPtLegIsFirstOfTrip = true ;
//	
//	public OptimizationLegScoringFunction(CharyparNagelScoringParameters params, Network network, Id personId, PtLegHandler ptLegHandler, double access, double egress) {
//		
//		super(params, network);
//		
//		this.personId = personId;
//		this.ptLegHandler = ptLegHandler;
//		
//		this.TRAVEL_PT_ACCESS = access;
//		this.TRAVEL_PT_EGRESS = egress;
//		
//	}
//	
//	@Override
//	public double getScore() {
//
//		if (this.ptLegHandler.getPersonId2InVehicleTime().get(personId) != null && this.ptLegHandler.getPersonId2WaitingTime().get(personId) != null){
//			double inVehTime = this.ptLegHandler.getPersonId2InVehicleTime().get(personId);
//			double waitingTime = this.ptLegHandler.getPersonId2WaitingTime().get(personId);
//			System.out.println("waiting time: " + waitingTime + " // marginalUtilsWaitingPt: " + this.params.marginalUtilityOfWaitingPt_s * 3600.);
//			System.out.println("inVehTime: " + inVehTime + " // marginalUtilsInVeh: " + this.params.marginalUtilityOfTravelingPT_s * 3600.);
//			this.score += this.params.marginalUtilityOfWaitingPt_s * waitingTime;
//			this.score += this.params.marginalUtilityOfTravelingPT_s * inVehTime;
//		}
//		System.out.println("Score: " + this.score);
//		return this.score;
//	}
//
//	@Override
//	protected double calcLegScore(final double departureTime, final double arrivalTime, final Leg leg) {
//		double tmpScore = 0.0;
//		double travelTime = arrivalTime - departureTime;
//
//		if (TransportMode.car.equals(leg.getMode())) {			
//			Route route = leg.getRoute();
//			double dist = getDistance(route);
//			double monetaryCostsCar = dist * this.params.monetaryDistanceCostRateCar;
//			tmpScore += monetaryCostsCar * this.params.marginalUtilityOfMoney;
//			tmpScore += travelTime * this.params.marginalUtilityOfTraveling_s;
//			tmpScore += this.params.constantCar ;
//
//		} else if (TransportMode.pt.equals(leg.getMode())) {
//
//			tmpScore += this.params.constantPt;
//
//		} else if (TransportMode.transit_walk.equals(leg.getMode())) {
//			if (this.params.marginalUtilityOfDistanceWalk_m != 0.0) {
//				throw new RuntimeException("Marginal utility of distance walk is deprecated and should not be used. Aborting...");
//			}
//			
//			if (this.ptLegHandler.getPersonId2IsEgress().get(personId) == null){
//				this.ptLegHandler.getPersonId2IsEgress().put(personId, false);
//			}
//			
//			if (!this.ptLegHandler.getPersonId2IsEgress().get(personId)){
//				tmpScore += travelTime * this.TRAVEL_PT_ACCESS / 3600.0;
//			} else {
//				tmpScore += travelTime * this.TRAVEL_PT_EGRESS / 3600.0;
//				this.ptLegHandler.getPersonId2IsEgress().put(personId, false);	
//			}
//			
//// 			A transit_walk leg shouldn't have a constant because it already has the constant of the pt leg!?! (ihab, benjamin; mai'12)
//
//		} else {
//			throw new RuntimeException("Transport mode " + leg.getMode() + " is unknown for this scenario. Aborting...");
//		}
//		return tmpScore;
//	}
//
//	@Override
//	public void handleEvent(Event event) {
//		if ( event instanceof ActivityEndEvent ) {
//			// When there is a "real" activity, flags are reset:
//			if ( !PtConstants.TRANSIT_ACTIVITY_TYPE.equals( ((ActivityEndEvent)event).getActType()) ) {
//				this.nextEnterVehicleIsFirstOfTrip  = true ;
//				this.nextStartPtLegIsFirstOfTrip = true ;
//			}
//		} else if ( event instanceof PersonEntersVehicleEvent ) {
//			if ( !this.nextEnterVehicleIsFirstOfTrip ) {
//				// all vehicle entering after the first triggers the disutility of line switch:
//				this.score  += params.utilityOfLineSwitch ;
//			}
//			this.nextEnterVehicleIsFirstOfTrip = false ;
//		} else if ( event instanceof AgentDepartureEvent ) {
//			if ( TransportMode.pt.equals( ((AgentDepartureEvent)event).getLegMode() ) ) {
//				if ( !this.nextStartPtLegIsFirstOfTrip ) {
//					this.score -= params.constantPt ;
//					// (yyyy deducting this again, since is it wrongly added above.  should be consolidated; this is so the code
//					// modification is minimally invasive.  kai, dec'12)
//				}
//				this.nextStartPtLegIsFirstOfTrip = false ;
//			}
//		}
//	}
//}
//
//
