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

package org.matsim.core.scoring.functions;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.ScoringFunctionAccumulator.ArbitraryEventScoring;
import org.matsim.core.scoring.ScoringFunctionAccumulator.LegScoring;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;

/**
 * This is a re-implementation of the original CharyparNagel function, based on a
 * modular approach.
 * @see <a href="http://www.matsim.org/node/263">http://www.matsim.org/node/263</a>
 * @author rashid_waraich
 */
public class CharyparNagelLegScoring implements LegScoring, ArbitraryEventScoring {

	protected double score;
	private double lastTime;

	private static final double INITIAL_LAST_TIME = 0.0;
	private static final double INITIAL_SCORE = 0.0;

	/** The parameters used for scoring */
	protected final CharyparNagelScoringParameters params;
	private Leg currentLeg;
	protected Network network;
	private boolean nextEnterVehicleIsFirstOfTrip = true ;
	private boolean nextStartPtLegIsFirstOfTrip = true ;
	private boolean currentLegIsPtLeg = false;
	private double lastActivityEndTime = Time.UNDEFINED_TIME ;

	public CharyparNagelLegScoring(final CharyparNagelScoringParameters params, Network network) {
		this.params = params;
		this.network = network;
		this.reset();
	}

	@Override
	public void reset() {
		this.lastTime = INITIAL_LAST_TIME;
		this.score = INITIAL_SCORE;
		this.nextEnterVehicleIsFirstOfTrip = true ;
		this.nextStartPtLegIsFirstOfTrip = true ;
		this.currentLegIsPtLeg = false;
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

	private void handleLeg(Leg leg, final double time) {
		this.score += calcLegScore(this.lastTime, time, leg);
	}

	@Override
	public void finish() {

	}

	@Override
	public double getScore() {
		return this.score;
	}

	private static int ccc=0 ;
	
	protected double calcLegScore(final double departureTime, final double arrivalTime, final Leg leg) {
		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // travel time in seconds	
		if (TransportMode.car.equals(leg.getMode())) {
			double dist = 0.0; // distance in meters
			if (this.params.marginalUtilityOfDistanceCar_m != 0.0) {
				Route route = leg.getRoute();
				dist = getDistance(route);
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling_s + this.params.marginalUtilityOfDistanceCar_m * dist;
			tmpScore += this.params.constantCar ;
			// (yyyy once we have multiple car legs without "real" activities in between, this will produce wrong results.  kai, dec'12)
		} else if (TransportMode.pt.equals(leg.getMode())) {
			double dist = 0.0; // distance in meters
			if (this.params.marginalUtilityOfDistancePt_m != 0.0) {
				Route route = leg.getRoute();
				dist = getDistance(route);
				if ( Double.isNaN(dist) ) {
					if ( ccc<10 ) {
						ccc++ ;
						Logger.getLogger(this.getClass()).warn("distance is NaN. Will make score of this plan NaN. Possible reason: router does not " +
								"write distance into plan.  Needs to be fixed or these plans will die out.") ;
						if ( ccc==10 ) {
							Logger.getLogger(this.getClass()).warn(Gbl.FUTURE_SUPPRESSED) ;
						}
					}
				}
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingPT_s + this.params.marginalUtilityOfDistancePt_m * dist;
			
			// (yy NOTE: pt wait is not separately scored!! --> should be done!  kai, nov'12)
			// see below, but might be nicer to do it here.  kai, may'13, but added earlier than that

			tmpScore += this.params.constantPt ;
			// (yy NOTE: the pt constant is added for _every_ pt leg.  This is not how such models are estimated.  kai, nov'12)
			// see below, but might be nicer to do it here.  kai, dec'12

		} else if (TransportMode.walk.equals(leg.getMode()) || TransportMode.transit_walk.equals(leg.getMode())) {
			double dist = 0.0; // distance in meters
			if (this.params.marginalUtilityOfDistanceWalk_m != 0.0) {
				Route route = leg.getRoute();
				dist = getDistance(route);
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingWalk_s + this.params.marginalUtilityOfDistanceWalk_m * dist;
			tmpScore += this.params.constantWalk ;
		} else if (TransportMode.bike.equals(leg.getMode())) {
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingBike_s;
			tmpScore += this.params.constantBike ;
			// (yyyy once we have multiple bike legs without "real" activities in between, this will produce wrong results.  kai, dec'12)
		} else {
			double dist = 0.0; // distance in meters
			if (this.params.marginalUtilityOfDistanceOther_m != 0.0) {
				Route route = leg.getRoute();
				dist = getDistance(route);
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingOther_s + this.params.marginalUtilityOfDistanceOther_m * dist;
			tmpScore += this.params.constantOther ;
			// (yyyy once we have multiple "other" legs without "real" activities in between, this will produce wrong results.  kai, dec'12)
		}
		return tmpScore;
	}

	protected double getDistance(Route route) {
		double dist;
		if (route instanceof NetworkRoute) {
			dist =  RouteUtils.calcDistance((NetworkRoute) route, network);
		} else {
			dist = route.getDistance();
		}
		return dist;
	}
	
	@Override
	public void handleEvent(Event event) {
		if ( event instanceof ActivityEndEvent ) {
			// When there is a "real" activity, flags are reset:
			if ( !PtConstants.TRANSIT_ACTIVITY_TYPE.equals( ((ActivityEndEvent)event).getActType()) ) {
				this.nextEnterVehicleIsFirstOfTrip  = true ;
				this.nextStartPtLegIsFirstOfTrip = true ;
			}
			this.lastActivityEndTime = event.getTime() ;
		}

		if ( event instanceof PersonEntersVehicleEvent && currentLegIsPtLeg ) {
			if ( !this.nextEnterVehicleIsFirstOfTrip ) {
				// all vehicle entering after the first triggers the disutility of line switch:
				this.score  += params.utilityOfLineSwitch ;
			}
			this.nextEnterVehicleIsFirstOfTrip = false ;
			// add score of waiting, _minus_ score of travelling (since it is added in the legscoring above):
			this.score += (event.getTime() - this.lastActivityEndTime) * (this.params.marginalUtilityOfWaitingPt_s - this.params.marginalUtilityOfTravelingPT_s) ;
		}

		if ( event instanceof PersonDepartureEvent ) {
			this.currentLegIsPtLeg = TransportMode.pt.equals( ((PersonDepartureEvent)event).getLegMode() );
			if ( currentLegIsPtLeg ) {
				if ( !this.nextStartPtLegIsFirstOfTrip ) {
					this.score -= params.constantPt ;
					// (yyyy deducting this again, since is it wrongly added above.  should be consolidated; this is so the code
					// modification is minimally invasive.  kai, dec'12)
				}
				this.nextStartPtLegIsFirstOfTrip = false ;
			}
		}
	}


}
