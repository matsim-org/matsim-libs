package org.matsim.core.utils.timing;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.utils.misc.OptionalTime;

import com.google.common.base.Preconditions;

public class TimeInterpretation {
	private final ActivityDurationInterpretation activityDurationInterpretation;
	private final double simulationStartTime;

	static public TimeInterpretation create(Config config) {
		return new TimeInterpretation( //
				config.plans().getActivityDurationInterpretation(), //
				config.qsim().getStartTime().orElse(0.0) // Corresponds to QSim::initSimTimer
		);
	}

	static public TimeInterpretation create(ActivityDurationInterpretation interpretation, double simulationStartTime) {
		return new TimeInterpretation(interpretation, simulationStartTime);
	}

	static public TimeInterpretation create(ActivityDurationInterpretation interpretation) {
		return new TimeInterpretation(interpretation, 0.0); // Corresponds to QSim::initSimTimer
	}

	private TimeInterpretation(ActivityDurationInterpretation activityDurationInterpretation,
			double simulationStartTime) {
		this.activityDurationInterpretation = activityDurationInterpretation;
		this.simulationStartTime = simulationStartTime;
	}

	/**
	 * Returns the end time of an activity, given the whole plan. Note that there is
	 * a shortcut: If activity end time is given, it will always be returned. This
	 * is NOT always in line with activityDurationInterpretation! In general, this
	 * function is here to preserve backwards compatibility. However, we should:
	 * 
	 * <ul>
	 * <li>Remove this function as it provides an illegal shortcut</li>
	 * <li>Provide a class (similar as in DMC package) that allows to track times
	 * along a plan, making use of TimeInterpretation</li>
	 * <li>Use this tracking functionality (potentially also much more efficient)
	 * wherever this function was used before</li>
	 * </ul>
	 */
	public double calcEndOfActivity(Activity activity, Plan plan) {
		if (activity.getEndTime().isDefined()) {
			return activity.getEndTime().seconds();
		}

		// Need to start counting from simulation start
		double now = simulationStartTime;

		for (PlanElement element : plan.getPlanElements()) {
			now = decideOnElementEndTime(element, now);

			if (element == activity) {
				return now;
			}
		}

		throw new RuntimeException("Activity " + activity + " not found in plan of agent " + plan.getPerson().getId());
	}

	/**
	 * Given the start time of an activity, the function returns the end time. The
	 * calculation depends on whether an activity is defined by an end time or a
	 * duration and, if both are given, in which order they are evaluated. This is
	 * controlled by the activityDurationInterpretation in the plans config group.
	 */
	public OptionalTime decideOnActivityEndTime(Activity activity, double startTime) {
		Preconditions.checkArgument(Double.isFinite(startTime));

		switch (activityDurationInterpretation) {
		case endTimeOnly:
			return activity.getEndTime();

		case tryEndTimeThenDuration:
			if (activity.getEndTime().isDefined()) {
				return activity.getEndTime();
			} else if (activity.getMaximumDuration().isDefined()) {
				return OptionalTime.defined(startTime + activity.getMaximumDuration().seconds());
			} else {
				return OptionalTime.undefined();
			}

		case minOfDurationAndEndTime:
			if (activity.getEndTime().isUndefined() && activity.getMaximumDuration().isUndefined()) {
				return OptionalTime.undefined();
			} else if (activity.getMaximumDuration().isUndefined()) {
				return activity.getEndTime();
			} else if (activity.getEndTime().isUndefined()) {
				double durationBasedEndTime = startTime + activity.getMaximumDuration().seconds();
				return OptionalTime.defined(durationBasedEndTime);
			} else {
				double durationBasedEndTime = startTime + activity.getMaximumDuration().seconds();
				return activity.getEndTime().seconds() <= durationBasedEndTime ? activity.getEndTime()
						: OptionalTime.defined(durationBasedEndTime);
			}

		default:
			throw new IllegalArgumentException(
					"Unsupported 'activityDurationInterpretation' enum type: " + activityDurationInterpretation);
		}
	}

	/**
	 * Returns the travel time for a leg. If travel time is available in the leg's
	 * route, the route travel time is used, otherwise the leg travel time.
	 */
	public OptionalTime decideOnLegTravelTime(Leg leg) {
		if (leg.getRoute() == null) {
			return leg.getTravelTime();
		} else {
			return leg.getRoute().getTravelTime().or(leg.getTravelTime());
		}
	}

	/**
	 * TODO: Transform this to OptionalTime ? TODO: Leg returns 0.0 as special case.
	 * Should we return OptionalTime? Or simulationStartTime? How is it used?
	 */
	public double decideOnElementEndTime(PlanElement element, double startTime) {
		Preconditions.checkArgument(Double.isFinite(startTime));

		if (element instanceof Activity) {
			return decideOnActivityEndTime((Activity) element, startTime).seconds();
		} else {
			double travelTime = decideOnLegTravelTime((Leg) element).orElse(0);
			return travelTime + startTime;
		}
	}
}
