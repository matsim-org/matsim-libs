/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package eu.eunoiaproject.bikesharing.framework.router;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;

import java.util.List;

public class InitialNodeRouter {
	private final RoutingModule delegate;
	private final double searchRadius;

	private final int desiredNumberOfCalls;
	private final CharyparNagelScoringParameters scoringParams;

	public InitialNodeRouter(
			final RoutingModule delegate,
			final double searchRadius,
			final int desiredNumberOfCalls,
			final CharyparNagelScoringParameters scoringParams) {
		this.delegate = delegate;
		this.searchRadius = searchRadius;
		this.desiredNumberOfCalls = desiredNumberOfCalls;
		this.scoringParams = scoringParams;
	}

	public InitialNodeWithSubTrip calcRoute(
			final TransitRouterNetworkNode node,
			final Facility from,
			final Facility to,
			final double dep,
			final Person pers) {
		final List<? extends PlanElement> trip = delegate.calcRoute( from , to , dep , pers );
		final double duration = calcDuration( trip );
		final double cost = calcCost( trip );
		return new InitialNodeWithSubTrip( node , cost , dep + duration , trip );
	}


	protected double calcDuration(final List<? extends PlanElement> trip) {
		double tt = 0;

		for ( PlanElement pe : trip ) {
			if ( pe instanceof Leg ) {
				final double curr = ((Leg) pe).getTravelTime();
				if ( curr == Time.UNDEFINED_TIME ) throw new RuntimeException( pe+" has not travel time" );
				tt += curr;
			}

			if ( pe instanceof Activity ) {
				final double dur = ((Activity) pe).getMaximumDuration();
				if ( dur != Time.UNDEFINED_TIME ) {
					tt += dur;
				}
			}

		}

		return tt;
	}

	protected double calcCost(final List<? extends PlanElement> trip) {
		double cost = 0;

		for ( PlanElement pe : trip ) {
			if ( pe instanceof Leg ) {
				final Leg leg = (Leg) pe;
				final double time = leg.getTravelTime();
				if ( time == Time.UNDEFINED_TIME ) throw new RuntimeException( pe+" has not travel time" );
				// XXX no distance!
				// /!\ this is cost, thus minus utility!
				cost -= scoringParams.modeParams.get( leg.getMode() ).marginalUtilityOfTraveling_s * time;
				cost -= scoringParams.modeParams.get( leg.getMode() ).constant;
			}
		}

		return cost;

	}

	public StageActivityTypes getStageActivities() {
		return delegate.getStageActivityTypes();
	}

	/**
	 * @return the number of time the calcRoute method should be called
	 * for each origin/destination pair. This should be 1 in most of the cases,
	 * but higher rates might be useful for randomized routers, such as bike sharing.
	 */
	public int getDesiredNumberOfCalls() {
		return desiredNumberOfCalls;
	}

	/**
	 * @return the radius within which the stations should be searched.
	 */
	public double getSearchRadius() {
		return searchRadius;
	}
}
