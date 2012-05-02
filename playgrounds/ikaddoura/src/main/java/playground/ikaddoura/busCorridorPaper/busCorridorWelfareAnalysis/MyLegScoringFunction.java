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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.interfaces.BasicScoring;
import org.matsim.core.scoring.interfaces.LegScoring;
import org.matsim.core.utils.misc.Time;

public class MyLegScoringFunction implements LegScoring, BasicScoring {
	private final static Logger log = Logger.getLogger(MyLegScoringFunction.class);

	protected double score;
	private double lastTime;

	private final double TRAVEL_PT_IN_VEHICLE;
	private final double TRAVEL_PT_WAITING;
	private final Id personId;
	Map <Id, Double> personId2InVehTime;
	Map <Id, Double> personId2WaitingTime;

	private static final double INITIAL_LAST_TIME = 0.0;
	private static final double INITIAL_SCORE = 0.0;

	protected final CharyparNagelScoringParameters params;
	private Leg currentLeg;


	public MyLegScoringFunction(
			final CharyparNagelScoringParameters params,
			Id personId,
			Map <Id, Double> personId2InVehTime,
			Map <Id, Double> personId2WaitingTime,
			double travelingPtInVehicle,
			double travelingPtWaiting) {
		this.params = params;
		this.personId = personId;
		this.personId2InVehTime = personId2InVehTime;
		this.personId2WaitingTime = personId2WaitingTime;
		this.TRAVEL_PT_IN_VEHICLE = travelingPtInVehicle;
		this.TRAVEL_PT_WAITING = travelingPtWaiting;
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
			
			log.warn("inVehTime: " + Time.writeTime(inVehTime, Time.TIMEFORMAT_HHMMSS));
			log.warn("waitingTime: " + Time.writeTime(waitingTime, Time.TIMEFORMAT_HHMMSS));
		}
		return this.score;
	}

	protected double calcLegScore(final double departureTime, final double arrivalTime, final Leg leg) {
		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime;

		if (TransportMode.car.equals(leg.getMode())) {
			if (this.params.marginalUtilityOfDistanceCar_m != 0.0) {
				throw new RuntimeException("Marginal utility of distance car is deprecated and should not be used. Aborting...");
			}
			double monetaryCostsCar = leg.getRoute().getDistance() * this.params.monetaryDistanceCostRateCar;
			tmpScore += monetaryCostsCar * this.params.marginalUtilityOfMoney;
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling_s;
			tmpScore += this.params.constantCar ;

		} else if (TransportMode.pt.equals(leg.getMode())) {
			//			double inVehTime;
			//			double waitingTime;
			//			
			//			if (this.personId2InVehTime.get(personId) == null || this.personId2WaitingTime.get(personId) == null){
			//				throw new RuntimeException("Person " + personId + " does not have either a in-vehicle time or a waiting time. Aborting...");
			//			} else {
			//				inVehTime = this.personId2InVehTime.get(personId);
			//				waitingTime = this.personId2WaitingTime.get(personId);
			//			}
			//			tmpScore += (this.TRAVEL_PT_IN_VEHICLE / 3600) * inVehTime;
			//			tmpScore += (this.TRAVEL_PT_WAITING / 3600) * waitingTime;

			tmpScore += this.params.constantPt;

		} else if (TransportMode.transit_walk.equals(leg.getMode())) {
			if (this.params.marginalUtilityOfDistanceWalk_m != 0.0) {
				throw new RuntimeException("Marginal utility of distance walk is deprecated and should not be used. Aborting...");
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingWalk_s;
			tmpScore += this.params.constantWalk ;

		} else {
			throw new RuntimeException("Transport mode " + leg.getMode() + " is unknown for this scenario. Aborting...");
		}
		return tmpScore;
	}
}