package org.matsim.contrib.discrete_mode_choice.components.constraints;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.discrete_mode_choice.test_utils.PlanBuilder;
import org.matsim.contribs.discrete_mode_choice.components.constraints.VehicleTourConstraint;
import org.matsim.contribs.discrete_mode_choice.components.utils.home_finder.HomeFinder;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraint;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraintFactory;
import org.matsim.facilities.ActivityFacility;

public class VehicleTourConstraintTest {
	@Test
	void testWithHome() {
		// PREPARATION
		HomeFinder homeFinder = (List<DiscreteModeChoiceTrip> trips) -> Id.create("A", ActivityFacility.class);
		Collection<String> availableModes = Arrays.asList("car", "walk");
		Collection<String> restrictedModes = Arrays.asList("car");

		TourConstraintFactory constraintFactory = new VehicleTourConstraint.Factory(restrictedModes, homeFinder);

		PlanBuilder planBuilder;

		planBuilder = new PlanBuilder() //
				.addActivityWithFacilityId("home", "A") //
				.addLeg() //
				.addActivityWithFacilityId("other", "B") //
				.addLeg() //
				.addActivityWithFacilityId("other", "C") //
				.addLeg() //
				.addActivityWithFacilityId("other", "A") //
				.addLeg() //
				.addActivityWithFacilityId("other", "C") //
				.addLeg() //
				.addActivityWithFacilityId("home", "A");

		List<DiscreteModeChoiceTrip> trips = planBuilder.buildDiscreteModeChoiceTrips();
		Person person = planBuilder.buildPlan().getPerson();

		TourConstraint constraint = constraintFactory.createConstraint(person, trips, availableModes);
		List<String> modes;

		// Test continuity
		modes = Arrays.asList("car", "car", "car", "car", "car");
		assertTrue(constraint.validateBeforeEstimation(trips, modes, Arrays.asList()));

		modes = Arrays.asList("car", "walk", "car", "walk", "walk");
		assertFalse(constraint.validateBeforeEstimation(trips, modes, Arrays.asList()));

		// Test start at home
		modes = Arrays.asList("walk", "walk", "walk", "car", "car");
		assertTrue(constraint.validateBeforeEstimation(trips, modes, Arrays.asList()));

		modes = Arrays.asList("walk", "walk", "car", "car", "walk");
		assertFalse(constraint.validateBeforeEstimation(trips, modes, Arrays.asList()));

		// Test end at home
		modes = Arrays.asList("walk", "walk", "walk", "car", "walk");
		assertFalse(constraint.validateBeforeEstimation(trips, modes, Arrays.asList()));
	}

	@Test
	void testWithoutHome() {
		// PREPARATION
		HomeFinder homeFinder = (List<DiscreteModeChoiceTrip> trips) -> null;
		Collection<String> availableModes = Arrays.asList("car", "walk");
		Collection<String> restrictedModes = Arrays.asList("car");

		TourConstraintFactory constraintFactory = new VehicleTourConstraint.Factory(restrictedModes, homeFinder);

		PlanBuilder planBuilder;

		planBuilder = new PlanBuilder() //
				.addActivityWithFacilityId("home", "A") //
				.addLeg() //
				.addActivityWithFacilityId("other", "B") //
				.addLeg() //
				.addActivityWithFacilityId("other", "C") //
				.addLeg() //
				.addActivityWithFacilityId("other", "A") //
				.addLeg() //
				.addActivityWithFacilityId("other", "C") //
				.addLeg() //
				.addActivityWithFacilityId("home", "A");

		List<DiscreteModeChoiceTrip> trips = planBuilder.buildDiscreteModeChoiceTrips();
		Person person = planBuilder.buildPlan().getPerson();

		TourConstraint constraint = constraintFactory.createConstraint(person, trips, availableModes);
		List<String> modes;

		// Test unknown home
		modes = Arrays.asList("car", "car", "car", "car", "car");
		assertTrue(constraint.validateBeforeEstimation(trips, modes, Arrays.asList()));

		modes = Arrays.asList("walk", "walk", "walk", "car", "car");
		assertFalse(constraint.validateBeforeEstimation(trips, modes, Arrays.asList()));
	}

	@Test
	void testTour() {
		// PREPARATION
		HomeFinder homeFinder = (List<DiscreteModeChoiceTrip> trips) -> Id.create("A", ActivityFacility.class);
		Collection<String> availableModes = Arrays.asList("car", "walk");
		Collection<String> restrictedModes = Arrays.asList("car");

		TourConstraintFactory constraintFactory = new VehicleTourConstraint.Factory(restrictedModes, homeFinder);

		PlanBuilder planBuilder;

		planBuilder = new PlanBuilder() //
				.addActivityWithFacilityId("home", "A") //
				.addLeg() //
				.addActivityWithFacilityId("other", "B") //
				.addLeg() //
				.addActivityWithFacilityId("other", "C") //
				.addLeg() //
				.addActivityWithFacilityId("other", "A") //
				.addLeg() //
				.addActivityWithFacilityId("other", "C") //
				.addLeg() //
				.addActivityWithFacilityId("home", "A");

		List<DiscreteModeChoiceTrip> trips = planBuilder.buildDiscreteModeChoiceTrips();
		Person person = planBuilder.buildPlan().getPerson();

		TourConstraint constraint = constraintFactory.createConstraint(person, trips, availableModes);
		List<String> modes;

		// Test tour
		List<DiscreteModeChoiceTrip> tour1 = trips.subList(0, 3);
		List<DiscreteModeChoiceTrip> tour2 = trips.subList(3, 5);

		modes = Arrays.asList("car", "car", "car");
		assertTrue(constraint.validateBeforeEstimation(tour1, modes, Arrays.asList()));

		modes = Arrays.asList("car", "walk", "car");
		assertFalse(constraint.validateBeforeEstimation(tour1, modes, Arrays.asList()));

		modes = Arrays.asList("car", "car");
		assertTrue(constraint.validateBeforeEstimation(tour2, modes, Arrays.asList()));
	}
}
