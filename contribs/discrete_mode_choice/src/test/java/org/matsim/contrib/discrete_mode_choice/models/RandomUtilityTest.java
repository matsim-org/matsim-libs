package org.matsim.contrib.discrete_mode_choice.models;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contribs.discrete_mode_choice.components.estimators.UniformTripEstimator;
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
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.utilities.RandomSelector;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilitySelectorFactory;
import org.matsim.contribs.discrete_mode_choice.replanning.time_interpreter.EndTimeThenDurationInterpreter;
import org.matsim.core.population.PopulationUtils;

public class RandomUtilityTest {
	@Test
	public void testRandomUtility() throws NoFeasibleChoiceException {
		TripFilter tripFilter = new CompositeTripFilter(Collections.emptySet());
		ModeAvailability modeAvailability = new DefaultModeAvailability(Arrays.asList("car", "pt", "walk"));
		TripConstraintFactory constraintFactory = new CompositeTripConstraintFactory();
		FallbackBehaviour fallbackBehaviour = FallbackBehaviour.EXCEPTION;
		TripEstimator estimator = new UniformTripEstimator(new EndTimeThenDurationInterpreter.Factory(0.0, true));
		UtilitySelectorFactory selectorFactory = new RandomSelector.Factory();

		Activity originActivity = PopulationUtils.createActivityFromCoord("generic", new Coord(0.0, 0.0));
		originActivity.setEndTime(0.0);

		Activity destinationActivity = PopulationUtils.createActivityFromCoord("generic", new Coord(0.0, 0.0));
		originActivity.setEndTime(0.0);

		List<DiscreteModeChoiceTrip> trips = Collections.singletonList(new DiscreteModeChoiceTrip(originActivity,
				destinationActivity, null, Collections.emptyList(), 0, 0, 0));

		TripBasedModel model = new TripBasedModel(estimator, tripFilter, modeAvailability, constraintFactory,
				selectorFactory, fallbackBehaviour, new EndTimeThenDurationInterpreter.Factory(0.0, true));

		Map<String, Integer> choices = new HashMap<>();
		Random random = new Random(0);

		int numberOfSamples = 1000000;

		for (int i = 0; i < numberOfSamples; i++) {
			List<TripCandidate> result = model.chooseModes(null, trips, random);
			String mode = result.get(0).getMode();
			choices.put(mode, choices.getOrDefault(mode, 0) + 1);
		}

		assertEquals(0.33, (double) choices.get("car") / numberOfSamples, 1e-2);
		assertEquals(0.33, (double) choices.get("pt") / numberOfSamples, 1e-2);
		assertEquals(0.33, (double) choices.get("walk") / numberOfSamples, 1e-2);
	}
}