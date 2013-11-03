/* *********************************************************************** *
 * project: org.matsim.*
 * MyLegScoringFunction.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.ScoringFunctionAccumulator.BasicScoring;
import org.matsim.core.scoring.ScoringFunctionAccumulator.LegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.utils.misc.RouteUtils;

public class MyLegScoringFunction implements LegScoring, BasicScoring {

	protected double score;
	private double lastTime;

	private final double TRAVEL_PT_IN_VEHICLE;
	private final double TRAVEL_PT_WAITING;
	private final double TRAVEL_PT_ACCESS;
	private final double TRAVEL_PT_EGRESS;

	private final Id personId;
	private final PtLegHandler ptLegHandler;
	Map <Id, Double> personId2InVehTime;
	Map <Id, Double> personId2WaitingTime;

	private static final double INITIAL_LAST_TIME = 0.0;
	private static final double INITIAL_SCORE = 0.0;

	protected final CharyparNagelScoringParameters params;
	private Leg currentLeg;
    protected Network network;

	public MyLegScoringFunction(
			final CharyparNagelScoringParameters params,
			Network network,
			Id personId,
			PtLegHandler ptLegHandler,
			Map <Id, Double> personId2InVehTime,
			Map <Id, Double> personId2WaitingTime,
			double travelingPtInVehicle,
			double travelingPtWaiting,
			double access,
			double egress) {
		this.params = params;
		this.network = network;
		this.personId = personId;
		this.ptLegHandler = ptLegHandler;
		this.personId2InVehTime = personId2InVehTime;
		this.personId2WaitingTime = personId2WaitingTime;
		this.TRAVEL_PT_IN_VEHICLE = travelingPtInVehicle;
		this.TRAVEL_PT_WAITING = travelingPtWaiting;
		this.TRAVEL_PT_ACCESS = access;
		this.TRAVEL_PT_EGRESS = egress;
		this.reset();
	}

	@Override
	public void reset() {
		this.lastTime = INITIAL_LAST_TIME;
		this.score = INITIAL_SCORE;
	}

	@Override
	public void startLeg(final double time, final Leg leg) {
		this.lastTime = time;
		this.currentLeg = leg;
	}

	@Override
	public void endLeg(final double time) {
		handleLeg(this.currentLeg, time);
		this.lastTime = time;
	}

	private void handleLeg(Leg leg, final double time) {
		this.score += calcLegScore(this.lastTime, time, leg);
	}

	@Override
	public void finish() {

	}

	@Override
	public double getScore() {

		// TODO: is there a better way to do scoring from events (e.g. in directly in calcLegScore below)?
		double inVehTime;
		double waitingTime;

		if (this.personId2InVehTime.get(personId) != null && this.personId2WaitingTime.get(personId) != null){
			inVehTime = this.personId2InVehTime.get(personId);
			waitingTime = this.personId2WaitingTime.get(personId);
			this.score += (this.TRAVEL_PT_IN_VEHICLE / 3600) * inVehTime;
			this.score += (this.TRAVEL_PT_WAITING / 3600) * waitingTime;
//			System.out.println("inVehTime: " + inVehTime + " --> " + Time.writeTime(inVehTime, Time.TIMEFORMAT_HHMMSS));
//			System.out.println("waitingTime: " + waitingTime + " --> " + Time.writeTime(waitingTime, Time.TIMEFORMAT_HHMMSS));
		}
		
		return this.score;
	}

	protected double calcLegScore(final double departureTime, final double arrivalTime, final Leg leg) {
		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime;

		if (TransportMode.car.equals(leg.getMode())) {
			
//			TODO: check why "this.params.marginalUtilityOfDistanceCar_m" is 0.0000154286 and not 0.0
//			if (this.params.marginalUtilityOfDistanceCar_m != 0.0) {
//				System.out.println("MarginalUtilityOfDistanceCar: " + this.params.marginalUtilityOfDistanceCar_m);
//				throw new RuntimeException("Marginal utility of distance car is deprecated and should not be used. Aborting...");
//			}
			Route route = leg.getRoute();
			double dist = getDistance(route);
			double monetaryCostsCar = dist * this.params.modeParams.get(TransportMode.car).monetaryDistanceCostRate;
			tmpScore += monetaryCostsCar * this.params.marginalUtilityOfMoney;
			tmpScore += travelTime * this.params.modeParams.get(TransportMode.car).marginalUtilityOfTraveling_s;
			tmpScore += this.params.modeParams.get(TransportMode.car).constant ;

		} else if (TransportMode.pt.equals(leg.getMode())) {

			tmpScore += this.params.modeParams.get(TransportMode.pt).constant;

		} else if (TransportMode.transit_walk.equals(leg.getMode())) {
			if (this.params.modeParams.get(TransportMode.walk).marginalUtilityOfDistance_m != 0.0) {
				throw new RuntimeException("Marginal utility of distance walk is deprecated and should not be used. Aborting...");
			}
			
			if (this.ptLegHandler.getPersonId2IsEgress().get(personId) == null){
				this.ptLegHandler.getPersonId2IsEgress().put(personId, false);
			}
			
			if (!this.ptLegHandler.getPersonId2IsEgress().get(personId)){
//				System.out.println("AccessTime: " + travelTime);
//				System.out.println("(personId: " + personId);
				tmpScore += travelTime * this.TRAVEL_PT_ACCESS / 3600.0;
			} else {
//				System.out.println("EgressTime: " + travelTime);
//				System.out.println("(personId: " + personId);
				tmpScore += travelTime * this.TRAVEL_PT_EGRESS / 3600.0;
				this.ptLegHandler.getPersonId2IsEgress().put(personId, false);	
			}
			
// A transit_walk leg shouldn't have a constant because it already has the constant of the pt leg!?! (ihab, benjamin; mai'12)
//			tmpScore += this.params.constantWalk ;

		} else {
			throw new RuntimeException("Transport mode " + leg.getMode() + " is unknown for this scenario. Aborting...");
		}
		return tmpScore;
	}
	
	private double getDistance(Route route) {
		double dist;
		if (route instanceof NetworkRoute) {
			dist =  RouteUtils.calcDistance((NetworkRoute) route, network);
		} else {
			dist = route.getDistance();
		}
		return dist;
	}
}


