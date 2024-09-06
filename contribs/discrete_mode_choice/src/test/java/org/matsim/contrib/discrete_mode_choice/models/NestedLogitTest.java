package org.matsim.contrib.discrete_mode_choice.models;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.matsim.contribs.discrete_mode_choice.model.nested.DefaultNest;
import org.matsim.contribs.discrete_mode_choice.model.nested.DefaultNestStructure;
import org.matsim.contribs.discrete_mode_choice.model.nested.NestedLogitSelector;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TripFilter;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripBasedModel;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilitySelectorFactory;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.PlansConfigGroup.TripDurationHandling;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

public class NestedLogitTest {
	@Test
	void testRedBusBlueBus() throws NoFeasibleChoiceException {
		TripFilter tripFilter = new CompositeTripFilter(Collections.emptySet());
		ModeAvailability modeAvailability = new DefaultModeAvailability(Arrays.asList("car", "redbus", "bluebus"));
		TripConstraintFactory constraintFactory = new CompositeTripConstraintFactory();
		FallbackBehaviour fallbackBehaviour = FallbackBehaviour.EXCEPTION;
		ConstantTripEstimator estimator = new ConstantTripEstimator();

		double minimumUtility = Double.NEGATIVE_INFINITY;
		double maximumUtility = Double.POSITIVE_INFINITY;

		Activity originActivity = PopulationUtils.createActivityFromCoord("generic", new Coord(0.0, 0.0));
		originActivity.setEndTime(0.0);

		Activity destinationActivity = PopulationUtils.createActivityFromCoord("generic", new Coord(0.0, 0.0));
		originActivity.setEndTime(0.0);

		DefaultNestStructure structure = new DefaultNestStructure();
		DefaultNest ptNest = new DefaultNest("pt", 1.0);
		structure.addNest(structure.getRoot(), ptNest);

		UtilitySelectorFactory selectorFactory = new NestedLogitSelector.Factory(structure, minimumUtility,
				maximumUtility);

		List<DiscreteModeChoiceTrip> trips = Collections.singletonList(new DiscreteModeChoiceTrip(originActivity,
				destinationActivity, null, Collections.emptyList(), 0, 0, 0, new AttributesImpl()));

		TripBasedModel model = new TripBasedModel(estimator, tripFilter, modeAvailability, constraintFactory,
				selectorFactory, fallbackBehaviour,
				TimeInterpretation.create(ActivityDurationInterpretation.tryEndTimeThenDuration,
						TripDurationHandling.shiftActivityEndTimes));
		Map<String, Integer> choices = new HashMap<>();
		Random random = new Random(0);

		int numberOfSamples = 10000;

		estimator.setAlternative("car", -1.0, structure.getRoot());
		estimator.setAlternative("redbus", -1.0, ptNest);
		estimator.setAlternative("bluebus", -1.0, ptNest);
		estimator.setAlternative("walk", -1.0, structure.getRoot());

		{
			ptNest.setScaleParameter(1.0); // NO nesting in principle
			choices.clear();

			for (int i = 0; i < numberOfSamples; i++) {
				List<TripCandidate> result = model.chooseModes(null, trips, random);
				String mode = result.get(0).getMode();
				choices.put(mode, choices.getOrDefault(mode, 0) + 1);
			}

			assertEquals(0.33, (double) choices.get("car") / numberOfSamples, 1e-2);
			assertEquals(0.33, (double) choices.get("redbus") / numberOfSamples, 1e-2);
			assertEquals(0.33, (double) choices.get("bluebus") / numberOfSamples, 1e-2);
		}

		{
			ptNest.setScaleParameter(20.0); // Strong nesting
			choices.clear();

			for (int i = 0; i < numberOfSamples; i++) {
				List<TripCandidate> result = model.chooseModes(null, trips, random);
				String mode = result.get(0).getMode();
				choices.put(mode, choices.getOrDefault(mode, 0) + 1);
			}

			assertEquals(0.5, (double) choices.get("car") / numberOfSamples, 1e-2);
			assertEquals(0.25, (double) choices.get("redbus") / numberOfSamples, 1e-2);
			assertEquals(0.25, (double) choices.get("bluebus") / numberOfSamples, 1e-2);
		}
	}
}