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
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.SumScoringFunction.ActivityScoring;
import org.matsim.core.scoring.SumScoringFunction.LegScoring;

/**
 * TODO: propose to put in location choice contrib
 * @author thibautd
 */
public class DestinationEspilonScoring implements ActivityScoring, LegScoring {
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

	@Override
	public void handleFirstActivity(final Activity act) {
		handleActivity( act );
	}

	@Override
	public void handleActivity(final Activity act) {
		// XXX this is because location choice uses the index of the activity in the
		// plan to change the epsilon (which is wrong, but anyway).
		currentPlan.addActivity( act );
		if ( !context.getScaleEpsilon().isFlexibleType( act.getType() ) ) return;

		// XXX What the hell is that???? a static config parameter??? Why?
		final double fVar =
			BestReplyDestinationChoice.useScaleEpsilonFromConfig < 0.0 ?
				context.getScaleEpsilon().getEpsilonFactor( act.getType() ) :
				BestReplyDestinationChoice.useScaleEpsilonFromConfig;

		score += scoring.getDestinationScore(
				currentPlan,
				(ActivityImpl) act, // XXX Activity would be sufficient...
				fVar );
	}

	@Override
	public void handleLastActivity(final Activity act) {
		// should be the same as the first => already done
	}

	@Override
	public void handleLeg(Leg leg) {
		currentPlan.addLeg( leg );
	}
}

