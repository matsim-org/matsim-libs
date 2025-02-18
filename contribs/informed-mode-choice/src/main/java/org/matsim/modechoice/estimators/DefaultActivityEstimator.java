package org.matsim.modechoice.estimators;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.modechoice.EstimatorContext;

/**
 * Activity estimator based on MATSim default scoring function.
 *
 * @see org.matsim.core.scoring.functions.CharyparNagelActivityScoring
 */
public class DefaultActivityEstimator implements ActivityEstimator {

	private final TimeInterpretation timeInterpretation;

	@Inject
	public DefaultActivityEstimator(TimeInterpretation timeInterpretation) {
		this.timeInterpretation = timeInterpretation;
	}

	@Override
	public double estimate(EstimatorContext context, double arrivalTime, Activity act) {

		double departureTime = Math.max(arrivalTime, timeInterpretation.decideOnActivityEndTime(act, arrivalTime).orElse(context.scoring.simulationPeriodInDays * 24 * 3600) );
		return estScore(context, arrivalTime, departureTime, act);
	}

	@Override
	public double estimateLastAndFirstOfDay(EstimatorContext context, double arrivalTime, Activity last, Activity first) {

		double firstEnd = timeInterpretation.decideOnActivityEndTime(first, 0).orElse(0);

		if (last.getType().equals(first.getType())) {
			// Shift end time to the next day
			double endTime = firstEnd + 24 * 3600;
			return estScore(context, arrivalTime, endTime, last);
		}

		// Score first and last separately
		return estScore(context, 0, firstEnd, first) +
			estScore(context, arrivalTime, context.scoring.simulationPeriodInDays * 24 * 3600, last);
	}

	private double estScore(EstimatorContext context, double arrivalTime, double departureTime, Activity act) {

		ActivityUtilityParameters actParams = context.scoring.utilParams.get(act.getType());

		if (!actParams.isScoreAtAll())
			return 0;

		OptionalTime openingTime = actParams.getOpeningTime();
		OptionalTime closingTime = actParams.getClosingTime();

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
			// agent could not perform action
			activityStart = departureTime;
			activityEnd = departureTime;
		}

		double duration = activityEnd - activityStart;

		assert duration >= 0: "Duration must be positive";

		double score = 0;

		// disutility if too early
		if (arrivalTime < activityStart) {
			// agent arrives too early, has to wait
			score += context.scoring.marginalUtilityOfWaiting_s * (activityStart - arrivalTime);
		}

		// disutility if too late
		OptionalTime latestStartTime = actParams.getLatestStartTime();
		if (latestStartTime.isDefined() && (activityStart > latestStartTime.seconds())) {
			score += context.scoring.marginalUtilityOfLateArrival_s * (activityStart - latestStartTime.seconds());
		}

		// utility of performing an action, duration is >= 1, thus log is no problem
		double typicalDuration = actParams.getTypicalDuration();


		if ( duration >= 3600.*actParams.getZeroUtilityDuration_h() ) {
			double utilPerf = context.scoring.marginalUtilityOfPerforming_s * typicalDuration
				* Math.log((duration / 3600.0) / actParams.getZeroUtilityDuration_h());
			// also removing the "wait" alternative scoring.
			score += utilPerf;
		} else {
			// See original implementation for further assertions
			double slopeAtZeroUtility = context.scoring.marginalUtilityOfPerforming_s * typicalDuration / ( 3600.*actParams.getZeroUtilityDuration_h() ) ;
			double durationUnderrun = actParams.getZeroUtilityDuration_h()*3600. - duration ;
			score -= slopeAtZeroUtility * durationUnderrun ;
		}

		// disutility if stopping too early
		OptionalTime earliestEndTime = actParams.getEarliestEndTime();
		if ((earliestEndTime.isDefined()) && (activityEnd < earliestEndTime.seconds())) {
			score += context.scoring.marginalUtilityOfEarlyDeparture_s * (earliestEndTime.seconds() - activityEnd);
		}

		// disutility if going to away to late
		if (activityEnd < departureTime) {
			score += context.scoring.marginalUtilityOfWaiting_s * (departureTime - activityEnd);
		}

		// disutility if duration was too short
		OptionalTime minimalDuration = actParams.getMinimalDuration();
		if ((minimalDuration.isDefined()) && (duration < minimalDuration.seconds())) {
			score += context.scoring.marginalUtilityOfEarlyDeparture_s * (minimalDuration.seconds() - duration);
		}

		return score;

	}
}
