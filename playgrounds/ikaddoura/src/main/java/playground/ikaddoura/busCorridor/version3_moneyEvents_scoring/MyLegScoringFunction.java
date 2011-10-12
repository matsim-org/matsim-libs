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

package playground.ikaddoura.busCorridor.version3_moneyEvents_scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.interfaces.BasicScoring;
import org.matsim.core.scoring.interfaces.LegScoring;

public class MyLegScoringFunction implements LegScoring, BasicScoring {

	private final static Logger log = Logger.getLogger(MyLegScoringFunction.class);
	protected final Plan plan;

	protected double score;
	private double lastTime;
	private int index; // the current position in plan.actslegs
	private static final double INITIAL_LAST_TIME = 0.0;
	private static final int INITIAL_INDEX = 1;
	private static final double INITIAL_SCORE = 0.0;

	/** The parameters used for scoring */
	protected final CharyparNagelScoringParameters params;

	public MyLegScoringFunction(final Plan plan, final CharyparNagelScoringParameters params) {
		this.params = params;
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

	private static int distanceWrnCnt = 0 ;
	protected double calcLegScore(final double departureTime, final double arrivalTime, final Leg leg) {
		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // traveltime in seconds
		
		/*
		 * we only as for the route when we have to calculate a distance cost,
		 * because route.getDist() may calculate the distance if not yet
		 * available, which is quite an expensive operation
		 */
		double dist = 0.0; // distance in meters

		if (TransportMode.car.equals(leg.getMode())) {
			if (this.params.marginalUtilityOfDistanceCar_m != 0.0) {
				Route route = leg.getRoute();
				dist = route.getDistance();
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
			tmpScore += this.params.constantPt;
//			tmpScore += 1000 ;

		} else if (TransportMode.walk.equals(leg.getMode())
				|| TransportMode.transit_walk.equals(leg.getMode())) {
			if (this.params.marginalUtilityOfDistanceWalk_m != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingWalk_s + this.params.marginalUtilityOfDistanceWalk_m * dist;
			tmpScore += this.params.constantWalk ;
//			tmpScore += 0 ;
			
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

	private void handleLeg(final double time) {
		LegImpl leg = (LegImpl) this.plan.getPlanElements().get(this.index);
		this.score += calcLegScore(this.lastTime, time, leg);
		this.index += 2;
	}

}
