package org.matsim.core.scoring.functions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.util.Map;

public class ActivityScoringForCommercialActivities implements SumScoringFunction.ActivityScoring {
	private static final double INITIAL_SCORE = 0.0;

	private final Score score = new Score();

	private static int firstLastActWarning = 0;
	private static short firstLastActOpeningTimesWarning = 0;

	private final ScoringParameters params;
	private final OpeningIntervalCalculator openingIntervalCalculator;
	private Activity firstActivity;

	private static final Logger log = LogManager.getLogger(ActivityScoringForCommercialActivities.class);

	public ActivityScoringForCommercialActivities(final ScoringParameters params) {
		this(params, new ActivityTypeOpeningIntervalCalculator(params));
	}

	public ActivityScoringForCommercialActivities(final ScoringParameters params, final OpeningIntervalCalculator openingIntervalCalculator) {
		this.params = params;
		this.openingIntervalCalculator = openingIntervalCalculator;
	}

	@Override
	public void finish() {
		if (this.firstActivity != null) {
			handleMorningActivity();
		}
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
				tmpScore.actWaiting_util += this.params.marginalUtilityOfWaiting_s * waitTime;
			}

			OptionalTime latestStartTime = actParams.getLatestStartTime();
			if (latestStartTime.isDefined() && activityStart > latestStartTime.seconds()) {
				double lateTime = activityStart - latestStartTime.seconds();
				tmpScore.actLateArrival_s += lateTime;
				tmpScore.actLateArrival_util += this.params.marginalUtilityOfLateArrival_s * lateTime;
			}

			tmpScore.actPerforming_s += duration;
			tmpScore.actPerforming_util += this.params.marginalUtilityOfPerforming_s * duration;

			OptionalTime earliestEndTime = actParams.getEarliestEndTime();
			if (earliestEndTime.isDefined() && activityEnd < earliestEndTime.seconds()) {
				double earlyDeparture = earliestEndTime.seconds() - activityEnd;
				tmpScore.actEarlyDeparture_s += earlyDeparture;
				tmpScore.actEarlyDeparture_util += this.params.marginalUtilityOfEarlyDeparture_s * earlyDeparture;
			}

			if (activityEnd < departureTime) {
				double waiting = departureTime - activityEnd;
				tmpScore.actWaiting_s += waiting;
				tmpScore.actWaiting_util += this.params.marginalUtilityOfWaiting_s * waiting;
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

	private void handleOvernightActivity(Activity lastActivity) {
		assert firstActivity != null;
		assert lastActivity != null;

		if (lastActivity.getType().equals(this.firstActivity.getType()) || this.firstActivity.getType().equals("not specified")) {
			if (firstLastActOpeningTimesWarning <= 10) {
				OptionalTime[] openInterval = openingIntervalCalculator.getOpeningInterval(lastActivity);
				if (openInterval[0].isDefined() || openInterval[1].isDefined()) {
					log.warn("There are opening or closing times defined for the first and last activity. The correctness of the scoring function can thus not be guaranteed.");
					log.warn("first activity: {}", firstActivity);
					log.warn("last activity: {}", lastActivity);
					if (firstLastActOpeningTimesWarning == 10) {
						log.warn("Additional warnings of this type are suppressed.");
					}
					firstLastActOpeningTimesWarning++;
				}
			}

			Score calcActScore = calcActScore(lastActivity.getStartTime().seconds(),
					this.firstActivity.getEndTime().seconds() + 24 * 3600, lastActivity);
			this.score.add(calcActScore);
		} else {
			if (this.params.scoreActs) {
				int last = 0;
				if (firstLastActWarning <= last) {
					log.warn("The first and the last activity do not have the same type.");
					log.warn("Will score the first activity from midnight to its end, and the last activity from its start to midnight.");
					log.warn("Because of the nonlinear function, this is not the same as scoring from start to end.");
					log.warn("first activity: {}", firstActivity);
					log.warn("last activity: {}", lastActivity);
					log.warn("This may also happen when plans are not completed when the simulation ends.");
					if (firstLastActWarning == last) {
						log.warn("Additional warnings of this type are suppressed.");
					}
					firstLastActWarning++;
				}

				this.score.add(calcActScore(0.0, this.firstActivity.getEndTime().seconds(), firstActivity));
				this.score.add(calcActScore(lastActivity.getStartTime().seconds(),
						this.params.simulationPeriodInDays * 24 * 3600, lastActivity));
			}
		}
	}

	private void handleMorningActivity() {
		assert firstActivity != null;
		this.score.add(calcActScore(0.0, this.firstActivity.getEndTime().seconds(), firstActivity));
	}

	@Override
	public void handleFirstActivity(Activity act) {
		assert act != null;
		this.firstActivity = act;
	}

	@Override
	public void handleActivity(Activity act) {
		this.score.add(calcActScore(act.getStartTime().seconds(), act.getEndTime().seconds(), act));
	}

	@Override
	public void handleLastActivity(Activity act) {
		this.handleOvernightActivity(act);
		this.firstActivity = null;
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
