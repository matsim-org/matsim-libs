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
	
	static public TimeInterpretation create(Config config) {
		return new TimeInterpretation(config.plans().getActivityDurationInterpretation());
	}
	
	static public TimeInterpretation create(ActivityDurationInterpretation interpretation) {
		return new TimeInterpretation(interpretation);
	}
	
	private TimeInterpretation(ActivityDurationInterpretation activityDurationInterpretation) {
		this.activityDurationInterpretation = activityDurationInterpretation;
	}
	
	
	public double calcEndOfActivity( // from PlanRouter
			final Activity activity,
			final Plan plan) {
		// yyyy similar method in PopulationUtils.  TripRouter.calcEndOfPlanElement in fact uses it.  However, this seems doubly inefficient; calling the
		// method in PopulationUtils directly would probably be faster.  kai, jul'19

		if (activity.getEndTime().isDefined())
			return activity.getEndTime().seconds();

		// no sufficient information in the activity...
		// do it the long way.
		// XXX This is inefficient! Using a cache for each plan may be an option
		// (knowing that plan elements are iterated in proper sequence,
		// no need to re-examine the parts of the plan already known)
		double now = 0;

		for (PlanElement pe : plan.getPlanElements()) {
			now = calcEndOfPlanElement(now, pe);
			if (pe == activity) return now;
		}

		throw new RuntimeException( "activity "+activity+" not found in "+plan.getPlanElements() );
	}
	
	
	/**
	 * @deprecated Use {@link #decideOnActivityEndTime(Activity, double, Config)}
	 */
	@Deprecated // was renamed
	public double getActivityEndTime( Activity act, double now ) { // from PopulationUtils
		return decideOnActivityEndTime( act, now ).seconds() ;
	}

	public OptionalTime decideOnActivityEndTime( Activity act, double now) { // from PopulationUtils

		switch (activityDurationInterpretation) {
			case endTimeOnly:
				return act.getEndTime();

			case tryEndTimeThenDuration:
				if (act.getEndTime().isDefined()) {
					return act.getEndTime();
				} else if (act.getMaximumDuration().isDefined()) {
					return OptionalTime.defined(now + act.getMaximumDuration().seconds());
				} else {
					return OptionalTime.undefined();
				}

			case minOfDurationAndEndTime:
				if (act.getEndTime().isUndefined() && act.getMaximumDuration().isUndefined()) {
					return OptionalTime.undefined();
				} else if (act.getMaximumDuration().isUndefined()) {
					return act.getEndTime();
				} else if (act.getEndTime().isUndefined()) {
					double durationBasedEndTime = now + act.getMaximumDuration().seconds();
					return OptionalTime.defined(durationBasedEndTime);
				} else {
					double durationBasedEndTime = now + act.getMaximumDuration().seconds();
					return act.getEndTime().seconds() <= durationBasedEndTime ?
							act.getEndTime() :
							OptionalTime.defined(durationBasedEndTime);
				}

			default:
				throw new IllegalArgumentException(
						"Unsupported 'activityDurationInterpretation' enum type: " + activityDurationInterpretation);
		}
	}
	
	public OptionalTime decideOnTravelTimeForLeg( Leg leg ) { // from PopulationUtils
		return leg.getRoute() != null ? leg.getRoute().getTravelTime().or(leg::getTravelTime) : leg.getTravelTime();
	}
	
	/**
	 * 
	 * Contains rules about when to leave an Activity, considering the current time and the properties of the Activity.
	 * Specifically, determines how maximum duration and specific end time are played against each other.
	 * 
	 * @param act The Activity
	 * @param now The current simulation time
	 * @param activityDurationInterpretation The name of one of several rules of how to interpret Activity fields.
	 * @return The departure time
	 */
	// From ActivityDurationsUtils
	public double calculateDepartureTime(Activity act, double now) {
		OptionalTime endTime = decideOnActivityEndTime(act, now);
		if (endTime.isUndefined()) {
			return Double.POSITIVE_INFINITY;
		} else {
			// we cannot depart before we arrived, thus change the time so the time stamp in events will be right
			//			[[how can events not use the simulation time?  kai, aug'10]]
			// actually, we will depart in (now+1) because we already missed the departing in this time step
			return Math.max(endTime.seconds(), now);
		}
	}
	
	
	/**
	 * Helper method, that can be used to compute start time of legs.
	 * (it is also used internally).
	 * It is provided here, because such an operation is mainly useful for routing,
	 * but it may be externalized in a "util" class...
	 * @param config TODO
	 */
	public double calcEndOfPlanElement( // From TripRouter
			final double now,
			final PlanElement pe) {
		Preconditions.checkArgument(Double.isFinite(now));//probably unnecessary after switching to OptionalTime

		if (pe instanceof Activity) {
			Activity act = (Activity) pe;
			return decideOnActivityEndTime(act, now ).seconds() ;
		}
		else {
			// take travel time from route if possible
			// TODO throw exception if undefined? (currently 0 is returned)
			double ttime = decideOnTravelTimeForLeg( (Leg) pe ).orElse(0);
			return now + ttime;
		}
	}
}
