package org.matsim.contrib.discrete_mode_choice.components.tour_finder;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contribs.discrete_mode_choice.components.tour_finder.ActivityTourFinder;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

public class ActivityTourFinderTest {
	private List<DiscreteModeChoiceTrip> createFixture(String... activityTypes) {
		Plan plan = PopulationUtils.createPlan();
		boolean isFirst = true;

		for (String activityType : activityTypes) {
			if (!isFirst) {
				PopulationUtils.createAndAddLeg(plan, "");
			}

			PopulationUtils.createAndAddActivity(plan, activityType);
			isFirst = false;
		}

		List<Trip> trips = TripStructureUtils.getTrips(plan);
		List<DiscreteModeChoiceTrip> modeChoiceTrips = new LinkedList<>();

		for (Trip trip : trips) {
			String initialMode = trip.getLegsOnly().get(0).getMode();
			modeChoiceTrips.add(new DiscreteModeChoiceTrip(trip.getOriginActivity(), trip.getDestinationActivity(),
					initialMode, trip.getTripElements(), 0, 0, 0, new AttributesImpl()));
		}

		return modeChoiceTrips;
	}

	@Test
	void testActivityTourFinder() {
		ActivityTourFinder finder = new ActivityTourFinder(Arrays.asList("home"));

		List<DiscreteModeChoiceTrip> trips;
		List<List<DiscreteModeChoiceTrip>> result;

		trips = createFixture("home", "work", "home");
		result = finder.findTours(trips);
		assertEquals(1, result.size());
		assertEquals(2, result.stream().mapToInt(List::size).sum());

		trips = createFixture("other", "home", "work", "home");
		result = finder.findTours(trips);
		assertEquals(2, result.size());
		assertEquals(3, result.stream().mapToInt(List::size).sum());
		assertEquals(1, result.get(0).size());
		assertEquals(2, result.get(1).size());

		trips = createFixture("home", "work", "home", "other");
		result = finder.findTours(trips);
		assertEquals(2, result.size());
		assertEquals(3, result.stream().mapToInt(List::size).sum());
		assertEquals(2, result.get(0).size());
		assertEquals(1, result.get(1).size());

		trips = createFixture("home", "work", "shop", "home", "other", "home");
		result = finder.findTours(trips);
		assertEquals(2, result.size());
		assertEquals(5, result.stream().mapToInt(List::size).sum());
		assertEquals(3, result.get(0).size());
		assertEquals(2, result.get(1).size());

		trips = createFixture("home", "work", "home", "home", "work", "home");
		result = finder.findTours(trips);
		assertEquals(3, result.size());
		assertEquals(5, result.stream().mapToInt(List::size).sum());
		assertEquals(2, result.get(0).size());
		assertEquals(1, result.get(1).size());
		assertEquals(2, result.get(2).size());
	}

	@Test
	void testActivityTourFinderMultiple() {
		ActivityTourFinder finder = new ActivityTourFinder(Arrays.asList("home1", "home2", "home3", "home4"));

		List<DiscreteModeChoiceTrip> trips;
		List<List<DiscreteModeChoiceTrip>> result;

		trips = createFixture("home1", "work", "home2");
		result = finder.findTours(trips);
		assertEquals(1, result.size());
		assertEquals(2, result.stream().mapToInt(List::size).sum());

		trips = createFixture("other", "home2", "work", "home1");
		result = finder.findTours(trips);
		assertEquals(2, result.size());
		assertEquals(3, result.stream().mapToInt(List::size).sum());
		assertEquals(1, result.get(0).size());
		assertEquals(2, result.get(1).size());

		trips = createFixture("home1", "work", "home2", "other");
		result = finder.findTours(trips);
		assertEquals(2, result.size());
		assertEquals(3, result.stream().mapToInt(List::size).sum());
		assertEquals(2, result.get(0).size());
		assertEquals(1, result.get(1).size());

		trips = createFixture("home1", "work", "shop", "home1", "other", "home3");
		result = finder.findTours(trips);
		assertEquals(2, result.size());
		assertEquals(5, result.stream().mapToInt(List::size).sum());
		assertEquals(3, result.get(0).size());
		assertEquals(2, result.get(1).size());

		trips = createFixture("home1", "work", "home2", "home3", "work", "home2");
		result = finder.findTours(trips);
		assertEquals(3, result.size());
		assertEquals(5, result.stream().mapToInt(List::size).sum());
		assertEquals(2, result.get(0).size());
		assertEquals(1, result.get(1).size());
		assertEquals(2, result.get(2).size());
	}
}
