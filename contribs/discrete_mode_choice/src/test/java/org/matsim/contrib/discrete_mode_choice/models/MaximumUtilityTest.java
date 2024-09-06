package org.matsim.contrib.discrete_mode_choice.models;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel.FallbackBehaviour;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel.NoFeasibleChoiceException;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.CompositeTripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.filters.CompositeTripFilter;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.DefaultModeAvailability;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TripFilter;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripBasedModel;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.utilities.MaximumSelector;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilitySelectorFactory;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.PlansConfigGroup.TripDurationHandling;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

public class MaximumUtilityTest {
	@Test
	void testMaximumUtility() throws NoFeasibleChoiceException {
		TripFilter tripFilter = new CompositeTripFilter(Collections.emptySet());
		ModeAvailability modeAvailability = new DefaultModeAvailability(Arrays.asList("car", "pt", "walk"));
		TripConstraintFactory constraintFactory = new CompositeTripConstraintFactory();
		FallbackBehaviour fallbackBehaviour = FallbackBehaviour.EXCEPTION;
		ConstantTripEstimator estimator = new ConstantTripEstimator();
		UtilitySelectorFactory selectorFactory = new MaximumSelector.Factory();

		Activity originActivity = PopulationUtils.createActivityFromCoord("generic", new Coord(0.0, 0.0));
		originActivity.setEndTime(0.0);

		Activity destinationActivity = PopulationUtils.createActivityFromCoord("generic", new Coord(0.0, 0.0));
		originActivity.setEndTime(0.0);

		List<DiscreteModeChoiceTrip> trips = Collections
				.singletonList(new DiscreteModeChoiceTrip(originActivity, destinationActivity, null, null, 0, 0, 0, new AttributesImpl()));

		TripBasedModel model = new TripBasedModel(estimator, tripFilter, modeAvailability, constraintFactory,
				selectorFactory, fallbackBehaviour,
				TimeInterpretation.create(ActivityDurationInterpretation.tryEndTimeThenDuration,
						TripDurationHandling.shiftActivityEndTimes));

		List<TripCandidate> result;

		// Test 1
		estimator.setAlternative("car", -1.0);
		estimator.setAlternative("pt", -1.5);
		estimator.setAlternative("walk", -2.0);

		result = model.chooseModes(null, trips, new Random(0));
		assertEquals(1, result.size());
		assertEquals("car", result.get(0).getMode());
		assertEquals(-1.0, result.get(0).getUtility(), 1e-3);

		// Test 2
		estimator.setAlternative("car", -1.0);
		estimator.setAlternative("pt", 2.5);
		estimator.setAlternative("walk", -2.0);

		result = model.chooseModes(null, trips, new Random(0));
		assertEquals(1, result.size());
		assertEquals("pt", result.get(0).getMode());
		assertEquals(2.5, result.get(0).getUtility(), 1e-3);

		// Test 3
		estimator.setAlternative("car", -1.0);
		estimator.setAlternative("pt", -1.5);
		estimator.setAlternative("walk", -0.9);

		result = model.chooseModes(null, trips, new Random(0));
		assertEquals(1, result.size());
		assertEquals("walk", result.get(0).getMode());
		assertEquals(-0.9, result.get(0).getUtility(), 1e-3);
	}
}