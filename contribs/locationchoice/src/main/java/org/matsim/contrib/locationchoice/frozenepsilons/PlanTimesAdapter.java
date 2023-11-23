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

import static org.matsim.core.router.TripStructureUtils.Trip;
import static org.matsim.core.router.TripStructureUtils.getTrips;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.timing.TimeInterpretation;

class PlanTimesAdapter {
	private static final Logger log = LogManager.getLogger( PlanTimesAdapter.class ) ;

	private final TimeInterpretation timeInterpretation;

	/* package */ PlanTimesAdapter( final TimeInterpretation timeInterpretation ) {
		this.timeInterpretation = timeInterpretation;
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
		double now = Double.NaN ;
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
				now = timeInterpretation.decideOnActivityEndTimeAlongPlan( Objects.requireNonNull( rememberedActivity ), planTmp ).seconds() ;
				rememberedActivity.setEndTime( now );
				scoringFunction.handleActivity( rememberedActivity );
				// ---
				final Leg leg = (Leg) pe;
				// the scoring needs dpTime and tTime filled out, even if qsim input does not require that:
				leg.setDepartureTime( now ) ;
				double travelTime = timeInterpretation.decideOnLegTravelTime( leg ).orElse(0) ;
				leg.setTravelTime( travelTime);
				scoringFunction.handleLeg( leg );
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
