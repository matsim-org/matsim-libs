package org.matsim.contribs.discrete_mode_choice.replanning;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;

/**
 * Helper class for converting a MATSim plan into a list of
 * DiscreteModeChoiceTrip.
 * 
 * @author sebhoerl
 */
public final class TripListConverter {
	/**
	 * Convert a MATSim plan into a list of DiscreteModeChoiceTrip and extract the
	 * respective legs. It is expected that the plan is already flattened (i.e.
	 * there are no interaction activities).
	 */
	public List<DiscreteModeChoiceTrip> convert(Plan plan) {
		List<Trip> initialTrips = TripStructureUtils.getTrips(plan);
		List<DiscreteModeChoiceTrip> trips = new ArrayList<>(initialTrips.size());

		int index = 0;

		for (Trip initialTrip : initialTrips) {
			Activity originActivity = initialTrip.getOriginActivity();
			Activity destinationActivity = initialTrip.getDestinationActivity();

			Leg firstLeg = (Leg) initialTrip.getTripElements().get(0);
			String routingMode = TripStructureUtils.getRoutingMode(firstLeg);

			trips.add(new DiscreteModeChoiceTrip(originActivity, destinationActivity, routingMode,
					initialTrip.getTripElements(), plan.getPerson().hashCode(), index, index, initialTrip.getTripAttributes()));

			index++;
		}

		return trips;
	}
}
