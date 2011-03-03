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

package playground.andreas.bvgScoringFunction;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.charyparNagel.LegScoringFunction;
import org.matsim.core.scoring.interfaces.BasicScoring;
import org.matsim.core.scoring.interfaces.LegScoring;

/**
 *
 * Scoring function featuring offsets and distance costs for the modes car, pt, ride, bike, walk and transit_walk
 *
 * @author aneumann roughly based on {@link LegScoringFunction}
 *
 */
public class BvgLegScoringFunction implements LegScoring, BasicScoring {

	private final static Logger log = Logger.getLogger(BvgLegScoringFunction.class);

	protected final Plan plan;

	private NetworkImpl network;

	protected double score;
	private double lastTime;
	private int index; // the current position in plan.actslegs

	private static final double INITIAL_LAST_TIME = 0.0;
	private static final int INITIAL_INDEX = 1;
	private static final double INITIAL_SCORE = 0.0;

	/** The parameters used for scoring */
	protected final CharyparNagelScoringParameters charyparNagelParameters;
	protected final BvgScoringFunctionParameters bvgParameters;

	public BvgLegScoringFunction(final Plan plan, final CharyparNagelScoringParameters charyparNagelParameters, final BvgScoringFunctionParameters bvgParameters, NetworkImpl network) {
		this.charyparNagelParameters = charyparNagelParameters;
		this.bvgParameters = bvgParameters;
		this.network = network;
		this.reset();

		this.plan = plan;
	}

	@Override
	public void reset() {
		this.lastTime = INITIAL_LAST_TIME;
		this.index = INITIAL_INDEX;
		this.score = INITIAL_SCORE;
	}

	@Override
	public void startLeg(final double time, final Leg leg) {
		this.lastTime = time;
		// check leg for offset
	}

	@Override
	public void endLeg(final double time) {
		handleLeg(time);
		this.lastTime = time;
	}

	@Override
	public void finish() {

	}

	@Override
	public double getScore() {
		return this.score;
	}

	private static int distanceWrnCnt = 0;

	protected double calcLegScore(final double departureTime, final double arrivalTime, final Leg leg) {

		/*
		 * we only as for the route when we have to calculate a distance cost,
		 * because route.getDist() may calculate the distance if not yet
		 * available, which is quite an expensive operation
		 */
		double dist = 0.0; // distance in meters
		double travelTime = arrivalTime - departureTime; // traveltime in seconds
		double tmpScore = 0.0;


		if (TransportMode.car.equals(leg.getMode())) {

			if (this.charyparNagelParameters.monetaryDistanceCostRateCar != 0.0) {

				dist = leg.getRoute().getDistance(); // Should be RouteUtils.calcDistance(route, network)
				dist += this.network.getLinks().get(leg.getRoute().getEndLinkId()).getLength();

				if (distanceWrnCnt < 1) {
					/*
					 * TODO the route-distance does not contain the length of
					 * the first or last link of the route, because the route
					 * doesn't know those. Should be fixed somehow, but how? MR,
					 * jan07
					 */
					/*
					 * TODO in the case of within-day replanning, we cannot be
					 * sure that the distance in the leg is the actual distance
					 * driven by the agent.
					 */
					log.warn("leg distance for scoring computed from plan, not from execution (=events)."
							+ "This is not how it is meant to be, and it will fail for within-day replanning.");
					log.warn("Length of first link is not included, length of last Link is.");
					log.warn(Gbl.ONLYONCE);
					distanceWrnCnt++;
				}
			}

			// add travel time related part
			tmpScore += travelTime * this.charyparNagelParameters.marginalUtilityOfTraveling_s;

			// add distance related part
			tmpScore += dist * this.charyparNagelParameters.monetaryDistanceCostRateCar * this.charyparNagelParameters.marginalUtilityOfMoney;

			// add offset for using car mode
			tmpScore += this.bvgParameters.offsetCar * this.bvgParameters.betaOffsetCar * this.charyparNagelParameters.marginalUtilityOfMoney;

		} else if (TransportMode.pt.equals(leg.getMode())) {

			if (this.charyparNagelParameters.monetaryDistanceCostRatePt != 0.0) {
				// Should be ok, since pt legs are handled in a different way
				dist = leg.getRoute().getDistance();
			}

			// add travel time related part
			tmpScore += travelTime * this.charyparNagelParameters.marginalUtilityOfTravelingPT_s;

			// add distance related part, as of jan 2011 transit_walk and pt distance is not set
			if(!Double.isNaN(dist)){
				tmpScore += dist * this.charyparNagelParameters.monetaryDistanceCostRatePt * this.charyparNagelParameters.marginalUtilityOfMoney;
			}

			// add offset for using pt mode
			tmpScore += this.bvgParameters.offsetPt * this.bvgParameters.betaOffsetPt * this.charyparNagelParameters.marginalUtilityOfMoney;

		} else if (TransportMode.ride.equals(leg.getMode())) {

			if (this.bvgParameters.monetaryDistanceCostRateRide != 0.0) {
				// Should be ok, since ride legs are handled in a different way
				dist = leg.getRoute().getDistance();
			}

			// add travel time related part
			tmpScore += travelTime * this.charyparNagelParameters.marginalUtilityOfTraveling_s;

			// add distance related part
			tmpScore += dist * this.bvgParameters.monetaryDistanceCostRateRide * this.charyparNagelParameters.marginalUtilityOfMoney;

			// add offset for using ride mode
			tmpScore += this.bvgParameters.offsetRide * this.bvgParameters.betaOffsetRide * this.charyparNagelParameters.marginalUtilityOfMoney;

		} else if (TransportMode.bike.equals(leg.getMode())) {

			if (this.bvgParameters.monetaryDistanceCostRateBike != 0.0) {
				// Should be ok, since bike legs are handled in a different way
				dist = leg.getRoute().getDistance();
			}

			// add travel time related part
			tmpScore += travelTime * this.charyparNagelParameters.marginalUtilityOfTravelingBike_s;

			// add distance related part
			tmpScore += dist * this.bvgParameters.monetaryDistanceCostRateBike * this.charyparNagelParameters.marginalUtilityOfMoney;

			// add offset for using bike mode
			tmpScore += this.bvgParameters.offsetBike * this.bvgParameters.betaOffsetBike * this.charyparNagelParameters.marginalUtilityOfMoney;

		} else if (TransportMode.walk.equals(leg.getMode())) {

			if (this.bvgParameters.monetaryDistanceCostRateWalk != 0.0) {
				// Should be ok, since walk legs are handled in a different way
				dist = leg.getRoute().getDistance();
			}

			// add travel time related part
			tmpScore += travelTime * this.charyparNagelParameters.marginalUtilityOfTravelingWalk_s;

			// add distance related part
			tmpScore += dist * this.bvgParameters.monetaryDistanceCostRateWalk * this.charyparNagelParameters.marginalUtilityOfMoney;

			// add offset for using walk mode
			tmpScore += this.bvgParameters.offsetWalk * this.bvgParameters.betaOffsetWalk * this.charyparNagelParameters.marginalUtilityOfMoney;

		} else if (TransportMode.transit_walk.equals(leg.getMode())) {

			if (this.bvgParameters.monetaryDistanceCostRateWalk != 0.0) {
				// Should be ok, since bike legs are handled in a different way
				dist = leg.getRoute().getDistance();
			}

			// add travel time related part
			tmpScore += travelTime * this.charyparNagelParameters.marginalUtilityOfTravelingWalk_s;

			// add distance related part, as of jan 2011 transit_walk and pt distance is not set
			if(!Double.isNaN(dist)){
				tmpScore += dist * this.bvgParameters.monetaryDistanceCostRateWalk * this.charyparNagelParameters.marginalUtilityOfMoney;
			}

			// add offset for using walk mode
			tmpScore += this.bvgParameters.offsetWalk * this.bvgParameters.betaOffsetWalk * this.charyparNagelParameters.marginalUtilityOfMoney;

		} else {

			log.error("The mode " + leg.getMode() + " is not implemented, yet. Please stick to the following modes: " + TransportMode.car + ", " + TransportMode.pt + ", " + TransportMode.ride + ", " + TransportMode.bike + ", " + TransportMode.walk + " and " + TransportMode.transit_walk);

		}

		return tmpScore;
	}

	private void handleLeg(final double time) {
		LegImpl leg = (LegImpl) this.plan.getPlanElements().get(this.index);
		this.score += calcLegScore(this.lastTime, time, leg);
		this.index += 2;
	}
}
