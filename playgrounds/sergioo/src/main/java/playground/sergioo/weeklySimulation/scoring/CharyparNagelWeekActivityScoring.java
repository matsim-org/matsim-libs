/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelOpenTimesScoringFunction.java
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

package playground.sergioo.weeklySimulation.scoring;

import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scoring.ScoringFunctionAccumulator.ActivityScoring;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.OpeningTime;

/**
 * Same as CharyparNagelScoringFunction, but retrieves opening time information
 * from facility object of an activity instead of the config file.
 *
 * @author meisterk
 *
 */
public class CharyparNagelWeekActivityScoring implements ActivityScoring, org.matsim.core.scoring.SumScoringFunction.ActivityScoring {

	private static final Logger log = Logger.getLogger(CharyparNagelWeekActivityScoring.class);
	private static final double INITIAL_LAST_TIME = 0.0;
	private static final double INITIAL_FIRST_ACT_END_TIME = Time.UNDEFINED_TIME;
	private static final double INITIAL_SCORE = 0.0;
	private static int firstLastActWarning = 0;
	private static short firstLastActOpeningTimesWarning = 0;
	
	private final ActivityFacilities facilities;
	protected double score;
	private double currentActivityStartTime;
	private double firstActivityEndTime;
	private final CharyparNagelScoringParameters params;
	private Activity currentActivity;
	private boolean firstAct = true;
	private Activity firstActivity;
	
	public CharyparNagelWeekActivityScoring(final CharyparNagelScoringParameters params, final ActivityFacilities facilities) {
		this.params = params;
		this.reset();
		this.facilities = facilities;
	}

	@Override
	public void reset() {
		this.firstAct = true;
		this.currentActivityStartTime = INITIAL_LAST_TIME;
		this.firstActivityEndTime = INITIAL_FIRST_ACT_END_TIME;
		this.score = INITIAL_SCORE;
		
		firstLastActWarning = 0 ;
		firstLastActOpeningTimesWarning = 0 ;
	}
	@Override
	@Deprecated // preferably use SumScoringFunction.  kai, oct'13
	public void startActivity(final double time, final Activity act) {
		assert act != null;
		this.currentActivity = act;
		this.currentActivityStartTime = time;
	}
	@Override
	@Deprecated // preferably use SumScoringFunction.  kai, oct'13
	public void endActivity(final double time, final Activity act) {
		assert act != null;
		assert currentActivity == null || currentActivity.getType().equals(act.getType());
		if (this.firstAct) {
			this.firstActivityEndTime = time;
			this.firstActivity = act;
			this.firstAct = false;
		} else {
			this.score += calcActScore(this.currentActivityStartTime, time, act);
		}
		currentActivity = null;
	}
	@Override
	public void finish() {
		if (this.currentActivity != null) {
			handleOvernightActivity(this.currentActivity); 
		} else {
			if (this.firstActivity != null) {
				handleMorningActivity();
			}
			// Else, no activity has started so far.
			// This probably means that the plan contains at most one activity.
			// We cannot handle that correctly, because we do not know what it is.
		}
	}
	@Override
	public double getScore() {
		return this.score;
	}
	private static int wrnCnt = 0 ;

	protected double calcActScore(final double arrivalTime, final double departureTime, final Activity act) {

		ActivityUtilityParameters actParams = this.params.utilParams.get(act.getType());
		if (actParams == null) {
			throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in utility parameters " +
					"(module name=\"planCalcScore\" in the config file).");
		}

		double tmpScore = 0.0;

		if (actParams.isScoreAtAll()) {
			/* Calculate the times the agent actually performs the
			 * activity.  The facility must be open for the agent to
			 * perform the activity.  If it's closed, but the agent is
			 * there, the agent must wait instead of performing the
			 * activity (until it opens).
			 *
			 *                                             Interval during which
			 * Relationship between times:                 activity is performed:
			 *
			 *      O________C A~~D  ( 0 <= C <= A <= D )   D...D (not performed)
			 * A~~D O________C       ( A <= D <= O <= C )   D...D (not performed)
			 *      O__A+++++C~~D    ( O <= A <= C <= D )   A...C
			 *      O__A++D__C       ( O <= A <= D <= C )   A...D
			 *   A~~O++++++++C~~D    ( A <= O <= C <= D )   O...C
			 *   A~~O+++++D__C       ( A <= O <= D <= C )   O...D
			 *
			 * Legend:
			 *  A = arrivalTime    (when agent gets to the facility)
			 *  D = departureTime  (when agent leaves the facility)
			 *  O = openingTime    (when facility opens)
			 *  C = closingTime    (when facility closes)
			 *  + = agent performs activity
			 *  ~ = agent waits (agent at facility, but not performing activity)
			 *  _ = facility open, but agent not there
			 *
			 * assume O <= C
			 * assume A <= D
			 */

			double[] openingInterval = this.getOpeningInterval(act);
			double openingTime = openingInterval[0];
			double closingTime = openingInterval[1];

			double activityStart = arrivalTime;
			double activityEnd = departureTime;

			if ((openingTime >=  0) && (arrivalTime < openingTime)) {
				activityStart = openingTime;
			}
			if ((closingTime >= 0) && (closingTime < departureTime)) {
				activityEnd = closingTime;
			}
			if ((openingTime >= 0) && (closingTime >= 0)
					&& ((openingTime > departureTime) || (closingTime < arrivalTime))) {
				// agent could not perform action
				activityStart = departureTime;
				activityEnd = departureTime;
			}
			double duration = activityEnd - activityStart;

			// disutility if too early
			if (arrivalTime < activityStart) {
				// agent arrives to early, has to wait
				tmpScore += this.params.marginalUtilityOfWaiting_s * (activityStart - arrivalTime);
			}

			// disutility if too late

			double latestStartTime = actParams.getLatestStartTime();
			if ((latestStartTime >= 0) && (activityStart > latestStartTime)) {
				tmpScore += this.params.marginalUtilityOfLateArrival_s * (activityStart - latestStartTime);
			}

			// utility of performing an action, duration is >= 1, thus log is no problem
			double typicalDuration = actParams.getTypicalDuration();

			if ( this.params.usingOldScoringBelowZeroUtilityDuration ) {
				if (duration > 0) {
					double utilPerf = this.params.marginalUtilityOfPerforming_s * typicalDuration
							* Math.log((duration / 3600.0) / actParams.getZeroUtilityDuration_h());
					double utilWait = this.params.marginalUtilityOfWaiting_s * duration;
					tmpScore += Math.max(0, Math.max(utilPerf, utilWait));
				} else {
					tmpScore += 2*this.params.marginalUtilityOfLateArrival_s*Math.abs(duration);
				}
			} else {
				if ( duration >= 3600.*actParams.getZeroUtilityDuration_h() ) {
					double utilPerf = this.params.marginalUtilityOfPerforming_s * typicalDuration
							* Math.log((duration / 3600.0) / actParams.getZeroUtilityDuration_h());
					// also removing the "wait" alternative scoring.
					tmpScore += utilPerf ;
				} else {
					if ( wrnCnt < 1 ) {
						wrnCnt++ ;
						log.warn("encountering duration < zeroUtilityDuration; the logic for this was changed around mid-nov 2013.") ;
						log.warn( "your final score thus will be different from earlier runs; set usingOldScoringBelowZeroUtilityDuration to true if you "
								+ "absolutely need the old version.  See https://matsim.atlassian.net/browse/MATSIM-191." );
						log.warn( Gbl.ONLYONCE ) ;
					}
					
					// below zeroUtilityDuration, we linearly extend the slope ...:
					double slopeAtZeroUtility = this.params.marginalUtilityOfPerforming_s * typicalDuration / ( 3600.*actParams.getZeroUtilityDuration_h() ) ;
					if ( slopeAtZeroUtility < 0. ) {
						// (beta_perf might be = 0)
						System.err.println("beta_perf: " + this.params.marginalUtilityOfPerforming_s);
						System.err.println("typicalDuration: " + typicalDuration );
						System.err.println( "zero utl duration: " + actParams.getZeroUtilityDuration_h() );
						throw new RuntimeException( "slope at zero utility < 0.; this should not happen ...");
					}
					double durationUnderrun = actParams.getZeroUtilityDuration_h()*3600. - duration ;
					if ( durationUnderrun < 0. ) {
						throw new RuntimeException( "durationUnderrun < 0; this should not happen ...") ;
					}
					tmpScore -= slopeAtZeroUtility * durationUnderrun ;
				}
				
			}

			// disutility if stopping too early
			double earliestEndTime = actParams.getEarliestEndTime();
			if ((earliestEndTime >= 0) && (activityEnd < earliestEndTime)) {
				tmpScore += this.params.marginalUtilityOfEarlyDeparture_s * (earliestEndTime - activityEnd);
			}

			// disutility if going to away to late
			if (activityEnd < departureTime) {
				tmpScore += this.params.marginalUtilityOfWaiting_s * (departureTime - activityEnd);
			}

			// disutility if duration was too short
			double minimalDuration = actParams.getMinimalDuration();
			if ((minimalDuration >= 0) && (duration < minimalDuration)) {
				tmpScore += this.params.marginalUtilityOfEarlyDeparture_s * (minimalDuration - duration);
			}
		}
		return tmpScore;
	}
	protected double[] getOpeningInterval(Activity act) {
		// openInterval has two values
		// openInterval[0] will be the opening time
		// openInterval[1] will be the closing time
		double[] openInterval = new double[]{Time.UNDEFINED_TIME, Time.UNDEFINED_TIME};
		boolean foundAct = false;
		ActivityFacility facility = this.facilities.getFacilities().get(act.getFacilityId());
		Iterator<String> facilityActTypeIterator = facility.getActivityOptions().keySet().iterator();
		String facilityActType = null;
		Set<OpeningTime> opentimes = null;
		while (facilityActTypeIterator.hasNext() && !foundAct) {
			facilityActType = facilityActTypeIterator.next();
			if (act.getType().equals(facilityActType)) {
				foundAct = true;
				opentimes = facility.getActivityOptions().get(facilityActType).getOpeningTimes();
				if (opentimes != null && opentimes.size()>0)
					for (OpeningTime opentime : opentimes)
						//Assumes just first interval which intersects the activity can be scored
						if(intersects(act.getStartTime(), act.getEndTime(), opentime.getStartTime(), opentime.getEndTime())) {
							openInterval[0] = opentime.getStartTime();
							openInterval[1] = opentime.getEndTime();
							return openInterval;
						}
			}
		}
		if (!foundAct)
			throw new RuntimeException("No suitable facility activity type found. Aborting...");
		return openInterval;
	}

	private boolean intersects(double startTime, double endTime, double startTime2, double endTime2) {
		return endTime>=startTime2 && endTime2>=startTime;
	}
	@Override
	public void handleFirstActivity(Activity act) {
		assert act != null;
		this.firstActivityEndTime = act.getEndTime();
		this.firstActivity = act;
		this.firstAct = false;

	}
	@Override
	public void handleLastActivity(Activity act) {
		this.currentActivityStartTime = act.getStartTime();
		this.handleOvernightActivity(act);
		this.firstActivity = null;
	}
	private void handleMorningActivity() {
		assert firstActivity != null;
		// score first activity
		this.score += calcActScore(0.0, this.firstActivityEndTime, firstActivity);
	}
	private void handleOvernightActivity(Activity lastActivity) {
		assert firstActivity != null;
		assert lastActivity != null;


		if (lastActivity.getType().equals(this.firstActivity.getType())) {
			// the first Act and the last Act have the same type
			if (firstLastActOpeningTimesWarning <= 10) {
				double[] openInterval = this.getOpeningInterval(lastActivity);
				if (openInterval[0] != Time.UNDEFINED_TIME || openInterval[1] != Time.UNDEFINED_TIME){
					log.warn("There are opening or closing times defined for the first and last activity. The correctness of the scoring function can thus not be guaranteed.");
					log.warn("first activity: " + firstActivity ) ;
					log.warn("last activity: " + lastActivity ) ;
					if (firstLastActOpeningTimesWarning == 10) {
						log.warn("Additional warnings of this type are suppressed.");
					}
					firstLastActOpeningTimesWarning++;
				}
			}
			
			double calcActScore = calcActScore(this.currentActivityStartTime, this.firstActivityEndTime + 7*24*3600, lastActivity);
			this.score += calcActScore; // SCENARIO_DURATION
		} else {
			if (this.params.scoreActs) {
				if (firstLastActWarning <= 10) {
					log.warn("The first and the last activity do not have the same type. The correctness of the scoring function can thus not be guaranteed.");
					log.warn("first activity: " + firstActivity ) ;
					log.warn("last activity: " + lastActivity ) ;
					log.warn("This may also happen when plans are not completed when the simulation ends.") ;
					if (firstLastActWarning == 10) {
						log.warn("Additional warnings of this type are suppressed.");
					}
					firstLastActWarning++;
				}

				// score first activity
				this.score += calcActScore(0.0, this.firstActivityEndTime, firstActivity);
				// score last activity
				this.score += calcActScore(this.currentActivityStartTime, 7*24*3600, lastActivity); // SCENARIO_DURATION
			}
		}
	}
	@Override
	public void handleActivity(Activity act) {
		this.score += calcActScore(act.getStartTime(), act.getEndTime(), act);
	}
	
}
