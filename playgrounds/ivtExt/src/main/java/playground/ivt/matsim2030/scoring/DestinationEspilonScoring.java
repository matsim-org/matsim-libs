/* *********************************************************************** *
 * project: org.matsim.*
 * DestinationEspilonScoring.java
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
package playground.ivt.matsim2030.scoring;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.BestReplyDestinationChoice;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DestinationScoring;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.SumScoringFunction.ActivityScoring;
import org.matsim.core.scoring.SumScoringFunction.LegScoring;
import org.matsim.core.utils.misc.Time;

/**
 * TODO: propose to put in location choice contrib
 * @author thibautd
 */
public class DestinationEspilonScoring implements ActivityScoring, LegScoring,
	   ScoringFunctionAccumulator.ActivityScoring, ScoringFunctionAccumulator.LegScoring {
	private final DestinationChoiceBestResponseContext context;
	private final DestinationScoring scoring;

	private double score = 0;

	private final PlanImpl currentPlan;

	public DestinationEspilonScoring(
			final Person person,
			final DestinationChoiceBestResponseContext context ) {
		this.context = context;
		this.scoring = new DestinationScoring( context );
		this.currentPlan = new PlanImpl( person );
	}

	@Override
	public void finish() {
	}

	@Override
	public double getScore() {
		return score;
	}

	private int activityIndex = 0 ;
	@Override
	public void handleFirstActivity(final Activity act) {
		activityIndex = 0 ;
		handleActivity( act );
	}

	@Override
	public void handleActivity(final Activity act) {
		activityIndex ++;
		// XXX this is because location choice uses the index of the activity in the
		// plan to change the epsilon (which is wrong, but anyway).
		// Thibaut, we now changed this to counting internally via activityIndex, but we recognize that this is still far from optimal. kai/mz, oct'14
		currentPlan.addActivity( act );
		if ( !context.getScaleEpsilon().isFlexibleType( act.getType() ) ) return;

		// XXX What the hell is that???? a static config parameter??? Why?
		final double fVar =
			BestReplyDestinationChoice.useScaleEpsilonFromConfig < 0.0 ?
				context.getScaleEpsilon().getEpsilonFactor( act.getType() ) :
				BestReplyDestinationChoice.useScaleEpsilonFromConfig;

//				score += scoring.getDestinationScore(
//						currentPlan,
//						(ActivityImpl) act, // XXX Activity would be sufficient...
//						fVar );
				score += scoring.getDestinationScore(
						act,
						fVar, activityIndex, currentPlan.getPerson().getId() );
	}

	@Override
	public void handleLastActivity(final Activity act) {
		activityIndex ++;
		// should be the same as the first => already done
	}

	@Override
	public void handleLeg(Leg leg) {
		currentPlan.addLeg( leg );
	}

	// /////////////////////////////////////////////////////////////////////////
	// For compatibility with elements still implementing the old deprecated interfaces.
	@Override
	@Deprecated
	public void reset() {}

	@Override
	@Deprecated
	public void startLeg(double time, Leg leg) {
		handleLeg( leg );
	}

	@Override
	@Deprecated
	public void endLeg(double time) {}

	@Override
	@Deprecated
	public void startActivity(double time, Activity act) {
		if ( act.getEndTime() != Time.UNDEFINED_TIME ) {
			handleActivity( act );
		}
		else {
			handleLastActivity( act );
		}
	}

	@Override
	@Deprecated
	public void endActivity(double time, Activity act) {
		if ( act.getStartTime() == Time.UNDEFINED_TIME ) {
			handleFirstActivity( act );
		}
	}
}

