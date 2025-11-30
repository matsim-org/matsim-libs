/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.application.analysis.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.ScoringParameters;

/**
 * @author ikaddoura
 *
 */
public final class MarginalSumScoringFunction {
	private final static Logger log = LogManager.getLogger(MarginalSumScoringFunction.class);
	private final ScoringParameters params;

	CharyparNagelActivityScoring activityScoringA;
	CharyparNagelActivityScoring activityScoringB;

	public MarginalSumScoringFunction(ScoringParameters params) {
		this.params = params;
		activityScoringA = new CharyparNagelActivityScoring(params);
		activityScoringB = new CharyparNagelActivityScoring(params);
	}

	private static int deltaScoreZeroWrnCnt = 0;

	/**
	 * @param personId
	 * @param activity
	 * @param delay -- in the current implementation, this will be treated as a negative delay!
	 * @return
	 */
	public final double getNormalActivityDelayDisutility( Id<Person> personId, Activity activity, double delay ) {

		SumScoringFunction sumScoringA = new SumScoringFunction() ;
		sumScoringA.addScoringFunction(activityScoringA);

		SumScoringFunction sumScoringB = new SumScoringFunction() ;
		sumScoringB.addScoringFunction(activityScoringB);

		if (activity.getStartTime().seconds() != Double.NEGATIVE_INFINITY && activity.getEndTime().seconds() != Double.NEGATIVE_INFINITY) {
        	// activity is not the first and not the last activity
        } else {
        	throw new RuntimeException("Missing start or end time! The provided activity is probably the first or last activity. Aborting...");
        }

		double scoreA0 = sumScoringA.getScore();
		double scoreB0 = sumScoringB.getScore();

		Activity activityWithoutDelay = PopulationUtils.createActivity(activity);
		activityWithoutDelay.setStartTime(activity.getStartTime().seconds() - delay);
		// yy Depending on how complete the later used "handleActivity" is set up, the facility may become closed at exactly this time step, and then the resulting VTTS will be zero. kai, nov'25
		// --> However, may also work the other way around, and the facility may just become open at exactly this time step.

		sumScoringA.handleActivity(activity);
		sumScoringB.handleActivity(activityWithoutDelay);

		sumScoringA.finish();
		sumScoringB.finish();

		double scoreA1 = sumScoringA.getScore();
		double scoreB1 = sumScoringB.getScore();

		double scoreWithDelay = scoreA1 - scoreA0;
		double scoreWithoutDelay = scoreB1 - scoreB0;

		final double deltaScore = scoreWithoutDelay - scoreWithDelay;

		if ( deltaScore==0. && deltaScoreZeroWrnCnt<10 ) {
			deltaScoreZeroWrnCnt++;
			final ActivityUtilityParameters activityUtilityParameters = params.actParams.get( activity.getType() );
			log.warn( "actDelayDisutil=0; presumably actStart outside opening times; personId={}; actType={}; actStart={}; actEnd={}; actOpening={}; actClosing={}",
				personId, activity.getType(), activity.getStartTime().seconds()/3600., activity.getEndTime().seconds()/3600.,
				activityUtilityParameters.getOpeningTime(), activityUtilityParameters.getClosingTime() );
//			log.warn( "score0={}; scoreWDelay={}", new BigDecimal( scoreWithoutDelay), new BigDecimal( scoreWithDelay) );
			if ( deltaScoreZeroWrnCnt==10 ) {
				log.warn( Gbl.FUTURE_SUPPRESSED );
			}
		}

		return deltaScore;
	}

	public final double getOvernightActivityDelayDisutility(Activity activityMorning, Activity activityEvening, double delay) {

		SumScoringFunction delegateA = new SumScoringFunction() ;
		delegateA.addScoringFunction(activityScoringA);

		SumScoringFunction delegateB = new SumScoringFunction() ;
		delegateB.addScoringFunction(activityScoringB);

	//	log.info("activityMorning: " + activityMorning.toString());
	//	log.info("activityEvening: " + activityEvening.toString());
	//	log.info("activityEveningWithoutDelay: " + activityEveningWithoutDelay.toString());

		if (activityMorning.getStartTime().isUndefined()  && activityMorning.getEndTime().isDefined()) {
        	// 'morningActivity' is the first activity
        } else {
        	throw new RuntimeException("activityMorning is not the first activity. Or why does it have a start time? Aborting...");
        }

        if (activityEvening.getStartTime().isDefined() && activityEvening.getEndTime().isUndefined()) {
        	// 'eveningActivity' is the last activity
        } else {
        	throw new RuntimeException("activityEvening is not the last activity. Or why does it have an end time? Aborting...");
        }

		double scoreA0 = delegateA.getScore();
		double scoreB0 = delegateB.getScore();

		delegateA.handleActivity(activityMorning);
		delegateB.handleActivity(activityMorning);

		Activity activityEveningWithoutDelay = PopulationUtils.createActivity(activityEvening);
		activityEveningWithoutDelay.setStartTime(activityEvening.getStartTime().seconds() - delay);


		delegateA.handleActivity(activityEvening);
		delegateB.handleActivity(activityEveningWithoutDelay);

		delegateA.finish();
		delegateB.finish();

		double scoreA1 = delegateA.getScore();
		double scoreB1 = delegateB.getScore();

		double scoreWithDelay = scoreA1 - scoreA0;
		double scoreWithoutDelay = scoreB1 - scoreB0;

		return scoreWithoutDelay - scoreWithDelay;
	}

}
