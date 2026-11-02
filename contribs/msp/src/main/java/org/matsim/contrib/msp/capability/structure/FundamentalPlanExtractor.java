package org.matsim.contrib.msp.capability.structure;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.msp.capability.CapabilityConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.DefaultAnalysisMainModeIdentifier;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.OptionalTime;

import java.util.ArrayList;
import java.util.List;

/**
 * Derives the <i>fundamental plan</i> of an agent, i.e. the plan reduced to those activities the
 * agent cannot choose not to perform. All discretionary activities are removed; the mandatory activities
 * that remain are connected by a single leg each, carrying the main mode of the trip chain that was
 * replaced.
 * <p>
 * The fundamental plan is the starting point of the capability analysis: the capability of an agent
 * is expressed by the discretionary activities that can be inserted back into this plan.
 * <p>
 * The times of the mandatory activities are the ones they had in the original plan: the start time
 * is when the agent arrived at the activity before the plan was shrunk, the end time is when it left
 * again. Removing the discretionary activities therefore opens up gaps between the mandatory activities,
 * and it is those gaps that the insertion step has to fit discretionary activities into. Start times are
 * usually not written to a plans file, so they are reconstructed from the departure and travel times
 * of the trips (see {@link #startTimes(Plan)}).
 * <p>
 * Note that the legs of the fundamental plan are not routed. Whoever needs routed legs has to run a
 * router on the resulting plan.
 */
public final class FundamentalPlanExtractor {

	private final CapabilityConfigGroup config;
	private final AnalysisMainModeIdentifier mainModeIdentifier;

	public FundamentalPlanExtractor(CapabilityConfigGroup config) {
		this(config, new DefaultAnalysisMainModeIdentifier());
	}

	public FundamentalPlanExtractor(CapabilityConfigGroup config, AnalysisMainModeIdentifier mainModeIdentifier) {
		this.config = config;
		this.mainModeIdentifier = mainModeIdentifier;
	}

	/**
	 * Reduces the given plan to its mandatory activities. The given plan is not modified.
	 *
	 * @return a new, unrouted plan containing only the mandatory activities of the given plan, with
	 * the times these activities had in the given plan. The plan is empty if the given plan does not
	 * contain any mandatory activity at all.
	 */
	public Plan extract(Plan plan) {

		List<Activity> activities = TripStructureUtils.getActivities(plan, StageActivityHandling.ExcludeStageActivities);
		List<Trip> trips = TripStructureUtils.getTrips(plan);
		List<OptionalTime> startTimes = startTimes(plan);

		Plan fundamentalPlan = PopulationUtils.createPlan();
		fundamentalPlan.setType(plan.getType());

		// index of the preceding mandatory activity, -1 as long as none has been found yet
		int previous = -1;

		for (int i = 0; i < activities.size(); i++) {
			Activity activity = activities.get(i);

			if (!this.config.isMandatory(activity.getType())) {
				continue;
			}

			if (previous >= 0) {
				// the trips between the two mandatory activities collapse into a single leg
				fundamentalPlan.addLeg(createLeg(trips.subList(previous, i)));
			}

			// the copy already carries end time and maximum duration, the start time has to be the
			// one reconstructed for the original plan rather than the one the plan happened to store
			Activity copy = PopulationUtils.createActivity(activity);
			startTimes.get(i).ifDefinedOrElse(copy::setStartTime, copy::setStartTimeUndefined);
			fundamentalPlan.addActivity(copy);

			previous = i;
		}

		return fundamentalPlan;
	}

	/**
	 * Determines when the agent starts each activity of the given plan. Plans files rarely store
	 * activity start times, so the time is reconstructed from the trip that leads to the activity:
	 * for a routed plan it is the departure time of the last leg of that trip plus its travel time,
	 * otherwise the departure from the preceding activity plus the travel times of all legs in
	 * between. The first activity of a plan starts at 00:00.
	 *
	 * @return one entry per non-stage activity of the plan, in the order of the plan. An entry is
	 * undefined if the plan does not carry enough times to reconstruct it.
	 */
	public static List<OptionalTime> startTimes(Plan plan) {

		List<Activity> activities = TripStructureUtils.getActivities(plan, StageActivityHandling.ExcludeStageActivities);
		List<Trip> trips = TripStructureUtils.getTrips(plan);

		List<OptionalTime> startTimes = new ArrayList<>(activities.size());

		for (int i = 0; i < activities.size(); i++) {
			Activity activity = activities.get(i);

			if (activity.getStartTime().isDefined()) {
				// an experienced plan knows it already
				startTimes.add(activity.getStartTime());
			} else if (i == 0) {
				// the agent is at its first activity from the beginning of the day
				startTimes.add(OptionalTime.zeroSeconds());
			} else {
				startTimes.add(arrivalTime(trips.get(i - 1), activities.get(i - 1), startTimes.get(i - 1)));
			}
		}

		return startTimes;
	}

	/**
	 * @return the number of discretionary, non-stage activities of the given plan, i.e. the activities
	 * that {@link #extract(Plan)} removes.
	 */
	public int countDiscretionaryActivities(Plan plan) {
		return (int) TripStructureUtils.getActivities(plan, StageActivityHandling.ExcludeStageActivities).stream()
			.filter(activity -> !this.config.isMandatory(activity.getType()))
			.count();
	}

	/**
	 * @return when the given trip ends, i.e. when the agent arrives at the activity following it
	 */
	private static OptionalTime arrivalTime(Trip trip, Activity origin, OptionalTime originStartTime) {

		List<Leg> legs = trip.getLegsOnly();
		if (legs.isEmpty()) {
			return OptionalTime.undefined();
		}

		// a plan that went through a router carries departure and travel time on every leg, so the
		// arrival time can simply be read off the last leg of the trip
		Leg lastLeg = legs.get(legs.size() - 1);
		if (lastLeg.getDepartureTime().isDefined() && lastLeg.getTravelTime().isDefined()) {
			return OptionalTime.defined(lastLeg.getDepartureTime().seconds() + lastLeg.getTravelTime().seconds());
		}

		// otherwise: when the agent left the preceding activity, plus how long it was under way.
		// Stage activities are not added up, they are of zero duration by construction.
		OptionalTime departure = departureTime(origin, originStartTime);
		if (departure.isUndefined()) {
			return lastDeparture(legs);
		}

		double travelTime = 0;
		boolean allKnown = true;
		for (Leg leg : legs) {
			if (leg.getTravelTime().isUndefined()) {
				allKnown = false;
				break;
			}
			travelTime += leg.getTravelTime().seconds();
		}

		if (allKnown) {
			return OptionalTime.defined(departure.seconds() + travelTime);
		}

		// Last resort: some plans files carry a departure time per leg but no travel time - output plans
		// that had their routes stripped, for instance. The agent arrives after the last leg of the trip
		// departs, so that departure time is a lower bound on the arrival, and a tight one whenever the
		// final access leg is short. Better than giving up, but it slightly overstates the time the agent
		// has on site.
		return lastDeparture(legs);
	}

	/**
	 * @return the departure time of the last leg that has one, undefined if no leg does
	 */
	private static OptionalTime lastDeparture(List<Leg> legs) {
		for (int i = legs.size() - 1; i >= 0; i--) {
			if (legs.get(i).getDepartureTime().isDefined()) {
				return legs.get(i).getDepartureTime();
			}
		}
		return OptionalTime.undefined();
	}

	/**
	 * When the agent leaves the given activity. Plans do not always carry an end time - a scenario may
	 * give an activity a maximum duration instead, as the {@code equil} example does - so both have to be
	 * taken into account.
	 *
	 * @return the end time if the activity has one, otherwise its start time plus its maximum duration,
	 * otherwise undefined
	 */
	public static OptionalTime departureTime(Activity activity) {
		return departureTime(activity, activity.getStartTime());
	}

	/**
	 * @param startTime when the agent arrives at the activity, as reconstructed by
	 * {@link #startTimes(Plan)}. Needed because an activity that carries a maximum duration rather than
	 * an end time cannot say when it is over without it, and plans files rarely store start times.
	 * @return when the agent leaves the given activity. An activity that has both an end time and a
	 * maximum duration is left at its end time, which is what the default activity duration
	 * interpretation of MATSim does as well.
	 */
	public static OptionalTime departureTime(Activity activity, OptionalTime startTime) {
		if (activity.getEndTime().isDefined()) {
			return activity.getEndTime();
		}
		if (activity.getMaximumDuration().isDefined() && startTime.isDefined()) {
			return OptionalTime.defined(startTime.seconds() + activity.getMaximumDuration().seconds());
		}
		return OptionalTime.undefined();
	}

	private Leg createLeg(List<Trip> replacedTrips) {
		List<Leg> legs = new ArrayList<>();
		for (Trip trip : replacedTrips) {
			legs.addAll(trip.getLegsOnly());
		}
		return PopulationUtils.createLeg(this.mainModeIdentifier.identifyMainMode(legs));
	}
}
