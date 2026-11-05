/* *********************************************************************** *
 * project: org.matsim.*
 * SubtourModeConstraintTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                            *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.contrib.discrete_mode_choice.components.constraints;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.discrete_mode_choice.test_utils.PlanBuilder;
import org.matsim.contribs.discrete_mode_choice.components.constraints.SubtourModeConstraint;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraint;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraintFactory;

public class SubtourModeConstraintTest {
	@Test
	void validatesLaterTourWithItsOwnModes() {
		// PREPARATION
		Collection<String> constrainedModes = Arrays.asList("car", "walk");
		Collection<String> availableModes = Arrays.asList("car", "walk");

		TourConstraintFactory constraintFactory = new SubtourModeConstraint.Factory(constrainedModes);

		PlanBuilder planBuilder = new PlanBuilder() //
				.addActivityWithFacilityId("home", "A") //
				.addLeg() //
				.addActivityWithFacilityId("other", "B") //
				.addLeg() //
				.addActivityWithFacilityId("home", "A") //
				.addLeg() //
				.addActivityWithFacilityId("other", "C") //
				.addLeg() //
				.addActivityWithFacilityId("home", "A");

		List<DiscreteModeChoiceTrip> trips = planBuilder.buildDiscreteModeChoiceTrips();
		Person person = planBuilder.buildPlan().getPerson();

		TourConstraint constraint = constraintFactory.createConstraint(person, trips, availableModes);
		List<String> modes;

		// Test second tour
		List<DiscreteModeChoiceTrip> tour2 = trips.subList(2, 4);
		List<List<String>> previousModes = Arrays.asList(Arrays.asList("car", "car"));

		modes = Arrays.asList("walk", "walk");
		assertTrue(constraint.validateBeforeEstimation(tour2, modes, previousModes));

		modes = Arrays.asList("car", "walk");
		assertFalse(constraint.validateBeforeEstimation(tour2, modes, previousModes));
	}

	@Test
	void laterTourUsesFirstModeOfSubtour() {
		// PREPARATION
		Collection<String> constrainedModes = Arrays.asList("car", "walk");
		Collection<String> availableModes = Arrays.asList("car", "walk");

		TourConstraintFactory constraintFactory = new SubtourModeConstraint.Factory(constrainedModes);

		PlanBuilder planBuilder = new PlanBuilder() //
				.addActivityWithFacilityId("home", "A") //
				.addLeg() //
				.addActivityWithFacilityId("other", "B") //
				.addLeg() //
				.addActivityWithFacilityId("home", "A") //
				.addLeg() //
				.addActivityWithFacilityId("other", "C") //
				.addLeg() //
				.addActivityWithFacilityId("other", "D") //
				.addLeg() //
				.addActivityWithFacilityId("other", "C") //
				.addLeg() //
				.addActivityWithFacilityId("home", "A");

		List<DiscreteModeChoiceTrip> trips = planBuilder.buildDiscreteModeChoiceTrips();
		Person person = planBuilder.buildPlan().getPerson();

		TourConstraint constraint = constraintFactory.createConstraint(person, trips, availableModes);
		List<String> modes;

		// Test second tour
		List<DiscreteModeChoiceTrip> tour2 = trips.subList(2, 6);
		List<List<String>> previousModes = Arrays.asList(Arrays.asList("walk", "walk"));

		modes = Arrays.asList("walk", "car", "car", "car");
		assertFalse(constraint.validateBeforeEstimation(tour2, modes, previousModes));

		modes = Arrays.asList("car", "car", "car", "car");
		assertTrue(constraint.validateBeforeEstimation(tour2, modes, previousModes));
	}
}
