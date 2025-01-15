package org.matsim.contrib.ev.withinday;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;

/**
 * This is a convenience class that helps identifying viable charging slots
 * throughout a day.
 * 
 * A viable charging slot for leg-based charging is a leg of the given transport
 * mode.
 * 
 * A viable charging slot for activity-based charging is a sequence of
 * activities with specific conditions. The first condtion is that the sequence
 * is preceded by a leg of the specified mode and that the sequence is left by a
 * leg of that mode. Furthermore, no leg of that mode should take place between
 * the first and the last activity in the sequence.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class ChargingSlotFinder {
	private final Scenario scenario;
	private final String chargingMode;

	public ChargingSlotFinder(Scenario scenario, String chargingMode) {
		this.scenario = scenario;
		this.chargingMode = chargingMode;
	}

	public record ActivityBasedCandidate(Activity startActivity, Activity endActivity) {
	}

	public record LegBasedCandidate(Leg leg, Activity followingActivity) {
	}

	/**
	 * Finds all candidates for leg-based charging along the agent plan.
	 */
	public List<LegBasedCandidate> findLegBased(Person person, Plan plan) {
		List<LegBasedCandidate> candidates = new LinkedList<>();
		for (Trip trip : TripStructureUtils.getTrips(plan)) {
			for (Leg leg : trip.getLegsOnly()) {
				if (leg.getRoutingMode().equals(chargingMode)) {
					if (leg.getMode().equals(chargingMode)) {
						candidates.add(new LegBasedCandidate(leg, trip.getDestinationActivity()));
					}
				}
			}
		}

		return candidates;
	}

	/**
	 * Finds all candidates for activity-based charging along an agent plan.
	 */
	public List<ActivityBasedCandidate> findActivityBased(Person person, Plan plan) {
		// find trips that change the location of the vehicle
		List<Trip> breakpoints = new LinkedList<>();
		for (Trip trip : TripStructureUtils.getTrips(plan)) {
			String routingMode = TripStructureUtils.getRoutingModeIdentifier().identifyMainMode(trip.getTripElements());

			if (routingMode.equals(chargingMode)) {
				Id<Link> originLinkId = PopulationUtils.decideOnLinkIdForActivity(trip.getOriginActivity(), scenario);
				Id<Link> destinationLinkId = PopulationUtils.decideOnLinkIdForActivity(trip.getDestinationActivity(),
						scenario);

				if (originLinkId != destinationLinkId) {
					breakpoints.add(trip);
				}
			}
		}

		// find candidates for charging slots
		List<ActivityBasedCandidate> candidates = new LinkedList<>();

		if (breakpoints.size() == 0) {
			Activity startActivity = (Activity) plan.getPlanElements().get(0);
			Activity endActivity = (Activity) plan.getPlanElements().get(plan.getPlanElements().size() - 1);

			candidates.add(new ActivityBasedCandidate(startActivity, endActivity));
		} else {
			{
				// First slot
				Activity startActivity = (Activity) plan.getPlanElements().get(0);
				Activity endActivity = breakpoints.getFirst().getOriginActivity();

				candidates.add(
						new ActivityBasedCandidate(startActivity, endActivity));
			}

			List<? extends PlanElement> elements = plan.getPlanElements();
			for (int i = 1; i < breakpoints.size(); i++) {
				// Intermediate slot

				Trip startTrip = breakpoints.get(i - 1);
				Trip endTrip = breakpoints.get(i);

				Activity startActivity = startTrip.getDestinationActivity();
				Activity endActivity = endTrip.getOriginActivity();

				// check if there are intermediate uses of the car
				boolean isValid = true;

				for (int k = elements.indexOf(startActivity); k < elements.indexOf(endActivity); k++) {
					if (elements.get(k) instanceof Leg leg) {
						if (leg.getMode().equals(chargingMode)) {
							// we found antoher car leg that has not generated a breakpoint, meaning that
							// the car is used along the way to go from a place to itself
							isValid = false;
							break;
						}
					}
				}

				if (isValid) {
					candidates.add(
							new ActivityBasedCandidate(startActivity, endActivity));
				}
			}

			{
				// Last slot
				Activity startActivity = breakpoints.getLast().getDestinationActivity();
				Activity endActivity = (Activity) plan.getPlanElements().get(plan.getPlanElements().size() - 1);

				candidates.add(
						new ActivityBasedCandidate(startActivity, endActivity));
			}
		}

		return candidates;
	}

	/**
	 * Some activity-based and leg-based slot candidates are incompatible. This
	 * method removes from a list of activity-based candiates those that are
	 * incompatible with the provided list of leg-based candidates.
	 */
	public void reduceActivityBased(List<ActivityBasedCandidate> activityBased, List<LegBasedCandidate> legBased,
			List<? extends PlanElement> elements) {
		Iterator<ActivityBasedCandidate> iterator = activityBased.iterator();

		while (iterator.hasNext()) {
			ActivityBasedCandidate activity = iterator.next();

			int startIndex = elements.indexOf(activity.startActivity);
			int endIndex = elements.indexOf(activity.endActivity);

			for (LegBasedCandidate leg : legBased) {
				int followingActivityIndex = elements.indexOf(leg.followingActivity);

				if (followingActivityIndex >= startIndex && followingActivityIndex <= endIndex) {
					iterator.remove();
				}
			}
		}
	}

	/**
	 * Some activity-based and leg-based slot candidates are incompatible. This
	 * method removes from a list of leg-based candiates those that are
	 * incompatible with the provided list of activity-based candidates.
	 */
	public void reduceLegBased(List<LegBasedCandidate> legBased, List<ActivityBasedCandidate> activityBased,
			List<? extends PlanElement> elements) {
		Iterator<LegBasedCandidate> iterator = legBased.iterator();

		while (iterator.hasNext()) {
			LegBasedCandidate leg = iterator.next();
			int followingActivityIndex = elements.indexOf(leg.followingActivity);

			for (ActivityBasedCandidate activity : activityBased) {
				int startIndex = elements.indexOf(activity.startActivity);
				int endIndex = elements.indexOf(activity.endActivity);

				if (followingActivityIndex >= startIndex && followingActivityIndex <= endIndex) {
					iterator.remove();
				}
			}
		}
	}
}
