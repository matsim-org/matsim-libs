/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.locationchoice.frozenepsilons;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.misc.Time;

import java.util.Objects;

import static org.matsim.core.router.TripStructureUtils.Trip;
import static org.matsim.core.router.TripStructureUtils.getTrips;

class PlanTimesAdapter {
	private static final Logger log = Logger.getLogger( PlanTimesAdapter.class ) ;

	private final Config config;

	/* package */ PlanTimesAdapter( final Scenario scenario ) {
		this.config = scenario.getConfig();
	}

	/* package */ double scorePlan (
		  final Plan planTmp,
		  final ScoringFunctionFactory scoringFunctionFactory, Person person ) {
		// We need a copy of the plan since for scoring other fields need to be filled out than in the original plan (e.g. activityEndTime).  The plan is afterwards thrown
		// away (which implies that we should make the defensive copy rather inside this method).

		final ScoringFunction scoringFunction = scoringFunctionFactory.createNewScoringFunction( person);

		// yy The honest way of doing the following would be using the psim. kai, mar'19
		boolean firstAct = true ;
		Activity rememberedActivity = null ;
		double now = Time.getUndefinedTime() ;
		for( PlanElement pe : planTmp.getPlanElements() ){
			if ( pe instanceof Activity ) {
				rememberedActivity = (Activity) pe;
			} else if ( pe instanceof Leg ) {
				// the scoring needs, for "middle" acts, actStart/EndTime filled out, even if the mobsim does not need that:
				if ( firstAct ) {
					firstAct=false ;
				} else {
					rememberedActivity.setStartTime( now );
				}
				now = PlanRouter.calcEndOfActivity( Objects.requireNonNull( rememberedActivity ), planTmp, config ) ;
				rememberedActivity.setEndTime( now );
				scoringFunction.handleActivity( rememberedActivity );
				// ---
				final Leg leg = (Leg) pe;
				// the scoring needs dpTime and tTime filled out, even if qsim input does not require that:
				leg.setDepartureTime( now ) ;
				double travelTime = PopulationUtils.decideOnTravelTimeForLeg( leg ) ;
				if ( Time.isUndefinedTime( travelTime ) ) {
					travelTime = 0. ;
				}
				leg.setTravelTime( travelTime );

				// retrofit routes so that the distance lookup does not fail:
				// todo yyyy remove this again and instead get rid of TripsToLegs.  kai/ihab/md, jan'20
				boolean toBeRemovedAgain = false ;

				if ( leg.getRoute()==null ){
					toBeRemovedAgain = true ;
					Route route = RouteUtils.createGenericRouteImpl( null, null );
					route.setDistance( 0. );
					leg.setRoute( route );
				}

				scoringFunction.handleLeg( leg );

				// remove routes again so that they do not survive
				if ( toBeRemovedAgain ) {
					leg.setRoute( null );
				}

				now += travelTime ;
			} else {
				throw new RuntimeException( "Unsupported PlanElement type" ) ;
			}
		}
		Activity lastAct = (Activity) planTmp.getPlanElements().get( planTmp.getPlanElements().size()-1 );
		lastAct.setStartTime( now );
		scoringFunction.handleActivity( lastAct );
		// the following is hedging against the newer tripscoring; not clear if this will work out-of-sequence.
		for( Trip trip : getTrips( planTmp ) ){
			scoringFunction.handleTrip( trip );
		}

		scoringFunction.finish();
		return scoringFunction.getScore();
	}

}
