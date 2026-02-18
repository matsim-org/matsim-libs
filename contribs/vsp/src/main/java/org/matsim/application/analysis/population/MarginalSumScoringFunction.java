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

	static record Scores( double scoreNormal, double deltaScore ) {
	}

	/**
	 * @param personId
	 * @param activity
	 * @param earlier -- in the current implementation, this will be treated as a negative earlier!
	 * @return
	 */
	public final Scores getNormalActivityDelayDisutility( Id<Person> personId, Activity activity, double earlier ) {

		SumScoringFunction sumScoringNormal = new SumScoringFunction() ;
		sumScoringNormal.addScoringFunction(activityScoringA);
		// yyyyyy it is not clear to me why this does not add the same scoring fct contribution multiple times.  kai, dec'25

		SumScoringFunction sumScoringEarly = new SumScoringFunction() ;
		sumScoringEarly.addScoringFunction(activityScoringB);

		if (activity.getStartTime().seconds() != Double.NEGATIVE_INFINITY && activity.getEndTime().seconds() != Double.NEGATIVE_INFINITY) {
        	// activity is not the first and not the last activity
        } else {
        	throw new RuntimeException("Missing start or end time! The provided activity is probably the first or last activity. Aborting...");
        }

		double scoreBeforeActNormal = sumScoringNormal.getScore();
		double scoreBeforeActEarly = sumScoringEarly.getScore();

		Activity earlyActivity = PopulationUtils.createActivity(activity);
		earlyActivity.setStartTime(activity.getStartTime().seconds() - earlier);
		// yy Depending on how complete the later used "handleActivity" is set up, the facility may become closed at exactly this time step, and then the resulting VTTS will be zero. kai, nov'25
		// --> However, may also work the other way around, and the facility may just become open at exactly this time step.

		sumScoringNormal.handleActivity(activity);
		sumScoringEarly.handleActivity(earlyActivity);

		sumScoringNormal.finish();
		sumScoringEarly.finish();

		double scoreAfterActNormal = sumScoringNormal.getScore();
		double scoreAfterActEarly = sumScoringEarly.getScore();

		double scoreNormal = scoreAfterActNormal - scoreBeforeActNormal;
		double scoreForLongerActivity = scoreAfterActEarly - scoreBeforeActEarly;

		final double deltaScore = scoreForLongerActivity - scoreNormal;

		if ( deltaScore==0. && deltaScoreZeroWrnCnt<10 ) {
			deltaScoreZeroWrnCnt++;
			final ActivityUtilityParameters activityUtilityParameters = params.actParams.get( activity.getType() );
			log.warn( "actDelayDisutil=0; presumably actStart outside opening times; personId={}; actType={}; actStart={}; actEnd={}; actOpening={}; actClosing={}",
				personId, activity.getType(), activity.getStartTime().seconds()/3600., activity.getEndTime().seconds()/3600.,
				activityUtilityParameters.getOpeningTime(), activityUtilityParameters.getClosingTime() );
//			log.warn( "score0={}; scoreWDelay={}", new BigDecimal( scoreForLongerActivity), new BigDecimal( scoreNormal) );
			if ( deltaScoreZeroWrnCnt==10 ) {
				log.warn( Gbl.FUTURE_SUPPRESSED );
			}
		}

		return new Scores( scoreNormal, deltaScore );
	}

	public final Scores getOvernightActivityDelayDisutility(Activity activityMorning, Activity activityEveningNormal, double earlier) {

		SumScoringFunction normalScoring = new SumScoringFunction() ;
		normalScoring.addScoringFunction(activityScoringA);

		SumScoringFunction earlyScoring = new SumScoringFunction() ;
		earlyScoring.addScoringFunction(activityScoringB);

	//	log.info("activityMorning: " + activityMorning.toString());
	//	log.info("activityEveningNormal: " + activityEveningNormal.toString());
	//	log.info("activityEveningWithEarlyStart: " + activityEveningWithEarlyStart.toString());

		if (activityMorning.getStartTime().isUndefined()  && activityMorning.getEndTime().isDefined()) {
        	// 'morningActivity' is the first activity
        } else {
        	throw new RuntimeException("activityMorning is not the first activity. Or why does it have a start time? Aborting...");
        }

        if (activityEveningNormal.getStartTime().isDefined() && activityEveningNormal.getEndTime().isUndefined()) {
        	// 'eveningActivity' is the last activity
        } else {
        	throw new RuntimeException("activityEveningNormal is not the last activity. Or why does it have an end time? Aborting...");
        }

		double scoreA0 = normalScoring.getScore();
		double scoreB0 = earlyScoring.getScore();

		normalScoring.handleActivity(activityMorning);
		earlyScoring.handleActivity(activityMorning);

		Activity activityEveningWithEarlyStart = PopulationUtils.createActivity(activityEveningNormal);
		activityEveningWithEarlyStart.setStartTime(activityEveningNormal.getStartTime().seconds() - earlier);


		normalScoring.handleActivity(activityEveningNormal);
		earlyScoring.handleActivity(activityEveningWithEarlyStart);

		normalScoring.finish();
		earlyScoring.finish();

		double scoreNormal = normalScoring.getScore() - scoreA0;
		double scoreWithEarlierStart = earlyScoring.getScore() - scoreB0;

		final double deltaScore = scoreWithEarlierStart - scoreNormal;
		return new Scores( scoreNormal, deltaScore );
	}

}
