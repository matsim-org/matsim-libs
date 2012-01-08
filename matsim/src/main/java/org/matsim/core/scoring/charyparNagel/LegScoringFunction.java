/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelOpenTimesScoringFunctionFactory.java
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

package org.matsim.core.scoring.charyparNagel;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.LinkNetworkRoute;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.interfaces.BasicScoring;
import org.matsim.core.scoring.interfaces.LegScoring;
import org.matsim.core.utils.misc.RouteUtils;

/**
 * This is a re-implementation of the original CharyparNagel function, based on a
 * modular approach.
 * @see http://www.matsim.org/node/263
 * @author rashid_waraich
 */
public class LegScoringFunction implements LegScoring, BasicScoring {

	private final static Logger log = Logger.getLogger(LegScoringFunction.class);

	protected double score;
	private double lastTime;

	private static final double INITIAL_LAST_TIME = 0.0;
	private static final double INITIAL_SCORE = 0.0;

	/** The parameters used for scoring */
	protected final CharyparNagelScoringParameters params;
	private Leg currentLeg;
    protected Network network;

    public LegScoringFunction(final CharyparNagelScoringParameters params, Network network) {
		this.params = params;
        this.network = network;
		this.reset();
	}

	@Override
	public void reset() {
		this.lastTime = INITIAL_LAST_TIME;
		this.score = INITIAL_SCORE;
	}

	@Override
	public void startLeg(final double time, final Leg leg) {
		assert leg != null;
		this.lastTime = time;
		this.currentLeg = leg;
	}

	@Override
	public void endLeg(final double time) {
		handleLeg(this.currentLeg, time);
		this.lastTime = time;
	}

	@Override
	public void finish() {

	}

	@Override
	public double getScore() {
		return this.score;
	}

	private static int distanceWrnCnt = 0 ;
	protected double calcLegScore(final double departureTime, final double arrivalTime, final Leg leg) {
		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // traveltime in
															// seconds

		/*
		 * we only as for the route when we have to calculate a distance cost,
		 * because route.getDist() may calculate the distance if not yet
		 * available, which is quite an expensive operation
		 */
		double dist = 0.0; // distance in meters

		if (TransportMode.car.equals(leg.getMode())) {
			if (this.params.marginalUtilityOfDistanceCar_m != 0.0) {
				Route route = leg.getRoute();
				dist = RouteUtils.calcDistance((LinkNetworkRoute) route, network);
                if ( distanceWrnCnt<1 ) {
				/*
				 * TODO the route-distance does not contain the length of the
				 * first or last link of the route, because the route doesn't
				 * know those. Should be fixed somehow, but how? MR, jan07
				 */
				/*
				 * TODO in the case of within-day replanning, we cannot be sure
				 * that the distance in the leg is the actual distance driven by
				 * the agent.
				 */
					log.warn("leg distance for scoring computed from plan, not from execution (=events)." +
							"This is not how it is meant to be, and it will fail for within-day replanning." ) ;
					log.warn("Also means that first and last link are not included." ) ;
					log.warn( Gbl.ONLYONCE ) ;
					distanceWrnCnt++ ;
				}
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling_s + this.params.marginalUtilityOfDistanceCar_m * dist;
			tmpScore += this.params.constantCar ;
		} else if (TransportMode.pt.equals(leg.getMode())) {
			if (this.params.marginalUtilityOfDistancePt_m != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingPT_s + this.params.marginalUtilityOfDistancePt_m * dist;
			tmpScore += this.params.constantPt ;
		} else if (TransportMode.walk.equals(leg.getMode())
				|| TransportMode.transit_walk.equals(leg.getMode())) {
			if (this.params.marginalUtilityOfDistanceWalk_m != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingWalk_s + this.params.marginalUtilityOfDistanceWalk_m * dist;
			tmpScore += this.params.constantWalk ;
		} else if (TransportMode.bike.equals(leg.getMode())) {
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingBike_s;
			tmpScore += this.params.constantBike ;
		} else {
			if (this.params.marginalUtilityOfDistanceCar_m != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			// use the same values as for "car"
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling_s + this.params.marginalUtilityOfDistanceCar_m * dist;
			tmpScore += this.params.constantCar ;
		}

		return tmpScore;
	}

	private void handleLeg(Leg leg, final double time) {
		this.score += calcLegScore(this.lastTime, time, leg);
	}

}
