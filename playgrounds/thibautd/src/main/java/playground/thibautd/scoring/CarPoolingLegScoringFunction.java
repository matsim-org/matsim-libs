/* *********************************************************************** *
 * project: org.matsim.*
 * CarPoolingLegScoringFunction.java
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
package playground.thibautd.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.utils.misc.RouteUtils;

import playground.thibautd.cliquessim.population.JointActingTypes;

/**
 * @author thibautd
 */
public class CarPoolingLegScoringFunction extends CharyparNagelLegScoring {
	private static final Logger log =
		Logger.getLogger(CarPoolingLegScoringFunction.class);

	private static int distanceWrnCnt = 0;
	private static int carLegCount = 0;
	private static int invalidCarRoutes = 0;

	public CarPoolingLegScoringFunction(
			final CharyparNagelScoringParameters params,
			final Network network) {
		super( params , network );
	}

	// /////////////////////////////////////////////////////////////////////////
	// overriden methods
	// /////////////////////////////////////////////////////////////////////////

	/**
	 * reimplementation taking distance from leg rather than route.
	 * TODO: include car pooling
	 */
	@Override
	protected double calcLegScore(
			final double departureTime,
			final double arrivalTime,
			final Leg leg) {
		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // traveltime in
															// seconds

		/*
		 * we only as for the route when we have to calculate a distance cost,
		 * because route.getDist() may calculate the distance if not yet
		 * available, which is quite an expensive operation
		 */
		double dist = 0.0; // distance in meters

		if (TransportMode.car.equals(leg.getMode()) || JointActingTypes.DRIVER.equals( leg.getMode() )) {
			if (this.params.marginalUtilityOfDistanceCar_m != 0.0) {
				NetworkRoute route = null;
				boolean isValidRoute = true;

				try {
					route = (NetworkRoute) leg.getRoute();
				} catch (ClassCastException e) {
					isValidRoute = false;
				}

				carLegCount++;
				if (isValidRoute) {
					dist = RouteUtils.calcDistance(route, network);
				}
				else {
					invalidCarRoutes++;
					dist = leg.getRoute().getDistance();
				}

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
		}
		else if (JointActingTypes.PASSENGER.equals( leg.getMode() )) {
			// same as car, without mariginal utility of distance (interpreted as monetary cost)
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling_s;
			tmpScore += this.params.constantCar ;
		}
		else if (TransportMode.pt.equals(leg.getMode())) {
			if (this.params.marginalUtilityOfDistancePt_m != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingPT_s + this.params.marginalUtilityOfDistancePt_m * dist;
			tmpScore += this.params.constantPt ;
		}
		else if (TransportMode.walk.equals(leg.getMode())
				|| TransportMode.transit_walk.equals(leg.getMode())) {
			if (this.params.marginalUtilityOfDistanceWalk_m != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingWalk_s + this.params.marginalUtilityOfDistanceWalk_m * dist;
			tmpScore += this.params.constantWalk ;
		}
		else if (TransportMode.bike.equals(leg.getMode())) {
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingBike_s;
			tmpScore += this.params.constantBike ;
		}
		else {
			if (this.params.marginalUtilityOfDistanceCar_m != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			// use the same values as for "car"
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling_s + this.params.marginalUtilityOfDistanceCar_m * dist;
			tmpScore += this.params.constantCar ;
		}

		return tmpScore;

	}

	private static class LogListener implements IterationEndsListener {
		@Override
		public void notifyIterationEnds(final IterationEndsEvent event) {
			log.info( "leg scoring information for iteration "+event.getIteration()+": got "+carLegCount+" car legs, "+invalidCarRoutes+" with no route." );

			carLegCount = 0;
			invalidCarRoutes = 0;
		}
	}

	public static IterationEndsListener getInformationLogger() {
		return new LogListener();
	}
}

