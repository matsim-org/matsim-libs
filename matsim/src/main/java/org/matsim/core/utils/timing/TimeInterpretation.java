package org.matsim.core.utils.timing;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.PlansConfigGroup.TripDurationHandling;
import org.matsim.core.utils.misc.OptionalTime;

import com.google.common.base.Preconditions;

public class TimeInterpretation {
	private final ActivityDurationInterpretation activityDurationInterpretation;

	private final double simulationStartTime;
	private final boolean onlyAdvance;

	static public TimeInterpretation create(Config config) {
		return new TimeInterpretation( //
				config.plans().getActivityDurationInterpretation(), //
				config.plans().getTripDurationHandling(), //
				config.qsim().getStartTime().orElse(0.0));
		// Value of 0.0 corresponds to QSim::initSimTimer if not set from configuration
	}

	static public TimeInterpretation create(ActivityDurationInterpretation interpretation,
			TripDurationHandling tripDurationHandling, double simulationStartTime) {
		return new TimeInterpretation(interpretation, tripDurationHandling, simulationStartTime);
	}

	static public TimeInterpretation create(ActivityDurationInterpretation interpretation,
			TripDurationHandling tripDurationHandling) {
		return new TimeInterpretation(interpretation, tripDurationHandling, 0.0);
		// Value of 0.0 corresponds to QSim::initSimTimer if not set from configuration
	}

	private TimeInterpretation(ActivityDurationInterpretation activityDurationInterpretation,
			TripDurationHandling tripDurationHandling, double simulationStartTime) {
		this.activityDurationInterpretation = activityDurationInterpretation;
		this.simulationStartTime = simulationStartTime;

		boolean onlyAdvance = false;

		switch (tripDurationHandling) {
		case ignoreDelays:
			onlyAdvance = false;
			break;
		case shiftActivityEndTimes:
			onlyAdvance = true;
			break;
		default:
			throw new IllegalStateException();
		}

		this.onlyAdvance = onlyAdvance;
	}

	private OptionalTime checkAdvance(OptionalTime time, double startTime) {
		if (time.isDefined() && onlyAdvance) {
			return OptionalTime.defined(Math.max(startTime, time.seconds()));
		}

		return time;
	}

	/**
	 * This function takes a plan and finds the end time of the activity along this
	 * plan. Note that this requires tracking end times and travel times along all
	 * plan elements. This is especially inefficient when already looping over an
	 * agent plan. Whenever possible, make use of TimeTracker which provides a
	 * simple step-by-step interface to track time along a plan.
	 * 
	 * TODO: Documentation TODO: Phase out shortcut.
	 **/
	public OptionalTime decideOnActivityEndTimeAlongPlan(Activity activity, Plan plan) {
		int activityIndex = plan.getPlanElements().indexOf(activity);

		if (activityIndex == -1) {
			throw new IllegalStateException(
					"Activity " + activity + " not found in plan of agent " + plan.getPerson().getId());
		}

		return decideOnElementsEndTime(plan.getPlanElements().subList(0, activityIndex + 1), simulationStartTime);
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
				return checkAdvance(activity.getEndTime(), startTime);
			} else if (activity.getMaximumDuration().isDefined()) {
				return OptionalTime.defined(startTime + activity.getMaximumDuration().seconds());
			} else {
				return OptionalTime.undefined();
			}

		case minOfDurationAndEndTime:
			if (activity.getEndTime().isUndefined() && activity.getMaximumDuration().isUndefined()) {
				return OptionalTime.undefined();
			} else if (activity.getMaximumDuration().isUndefined()) {
				return checkAdvance(activity.getEndTime(), startTime);
			} else if (activity.getEndTime().isUndefined()) {
				double durationBasedEndTime = startTime + activity.getMaximumDuration().seconds();
				return OptionalTime.defined(durationBasedEndTime);
			} else {
				double durationBasedEndTime = startTime + activity.getMaximumDuration().seconds();
				return checkAdvance(activity.getEndTime().seconds() <= durationBasedEndTime ? activity.getEndTime()
						: OptionalTime.defined(durationBasedEndTime), startTime);
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
	 * Obtains the end time of an element which is either and activity or a leg,
	 * according to the logic of decideOnActivityEndTime and decideOnLegtravelTime.
	 */
	public OptionalTime decideOnElementEndTime(PlanElement element, double startTime) {
		if (element instanceof Activity) {
			return decideOnActivityEndTime((Activity) element, startTime);
		} else {
			OptionalTime travelTime = decideOnLegTravelTime((Leg) element);

			if (travelTime.isDefined()) {
				return OptionalTime.defined(startTime + travelTime.seconds());
			} else {
				return OptionalTime.undefined();
			}
		}
	}

	/**
	 * Traverses through a list of elements (activities and legs) and finds the end
	 * time of the chain according to the logic of decideOnActivityEndTime and
	 * decideOnLegtravelTime.
	 */
	public OptionalTime decideOnElementsEndTime(List<? extends PlanElement> elements, final double startTime) {
		double now = startTime;

		for (PlanElement element : elements) {
			OptionalTime endTime = decideOnElementEndTime(element, now);

			if (endTime.isDefined()) {
				now = endTime.seconds();
			} else {
				return OptionalTime.undefined();
			}
		}

		return OptionalTime.defined(now);
	}

	/**
	 * Returns the simulation start time that is used for calculations.
	 */
	public double getSimulationStartTime() {
		return simulationStartTime;
	}
}
