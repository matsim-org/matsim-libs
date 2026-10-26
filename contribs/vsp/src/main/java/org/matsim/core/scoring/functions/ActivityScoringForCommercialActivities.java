package org.matsim.core.scoring.functions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * Scores commercial activity time linearly using a marginal utility of time.
 * Boundary activities contribute only when their start and end can be determined.
 */
public class ActivityScoringForCommercialActivities implements SumScoringFunction.ActivityScoring {
	private static final double INITIAL_SCORE = 0.0;

	private final Score score = new Score();

	private final ScoringParameters params;
	private final double personSpecificMarginalUtilityOfTime;
	private final OpeningIntervalCalculator openingIntervalCalculator;
	private static final Logger log = LogManager.getLogger(ActivityScoringForCommercialActivities.class);

	public ActivityScoringForCommercialActivities(final ScoringParameters params) {
		this(params, new ActivityTypeOpeningIntervalCalculator(params));
	}

	/**
	 * Uses a time utility derived from the person's vehicle for performing and waiting.
	 *
	 * @param params activity scoring parameters
	 * @param adjustedMarginalUtilityOfPerforming_s utility per second derived from vehicle time costs
	 */
	public ActivityScoringForCommercialActivities(final ScoringParameters params, double adjustedMarginalUtilityOfPerforming_s) {
		this.params = params;
		this.openingIntervalCalculator = new ActivityTypeOpeningIntervalCalculator(params);
		this.personSpecificMarginalUtilityOfTime = adjustedMarginalUtilityOfPerforming_s;
	}

	public ActivityScoringForCommercialActivities(final ScoringParameters params, final OpeningIntervalCalculator openingIntervalCalculator) {
		this.params = params;
		this.openingIntervalCalculator = openingIntervalCalculator;
		this.personSpecificMarginalUtilityOfTime = params.marginalUtilityOfPerforming_s;
	}

	@Override
	public void finish() {
		// Boundary activities are handled when they arrive; no overnight score is deferred.
	}

	@Override
	public double getScore() {
		return this.score.actPerforming_util + this.score.actWaiting_util + this.score.actLateArrival_util + this.score.actEarlyDeparture_util;
	}

	@Override
	public void explainScore(StringBuilder out) {
		out.append("actPerforming_util=").append(this.score.actPerforming_util).append(ScoringFunction.SCORE_DELIMITER);
		out.append("actPerforming_s=").append(this.score.actPerforming_s).append(ScoringFunction.SCORE_DELIMITER);
		out.append("actWaiting_util=").append(this.score.actWaiting_util).append(ScoringFunction.SCORE_DELIMITER);
		out.append("actWaiting_s=").append(this.score.actWaiting_s).append(ScoringFunction.SCORE_DELIMITER);
		out.append("actLateArrival_util=").append(this.score.actLateArrival_util).append(ScoringFunction.SCORE_DELIMITER);
		out.append("actLateArrival_s=").append(this.score.actLateArrival_s).append(ScoringFunction.SCORE_DELIMITER);
		out.append("actEarlyDeparture_util=").append(this.score.actEarlyDeparture_util).append(ScoringFunction.SCORE_DELIMITER);
		out.append("actEarlyDeparture_s=").append(this.score.actEarlyDeparture_s);
	}

	/**
	 * Scores a boundary activity only when its time interval is known. A maximum duration
	 * supplies one missing boundary; without a complete interval, the activity is skipped.
	 * This can ber assumed as a finish of the work time, so that the rest
	 */
	private void scoreIfFullyBounded(Activity act) {
		OptionalTime startTime = act.getStartTime();
		OptionalTime endTime = act.getEndTime();
		OptionalTime maximumDuration = act.getMaximumDuration();
		if (startTime.isUndefined() && endTime.isDefined() && maximumDuration.isDefined()) {
			startTime = OptionalTime.defined(endTime.seconds() - maximumDuration.seconds());
		}
		if (endTime.isUndefined() && startTime.isDefined() && maximumDuration.isDefined()) {
			endTime = OptionalTime.defined(startTime.seconds() + maximumDuration.seconds());
		}
		if (startTime.isDefined() && endTime.isDefined()) {
			this.score.add(calcActScore(startTime.seconds(), endTime.seconds(), act));
		}
	}

	/**
	 * Separates performing time from waiting outside opening hours, then applies the
	 * configured late-arrival and early-departure penalties.
	 */
	private Score calcActScore(final double arrivalTime, final double departureTime, final Activity act) {
		ActivityUtilityParameters actParams = this.params.actParams.get(act.getType());
		if (actParams == null) {
			throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in utility parameters "
				+ "(module name=\"scoring\" in the config file).");
		}

		Score tmpScore = new Score();
		if (actParams.isScoreAtAll()) {
			OptionalTime[] openingInterval = openingIntervalCalculator.getOpeningInterval(act);
			OptionalTime openingTime = openingInterval[0];
			OptionalTime closingTime = openingInterval[1];

			double activityStart = arrivalTime;
			double activityEnd = departureTime;

			// Time outside the opening interval is waiting rather than performing.
			if (openingTime.isDefined() && arrivalTime < openingTime.seconds()) {
				activityStart = openingTime.seconds();
			}
			if (closingTime.isDefined() && closingTime.seconds() < departureTime) {
				activityEnd = closingTime.seconds();
			}
			if (openingTime.isDefined() && closingTime.isDefined()
				&& (openingTime.seconds() > departureTime || closingTime.seconds() < arrivalTime)) {
				activityStart = departureTime;
				activityEnd = departureTime;
			}
			double duration = activityEnd - activityStart;

			if (arrivalTime < activityStart) {
				double waitTime = activityStart - arrivalTime;
				tmpScore.actWaiting_s += waitTime;
				tmpScore.actWaiting_util += this.personSpecificMarginalUtilityOfTime * waitTime;
			}

			OptionalTime latestStartTime = actParams.getLatestStartTime();
			if (latestStartTime.isDefined() && activityStart > latestStartTime.seconds()) {
				double lateTime = activityStart - latestStartTime.seconds();
				tmpScore.actLateArrival_s += lateTime;
				tmpScore.actLateArrival_util += this.params.marginalUtilityOfLateArrival_s * lateTime;
			}

			tmpScore.actPerforming_s += duration;
			tmpScore.actPerforming_util += this.personSpecificMarginalUtilityOfTime * duration;

			OptionalTime earliestEndTime = actParams.getEarliestEndTime();
			if (earliestEndTime.isDefined() && activityEnd < earliestEndTime.seconds()) {
				double earlyDeparture = earliestEndTime.seconds() - activityEnd;
				tmpScore.actEarlyDeparture_s += earlyDeparture;
				tmpScore.actEarlyDeparture_util += this.params.marginalUtilityOfEarlyDeparture_s * earlyDeparture;
			}

			if (activityEnd < departureTime) {
				double waiting = departureTime - activityEnd;
				tmpScore.actWaiting_s += waiting;
				tmpScore.actWaiting_util += this.personSpecificMarginalUtilityOfTime * waiting;
			}

			OptionalTime minimalDuration = actParams.getMinimalDuration();
			if (minimalDuration.isDefined() && duration < minimalDuration.seconds()) {
				double earlyDeparture = minimalDuration.seconds() - duration;
				tmpScore.actEarlyDeparture_s += earlyDeparture;
				tmpScore.actEarlyDeparture_util += this.params.marginalUtilityOfEarlyDeparture_s * earlyDeparture;
			}
		}
		return tmpScore;
	}

	@Override
	public void handleFirstActivity(Activity act) {
		assert act != null;
		scoreIfFullyBounded(act);
	}

	@Override
	public void handleActivity(Activity act) {
		this.score.add(calcActScore(act.getStartTime().seconds(), act.getEndTime().seconds(), act));
	}

	@Override
	public void handleLastActivity(Activity act) {
		scoreIfFullyBounded(act);
	}

	private static final class Score {

		private double actPerforming_util = INITIAL_SCORE;
		private double actPerforming_s = INITIAL_SCORE;
		private double actWaiting_util = INITIAL_SCORE;
		private double actWaiting_s = INITIAL_SCORE;
		private double actLateArrival_util = INITIAL_SCORE;
		private double actLateArrival_s = INITIAL_SCORE;
		private double actEarlyDeparture_util = INITIAL_SCORE;
		private double actEarlyDeparture_s = INITIAL_SCORE;

		private void add(Score s) {
			actPerforming_util += s.actPerforming_util;
			actPerforming_s += s.actPerforming_s;
			actWaiting_util += s.actWaiting_util;
			actWaiting_s += s.actWaiting_s;
			actLateArrival_util += s.actLateArrival_util;
			actLateArrival_s += s.actLateArrival_s;
			actEarlyDeparture_util += s.actEarlyDeparture_util;
			actEarlyDeparture_s += s.actEarlyDeparture_s;
		}
	}
}
